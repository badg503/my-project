<template>
  <div>
    <h2 class="page-title">用户管理</h2>
    <el-form inline style="margin-bottom:12px">
      <el-select v-model="query.role" placeholder="角色" clearable style="width:120px">
        <el-option label="系统管理员" value="SYS_ADMIN" />
        <el-option label="实验室管理员" value="LAB_ADMIN" />
        <el-option label="教师" value="TEACHER" />
        <el-option label="学生" value="STUDENT" />
      </el-select>
      <el-input v-model="query.keyword" placeholder="账号/姓名" clearable style="width:160px" />
      <el-button type="primary" @click="load">查询</el-button>
      <el-button @click="openAdd">新增用户</el-button>
      <el-button type="success" @click="openTeacherStudentAssign">教师学员分配</el-button>
    </el-form>
    <el-table :data="list" stripe v-loading="loading">
      <el-table-column prop="username" label="账号" width="120" />
      <el-table-column prop="realName" label="姓名" min-width="100" />
      <el-table-column prop="role" label="角色" width="100">
        <template #default="{ row }">{{ roleText(row.role) }}</template>
      </el-table-column>
      <el-table-column prop="className" label="班级" min-width="140" />
      <el-table-column prop="departmentName" label="院系" min-width="140" />
      <el-table-column prop="phone" label="手机" min-width="130" />
      <el-table-column prop="email" label="邮箱" min-width="200" />
      <el-table-column prop="gender" label="性别" width="80" />
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '正常' : '禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="180" />
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link @click="openResetPwd(row)">重置密码</el-button>
          <el-button link type="danger" @click="doDelete(row.id)">删除</el-button>
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
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑用户' : '新增用户'" width="500px" @close="form = {}">
      <el-form :model="form" label-width="80px">
        <el-form-item label="账号"><el-input v-model="form.username" :disabled="!!form.id" /></el-form-item>
        <el-form-item label="密码" v-if="!form.id"><el-input v-model="form.password" type="password" placeholder="不填默认123456" /></el-form-item>
        <el-form-item label="姓名"><el-input v-model="form.realName" /></el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.role" style="width:100%" :disabled="!!form.id">
            <el-option label="系统管理员" value="SYS_ADMIN" />
            <el-option label="实验室管理员" value="LAB_ADMIN" />
            <el-option label="教师" value="TEACHER" />
            <el-option label="学生" value="STUDENT" />
          </el-select>
        </el-form-item>
        <el-form-item label="班级" v-if="form.role === 'STUDENT'">
          <el-select v-model="form.classId" placeholder="选择班级" clearable style="width:100%">
            <el-option v-for="c in classes" :key="c.id" :label="c.className" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="form.gender">
            <el-radio label="男">男</el-radio>
            <el-radio label="女">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="手机"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="form.email" placeholder="请输入邮箱" /></el-form-item>
        <el-form-item label="状态" v-if="form.id">
          <el-radio-group v-model="form.status"><el-radio :label="1">正常</el-radio><el-radio :label="0">禁用</el-radio></el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">确定</el-button>
      </template>
    </el-dialog>
    <el-dialog v-model="resetVisible" title="重置密码" width="400px">
      <el-form :model="resetForm" label-width="80px">
        <el-form-item label="新密码"><el-input v-model="resetForm.newPassword" type="password" show-password /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetVisible = false">取消</el-button>
        <el-button type="primary" @click="doResetPwd">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="assignVisible" title="教师学员分配" width="900px">
      <el-form inline style="margin-bottom:12px">
        <el-select 
          v-model="assignQuery.teacherId" 
          placeholder="选择教师" 
          clearable 
          style="width:160px"
        >
          <el-option v-for="t in teachers" :key="t.id" :label="t.realName" :value="t.id" />
        </el-select>
        <el-select 
          v-model="assignQuery.classId" 
          placeholder="选择班级" 
          clearable 
          style="width:150px"
        >
          <el-option v-for="c in classes" :key="c.id" :label="c.className" :value="c.id" />
        </el-select>
        <el-button type="primary" @click="loadStudents">查询</el-button>
        <el-button type="success" @click="assignStudents" :disabled="!assignQuery.teacherId || selectedUnassigned.length === 0">分配</el-button>
      </el-form>
      <div style="display: flex; gap: 20px; height: 450px">
        <div style="flex: 1; display: flex; flex-direction: column;">
          <h4 style="margin: 0 0 10px 0;">未分配学员</h4>
          <el-table :data="unassignedStudents" stripe height="410" @selection-change="handleUnassignedSelectionChange">
            <el-table-column type="selection" width="40" />
            <el-table-column prop="username" label="学号" width="100" />
            <el-table-column prop="realName" label="姓名" width="80" />
            <el-table-column prop="className" label="班级" width="120" />
          </el-table>
        </div>
        <div style="flex: 1; display: flex; flex-direction: column;">
          <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px;">
            <h4 style="margin: 0;">已分配学员</h4>
          </div>
          <el-table :data="assignedStudentList" stripe height="410">
            <el-table-column prop="username" label="学号" width="100" />
            <el-table-column prop="realName" label="姓名" width="80" />
            <el-table-column prop="className" label="班级" width="120" />
            <el-table-column label="操作" width="80">
              <template #default="{ row }">
                <el-button link type="danger" size="small" @click="doUnassign(row.id)">移除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
      <template #footer>
        <el-button @click="assignVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getUserPage, addUser, updateUser, deleteUser, resetPassword, getTeachers, getStudents, assignStudents as assignStudentsApi, unassignStudents as unassignStudentsApi, getAssignedStudents, getClasses, getDepartments } from '@/api/user'
import { ElMessage, ElMessageBox } from 'element-plus'

const list = ref([])
const loading = ref(false)
const query = reactive({ role: '', keyword: '' })
const page = reactive({ current: 1, size: 10, total: 0 })
const dialogVisible = ref(false)
const resetVisible = ref(false)
const form = ref({})
const resetForm = ref({ userId: null, newPassword: '' })
const classes = ref([])

async function loadClasses() {
  classes.value = await getClasses()
}

const roleText = (r) => {
  const map = { SYS_ADMIN: '系统管理员', LAB_ADMIN: '实验室管理员', TEACHER: '教师', STUDENT: '学生' }
  return map[r] || r
}

async function load() {
  loading.value = true
  try {
    const res = await getUserPage({ current: page.current, size: page.size, role: query.role || undefined, keyword: query.keyword || undefined })
    list.value = res.records || []
    page.total = res.total || 0
  } finally {
    loading.value = false
  }
}

function openAdd() {
  form.value = { username: '', password: '', realName: '', role: 'STUDENT', phone: '', status: 1, classId: null, major: '' }
  dialogVisible.value = true
}

function openEdit(row) {
  form.value = { ...row }
  dialogVisible.value = true
}

async function submit() {
  try {
    if (form.value.id) await updateUser(form.value)
    else await addUser(form.value)
    ElMessage.success('保存成功')
    dialogVisible.value = false
    load()
  } catch (error) {
    ElMessage.error(error.response?.data?.msg || '保存失败')
  }
}

function openResetPwd(row) {
  resetForm.value = { userId: row.id, newPassword: '123456' }
  resetVisible.value = true
}

async function doResetPwd() {
  await resetPassword(resetForm.value.userId, resetForm.value.newPassword)
  ElMessage.success('密码已重置')
  resetVisible.value = false
}

function doDelete(id) {
  ElMessageBox.confirm('确定删除该用户？', '提示', { type: 'warning' }).then(async () => {
    await deleteUser(id)
    ElMessage.success('已删除')
    load()
  }).catch(() => {})
}

function handleSizeChange(size) {
  page.size = size
  load()
}

function handleCurrentChange(current) {
  page.current = current
  load()
}

const assignVisible = ref(false)
const teachers = ref([])
const unassignedStudents = ref([])
const assignedStudentList = ref([])
const selectedUnassigned = ref([])
const selectedAssigned = ref([])
const assignQuery = reactive({ teacherId: null, classId: null })

function handleUnassignedSelectionChange(selection) {
  selectedUnassigned.value = selection
}

function handleAssignedSelectionChange(selection) {
  selectedAssigned.value = selection
}

async function initTeachers() {
  teachers.value = await getTeachers()
}

function openTeacherStudentAssign() {
  assignVisible.value = true
  initTeachers()
  loadStudents()
}

async function loadStudents() {
  if (!assignQuery.teacherId) {
    unassignedStudents.value = []
    assignedStudentList.value = []
    return
  }
  
  const res = await getStudents()
  const allStudents = res || []
  const assigned = await getAssignedStudents(assignQuery.teacherId)
  
  // 过滤未分配的学生
  let unassigned = allStudents.filter(s => !assigned.some(a => a.id === s.id))
  
  // 如果选择了班级，则按班级过滤
  if (assignQuery.classId) {
    unassigned = unassigned.filter(s => s.classId === assignQuery.classId)
  }
  
  unassignedStudents.value = unassigned
  assignedStudentList.value = assigned
}

async function assignStudents() {
  if (!assignQuery.teacherId || selectedUnassigned.value.length === 0) {
    ElMessage.error('请选择教师和学员')
    return
  }
  
  const studentIds = selectedUnassigned.value.map(s => s.id)
  await assignStudentsApi(assignQuery.teacherId, studentIds)
  ElMessage.success('分配成功')
  loadStudents()
  selectedUnassigned.value = []
}

async function unassignStudents() {
  if (!assignQuery.teacherId || selectedAssigned.value.length === 0) {
    ElMessage.error('请选择教师和学员')
    return
  }
  
  const studentIds = selectedAssigned.value.map(s => s.id)
  await unassignStudentsApi(assignQuery.teacherId, studentIds)
  ElMessage.success('取消分配成功')
  loadStudents()
  selectedAssigned.value = []
}

async function doUnassign(studentId) {
  if (!assignQuery.teacherId) {
    ElMessage.error('请先选择教师')
    return
  }
  
  await unassignStudentsApi(assignQuery.teacherId, [studentId])
  ElMessage.success('移除成功')
  loadStudents()
}

onMounted(() => {
  load()
  loadClasses()
})
</script>

<style scoped>
.page-title { margin-bottom: 16px; font-size: 18px; }
</style>
