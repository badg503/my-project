<template>
  <div class="lab-manage">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>实验室管理</span>
          <el-button type="primary" @click="dialogVisible = true">新增实验室</el-button>
        </div>
      </template>
      
      <!-- 搜索和筛选 -->
      <div class="search-container">
        <el-input v-model="query.name" placeholder="实验室名称" style="width: 200px; margin-right: 10px" />
        <el-select v-model="query.status" placeholder="状态" style="width: 120px; margin-right: 10px">
          <el-option label="全部" value="" />
          <el-option label="可用" value="1" />
          <el-option label="不可用" value="0" />
        </el-select>
        <el-button type="primary" @click="load">查询</el-button>
      </div>
      
      <!-- 实验室列表 -->
      <el-table :data="list" stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="实验室名称" width="180" />
        <el-table-column prop="location" label="位置" width="180" />
        <el-table-column label="开放时间" width="200">
          <template #default="{ row }">
            {{ row.openTimeStart }} - {{ row.openTimeEnd }}
          </template>
        </el-table-column>
        <el-table-column prop="capacity" label="容量" width="80" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '可用' : '不可用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" show-overflow-tooltip />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="edit(row)">编辑</el-button>
            <el-button link type="danger" @click="del(row.id)">删除</el-button>
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
        @size-change="handleSizeChange" 
        @current-change="handleCurrentChange" 
        style="margin-top: 16px" 
      />
      
      <!-- 新增/编辑对话框 -->
      <el-dialog v-model="dialogVisible" :title="form.id ? '编辑实验室' : '新增实验室'" width="600px">
        <el-form :model="form" label-width="100px" :rules="rules" ref="formRef">
          <el-form-item label="实验室名称" prop="name">
            <el-input v-model="form.name" placeholder="请输入实验室名称" />
          </el-form-item>
          <el-form-item label="位置" prop="location">
            <el-input v-model="form.location" placeholder="请输入实验室位置" />
          </el-form-item>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="开放开始时间" prop="openTimeStart">
                <el-time-picker v-model="form.openTimeStart" placeholder="选择开始时间" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="开放结束时间" prop="openTimeEnd">
                <el-time-picker v-model="form.openTimeEnd" placeholder="选择结束时间" style="width: 100%" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="容量" prop="capacity">
            <el-input-number v-model="form.capacity" :min="1" placeholder="请输入容量" style="width: 100%" />
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-radio-group v-model="form.status">
              <el-radio :label="1">可用</el-radio>
              <el-radio :label="0">不可用</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="描述">
            <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入实验室描述" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submit" :loading="submitting">确定</el-button>
        </template>
      </el-dialog>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getLabPage, addLab, updateLab, deleteLab } from '@/api/lab'
import { ElMessage, ElMessageBox } from 'element-plus'

const list = ref([])
const loading = ref(false)
const page = reactive({ current: 1, size: 10, total: 0 })
const query = reactive({ name: '', status: '' })
const dialogVisible = ref(false)
const form = reactive({
  id: null,
  name: '',
  location: '',
  openTimeStart: '',
  openTimeEnd: '',
  capacity: 0,
  status: 1,
  description: ''
})
const rules = {
  name: [{ required: true, message: '请输入实验室名称', trigger: 'submit' }],
  location: [{ required: true, message: '请输入实验室位置', trigger: 'submit' }],
  openTimeStart: [{ required: true, message: '请选择开放开始时间', trigger: 'submit' }],
  openTimeEnd: [{ required: true, message: '请选择开放结束时间', trigger: 'submit' }],
  capacity: [{ required: true, message: '请输入容量', trigger: 'submit' }],
  status: [{ required: true, message: '请选择状态', trigger: 'submit' }]
}
const submitting = ref(false)

// 加载实验室列表
async function load() {
  loading.value = true
  try {
    const res = await getLabPage({
      current: page.current,
      size: page.size,
      name: query.name || undefined,
      status: query.status || undefined
    })
    list.value = res.records || []
    page.total = res.total || 0
  } finally {
    loading.value = false
  }
}

// 新增实验室
function add() {
  Object.assign(form, {
    id: null,
    name: '',
    location: '',
    openTimeStart: '',
    openTimeEnd: '',
    capacity: 0,
    status: 1,
    description: ''
  })
  dialogVisible.value = true
}

// 编辑实验室
function edit(row) {
  // 复制行数据
  Object.assign(form, row)
  // 时间选择器需要Date对象，后端返回的是字符串
  if (row.openTimeStart) {
    form.openTimeStart = new Date(`2000-01-01 ${row.openTimeStart}`)
  }
  if (row.openTimeEnd) {
    form.openTimeEnd = new Date(`2000-01-01 ${row.openTimeEnd}`)
  }
  dialogVisible.value = true
}

// 提交表单
async function submit() {
  submitting.value = true
  try {
    // 准备提交数据，处理时间格式
    const submitData = { ...form }
    // 将Date对象转换为字符串格式
    if (submitData.openTimeStart) {
      submitData.openTimeStart = submitData.openTimeStart.toTimeString().substring(0, 8)
    }
    if (submitData.openTimeEnd) {
      submitData.openTimeEnd = submitData.openTimeEnd.toTimeString().substring(0, 8)
    }
    
    if (form.id) {
      await updateLab(submitData)
      ElMessage.success('更新成功')
    } else {
      await addLab(submitData)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

// 删除实验室
async function del(id) {
  try {
    await ElMessageBox.confirm('确定要删除该实验室吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteLab(id)
    ElMessage.success('删除成功')
    load()
  } catch (e) {
    // 用户取消删除
  }
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

onMounted(() => {
  load()
})
</script>

<style scoped>
.lab-manage {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.search-container {
  margin-bottom: 16px;
  display: flex;
  align-items: center;
}
</style>
