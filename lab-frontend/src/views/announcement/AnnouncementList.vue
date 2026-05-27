<template>
  <div>
    <h2 class="page-title">公告管理</h2>
    <el-button type="primary" @click="openEdit(null)" style="margin-bottom:12px">发布公告</el-button>
    <el-table :data="list" stripe v-loading="loading">
      <el-table-column prop="title" label="标题" />
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '已发布' : '草稿' }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="createTime" label="发布时间" width="170" />
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="doDelete(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination v-model:current-page="page.current" v-model:page-size="page.size" :page-sizes="[10, 20, 30, 40, 50]" :total="page.total" layout="total, sizes, prev, pager, next, jumper" @size-change="handleSizeChange" @current-change="handleCurrentChange" style="margin-top:16px" />
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑公告' : '发布公告'" width="560px" @close="form = {}">
      <el-form :model="form" label-width="80px">
        <el-form-item label="标题"><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="内容"><el-input v-model="form.content" type="textarea" rows="6" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getAnnouncementPage, addAnnouncement, updateAnnouncement, deleteAnnouncement } from '@/api/announcement'
import { ElMessage, ElMessageBox } from 'element-plus'

const list = ref([])
const loading = ref(false)
const page = reactive({ current: 1, size: 10, total: 0 })
const dialogVisible = ref(false)
const form = ref({})

async function load() {
  loading.value = true
  try {
    const res = await getAnnouncementPage({ current: page.current, size: page.size })
    list.value = res.records || []
    page.total = res.total || 0
  } finally {
    loading.value = false
  }
}

function openEdit(row) {
  form.value = row ? { ...row } : { title: '', content: '' }
  dialogVisible.value = true
}

async function submit() {
  if (form.value.id) await updateAnnouncement(form.value)
  else await addAnnouncement(form.value)
  ElMessage.success('保存成功')
  dialogVisible.value = false
  load()
}

function doDelete(id) {
  ElMessageBox.confirm('确定删除该公告？', '提示', { type: 'warning' }).then(async () => {
    await deleteAnnouncement(id)
    ElMessage.success('已删除')
    load()
  }).catch(() => {})
}

// 分页处理
function handleSizeChange(size) {
  page.size = size
  load()
}

function handleCurrentChange(current) {
  page.current = current
  load()
}

onMounted(load)
</script>

<style scoped>
.page-title { margin-bottom: 16px; font-size: 18px; }
</style>
