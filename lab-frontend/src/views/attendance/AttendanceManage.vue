<template>
  <div class="student-stats">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>实验统计</span>
          <el-select v-model="selectedTaskId" placeholder="选择实验任务" style="width: 200px">
            <el-option 
              v-for="task in taskList" 
              :key="task.id" 
              :label="task.title" 
              :value="task.id" 
            />
          </el-select>
          <el-button type="primary" @click="generateReport" :disabled="!selectedTaskId">生成报表</el-button>
        </div>
      </template>
      
      <div v-if="!selectedTaskId" class="empty-state">
        <el-empty description="请选择实验任务" />
      </div>
      
      <div v-else>
        <!-- 考勤记录列表 -->
        <div class="attendance-list">
          <el-table :data="paginatedStudents" stripe v-loading="loading">
            <el-table-column prop="realName" label="学生姓名" width="120" />
            <el-table-column prop="gender" label="性别" width="80">
              <template #default="{ row }">
                {{ row.gender === '男' ? '男' : (row.gender === '女' ? '女' : '-') }}
              </template>
            </el-table-column>
            <el-table-column prop="status" label="提交状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="submitTime" label="提交时间" width="170" />
            <el-table-column prop="score" label="成绩" width="100">
              <template #default="{ row }">
                {{ row.score != null ? row.score : '-' }}
              </template>
            </el-table-column>
            <el-table-column prop="remark" label="评语" show-overflow-tooltip />
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="{ row }">
                <el-select v-model="row.status" @change="updateSubmissionStatus(row)" placeholder="选择状态" style="width: 120px">
                  <el-option label="未提交" value="NOT_SUBMITTED" />
                  <el-option label="已提交" value="SUBMITTED" />
                  <el-option label="已批阅" value="GRADED" />
                </el-select>
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
        </div>
        
        <!-- 性别统计图表 -->
        <div class="chart-container">
          <el-row :gutter="20">
            <el-col :span="12">
              <el-card shadow="hover">
                <template #header>
                  <div class="chart-title">性别分布柱状图</div>
                </template>
                <div ref="genderBarChart" style="height: 300px"></div>
              </el-card>
            </el-col>
            <el-col :span="12">
              <el-card shadow="hover">
                <template #header>
                  <div class="chart-title">性别分布饼状图</div>
                </template>
                <div ref="genderPieChart" style="height: 300px"></div>
              </el-card>
            </el-col>
          </el-row>
        </div>
        
        <!-- 成绩统计图表 -->
        <div class="chart-container">
          <el-row :gutter="20">
            <el-col :span="12">
              <el-card shadow="hover">
                <template #header>
                  <div class="chart-title">成绩分布柱状图（按性别）</div>
                </template>
                <div ref="scoreBarChart" style="height: 300px"></div>
              </el-card>
            </el-col>
            <el-col :span="12">
              <el-card shadow="hover">
                <template #header>
                  <div class="chart-title">成绩分布饼状图（按性别）</div>
                </template>
                <div ref="scorePieChart" style="height: 300px"></div>
              </el-card>
            </el-col>
          </el-row>
        </div>
        
        <!-- 底部统计数据 -->
        <div class="stats-footer">
          <el-row :gutter="20">
            <el-col :span="6">
              <el-card shadow="hover" class="stat-card">
                <div class="stat-item">
                  <div class="stat-label">总人数</div>
                  <div class="stat-value">{{ stats.total || 0 }}</div>
                </div>
              </el-card>
            </el-col>
            <el-col :span="6">
              <el-card shadow="hover" class="stat-card">
                <div class="stat-item">
                  <div class="stat-label">已提交人数</div>
                  <div class="stat-value">{{ stats.submittedCount || 0 }}</div>
                </div>
              </el-card>
            </el-col>
            <el-col :span="6">
              <el-card shadow="hover" class="stat-card">
                <div class="stat-item">
                  <div class="stat-label">未交人数</div>
                  <div class="stat-value">{{ (stats.total || 0) - (stats.submittedCount || 0) }}</div>
                </div>
              </el-card>
            </el-col>
            <el-col :span="6">
              <el-card shadow="hover" class="stat-card">
                <div class="stat-item">
                  <div class="stat-label">提交率</div>
                  <div class="stat-value">{{ (stats.submissionRate || 0).toFixed(1) }}%</div>
                </div>
              </el-card>
            </el-col>
          </el-row>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch, nextTick, computed } from 'vue'
import { getMyTasks } from '@/api/task'
import { getRecordByTask, getRecordStats, updateRecord } from '@/api/record'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'

const taskList = ref([])
const selectedTaskId = ref(null)
const stats = reactive({ total: 0, submittedCount: 0, gradedCount: 0, submissionRate: 0 })
const loading = ref(false)

// 分页
const page = reactive({ current: 1, size: 10, total: 0 })

// 图表实例
const genderBarChart = ref(null)
const genderPieChart = ref(null)
const scoreBarChart = ref(null)
const scorePieChart = ref(null)

let genderBarChartInstance = null
let genderPieChartInstance = null
let scoreBarChartInstance = null
let scorePieChartInstance = null

// 学员数据
const students = ref([])

// 分页后的学生数据
const paginatedStudents = computed(() => {
  const start = (page.current - 1) * page.size
  const end = start + page.size
  return students.value.slice(start, end)
})

// 加载任务列表
async function loadTasks() {
  try {
    const res = await getMyTasks({ current: 1, size: 100 })
    taskList.value = res.records || []
  } catch (e) {
    console.error('加载任务列表失败', e)
  }
}

// 加载学员数据
async function load() {
  if (!selectedTaskId.value) return
  
  loading.value = true
  try {
    const res = await getRecordByTask(selectedTaskId.value, {
      current: 1,
      size: 1000
    })
    students.value = res.records || []
    page.total = students.value.length
    page.current = 1 // 重置到第一页
    
    // 调试：打印学生对象结构
    if (students.value.length > 0) {
      console.log('学生对象结构:', students.value[0])
    }
    
    // 加载统计数据
    await loadStats()
    
    // 更新图表
    nextTick(() => {
      updateCharts()
    })
  } catch (e) {
    console.error('加载学员数据失败', e)
  } finally {
    loading.value = false
  }
}

// 处理分页大小变化
function handleSizeChange(size) {
  page.size = size
  page.current = 1 // 重置到第一页
}

// 处理页码变化
function handleCurrentChange(current) {
  page.current = current
}

// 加载统计数据
async function loadStats() {
  if (!selectedTaskId.value) return
  
  try {
    const res = await getRecordStats(selectedTaskId.value)
    Object.assign(stats, res)
  } catch (e) {
    console.error('加载统计数据失败', e)
  }
}

// 更新图表
function updateCharts() {
  updateGenderBarChart()
  updateGenderPieChart()
  updateScoreBarChart()
  updateScorePieChart()
}

// 更新性别分布柱状图
function updateGenderBarChart() {
  if (!genderBarChart.value) return
  
  if (genderBarChartInstance) {
    genderBarChartInstance.dispose()
  }
  
  genderBarChartInstance = echarts.init(genderBarChart.value)
  
  const maleCount = students.value.filter(s => s.gender === '男').length
  const femaleCount = students.value.filter(s => s.gender === '女').length
  
  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
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
      data: ['男', '女']
    },
    yAxis: {
      type: 'value',
      name: '人数'
    },
    series: [{
      name: '人数',
      type: 'bar',
      data: [maleCount, femaleCount],
      itemStyle: {
        color: function(params) {
          const colors = ['#409eff', '#f72a84']
          return colors[params.dataIndex]
        }
      },
      label: {
        show: true,
        position: 'top'
      }
    }]
  }
  
  genderBarChartInstance.setOption(option)
}

// 更新性别分布饼状图
function updateGenderPieChart() {
  if (!genderPieChart.value) return
  
  if (genderPieChartInstance) {
    genderPieChartInstance.dispose()
  }
  
  genderPieChartInstance = echarts.init(genderPieChart.value)
  
  const maleCount = students.value.filter(s => s.gender === '男').length
  const femaleCount = students.value.filter(s => s.gender === '女').length
  
  const option = {
    tooltip: {
      trigger: 'item',
      formatter: '{a} <br/>{b}: {c} ({d}%)'
    },
    legend: {
      top: '5%',
      left: 'center'
    },
    series: [{
      name: '性别分布',
      type: 'pie',
      radius: ['40%', '70%'],
      avoidLabelOverlap: false,
      itemStyle: {
        borderRadius: 10,
        borderColor: '#fff',
        borderWidth: 2
      },
      label: {
        show: true,
        formatter: '{b}: {c}人 ({d}%)'
      },
      emphasis: {
        label: {
          show: true,
          fontSize: 16,
          fontWeight: 'bold'
        }
      },
      data: [
        { value: maleCount, name: '男' },
        { value: femaleCount, name: '女' }
      ],
      color: ['#409eff', '#f72a84']
    }]
  }
  
  genderPieChartInstance.setOption(option)
}

// 更新成绩分布柱状图（按性别）
function updateScoreBarChart() {
  if (!scoreBarChart.value) return
  
  if (scoreBarChartInstance) {
    scoreBarChartInstance.dispose()
  }
  
  scoreBarChartInstance = echarts.init(scoreBarChart.value)
  
  // 按性别和成绩分段统计
  const maleStudents = students.value.filter(s => s.gender === '男')
  const femaleStudents = students.value.filter(s => s.gender === '女')
  
  const scoreRanges = ['90-100', '80-89', '70-79', '60-69', '60以下']
  const maleScores = [0, 0, 0, 0, 0]
  const femaleScores = [0, 0, 0, 0, 0]
  
  maleStudents.forEach(s => {
    const score = s.score || 0
    if (score >= 90) maleScores[0]++
    else if (score >= 80) maleScores[1]++
    else if (score >= 70) maleScores[2]++
    else if (score >= 60) maleScores[3]++
    else maleScores[4]++
  })
  
  femaleStudents.forEach(s => {
    const score = s.score || 0
    if (score >= 90) femaleScores[0]++
    else if (score >= 80) femaleScores[1]++
    else if (score >= 70) femaleScores[2]++
    else if (score >= 60) femaleScores[3]++
    else femaleScores[4]++
  })
  
  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      }
    },
    legend: {
      data: ['男', '女']
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: scoreRanges
    },
    yAxis: {
      type: 'value',
      name: '人数'
    },
    series: [
      {
        name: '男',
        type: 'bar',
        data: maleScores,
        itemStyle: {
          color: '#409eff'
        }
      },
      {
        name: '女',
        type: 'bar',
        data: femaleScores,
        itemStyle: {
          color: '#f72a84'
        }
      }
    ]
  }
  
  scoreBarChartInstance.setOption(option)
}

// 更新成绩分布饼状图（按性别）
function updateScorePieChart() {
  if (!scorePieChart.value) return
  
  if (scorePieChartInstance) {
    scorePieChartInstance.dispose()
  }
  
  scorePieChartInstance = echarts.init(scorePieChart.value)
  
  // 按性别统计平均成绩
  const maleStudents = students.value.filter(s => s.gender === '男' && s.score !== null && s.score !== undefined)
  const femaleStudents = students.value.filter(s => s.gender === '女' && s.score !== null && s.score !== undefined)
  
  const maleAvgScore = maleStudents.length > 0 
    ? maleStudents.reduce((sum, s) => sum + s.score, 0) / maleStudents.length 
    : 0
  const femaleAvgScore = femaleStudents.length > 0 
    ? femaleStudents.reduce((sum, s) => sum + s.score, 0) / femaleStudents.length 
    : 0
  
  const option = {
    tooltip: {
      trigger: 'item',
      formatter: '{a} <br/>{b}: {c}分 ({d}%)'
    },
    legend: {
      top: '5%',
      left: 'center'
    },
    series: [{
      name: '平均成绩',
      type: 'pie',
      radius: ['40%', '70%'],
      avoidLabelOverlap: false,
      itemStyle: {
        borderRadius: 10,
        borderColor: '#fff',
        borderWidth: 2
      },
      label: {
        show: true,
        formatter: '{b}: {c:.1f}分'
      },
      emphasis: {
        label: {
          show: true,
          fontSize: 16,
          fontWeight: 'bold'
        }
      },
      data: [
        { value: maleAvgScore, name: '男' },
        { value: femaleAvgScore, name: '女' }
      ],
      color: ['#409eff', '#f72a84']
    }]
  }
  
  scorePieChartInstance.setOption(option)
}

// 监听任务选择变化
watch(selectedTaskId, (newVal) => {
  if (newVal) {
    load()
  }
})

// 获取状态类型
function getStatusType(status) {
  const map = {
    'SUBMITTED': 'success',
    'NOT_SUBMITTED': 'info',
    'GRADED': 'warning'
  }
  return map[status] || 'info'
}

// 获取状态文本
function getStatusText(status) {
  const map = {
    'SUBMITTED': '已提交',
    'NOT_SUBMITTED': '未提交',
    'GRADED': '已批阅'
  }
  return map[status] || status
}

// 生成报表
function generateReport() {
  if (!selectedTaskId.value) {
    ElMessage.warning('请先选择实验任务')
    return
  }
  
  const task = taskList.value.find(t => t.id === selectedTaskId.value)
  if (!task) return
  
  // 生成报表内容
  let reportContent = `实验任务提交报表\n\n`
  reportContent += `任务名称：${task.title}\n`
  reportContent += `生成时间：${new Date().toLocaleString('zh-CN')}\n`
  reportContent += `====================================\n\n`
  
  reportContent += `提交统计：\n`
  reportContent += `总人数：${stats.total}\n`
  reportContent += `已提交人数：${stats.submittedCount}\n`
  reportContent += `已批阅人数：${stats.gradedCount}\n`
  reportContent += `提交率：${stats.submissionRate.toFixed(1)}%\n\n`
  
  reportContent += `提交记录详情：\n`
  reportContent += `------------------------------------\n`
  reportContent += `姓名\t性别\t状态\t提交时间\t成绩\t评语\n`
  reportContent += `------------------------------------\n`
  
  students.value.forEach(student => {
    reportContent += `${student.realName}\t${student.gender}\t${getStatusText(student.status)}\t${student.submitTime || '-'}\t${student.score || '-'}\t${student.remark || '-'}\n`
  })
  
  reportContent += `====================================\n`
  
  // 创建下载链接
  const blob = new Blob([reportContent], { type: 'text/plain;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `提交报表_${task.title}_${new Date().toISOString().split('T')[0]}.txt`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
  
  ElMessage.success('报表生成成功')
}

// 更新提交状态
async function updateSubmissionStatus(student) {
  try {
    console.log('更新提交状态:', student)
    
    if (student.id) {
      // 如果有 id，调用 update API
      await updateRecord({
        id: student.id,
        status: student.status
      })
    } else {
      // 如果没有 id，需要创建记录
      ElMessage.warning('该学生暂无提交记录')
    }
    ElMessage.success('状态更新成功')
    // 重新加载数据以更新统计信息
    await load()
  } catch (error) {
    console.error('更新状态失败', error)
    ElMessage.error('状态更新失败')
  }
}

// 窗口大小变化时重新调整图表
window.addEventListener('resize', () => {
  genderBarChartInstance?.resize()
  genderPieChartInstance?.resize()
  scoreBarChartInstance?.resize()
  scorePieChartInstance?.resize()
  attendanceRateChartInstance?.resize()
})

onMounted(() => {
  loadTasks()
})
</script>

<style scoped>
.student-stats {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.empty-state {
  padding: 60px 0;
  text-align: center;
}

.attendance-list {
  margin-bottom: 20px;
}

.chart-container {
  margin-bottom: 20px;
}

.chart-title {
  font-size: 14px;
  font-weight: 500;
}

.stats-footer {
  margin-top: 20px;
}

.stat-card {
  height: 100px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
}

.stat-label {
  font-size: 14px;
  color: #606266;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: #409eff;
}
</style>
