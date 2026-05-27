<template>
  <div>
    <h2 class="page-title">学员管理</h2>
    <el-form inline style="margin-bottom:12px">
      <el-input v-model="query.keyword" placeholder="账号/姓名" clearable style="width:160px" />
      <el-select v-model="query.classId" placeholder="班级" clearable style="width:150px">
        <el-option v-for="c in classes" :key="c.id" :label="c.className" :value="c.id" />
      </el-select>
      <el-button type="primary" @click="load">查询</el-button>
      <el-button type="success" @click="openAssign">分配学员</el-button>
    </el-form>
    <el-table :data="list" stripe v-loading="loading">
      <el-table-column prop="username" label="账号" width="120" />
      <el-table-column prop="realName" label="姓名" width="100" />
      <el-table-column prop="phone" label="手机" width="120" />
      <el-table-column prop="gender" label="性别" width="80" />
      <el-table-column prop="email" label="邮箱" width="180" />
      <el-table-column prop="className" label="班级" width="150" />
      <el-table-column label="操作" width="100">
        <template #default="{ row }">
          <el-button link type="danger" @click="doRemove(row.id)">移除</el-button>
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

    <el-dialog v-model="assignVisible" title="分配学员" width="700px">
      <el-form inline style="margin-bottom:12px">
        <el-input v-model="assignQuery.keyword" placeholder="学员姓名" clearable style="width:160px" />
        <el-select v-model="assignQuery.classId" placeholder="班级" clearable style="width:150px">
          <el-option v-for="c in classes" :key="c.id" :label="c.className" :value="c.id" />
        </el-select>
        <el-button type="primary" @click="loadAvailableStudents">查询</el-button>
      </el-form>
      <el-table :data="availableStudents" stripe v-loading="assignLoading">
        <el-table-column prop="username" label="账号" width="120" />
        <el-table-column prop="realName" label="姓名" width="100" />
        <el-table-column prop="phone" label="手机" width="120" />
        <el-table-column prop="className" label="班级" width="150" />
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button link type="primary" @click="doAssign(row.id)">添加</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getTeacherStudentsPage, getAvailableStudents, assignStudent, removeStudent } from '@/api/teacherStudent'
import { getClasses } from '@/api/user'
import { ElMessage } from 'element-plus'

const list = ref([])
const loading = ref(false)
const query = reactive({ keyword: '', classId: undefined })
const page = reactive({ current: 1, size: 10, total: 0 })
const assignVisible = ref(false)
const assignLoading = ref(false)
const availableStudents = ref([])
const studentRelations = ref([])
const classes = ref([])
const assignQuery = reactive({ keyword: '', classId: undefined })

async function loadClasses() {
  try {
    const res = await getClasses()
    classes.value = res || []
  } catch (error) {
    console.error('获取班级列表失败', error)
  }
}

async function load() {
  loading.value = true
  try {
    const res = await getTeacherStudentsPage({
      current: page.current,
      size: page.size,
      keyword: query.keyword || undefined,
      classId: query.classId || undefined
    })
    list.value = res.records || []
    page.total = res.total || 0
  } finally {
    loading.value = false
  }
}

async function loadAvailableStudents() {
  assignLoading.value = true
  try {
    const res = await getAvailableStudents({
      keyword: assignQuery.keyword || undefined,
      classId: assignQuery.classId || undefined
    })
    availableStudents.value = res || []
  } finally {
    assignLoading.value = false
  }
}

async function openAssign() {
  assignVisible.value = true
  await loadAvailableStudents()
}

async function doAssign(studentId) {
  await assignStudent(studentId)
  ElMessage.success('添加成功')
  load()
  openAssign()
}

async function doRemove(id) {
  await removeStudent(id)
  ElMessage.success('已移除')
  load()
}

onMounted(() => {
  loadClasses()
  load()
})
</script>

<style scoped>
.page-title { margin-bottom: 16px; font-size: 18px; }
</style>
