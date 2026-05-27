<template>
  <div class="task-manage">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>实验管理</span>
          <el-button type="primary" @click="openAdd">发布实验任务</el-button>
        </div>
      </template>
      
      <!-- 搜索区域 -->
      <el-form inline style="margin-bottom: 16px">
        <el-input v-model="query.keyword" placeholder="任务标题" clearable style="width: 200px" />
        <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px; margin-left: 8px">
          <el-option label="进行中" :value="1" />
          <el-option label="已关闭" :value="0" />
        </el-select>
        <el-button type="primary" @click="load" style="margin-left: 8px">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </el-form>
      
      <!-- 任务列表 -->
      <el-table :data="list" stripe v-loading="loading">
        <el-table-column prop="title" label="任务标题" min-width="150" show-overflow-tooltip />
        <el-table-column prop="labName" label="所属实验室" width="120" />
        <el-table-column prop="deadline" label="截止时间" width="170" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '进行中' : '已关闭' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="发布时间" width="170" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="success" @click="viewReports(row)">查看报告</el-button>
            <el-button link type="danger" @click="doDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <!-- 分页 -->
      <el-pagination 
        v-model:current-page="page.current" 
        v-model:page-size="page.size" 
        :page-sizes="[10, 20, 30, 40, 50]"
        :total="page.total" 
        layout="total, sizes, prev, pager, next, jumper" 
        @size-change="load"
        @current-change="load" 
        style="margin-top: 16px" 
      />
    </el-card>
    
    <!-- 发布/编辑任务对话框 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑实验任务' : '发布实验任务'" width="600px" @close="resetForm">
      <el-form :model="form" label-width="100px" :rules="rules" ref="formRef">
        <el-form-item label="任务标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入任务标题" />
        </el-form-item>
        <el-form-item label="所属实验室" prop="labId">
          <el-select v-model="form.labId" placeholder="请选择实验室" style="width: 100%">
            <el-option 
              v-for="lab in labList" 
              :key="lab.id" 
              :label="lab.name" 
              :value="lab.id" 
            />
          </el-select>
        </el-form-item>
        <el-form-item label="截止时间" prop="deadline">
          <el-date-picker
            v-model="form.deadline"
            type="datetime"
            placeholder="选择截止时间"
            style="width: 100%"
            value-format="YYYY-MM-DD HH:mm:ss"
          />
        </el-form-item>
        <el-form-item label="任务内容" prop="content">
          <el-input 
            v-model="form.content" 
            type="textarea" 
            :rows="6" 
            placeholder="请输入实验任务内容、要求等"
            spellcheck="false"
          />
        </el-form-item>
        <el-form-item label="附件链接" prop="attachmentUrl">
          <el-input v-model="form.attachmentUrl" placeholder="可选：附件URL链接" />
        </el-form-item>
        <el-form-item label="分配学员" prop="studentIds">
          <el-select v-model="form.studentIds" multiple placeholder="选择学员（留空则所有学员可见）" style="width: 100%">
            <el-option 
              v-for="s in myStudents" 
              :key="s.id" 
              :label="s.realName || s.username" 
              :value="s.id" 
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status" v-if="form.id">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">进行中</el-radio>
            <el-radio :label="0">已关闭</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>
    
    <!-- 查看报告对话框 -->
    <el-dialog v-model="reportDialogVisible" :title="'查看报告 - ' + currentTaskTitle" width="800px">
      <div style="margin-bottom: 16px; display: flex; justify-content: space-between; align-items: center;">
        <span>提交情况统计：</span>
        <el-tag type="success">{{ submittedCount }}/{{ totalCount }} 已提交</el-tag>
      </div>
      <el-table :data="reportList" stripe v-loading="reportLoading">
        <el-table-column prop="userName" label="学生姓名" width="120" />
        <el-table-column prop="content" label="报告内容" show-overflow-tooltip />
        <el-table-column prop="score" label="成绩" width="80" />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'GRADED' ? 'success' : (row.status === 'SUBMITTED' || row.status === 'COMPLETED' ? 'warning' : 'info')">
              {{ row.status === 'GRADED' ? '已评分' : (row.status === 'SUBMITTED' || row.status === 'COMPLETED' ? '已提交' : '未提交') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="提交时间" width="170" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'SUBMITTED' || row.status === 'COMPLETED'" link type="primary" @click="openGradeDialog(row)">评分</el-button>
            <el-button v-if="row.status === 'GRADED'" link type="warning" @click="openEditGradeDialog(row)">修改成绩</el-button>
            <el-button v-if="row.content" link type="info" @click="viewContent(row)">查看详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination 
        v-model:current-page="reportPage.current" 
        v-model:page-size="reportPage.size" 
        :page-sizes="[10, 20, 30, 40, 50]"
        :total="reportPage.total" 
        layout="total, sizes, prev, pager, next, jumper" 
        @size-change="loadReports"
        @current-change="loadReports" 
        style="margin-top: 16px" 
      />
    </el-dialog>
    
    <!-- 评分对话框 -->
    <el-dialog v-model="gradeDialogVisible" :title="isEditGrade ? '修改成绩' : '评分'" width="500px">
      <el-form :model="gradeForm" label-width="80px" :rules="gradeRules" ref="gradeFormRef">
        <el-form-item label="学生" prop="userName">
          <el-input v-model="gradeForm.userName" disabled />
        </el-form-item>
        <el-form-item label="报告内容" prop="content">
          <el-input v-model="gradeForm.content" type="textarea" :rows="4" disabled />
        </el-form-item>
        <el-form-item label="成绩" prop="score">
          <el-input-number v-model="gradeForm.score" :min="0" :max="100" :step="0.5" />
        </el-form-item>
        <el-form-item label="评语">
          <el-input v-model="gradeForm.remark" type="textarea" :rows="3" placeholder="请输入评语" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="gradeDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitGrade" :loading="grading">确定</el-button>
      </template>
    </el-dialog>
    
    <!-- 查看详情对话框 -->
    <el-dialog v-model="contentDialogVisible" title="报告详情" width="600px">
      <div style="white-space: pre-wrap; line-height: 1.5;">{{ currentReportContent }}</div>
      <div v-if="currentReportAttachment" style="margin-top: 16px;">
        <el-divider>附件</el-divider>
        <a :href="currentReportAttachment" target="_blank">{{ currentReportAttachment }}</a>
      </div>
      <div v-if="currentReportScore !== null" style="margin-top: 16px;">
        <el-divider>教师评价</el-divider>
        <div><strong>成绩：</strong>{{ currentReportScore }}</div>
        <div style="margin-top: 8px;">
          <strong>评语：</strong>
          <div style="white-space: pre-wrap; line-height: 1.5;">{{ currentReportRemark || '-' }}</div>
        </div>
      </div>
      <template #footer>
        <el-button @click="contentDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getMyTasks, addTask, updateTask, deleteTask } from '@/api/task'
import { getLabList } from '@/api/lab'
import { getReportsByTask, gradeReport } from '@/api/report'
import { getMyStudents } from '@/api/teacherStudent'
import { ElMessage, ElMessageBox } from 'element-plus'

const list = ref([])
const loading = ref(false)
const submitting = ref(false)
const labList = ref([])
const myStudents = ref([])
const dialogVisible = ref(false)
const formRef = ref(null)

// 报告相关
const reportDialogVisible = ref(false)
const reportList = ref([])
const reportLoading = ref(false)
const submittedCount = ref(0)
const totalCount = ref(0)
const currentTaskTitle = ref('')
const currentTaskId = ref(null)
const reportPage = reactive({ current: 1, size: 10, total: 0 })

// 评分相关
const gradeDialogVisible = ref(false)
const isEditGrade = ref(false) // 标记是否是修改成绩
const gradeForm = reactive({ id: null, userName: '', content: '', score: null, remark: '' })
const gradeFormRef = ref(null)
const grading = ref(false)
const gradeRules = {
  score: [{ required: true, message: '请输入成绩', trigger: 'blur' }]
}

// 查看详情相关
const contentDialogVisible = ref(false)
const currentReportContent = ref('')
const currentReportAttachment = ref('')
const currentReportScore = ref(null)
const currentReportRemark = ref('')

const query = reactive({ keyword: '', status: null })
const page = reactive({ current: 1, size: 10, total: 0 })

const form = reactive({
  id: null,
  title: '',
  labId: null,
  deadline: '',
  content: '',
  attachmentUrl: '',
  status: 1,
  studentIds: []
})

const rules = {
  title: [{ required: true, message: '请输入任务标题', trigger: 'submit' }],
  labId: [{ required: true, message: '请选择所属实验室', trigger: 'submit' }],
  deadline: [{ required: true, message: '请选择截止时间', trigger: 'submit' }],
  content: [{ required: true, message: '请输入任务内容', trigger: 'submit' }]
}

// 加载任务列表
async function load() {
  loading.value = true
  try {
    const res = await getMyTasks({ 
      current: page.current, 
      size: page.size,
      keyword: query.keyword || undefined,
      status: query.status
    })
    list.value = res.records || []
    page.total = res.total || 0
  } finally {
    loading.value = false
  }
}

// 加载实验室列表
async function loadLabs() {
  try {
    const res = await getLabList({ status: 1 })
    labList.value = res || []
  } catch (e) {
    console.error('加载实验室列表失败', e)
  }
}

// 加载报告列表
async function loadReports() {
  if (!currentTaskId.value) return
  
  reportLoading.value = true
  try {
    const res = await getReportsByTask(currentTaskId.value, {
      current: reportPage.current,
      size: reportPage.size
    })
    console.log('报告列表数据:', res.records)
    reportList.value = res.records || []
    reportPage.total = res.total || 0
    
    // 计算提交情况
    submittedCount.value = reportList.value.filter(r => r.status && (r.status === 'SUBMITTED' || r.status === 'GRADED')).length
    totalCount.value = reportPage.total
  } finally {
    reportLoading.value = false
  }
}

// 重置查询
function resetQuery() {
  query.keyword = ''
  query.status = null
  page.current = 1
  load()
}

// 打开新增对话框
function openAdd() {
  resetForm()
  dialogVisible.value = true
}

// 打开编辑对话框
function openEdit(row) {
  Object.assign(form, row)
  dialogVisible.value = true
}

// 重置表单
function resetForm() {
  form.id = null
  form.title = ''
  form.labId = null
  form.deadline = ''
  form.content = ''
  form.attachmentUrl = ''
  form.status = 1
  if (formRef.value) {
    formRef.value.resetFields()
  }
}

// 提交表单
async function submit() {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    
    submitting.value = true
    try {
      const submitData = {
        ...form,
        studentIds: form.studentIds && form.studentIds.length > 0 ? form.studentIds.join(',') : ''
      }
      if (form.id) {
        await updateTask(submitData)
        ElMessage.success('修改成功')
      } else {
        await addTask(submitData)
        ElMessage.success('发布成功')
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

// 删除任务
function doDelete(id) {
  ElMessageBox.confirm('确定删除该实验任务？', '提示', { type: 'warning' }).then(async () => {
    await deleteTask(id)
    ElMessage.success('删除成功')
    load()
  }).catch(() => {})
}

// 查看报告
function viewReports(row) {
  currentTaskId.value = row.id
  currentTaskTitle.value = row.title
  reportPage.current = 1
  loadReports()
  reportDialogVisible.value = true
}

// 评分
function openGradeDialog(row) {
  gradeForm.id = row.id
  gradeForm.userName = row.userName || '未知学生'
  gradeForm.content = row.content || ''
  gradeForm.score = null
  gradeForm.remark = ''
  isEditGrade.value = false
  gradeDialogVisible.value = true
}

// 修改成绩
function openEditGradeDialog(row) {
  gradeForm.id = row.id
  gradeForm.userName = row.userName || '未知学生'
  gradeForm.content = row.content || ''
  gradeForm.score = row.score != null ? row.score : 0
  gradeForm.remark = row.remark || ''
  isEditGrade.value = true
  gradeDialogVisible.value = true
}

// 提交评分
async function submitGrade() {
  if (!gradeFormRef.value) return
  
  await gradeFormRef.value.validate(async (valid) => {
    if (!valid) return
    
    grading.value = true
    try {
      await gradeReport(gradeForm.id, gradeForm.score, gradeForm.remark)
      ElMessage.success('评分成功')
      gradeDialogVisible.value = false
      loadReports()
    } catch (e) {
      console.error('评分失败', e)
    } finally {
      grading.value = false
    }
  })
}

// 查看详情
function viewContent(row) {
  currentReportContent.value = row.content || ''
  currentReportAttachment.value = row.attachmentUrl || ''
  currentReportScore.value = row.score || null
  currentReportRemark.value = row.remark || ''
  contentDialogVisible.value = true
}

onMounted(() => {
  load()
  loadLabs()
  loadMyStudents()
})

async function loadMyStudents() {
  try {
    const res = await getMyStudents()
    myStudents.value = res || []
  } catch (e) {
    console.error('加载学员列表失败', e)
  }
}
</script>

<style scoped>
.task-manage {
  padding: 20px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
