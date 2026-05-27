<template>
  <div>
    <h2 class="page-title">实验室开放信息</h2>
    
    <el-form :inline="true" :model="searchForm" class="mb-4">
      <el-form-item label="实验室名称">
        <el-input v-model="searchForm.name" placeholder="请输入实验室名称" style="width: 200px;" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="load">查询</el-button>
      </el-form-item>
    </el-form>
    
    <el-table :data="list" stripe v-loading="loading">
      <el-table-column prop="name" label="实验室名称" />
      <el-table-column prop="location" label="位置" />
      <el-table-column label="开放时段" width="160">
        <template #default="{ row }">{{ row.openTimeStart }} - {{ row.openTimeEnd }}</template>
      </el-table-column>
      <el-table-column prop="capacity" label="可容纳人数" width="100" />
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '开放' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100">
        <template #default="{ row }">
          <el-button link type="primary" @click="showDevices(row)">查看设备</el-button>
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

    <el-dialog v-model="deviceDialogVisible" :title="currentLab?.name + ' - 设备清单'" width="700px">
      <el-table :data="deviceList" stripe v-loading="deviceLoading">
        <el-table-column prop="id" label="设备ID" width="80" />
        <el-table-column prop="name" label="设备名称" />
        <el-table-column prop="model" label="型号" width="120" />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="deviceDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getLabPage, getDeviceList } from '@/api/lab'

const list = ref([])
const loading = ref(false)
const deviceDialogVisible = ref(false)
const deviceList = ref([])
const deviceLoading = ref(false)
const currentLab = ref(null)
const searchForm = ref({
  name: ''
})
const page = ref({
  current: 1,
  size: 10,
  total: 0
})

async function load() {
  loading.value = true
  try {
    const res = await getLabPage({
      current: page.value.current,
      size: page.value.size,
      name: searchForm.value.name || undefined,
      status: 1
    })
    list.value = res.records || []
    page.value.total = res.total || 0
  } finally {
    loading.value = false
  }
}

async function showDevices(lab) {
  currentLab.value = lab
  deviceDialogVisible.value = true
  deviceLoading.value = true
  try {
    deviceList.value = await getDeviceList(lab.id)
  } finally {
    deviceLoading.value = false
  }
}

function handleSizeChange(size) {
  page.value.size = size
  load()
}

function handleCurrentChange(current) {
  page.value.current = current
  load()
}

function getStatusType(status) {
  const map = {
    'AVAILABLE': 'success',
    'REPAIR': 'warning',
    'SCRAPPED': 'info'
  }
  return map[status] || 'info'
}

function getStatusText(status) {
  const map = {
    'AVAILABLE': '可用',
    'REPAIR': '维修中',
    'SCRAPPED': '报废'
  }
  return map[status] || status
}

onMounted(load)
</script>

<style scoped>
.page-title { margin-bottom: 16px; font-size: 18px; }
.mb-4 { margin-bottom: 20px; }
.mt-4 { margin-top: 20px; }
</style>
