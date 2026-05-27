<template>
  <div class="attendance-all">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>考勤管理</span>
        </div>
      </template>
      
      <el-form :inline="true" :model="searchForm" class="mb-4">
        <el-form-item label="实验室">
          <el-select v-model="searchForm.labId" placeholder="选择实验室" clearable style="width: 200px;">
            <el-option
              v-for="lab in labs"
              :key="lab.id"
              :label="lab.name"
              :value="lab.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="班级">
          <el-select v-model="searchForm.classId" placeholder="选择班级" clearable style="width: 200px;">
            <el-option
              v-for="cls in classes"
              :key="cls.id"
              :label="cls.className"
              :value="cls.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadAttendance">查询</el-button>
        </el-form-item>
      </el-form>
      
      <el-table v-loading="loading" :data="attendanceList" stripe>
        <el-table-column prop="realName" label="学生姓名" width="120" />
        <el-table-column prop="className" label="班级" width="150" />
        <el-table-column prop="labName" label="实验室" width="150" />
        <el-table-column prop="taskTitle" label="实验任务" width="200" />
        <el-table-column prop="status" label="考勤状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="checkInTime" label="签到时间" width="170" />
        <el-table-column prop="score" label="成绩" width="100" />
        <el-table-column prop="remark" label="备注" show-overflow-tooltip />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button 
              type="primary" 
              size="small" 
              @click="openEditDialog(row)"
              v-if="isLabAdmin"
            >
              修改状态
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <el-pagination
        v-model:current-page="page.current"
        v-model:page-size="page.size"
        :page-size="10"
        :page-sizes="[10]"
        :pager-count="5"
        layout="total, prev, pager, next"
        :total="page.total"
        @current-change="handleCurrentChange"
        class="mt-4"
      />
    </el-card>
    
    <el-row :gutter="20" class="mt-4">
      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="chart-title">各实验室出勤率</div>
          </template>
          <div ref="labAttendanceChart" style="height: 400px"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="chart-title">各班级出勤率</div>
          </template>
          <div ref="classAttendanceChart" style="height: 400px"></div>
        </el-card>
      </el-col>
    </el-row>
    
    <!-- 修改考勤状态对话框 -->
    <el-dialog 
      v-model="editDialogVisible" 
      title="修改考勤状态" 
      width="500px"
      @close="resetForm"
    >
      <el-form :model="editForm" label-width="100px">
        <el-form-item label="学生姓名">
          <el-input v-model="editForm.realName" disabled />
        </el-form-item>
        <el-form-item label="实验任务">
          <el-input v-model="editForm.taskTitle" disabled />
        </el-form-item>
        <el-form-item label="当前状态">
          <el-tag :type="getStatusType(editForm.status)">{{ getStatusText(editForm.status) }}</el-tag>
        </el-form-item>
        <el-form-item label="修改为">
          <el-select v-model="editForm.newStatus" placeholder="请选择新状态" style="width: 100%">
            <el-option label="已签到" value="ATTENDANCE" />
            <el-option label="缺勤" value="ABSENCE" />
            <el-option label="迟到" value="LATE" />
            <el-option label="未签到" value="NOT_SIGNED" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input 
            v-model="editForm.remark" 
            type="textarea" 
            :rows="3"
            placeholder="请输入备注（选填）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="updateAttendanceStatus" :loading="updating">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick, computed } from 'vue'
import { getAllAttendance, getAllAttendanceStats, updateAttendance } from '@/api/attendance'
import { getLabList } from '@/api/lab'
import { getClasses } from '@/api/user'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'

const loading = ref(false)
const attendanceList = ref([])
const labs = ref([])
const classes = ref([])
const searchForm = reactive({
  labId: null,
  classId: null
})
const page = reactive({
  current: 1,
  size: 10,
  total: 0
})

const labAttendanceChart = ref(null)
const classAttendanceChart = ref(null)
let labAttendanceChartInstance = null
let classAttendanceChartInstance = null

// 修改考勤状态相关
const editDialogVisible = ref(false)
const updating = ref(false)
const editForm = reactive({
  id: null,
  realName: '',
  taskTitle: '',
  status: '',
  newStatus: '',
  remark: ''
})

// 判断是否为实验室管理员
const isLabAdmin = computed(() => {
  const roles = localStorage.getItem('roles')
  return roles && roles.includes('ROLE_LAB_ADMIN')
})

onMounted(async () => {
  await loadLabs()
  await loadClasses()
  await loadAttendance()
  await loadStats()
})

async function loadLabs() {
  try {
    const res = await getLabList()
    labs.value = res || []
  } catch (error) {
    console.error('加载实验室列表失败', error)
  }
}

async function loadClasses() {
  try {
    const res = await getClasses()
    classes.value = res || []
  } catch (error) {
    console.error('加载班级列表失败', error)
  }
}

async function loadAttendance() {
  loading.value = true
  try {
    const res = await getAllAttendance({
      current: page.current,
      size: page.size,
      labId: searchForm.labId || undefined,
      classId: searchForm.classId || undefined
    })
    attendanceList.value = res.records || []
    page.total = res.total || 0
  } catch (error) {
    ElMessage.error('加载考勤记录失败')
    console.error('加载考勤记录失败', error)
  } finally {
    loading.value = false
  }
}

async function loadStats() {
  try {
    const res = await getAllAttendanceStats()
    updateLabAttendanceChart(res.labAttendanceRate || [])
    updateClassAttendanceChart(res.classAttendanceRate || [])
  } catch (error) {
    console.error('加载统计数据失败', error)
  }
}

function updateLabAttendanceChart(data) {
  if (!labAttendanceChart.value) return
  
  if (labAttendanceChartInstance) {
    labAttendanceChartInstance.dispose()
  }
  
  labAttendanceChartInstance = echarts.init(labAttendanceChart.value)
  
  const option = {
    tooltip: {
      trigger: 'item',
      formatter: function(params) {
        const item = data.find(d => d.name === params.name)
        return `${params.name}<br/>出勤率: ${params.value.toFixed(1)}%<br/>总人数: ${item.total}<br/>出勤人数: ${item.attendanceCount}`
      }
    },
    legend: {
      orient: 'vertical',
      left: 'left'
    },
    series: [{
      name: '出勤率',
      type: 'pie',
      radius: '50%',
      data: data.map(item => ({
        name: item.name,
        value: item.rate
      })),
      emphasis: {
        itemStyle: {
          shadowBlur: 10,
          shadowOffsetX: 0,
          shadowColor: 'rgba(0, 0, 0, 0.5)'
        }
      },
      label: {
        formatter: '{b}: {c}%'
      }
    }]
  }
  
  labAttendanceChartInstance.setOption(option)
}

function updateClassAttendanceChart(data) {
  if (!classAttendanceChart.value) return
  
  if (classAttendanceChartInstance) {
    classAttendanceChartInstance.dispose()
  }
  
  classAttendanceChartInstance = echarts.init(classAttendanceChart.value)
  
  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      },
      formatter: function(params) {
        const item = data[params[0].dataIndex]
        return `${item.name}<br/>出勤率: ${item.rate.toFixed(1)}%<br/>总人数: ${item.total}<br/>出勤人数: ${item.attendanceCount}`
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: data.map(item => item.name),
      axisLabel: {
        interval: 0,
        rotate: 45
      }
    },
    yAxis: {
      type: 'value',
      name: '出勤率(%)',
      min: 0,
      max: 100
    },
    series: [{
      name: '出勤率',
      type: 'bar',
      data: data.map(item => item.rate),
      itemStyle: {
        color: '#409EFF'
      },
      label: {
        show: true,
        position: 'top',
        formatter: '{c}%'
      }
    }]
  }
  
  classAttendanceChartInstance.setOption(option)
}

function getStatusType(status) {
  const map = {
    'ATTENDANCE': 'success',
    'ABSENCE': 'danger',
    'LATE': 'warning',
    'NOT_SIGNED': 'info'
  }
  return map[status] || 'info'
}

function getStatusText(status) {
  const map = {
    'ATTENDANCE': '已签到',
    'ABSENCE': '缺勤',
    'LATE': '迟到',
    'NOT_SIGNED': '未签到'
  }
  return map[status] || status
}

function handleSizeChange(size) {
  page.size = size
  loadAttendance()
}

function handleCurrentChange(current) {
  page.current = current
  loadAttendance()
}

// 打开修改对话框
function openEditDialog(row) {
  editForm.id = row.id
  editForm.realName = row.realName
  editForm.taskTitle = row.taskTitle
  editForm.status = row.status
  editForm.newStatus = row.status
  editForm.remark = row.remark || ''
  editDialogVisible.value = true
}

// 重置表单
function resetForm() {
  editForm.id = null
  editForm.realName = ''
  editForm.taskTitle = ''
  editForm.status = ''
  editForm.newStatus = ''
  editForm.remark = ''
}

// 更新考勤状态
async function updateAttendanceStatus() {
  if (!editForm.newStatus) {
    ElMessage.warning('请选择新状态')
    return
  }
  
  updating.value = true
  try {
    await updateAttendance({
      id: editForm.id,
      status: editForm.newStatus,
      remark: editForm.remark
    })
    
    ElMessage.success('考勤状态已更新')
    editDialogVisible.value = false
    await loadAttendance()
  } catch (error) {
    ElMessage.error('更新考勤状态失败')
    console.error('更新考勤状态失败', error)
  } finally {
    updating.value = false
  }
}
</script>

<style scoped>
.attendance-all {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chart-title {
  font-size: 16px;
  font-weight: bold;
}

.mt-4 {
  margin-top: 20px;
}

.mb-4 {
  margin-bottom: 20px;
}
</style>
