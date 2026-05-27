<template>
  <div>
    <h2 class="page-title">我的预约记录</h2>
    <el-button type="primary" @click="$router.push('/reserve')" style="margin-bottom:12px">新建预约</el-button>
    <el-table :data="list" stripe v-loading="loading">
      <el-table-column prop="labId" label="实验室ID" width="90" />
      <el-table-column prop="labName" label="实验室名称" min-width="120" />
      <el-table-column prop="reserveDate" label="预约日期" width="120" />
      <el-table-column label="时段" width="140">
        <template #default="{ row }">{{ row.timeSlotStart }} - {{ row.timeSlotEnd }}</template>
      </el-table-column>
      <el-table-column prop="purpose" label="用途" />
      <el-table-column label="借用设备" width="200" align="center">
        <template #default="{ row }">
          <span v-if="row.deviceIds">{{ getDeviceNames(row.deviceIds) }}</span>
          <span v-else>无</span>
        </template>
      </el-table-column>

      <el-table-column prop="status" label="状态" width="120">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="auditRemark" label="审核意见" min-width="200">
        <template #default="{ row }">
          <span v-if="row.status === 'REJECTED' || row.status === 'APPROVED'">
            {{ row.auditRemark || '无' }}
          </span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button v-if="row.status === 'PENDING'" link type="danger" @click="cancel(row.id)">取消</el-button>
          <el-button link type="danger" @click="deleteReserveRecord(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination 
      v-model:current-page="page.current" 
      v-model:page-size="page.size" 
      :page-size="10" 
      :page-sizes="[10]" 
      :total="page.total" 
      :pager-count="5" 
      layout="total, prev, pager, next" 
      @current-change="load" 
      style="margin-top:16px" 
    />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getMyReserves, cancelReserve, deleteReserve } from '@/api/reserve'
import { getDeviceList } from '@/api/lab'
import { ElMessage, ElMessageBox } from 'element-plus'

const list = ref([])
const loading = ref(false)
const page = reactive({ current:1, size: 10, total: 0 })
const devices = ref([])

function statusType(s) {
  const map = { PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger', CANCELLED: 'info', LAB_UNAVAILABLE: 'danger', DEVICE_UNAVAILABLE: 'warning' }
  return map[s] || 'info'
}
function statusText(s) {
  const map = { PENDING: '待审核', APPROVED: '已通过', REJECTED: '已驳回', CANCELLED: '已取消', LAB_UNAVAILABLE: '实验室不可用', DEVICE_UNAVAILABLE: '设备不可用' }
  return map[s] || s
}

function getDeviceNames(deviceIds) {
  if (!deviceIds) return ''
  const idArray = deviceIds.split(',').map(id => parseInt(id.trim()))
  const deviceNames = idArray.map(id => {
    const device = devices.value.find(d => d.id === id)
    return device ? device.name : ''
  }).filter(name => name)
  return deviceNames.join(', ')
}

async function load() {
  loading.value = true
  try {
    const res = await getMyReserves({ current: page.current, size: page.size })
    list.value = res.records || []
    page.total = res.total || 0
    
    // 加载设备列表用于显示设备名称
    if (res.records && res.records.length > 0) {
      const deviceRes = await getDeviceList()
      devices.value = deviceRes || []
    }
  } finally {
    loading.value = false
  }
}

async function cancel(id) {
  ElMessageBox.confirm('确定取消该预约？', '提示', { type: 'warning' }).then(async () => {
    await cancelReserve(id)
    ElMessage.success('已取消')
    load()
  }).catch(() => {})
}

function deleteReserveRecord(id) {
  ElMessageBox.confirm('确定删除该预约记录？删除后教师和管理员的审核记录也会同步消失。', '警告', { 
    type: 'danger',
    confirmButtonText: '确定',
    cancelButtonText: '取消'
  }).then(async () => {
    await deleteReserve(id)
    ElMessage.success('已删除')
    load()
  }).catch(() => {})
}

onMounted(() => {
  load()
})
</script>

<style scoped>
.page-title { margin-bottom: 16px; font-size: 18px; }
</style>
