<template>
  <div>
    <h2 class="page-title">设备报修</h2>
    <el-button type="primary" @click="openAdd" style="margin-bottom:12px">提交报修</el-button>
    <el-table :data="list" stripe v-loading="loading">
      <el-table-column prop="deviceId" label="设备ID" width="90" />
      <el-table-column prop="deviceName" label="设备名称" width="120" />
      <el-table-column prop="labName" label="实验室" width="150" />
      <el-table-column prop="faultDesc" label="故障描述" />
      <el-table-column prop="aiSuggestion" label="AI建议" show-overflow-tooltip />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="提交时间" width="170" />
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button v-if="row.status === 'PENDING'" link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button v-if="row.status === 'PENDING'" link type="danger" @click="cancelRepair(row.id)">取消</el-button>
          <el-button link type="danger" @click="deleteRepair(row.id)">删除</el-button>
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
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑报修' : '提交报修'" width="500px" @close="form = {}">
      <el-form :model="form" label-width="100px">
        <el-form-item label="设备选择" v-if="!form.id">
          <el-select 
            v-model="form.deviceId" 
            placeholder="请输入设备ID或名称搜索" 
            style="width:100%" 
            filterable
            :filter-method="filterDevice"
            default-first-option
          >
            <el-option 
              v-for="device in filteredDevices" 
              :key="device.id" 
              :label="`${device.id} - ${device.name} (${device.labName || '未知实验室'})${device.isRepaired ? ' [已报修]' : ''}`" 
              :value="device.id"
              :disabled="device.isRepaired"
            >
              <span style="float: left">{{ device.id }} - {{ device.name }} ({{ device.labName || '未知实验室' }})</span>
              <span v-if="device.isRepaired" style="float: right; color: #f56c6c; font-size: 13px;">已报修</span>
            </el-option>
          </el-select>
          <div v-if="form.deviceId && filteredDevices.find(d => d.id === form.deviceId)?.isRepaired" style="color: #f56c6c; font-size: 12px; margin-top: 5px;">
            该设备已有未完成的报修记录，请勿重复报修
          </div>
        </el-form-item>
        <el-form-item label="设备ID" v-else><el-input-number v-model="form.deviceId" :min="1" style="width:100%" /></el-form-item>
        <el-form-item label="故障描述"><el-input v-model="form.faultDesc" type="textarea" rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { getMyRepairs, addRepair, updateRepair, cancelRepair as cancelRepairApi, getPendingRepairDevices } from '@/api/repair'
import { getDevicePage } from '@/api/lab'
import { ElMessage, ElMessageBox } from 'element-plus'

const list = ref([])
const loading = ref(false)
const page = reactive({ current: 1, size: 10, total: 0 })
const dialogVisible = ref(false)
const form = ref({})
const devices = ref([])
const deviceLoading = ref(false)
const searchQuery = ref('')
const pendingDeviceIds = ref([])

const filteredDevices = computed(() => {
  let filtered = devices.value
  if (searchQuery.value) {
    const query = searchQuery.value.toString().toLowerCase()
    filtered = filtered.filter(device => 
      device.id.toString().includes(query) || 
      (device.name && device.name.toLowerCase().includes(query))
    )
  }
  // 标记已报修的设备
  return filtered.map(device => ({
    ...device,
    isRepaired: pendingDeviceIds.value.includes(device.id)
  }))
})

function filterDevice(val) {
  searchQuery.value = val
}

function statusType(s) {
  const map = { PENDING: 'warning', PROCESSING: 'primary', FIXED: 'success', CLOSED: 'info' }
  return map[s] || 'info'
}
function statusText(s) {
  const map = { PENDING: '待处理', PROCESSING: '维修中', FIXED: '已修复', CLOSED: '已关闭' }
  return map[s] || s
}

async function load() {
  loading.value = true
  try {
    const res = await getMyRepairs({ current: page.current, size: page.size })
    list.value = res.records || []
    page.total = res.total || 0
  } finally {
    loading.value = false
  }
}

async function loadDevices() {
  deviceLoading.value = true
  try {
    const [deviceRes, pendingRes] = await Promise.all([
      getDevicePage({ current: 1, size: 100 }),
      getPendingRepairDevices()
    ])
    // 过滤掉报废的设备，只有可用和维修中的设备可以报修
    devices.value = (deviceRes.records || []).filter(d => d.status === 'AVAILABLE' || d.status === 'REPAIR')
    pendingDeviceIds.value = pendingRes || []
  } finally {
    deviceLoading.value = false
  }
}

function openAdd() {
  loadDevices()
  form.value = { deviceId: null, faultDesc: '' }
  dialogVisible.value = true
}

function openEdit(row) {
  form.value = { ...row }
  dialogVisible.value = true
}

async function submit() {
  try {
    if (form.value.id) {
      await updateRepair(form.value)
      ElMessage.success('更新成功')
    } else {
      await addRepair(form.value)
      ElMessage.success('提交成功')
    }
    dialogVisible.value = false
    load()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '操作失败')
  }
}

async function cancelRepair(id) {
  try {
    await ElMessageBox.confirm('确定要取消该报修吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await cancelRepairApi(id)
    ElMessage.success('取消成功')
    load()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.message || '取消失败')
    }
  }
}

async function deleteRepair(id) {
  try {
    await ElMessageBox.confirm('确定要删除该报修记录吗？删除后将无法恢复。', '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'danger'
    })
    await cancelRepairApi(id)
    ElMessage.success('删除成功')
    load()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.message || '删除失败')
    }
  }
}

onMounted(load)
</script>

<style scoped>
.page-title { margin-bottom: 16px; font-size: 18px; }
</style>
