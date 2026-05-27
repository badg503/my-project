<template>
  <div class="reserve-all">
    <el-card>
      <template #header>
        <div class="flex justify-between items-center">
          <span>全局预约管理</span>
          <el-button type="primary" @click="loadReserves">刷新</el-button>
        </div>
      </template>
      
      <!-- AI 智能预约建议栏 -->
      <div v-if="aiScheduleSuggestions.length > 0" class="mb-4">
        <el-card shadow="hover" class="ai-schedule-card">
          <template #header>
            <div class="card-header">
              <span>🤖 AI 智能预约建议</span>
              <el-button type="primary" link @click="loadAISchedule" :loading="aiLoading">
                <el-icon><i-ep-refresh /></el-icon>
                刷新建议
              </el-button>
            </div>
          </template>
          <div class="ai-schedule-content">
            <el-alert
              v-for="(suggestion, index) in aiScheduleSuggestions"
              :key="index"
              :title="suggestion"
              type="success"
              :closable="false"
              show-icon
              class="mb-2"
            />
          </div>
        </el-card>
      </div>
      
      <el-form :inline="true" :model="searchForm" class="mb-4">
        <el-form-item label="实验室" style="width: 280px;">
          <el-select v-model="searchForm.labId" placeholder="选择实验室" style="width: 200px;">
            <el-option
              v-for="lab in labs"
              :key="lab.id"
              :label="lab.name"
              :value="lab.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" style="width: 200px;">
          <el-select v-model="searchForm.status" placeholder="选择状态" style="width: 140px;">
            <el-option label="全部" value="" />
            <el-option label="待审核" value="PENDING" />
            <el-option label="已通过" value="APPROVED" />
            <el-option label="已拒绝" value="REJECTED" />
            <el-option label="已取消" value="CANCELLED" />
            <el-option label="实验室不可用" value="LAB_UNAVAILABLE" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadReserves">查询</el-button>
        </el-form-item>
      </el-form>
      
      <el-table v-loading="loading" :data="reserves" style="width: 100%">
        <el-table-column prop="id" label="预约ID" width="80" />
        <el-table-column prop="labName" label="实验室" width="180" />
        <el-table-column prop="reserveDate" label="预约日期" width="120" />
        <el-table-column label="时间段" width="180">
          <template #default="scope">
            {{ scope.row.timeSlotStart }} - {{ scope.row.timeSlotEnd }}
          </template>
        </el-table-column>
        <el-table-column prop="userName" label="申请人" width="100" />
        <el-table-column prop="className" label="班级" width="120" />
        <el-table-column prop="purpose" label="预约目的" />
        <el-table-column prop="status" label="状态" width="120">
          <template #default="scope">
            <el-tag
              :type="getStatusType(scope.row.status)"
            >
              {{ getStatusText(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="scope">
            <el-button
              v-if="scope.row.status === 'PENDING'"
              type="primary"
              size="small"
              @click="handleAudit(scope.row, 'APPROVED')"
            >
              审核通过
            </el-button>
            <el-button
              v-if="scope.row.status === 'PENDING'"
              type="danger"
              size="small"
              @click="handleAudit(scope.row, 'REJECTED')"
            >
              驳回
            </el-button>
            <el-button
              v-if="scope.row.status === 'APPROVED'"
              type="warning"
              size="small"
              @click="handleCancel(scope.row)"
            >
              取消预约
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <el-pagination
        v-model:current-page="page.current"
        v-model:page-size="page.size"
        :page-sizes="[10, 20, 30, 40, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        :total="page.total"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
        class="mt-4"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getReservePage, auditReserve, cancelReserve } from '@/api/reserve'
import { getLabList } from '@/api/lab'
import { getAISchedule } from '@/api/ai'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const reserves = ref([])
const labs = ref([])
const page = reactive({
  current: 1,
  size: 10,
  total: 0
})
const searchForm = ref({
  labId: '',
  status: ''
})

// AI 预约建议相关
const aiLoading = ref(false)
const aiScheduleSuggestions = ref([])

function getStatusType(s) {
  const map = { PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger', CANCELLED: 'info', LAB_UNAVAILABLE: 'danger', DEVICE_UNAVAILABLE: 'warning' }
  return map[s] || 'info'
}

function getStatusText(s) {
  const map = { PENDING: '待审核', APPROVED: '已通过', REJECTED: '已拒绝', CANCELLED: '已取消', LAB_UNAVAILABLE: '实验室不可用', DEVICE_UNAVAILABLE: '设备不可用' }
  return map[s] || s
}

const loadLabs = async () => {
  try {
    const res = await getLabList()
    labs.value = res
  } catch (error) {
    ElMessage.error('获取实验室列表失败')
  }
}

const loadReserves = async () => {
  loading.value = true
  try {
    const res = await getReservePage({
      current: page.current,
      size: page.size,
      labId: searchForm.value.labId || undefined,
      status: searchForm.value.status || undefined
    })
    reserves.value = res.records || []
    page.total = res.total || 0
  } catch (error) {
    ElMessage.error('获取预约列表失败')
  } finally {
    loading.value = false
  }
}

const handleAudit = async (reserve, status) => {
  try {
    await auditReserve(reserve.id, status, status === 'REJECTED' ? '审核驳回' : '审核通过')
    ElMessage.success(`预约${status === 'APPROVED' ? '审核通过' : '审核驳回'}成功`)
    loadReserves()
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

const handleCancel = async (reserve) => {
  try {
    await ElMessageBox.confirm('确定要取消该预约吗？', '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await cancelReserve(reserve.id)
    ElMessage.success('预约取消成功')
    loadReserves()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('操作失败')
    }
  }
}

const handleSizeChange = (size) => {
  page.size = size
  loadReserves()
}

const handleCurrentChange = (current) => {
  page.current = current
  loadReserves()
}

// 加载 AI 预约建议
const loadAISchedule = async () => {
  aiLoading.value = true
  try {
    const res = await getAISchedule()
    console.log('AI 预约建议返回数据:', res)
    if (res && res.schedulePlan && Array.isArray(res.schedulePlan)) {
      aiScheduleSuggestions.value = res.schedulePlan.map(plan => {
        // 如果是字符串，直接返回
        if (typeof plan === 'string') return plan
        
        // 如果是对象，尝试格式化显示
        if (plan.labId && plan.assignedSlots) {
          const labName = plan.labName || `实验室${plan.labId}`
          const utilization = plan.utilization ? `${plan.utilization.toFixed(2)}%` : '0%'
          const capacity = plan.capacity || 0
          const demand = plan.demand || 0
          const assignedSlots = plan.assignedSlots || 0
          
          return `${labName}：已分配${assignedSlots}个时段，容量${capacity}，需求${demand}，使用率${utilization}`
        }
        
        // 其他情况返回 JSON 字符串
        return JSON.stringify(plan)
      })
      console.log('格式化后的建议:', aiScheduleSuggestions.value)
    } else if (res && res.message) {
      aiScheduleSuggestions.value = [res.message]
    } else if (res && res.suggestions && Array.isArray(res.suggestions)) {
      aiScheduleSuggestions.value = res.suggestions
    } else if (res && typeof res === 'object') {
      // 如果返回的是对象但没有 schedulePlan 字段，尝试解析
      const suggestions = []
      for (const [key, value] of Object.entries(res)) {
        if (Array.isArray(value)) {
          value.forEach(item => {
            if (typeof item === 'object') {
              suggestions.push(formatScheduleItem(item))
            }
          })
        }
      }
      if (suggestions.length > 0) {
        aiScheduleSuggestions.value = suggestions
      }
    }
  } catch (error) {
    console.error('AI 预约建议加载失败:', error)
  } finally {
    aiLoading.value = false
  }
}

// 格式化调度建议项
const formatScheduleItem = (item) => {
  if (item.labId && item.assignedSlots) {
    const labName = item.labName || `实验室${item.labId}`
    const utilization = item.utilization ? `${item.utilization.toFixed(2)}%` : '0%'
    const capacity = item.capacity || 0
    const demand = item.demand || 0
    const assignedSlots = item.assignedSlots || 0
    
    return `${labName}：已分配${assignedSlots}个时段，容量${capacity}，需求${demand}，使用率${utilization}`
  }
  return JSON.stringify(item)
}

onMounted(() => {
  loadLabs()
  loadReserves()
  loadAISchedule()
})
</script>

<style scoped>
.reserve-all {
  padding: 20px;
}
.ai-schedule-card {
  background: linear-gradient(135deg, #f0f9ff 0%, #ffffff 100%);
  border-left: 4px solid #409eff;
}
.ai-schedule-content {
  max-height: 300px;
  overflow-y: auto;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.mb-4 {
  margin-bottom: 16px;
}
.mb-2 {
  margin-bottom: 8px;
}
</style>
