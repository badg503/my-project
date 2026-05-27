<template>
  <div>
    <h2 class="page-title">实验记录</h2>
    <el-table :data="list" stripe v-loading="loading">
      <el-table-column label="任务标题" width="500">
        <template #default="{ row }">
          <div>
            <div>{{ row.title || row.taskTitle }}</div>
            <div v-if="row.teacherName" class="teacher-info">
              教师：{{ row.teacherName }}
              <span v-if="row.teacherDepartment" style="margin-left: 10px;">院系：{{ row.teacherDepartment }}</span>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="deadline" label="截止时间" width="170" />
      <el-table-column label="任务状态" width="100">
        <template #default="{ row }">
          <el-tag type="info">{{ getStatusText(row.taskStatus) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="报告状态" width="120">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.reportStatus)">{{ getStatusText(row.reportStatus) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.reportStatus === 'NOT_SUBMITTED'" link type="primary" @click="openSubmit(row)">提交</el-button>
          <el-button v-if="row.reportStatus === 'SUBMITTED' || row.reportStatus === 'COMPLETED'" link type="primary" @click="editReport(row)">编辑</el-button>
          <el-button v-if="row.reportStatus === 'SUBMITTED' || row.reportStatus === 'COMPLETED'" link type="danger" @click="cancelReport(row)">取消</el-button>
          <el-button v-if="(row.reportStatus === 'SUBMITTED' || row.reportStatus === 'COMPLETED') && isPastDeadline(row.deadline)" link type="warning" @click="resubmitReport(row)">补交</el-button>
          <el-button v-if="row.reportStatus === 'GRADED'" link type="info" @click="viewReportDetail(row)">查看</el-button>
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
    
    <el-dialog v-model="dialogVisible" :title="editingReportId ? (isResubmit ? '补交报告' : '编辑报告') : '提交报告'" width="500px" @close="resetForm">
      <el-form :model="form" label-width="100px" :rules="rules" ref="formRef">
        <el-form-item label="实验任务" prop="taskId">
          <el-select v-model="form.taskId" placeholder="请选择实验任务" style="width:100%" :disabled="!!editingReportId">
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
        <el-button type="primary" @click="submit" :loading="submitting">{{ editingReportId ? (isResubmit ? '补交' : '保存') : '提交' }}</el-button>
      </template>
    </el-dialog>
    
    <!-- 查看详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="报告详情" width="600px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="实验任务">{{ currentReport?.title || currentReport?.taskTitle }}</el-descriptions-item>
        <el-descriptions-item label="报告内容" :span="2">
          <div style="white-space: pre-wrap; line-height: 1.5;">{{ currentReport?.content }}</div>
        </el-descriptions-item>
        <el-descriptions-item v-if="currentReport?.attachmentUrl" label="附件链接">
          <a :href="currentReport.attachmentUrl" target="_blank">{{ currentReport.attachmentUrl }}</a>
        </el-descriptions-item>
        <el-descriptions-item label="成绩">{{ currentReport?.score || '-' }}</el-descriptions-item>
        <el-descriptions-item label="评语" :span="2">
          <div style="white-space: pre-wrap; line-height: 1.5;">{{ currentReport?.remark || '-' }}</div>
        </el-descriptions-item>
        <el-descriptions-item label="状态">{{ getStatusText(currentReport?.reportStatus) }}</el-descriptions-item>
        <el-descriptions-item label="提交时间">{{ currentReport?.createTime }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
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
const isResubmit = ref(false)

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

function getStatusType(status) {
  if (typeof status === 'number') {
    return 'info'
  }
  const map = {
    'NOT_SUBMITTED': 'warning',
    'SUBMITTED': 'success',
    'GRADED': 'success',
    'RESUBMITTED': 'info',
    'COMPLETED': 'success'
  }
  return map[status] || 'info'
}

function getStatusText(status) {
  if (typeof status === 'number') {
    return status === 1 ? '进行中' : '已关闭'
  }
  const map = {
    'NOT_SUBMITTED': '未提交',
    'SUBMITTED': '已提交',
    'GRADED': '已评分',
    'RESUBMITTED': '补交',
    'COMPLETED': '已完成'
  }
  return map[status] || status
}

// 判断是否已过截止时间
function isPastDeadline(deadline) {
  if (!deadline) return false
  const deadlineDate = new Date(deadline)
  const now = new Date()
  return now > deadlineDate
}

// 加载实验记录列表
async function load() {
  loading.value = true
  try {
    // 加载任务列表
    const tasksRes = await getTaskList({ current: 1, size: 100 })
    const tasks = tasksRes.records || []
    
    // 加载报告列表
    const reportsRes = await getMyReports({ current: 1, size: 100 })
    const reports = reportsRes.records || []
    console.log('报告列表:', reports)
    
    // 创建报告映射
    const reportMap = new Map()
    reports.forEach(report => {
      reportMap.set(report.taskId, report)
    })
    
    // 合并任务和报告数据
    const combinedList = tasks.map(task => {
      const report = reportMap.get(task.id)
      if (report) {
        return {
          ...task,
          ...report,
          taskId: task.id,
          reportId: report.id,
          reportStatus: report.status || 'SUBMITTED',
          taskStatus: task.status
        }
      } else {
        return {
          ...task,
          taskId: task.id,
          reportStatus: 'NOT_SUBMITTED',
          taskStatus: task.status
        }
      }
    })
    
    // 分页处理
    const start = (page.current - 1) * page.size
    const end = start + page.size
    list.value = combinedList.slice(start, end)
    page.total = combinedList.length
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
  isResubmit.value = false
  form.taskId = null
  form.content = ''
  form.attachmentUrl = ''
  if (formRef.value) {
    formRef.value.resetFields()
  }
}

// 打开提交对话框
function openSubmit(row) {
  if (isDeadlinePassed(row.deadline)) {
    ElMessage.warning('已经超过截止时间，无法提交报告')
    return
  }
  resetForm()
  form.taskId = row.id
  loadTasks()
  dialogVisible.value = true
}

// 检查是否超过截止时间
function isDeadlinePassed(deadline) {
  if (!deadline) return false
  const now = new Date()
  const deadlineDate = new Date(deadline)
  return now > deadlineDate
}

// 编辑报告
function editReport(row) {
  if (isDeadlinePassed(row.deadline)) {
    ElMessage.warning('已经超过截止时间，无法修改报告')
    return
  }
  editingReportId.value = row.reportId
  isResubmit.value = false
  form.taskId = row.taskId
  form.content = row.content
  form.attachmentUrl = row.attachmentUrl
  loadTasks()
  dialogVisible.value = true
}

// 补交报告
function resubmitReport(row) {
  if (isDeadlinePassed(row.deadline)) {
    ElMessage.warning('已经超过截止时间，无法补交报告')
    return
  }
  editingReportId.value = row.reportId
  isResubmit.value = true
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
      await deleteReport(row.reportId)
      ElMessage.success('取消成功')
      load()
    } catch (e) {
      console.error('取消失败', e)
    }
  }).catch(() => {})
}

// 查看详情
function viewReportDetail(row) {
  console.log('查看详情:', row)
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
        ElMessage.success(isResubmit.value ? '补交成功' : '编辑成功')
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
.teacher-info {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
  line-height: 1.4;
}
</style>