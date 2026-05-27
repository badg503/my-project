<template>
  <div>
    <h2 class="page-title">预约审核</h2>
    <el-table :data="list" stripe v-loading="loading">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="userName" label="申请人" width="120" />
      <el-table-column prop="labName" label="实验室" width="150" />
      <el-table-column prop="reserveDate" label="预约日期" width="120" />
      <el-table-column label="时段" width="140">
        <template #default="{ row }">{{ row.timeSlotStart }} - {{ row.timeSlotEnd }}</template>
      </el-table-column>
      <el-table-column prop="purpose" label="用途" show-overflow-tooltip />
      <el-table-column prop="status" label="状态" width="120">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="申请时间" width="170" />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <template v-if="row.status === 'PENDING'">
            <el-button link type="success" @click="handleAudit(row.id, 'APPROVED')">通过</el-button>
            <el-button link type="danger" @click="showRejectDialog(row.id)">驳回</el-button>
          </template>
          <el-tag v-else type="info" size="small">已处理</el-tag>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination 
      v-model:current-page="page.current" 
      v-model:page-size="page.size" 
      :page-size="10" 
      :page-sizes="[10]" 
      :pager-count="5" 
      :total="page.total" 
      layout="total, prev, pager, next" 
      @current-change="load" 
      style="margin-top:16px" 
    />

    <el-dialog v-model="rejectDialogVisible" title="驳回理由" width="400px">
      <el-form :model="rejectForm" label-width="80px">
        <el-form-item label="驳回理由" required>
          <el-input v-model="rejectForm.remark" type="textarea" rows="3" placeholder="请输入驳回理由" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitReject">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getReservePage, auditReserve } from '@/api/reserve'
import { ElMessage } from 'element-plus'

const list = ref([])
const loading = ref(false)
const page = reactive({ current: 1, size: 10, total: 0 })
const rejectDialogVisible = ref(false)
const rejectForm = ref({ id: null, remark: '' })

function statusType(status) {
  const map = { PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger', CANCELLED: 'info', LAB_UNAVAILABLE: 'danger', DEVICE_UNAVAILABLE: 'warning' }
  return map[status] || 'info'
}

function statusText(status) {
  const map = { PENDING: '待审核', APPROVED: '已通过', REJECTED: '已驳回', CANCELLED: '已取消', LAB_UNAVAILABLE: '实验室不可用', DEVICE_UNAVAILABLE: '设备不可用' }
  return map[status] || status
}

async function load() {
  loading.value = true
  try {
    const res = await getReservePage({ current: page.current, size: page.size })
    list.value = res.records || []
    page.total = res.total || 0
  } catch (error) {
    ElMessage.error('加载失败：' + (error.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

async function handleAudit(id, status) {
  try {
    await auditReserve(id, status, status === 'APPROVED' ? '同意' : '')
    ElMessage.success('审核成功')
    load()
  } catch (error) {
    ElMessage.error('审核失败：' + (error.message || '未知错误'))
  }
}

function showRejectDialog(id) {
  rejectForm.value = { id, remark: '' }
  rejectDialogVisible.value = true
}

async function submitReject() {
  if (!rejectForm.value.remark) {
    ElMessage.warning('请输入驳回理由')
    return
  }
  
  try {
    await auditReserve(rejectForm.value.id, 'REJECTED', rejectForm.value.remark)
    ElMessage.success('已驳回')
    rejectDialogVisible.value = false
    load()
  } catch (error) {
    ElMessage.error('驳回失败：' + (error.message || '未知错误'))
  }
}

onMounted(load)
</script>

<style scoped>
.page-title { margin-bottom: 16px; font-size: 18px; }
</style>
