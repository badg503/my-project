<template>
  <div>
    <h2 class="page-title">实验报告</h2>
    <el-button type="primary" @click="openSubmit" style="margin-bottom:12px">提交报告</el-button>
    <el-table :data="list" stripe v-loading="loading">
      <el-table-column prop="taskTitle" label="实验任务" min-width="150" show-overflow-tooltip />
      <el-table-column prop="content" label="内容摘要" show-overflow-tooltip />
      <el-table-column prop="score" label="成绩" width="80" />
      <el-table-column prop="status" label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 'GRADED' ? 'success' : 'warning'">{{ row.status === 'GRADED' ? '已评分' : '已提交' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="提交时间" width="170" />
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.status === 'SUBMITTED'" link type="primary" @click="editReport(row)">编辑</el-button>
          <el-button v-if="row.status === 'SUBMITTED'" link type="danger" @click="cancelReport(row)">取消</el-button>
          <el-button link type="info" @click="viewReportDetail(row)">查看详情</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination v-model:current-page="page.current" v-model:page-size="page.size" :total="page.total" layout="total, prev, pager, next" @current-change="load" style="margin-top:16px" />
    
    <el-dialog v-model="dialogVisible" :title="editingReportId ? '编辑报告' : '提交报告'" width="500px" @close="resetForm">
      <el-form :model="form" label-width="100px" :rules="rules" ref="formRef">
        <el-form-item label="实验任务" prop="taskId">
          <el-select v-model="form.taskId" placeholder="请选择实验任务" style="width:100%">
            <el-option 
              v-for="task in taskList" 
              :key="task.id" 
              :label="task.title" 
              :value="task.id" 
            />
          </el-select>
        </el-form-item>
        <el-form-item label="报告内容" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="6" placeholder="请输入实验报告内容" />
        </el-form-item>
        <el-form-item label="附件链接">
          <el-input v-model="form.attachmentUrl" placeholder="可选：上传附件的链接" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit" :loading="submitting">{{ editingReportId ? '保存' : '提交' }}</el-button>
      </template>
    </el-dialog>
    
    <!-- 查看详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="报告详情" width="600px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="实验任务">{{ currentReport?.taskTitle }}</el-descriptions-item>
        <el-descriptions-item label="报告内容" :span="2">
          <div style="white-space: pre-wrap; line-height: 1.5;">{{ currentReport?.content }}</div>
        </el-descriptions-item>
        <el-descriptions-item v-if="currentReport?.attachmentUrl" label="附件链接">
          <a :href="currentReport.attachmentUrl" target="_blank">{{ currentReport.attachmentUrl }}</a>
        </el-descriptions-item>
        <el-descriptions-item label="成绩">{{ currentReport?.score || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ currentReport?.status === 'GRADED' ? '已评分' : '已提交' }}</el-descriptions-item>
        <el-descriptions-item label="提交时间">{{ currentReport?.createTime }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getMyReports, submitReport, updateReport, deleteReport } from '@/api/report'
import { getTaskList } from '@/api/task'
import { ElMessage, ElMessageBox } from 'element-plus'

const list = ref([])
const loading = ref(false)
const submitting = ref(false)
const taskList = ref([])
const dialogVisible = ref(false)
const formRef = ref(null)

// 编辑相关
const editingReportId = ref(null)

// 查看详情相关
const detailDialogVisible = ref(false)
const currentReport = ref(null)

const page = reactive({ current: 1, size: 10, total: 0 })

const form = reactive({
  taskId: null,
  content: '',
  attachmentUrl: ''
})

const rules = {
  taskId: [{ required: true, message: '请选择实验任务', trigger: 'change' }],
  content: [{ required: true, message: '请输入报告内容', trigger: 'blur' }]
}

// 加载报告列表
async function load() {
  loading.value = true
  try {
    const res = await getMyReports({ current: page.current, size: page.size })
    list.value = res.records || []
    page.total = res.total || 0
  } finally {
    loading.value = false
  }
}

// 加载任务列表
async function loadTasks() {
  try {
    const res = await getTaskList({ current: 1, size: 100, status: 1 })
    taskList.value = res.records || []
  } catch (e) {
    console.error('加载任务列表失败', e)
  }
}

// 重置表单
function resetForm() {
  editingReportId.value = null
  form.taskId = null
  form.content = ''
  form.attachmentUrl = ''
  if (formRef.value) {
    formRef.value.resetFields()
  }
}

// 打开提交对话框
function openSubmit() {
  resetForm()
  loadTasks()
  dialogVisible.value = true
}

// 编辑报告
function editReport(row) {
  editingReportId.value = row.id
  form.taskId = row.taskId
  form.content = row.content
  form.attachmentUrl = row.attachmentUrl
  loadTasks()
  dialogVisible.value = true
}

// 取消报告
function cancelReport(row) {
  ElMessageBox.confirm('确定要取消该报告吗？', '提示', { type: 'warning' }).then(async () => {
    try {
      await deleteReport(row.id)
      ElMessage.success('取消成功')
      load()
    } catch (e) {
      console.error('取消失败', e)
    }
  }).catch(() => {})
}

// 查看详情
function viewReportDetail(row) {
  currentReport.value = row
  detailDialogVisible.value = true
}

// 提交报告
async function submit() {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    
    submitting.value = true
    try {
      if (editingReportId.value) {
        await updateReport(editingReportId.value, form)
        ElMessage.success('编辑成功')
      } else {
        await submitReport(form)
        ElMessage.success('提交成功')
      }
      dialogVisible.value = false
      load()
    } catch (e) {
      console.error('提交失败', e)
    } finally {
      submitting.value = false
    }
  })
}

onMounted(load)
</script>

<style scoped>
.page-title { margin-bottom: 16px; font-size: 18px; }
</style>
