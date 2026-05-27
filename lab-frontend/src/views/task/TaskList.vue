<template>
  <div>
    <h2 class="page-title">实验任务</h2>
    <el-table :data="list" stripe v-loading="loading">
      <el-table-column label="任务标题" width="400">
        <template #default="{ row }">
          <div>
            <div>{{ row.title }}</div>
            <div v-if="row.teacherId && row.teacherName" class="teacher-info">
              教师：{{ row.teacherName }} (ID: {{ row.teacherId }})
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="deadline" label="截止时间" width="170" />
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '进行中' : '已关闭' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button link type="primary" @click="$router.push({ path: '/report', query: { taskId: row.id } })">提交报告</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination v-model:current-page="page.current" v-model:page-size="page.size" :total="page.total" layout="total, prev, pager, next" @current-change="load" style="margin-top:16px" />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getTaskList } from '@/api/task'

const list = ref([])
const loading = ref(false)
const page = reactive({ current: 1, size: 10, total: 0 })

async function load() {
  loading.value = true
  try {
    const res = await getTaskList({ current: page.current, size: page.size })
    list.value = res.records || []
    page.total = res.total || 0
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.page-title { margin-bottom: 16px; font-size: 18px; }
.teacher-info {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
  line-height: 1.4;
}
</style>
