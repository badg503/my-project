<template>
  <div class="check-all">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>全局考勤管理</span>
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
        <el-form-item label="考勤状态">
          <el-select v-model="searchForm.status" placeholder="选择状态" clearable style="width: 150px;">
            <el-option label="已签到" value="ATTENDANCE" />
            <el-option label="缺勤" value="ABSENCE" />
            <el-option label="迟到" value="LATE" />
            <el-option label="未签到" value="NOT_SIGNED" />
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
        <el-table-column prop="checkOutStatus" label="考勤状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.checkOutStatus)">{{ getStatusText(row.checkOutStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="checkInTime" label="签到时间" width="170" />
        <el-table-column prop="score" label="成绩" width="100" />
        <el-table-column prop="remark" label="备注" show-overflow-tooltip />
      </el-table>
      
      <el-pagination
        v-model:current-page="page.current"
        v-model:page-size="page.size"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        :total="page.total"
        @size-change="handleSizeChange"
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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import { getAllCheckRecords, getAllAttendanceStats } from '@/api/check'
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
  classId: null,
  status: null
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
    const res = await getAllCheckRecords({
      current: page.current,
      size: page.size,
      labId: searchForm.labId || undefined,
      classId: searchForm.classId || undefined,
      status: searchForm.status || undefined
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
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      },
      formatter: function(params) {
        const item = params[0]
        return `${item.name}<br/>出勤率: ${item.value.toFixed(1)}%<br/>总人数: ${data[item.dataIndex].total}<br/>出勤人数: ${data[item.dataIndex].attendanceCount}`
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
        rotate: 30
      }
    },
    yAxis: {
      type: 'value',
      name: '出勤率 (%)',
      max: 100
    },
    series: [{
      name: '出勤率',
      type: 'bar',
      data: data.map(item => item.rate),
      itemStyle: {
        color: function(params) {
          const rate = params.value
          if (rate >= 80) return '#67c23a'
          if (rate >= 60) return '#e6a23c'
          return '#f56c6c'
        }
      },
      label: {
        show: true,
        position: 'top',
        formatter: '{c}%'
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
        const item = params[0]
        return `${item.name}<br/>出勤率: ${item.value.toFixed(1)}%<br/>总人数: ${data[item.dataIndex].total}<br/>出勤人数: ${data[item.dataIndex].attendanceCount}`
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
        rotate: 30
      }
    },
    yAxis: {
      type: 'value',
      name: '出勤率 (%)',
      max: 100
    },
    series: [{
      name: '出勤率',
      type: 'bar',
      data: data.map(item => item.rate),
      itemStyle: {
        color: function(params) {
          const rate = params.value
          if (rate >= 80) return '#67c23a'
          if (rate >= 60) return '#e6a23c'
          return '#f56c6c'
        }
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
    'NORMAL': 'success',
    'EARLY_LEAVE': 'warning',
    'ABSENT': 'danger',
    'LATE': 'warning'
  }
  return map[status] || 'info'
}

function getStatusText(status) {
  const map = {
    'NORMAL': '正常',
    'EARLY_LEAVE': '早退',
    'ABSENT': '缺勤',
    'LATE': '迟到'
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
</script>

<style scoped>
.check-all {
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
