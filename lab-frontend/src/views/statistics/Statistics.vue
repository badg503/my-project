<template>
  <div class="statistics">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>数据统计</span>
          <el-button type="primary" @click="loadAllStats">刷新数据</el-button>
        </div>
      </template>
      
      <!-- AI 建议栏 -->
      <div v-if="aiSuggestions.length > 0" class="mb-4">
        <el-card shadow="hover" class="ai-suggestions-card">
          <template #header>
            <div class="card-header">
              <span>💡 AI 数据分析建议</span>
              <el-button type="primary" link @click="showAISuggestions" :loading="aiLoading">
                <el-icon><i-ep-refresh /></el-icon>
                刷新建议
              </el-button>
            </div>
          </template>
          <div class="ai-suggestions-content">
            <el-alert
              v-for="(suggestion, index) in aiSuggestions"
              :key="index"
              :title="suggestion"
              type="warning"
              :closable="false"
              show-icon
              class="mb-2"
            />
          </div>
        </el-card>
      </div>

      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="实验室使用率" name="lab-usage">
          <div class="p-4">
            <div class="date-filter mb-4">
              <el-select v-model="selectedYear" placeholder="选择年" style="width: 120px; margin-right: 10px" @change="loadLabUsage">
                <el-option v-for="y in yearOptions" :key="y" :label="y + '年'" :value="y" />
              </el-select>
              <el-select v-model="selectedMonth" placeholder="选择月" style="width: 100px; margin-right: 10px" @change="loadLabUsage">
                <el-option v-for="m in monthOptions" :key="m" :label="m + '月'" :value="m" />
              </el-select>
              <el-select v-model="selectedDay" placeholder="选择日" style="width: 100px; margin-right: 10px" @change="loadLabUsage">
                <el-option v-for="d in dayOptions" :key="d" :label="d + '日'" :value="d" />
              </el-select>
              <el-select v-model="selectedRange" placeholder="快捷选项" style="width: 120px; margin-right: 10px" @change="handleRangeChange">
                <el-option label="今日" value="today" />
                <el-option label="本周" value="week" />
                <el-option label="本月" value="month" />
                <el-option label="本年" value="year" />
                <el-option label="自定义" value="custom" />
              </el-select>
              <el-date-picker
                v-if="selectedRange === 'custom'"
                v-model="dateRange"
                type="daterange"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                style="margin-left: 10px"
                @change="loadLabUsage"
              />
            </div>
            <div v-loading="loading" class="h-80">
              <div ref="labUsageChart" style="width: 100%; height: 100%"></div>
            </div>
            <el-table :data="labUsageTableData" style="width: 100%" class="mt-4">
              <el-table-column prop="labName" label="实验室" />
              <el-table-column prop="usageRate" label="使用率">
                <template #default="scope">
                  {{ scope.row.usageRate }}%
                </template>
              </el-table-column>
              <el-table-column prop="reserveCount" label="预约次数" />
            </el-table>
          </div>
        </el-tab-pane>
        
        <el-tab-pane label="设备故障率" name="device-failure">
          <div class="p-4">
            <div v-loading="loading" class="h-80">
              <div ref="deviceFailureChart" style="width: 100%; height: 100%"></div>
            </div>
            <el-table :data="deviceFailureTableData" style="width: 100%" class="mt-4">
              <el-table-column prop="status" label="设备状态" />
              <el-table-column prop="count" label="设备数量" />
            </el-table>
          </div>
        </el-tab-pane>
        
        <el-tab-pane label="学生出勤率" name="attendance">
          <div class="p-4">
            <div class="flex gap-2 mb-4 items-center">
              <el-select v-model="selectedRange" @change="handleRangeChange" style="width: 120px">
                <el-option label="今日" value="today" />
                <el-option label="本周" value="week" />
                <el-option label="本月" value="month" />
                <el-option label="本年" value="year" />
                <el-option label="自定义" value="custom" />
              </el-select>
              
              <el-select v-model="selectedYear" @change="handleRangeChange" style="width: 100px">
                <el-option
                  v-for="year in yearOptions"
                  :key="year"
                  :label="year + '年'"
                  :value="year"
                />
              </el-select>
              
              <el-select v-model="selectedMonth" @change="handleRangeChange" style="width: 80px">
                <el-option
                  v-for="month in monthOptions"
                  :key="month"
                  :label="month + '月'"
                  :value="month"
                />
              </el-select>
              
              <el-select v-model="selectedDay" @change="handleRangeChange" style="width: 80px">
                <el-option
                  v-for="day in dayOptions"
                  :key="day"
                  :label="day + '日'"
                  :value="day"
                />
              </el-select>
              
              <el-date-picker
                v-if="selectedRange === 'custom'"
                v-model="dateRange"
                type="daterange"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                @change="loadAttendance"
                style="margin-left: 10px"
              />
            </div>
            
            <div v-loading="loading" class="h-80">
              <div ref="attendanceChart" style="width: 100%; height: 100%"></div>
            </div>
            <el-table :data="attendanceTableData" style="width: 100%" class="mt-4">
              <el-table-column prop="studentName" label="学生姓名" />
              <el-table-column prop="attended" label="出勤次数" />
              <el-table-column prop="total" label="总次数" />
              <el-table-column prop="attendanceRate" label="出勤率">
                <template #default="scope">
                  {{ scope.row.attendanceRate }}%
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-tab-pane>
        
        <el-tab-pane label="维修统计" name="repair-stats">
          <div class="p-4">
            <div v-loading="loading" class="h-80">
              <div ref="repairStatsChart" style="width: 100%; height: 100%"></div>
            </div>
            <el-table :data="repairStatsTableData" style="width: 100%" class="mt-4">
              <el-table-column prop="status" label="状态" />
              <el-table-column prop="count" label="数量" />
            </el-table>
          </div>
        </el-tab-pane>
        
        <el-tab-pane name="ai-analysis">
          <template #label>
            <span>AI 数据分析</span>
          </template>
          <div class="p-4">
            <div v-loading="aiLoading" element-loading-text="正在分析数据，请稍候...">
              <!-- 统计摘要 -->
              <el-card class="mb-4" shadow="hover">
                <template #header>
                  <div class="card-header">
                    <span>📊 实验室运行统计</span>
                  </div>
                </template>
                <el-table :data="aiStatsData" style="width: 100%">
                  <el-table-column prop="labId" label="实验室" width="100" />
                  <el-table-column prop="avgUsageRate" label="平均使用率" width="120">
                    <template #default="scope">
                      <el-tag :type="getUsageRateType(scope.row.avgUsageRate)">
                        {{ scope.row.avgUsageRate }}
                      </el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column prop="totalUsers" label="总用户数" width="100" />
                  <el-table-column prop="totalFaults" label="故障次数" width="100" />
                  <el-table-column prop="anomalyCount" label="异常天数" width="100" />
                </el-table>
              </el-card>

              <!-- 未来 7 天预测 -->
              <el-card class="mb-4" shadow="hover">
                <template #header>
                  <div class="card-header">
                    <span>🔮 未来 7 天使用率预测</span>
                  </div>
                </template>
                <el-table :data="aiForecastsData" style="width: 100%">
                  <el-table-column prop="date" label="日期" width="120" />
                  <el-table-column prop="lab1Forecast" label="实验室 1 预测值" width="150">
                    <template #default="scope">
                      {{ scope.row.lab1Forecast }}
                    </template>
                  </el-table-column>
                  <el-table-column prop="lab2Forecast" label="实验室 2 预测值" width="150">
                    <template #default="scope">
                      {{ scope.row.lab2Forecast }}
                    </template>
                  </el-table-column>
                  <el-table-column prop="lab3Forecast" label="实验室 3 预测值" width="150">
                    <template #default="scope">
                      {{ scope.row.lab3Forecast }}
                    </template>
                  </el-table-column>
                </el-table>
              </el-card>

              <!-- 决策建议 -->
              <el-card shadow="hover">
                <template #header>
                  <div class="card-header">
                    <span>💡 决策建议</span>
                  </div>
                </template>
                <el-alert
                  v-for="(suggestion, index) in aiSuggestions"
                  :key="index"
                  :title="suggestion"
                  type="warning"
                  :closable="false"
                  show-icon
                  class="mb-2"
                />
                <div v-if="aiSuggestions.length === 0" class="text-center text-gray-500">
                  各实验室运行正常，无需特别调整
                </div>
              </el-card>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { getLabUsage, getDeviceFailure, getAttendance, getRepairStats } from '@/api/statistics'
import { getAIAnalysis } from '@/api/ai'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'

const activeTab = ref('lab-usage')
const loading = ref(false)
const dateRange = ref([])

// 日期选择器
const currentYear = new Date().getFullYear()
const currentMonth = new Date().getMonth() + 1
const currentDay = new Date().getDate()
const selectedYear = ref(currentYear)
const selectedMonth = ref(currentMonth)
const selectedDay = ref(currentDay)
const selectedRange = ref('month')
const yearOptions = ref(Array.from({ length: 5 }, (_, i) => currentYear - 2 + i))
const monthOptions = ref(Array.from({ length: 12 }, (_, i) => i + 1))
const dayOptions = ref(Array.from({ length: 31 }, (_, i) => i + 1))

// AI 建议相关
const aiLoading = ref(false)
const aiStatsData = ref([])
const aiForecastsData = ref([])
const aiSuggestions = ref([])

// 图表引用
const labUsageChart = ref(null)
const deviceFailureChart = ref(null)
const attendanceChart = ref(null)
const repairStatsChart = ref(null)

// 图表实例
let labUsageChartInstance = null
let deviceFailureChartInstance = null
let attendanceChartInstance = null
let repairStatsChartInstance = null

// 实验室使用率数据
const labUsageData = ref([])
const labUsageTableData = ref([])

// 设备故障率数据
const deviceFailureData = ref([])
const deviceFailureTableData = ref([])

// 学生出勤率数据
const attendanceData = ref([])
const attendanceTableData = ref([])

// 维修统计数据
const repairStatsData = ref([])
const repairStatsTableData = ref([])

// 更新实验室使用率图表
const updateLabUsageChart = () => {
  if (!labUsageChart.value) return
  
  if (labUsageChartInstance) {
    labUsageChartInstance.dispose()
  }
  
  labUsageChartInstance = echarts.init(labUsageChart.value)
  
  const option = {
    title: {
      text: '实验室使用率',
      left: 'center'
    },
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
      data: labUsageData.value.map(item => item.labName)
    },
    yAxis: {
      type: 'value',
      name: '使用率(%)',
      axisLabel: {
        formatter: '{value}%'
      }
    },
    series: [{
      name: '使用率',
      type: 'bar',
      data: labUsageData.value.map(item => item.usageRate),
      itemStyle: {
        color: '#409eff'
      },
      label: {
        show: true,
        position: 'top',
        formatter: '{c}%'
      }
    }]
  }
  
  labUsageChartInstance.setOption(option)
}

// 更新设备故障分布图表
const updateDeviceFailureChart = () => {
  if (!deviceFailureChart.value) return
  
  if (deviceFailureChartInstance) {
    deviceFailureChartInstance.dispose()
  }
  
  deviceFailureChartInstance = echarts.init(deviceFailureChart.value)
  
  const option = {
    title: {
      text: '设备故障分布',
      left: 'center'
    },
    tooltip: {
      trigger: 'item',
      formatter: '{a} <br/>{b}: {c} ({d}%)'
    },
    legend: {
      orient: 'vertical',
      left: 'left'
    },
    series: [{
      name: '设备状态',
      type: 'pie',
      radius: '50%',
      data: deviceFailureData.value,
      emphasis: {
        itemStyle: {
          shadowBlur: 10,
          shadowOffsetX: 0,
          shadowColor: 'rgba(0, 0, 0, 0.5)'
        }
      }
    }]
  }
  
  deviceFailureChartInstance.setOption(option)
}

// 更新学生出勤率图表
const updateAttendanceChart = () => {
  if (!attendanceChart.value) return
  
  if (attendanceChartInstance) {
    attendanceChartInstance.dispose()
  }
  
  attendanceChartInstance = echarts.init(attendanceChart.value)
  
  const option = {
    title: {
      text: '学生出勤率',
      left: 'center'
    },
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
      data: attendanceData.value.map(item => item.studentName),
      axisLabel: {
        rotate: 45
      }
    },
    yAxis: {
      type: 'value',
      name: '出勤率(%)',
      axisLabel: {
        formatter: '{value}%'
      }
    },
    series: [{
      name: '出勤率',
      type: 'bar',
      data: attendanceData.value.map(item => item.attendanceRate),
      itemStyle: {
        color: '#67c23a'
      },
      label: {
        show: true,
        position: 'top',
        formatter: '{c}%'
      }
    }]
  }
  
  attendanceChartInstance.setOption(option)
}

// 更新维修状态分布图表
const updateRepairStatsChart = () => {
  if (!repairStatsChart.value) return
  
  if (repairStatsChartInstance) {
    repairStatsChartInstance.dispose()
  }
  
  repairStatsChartInstance = echarts.init(repairStatsChart.value)
  
  const option = {
    title: {
      text: '维修状态分布',
      left: 'center'
    },
    tooltip: {
      trigger: 'item',
      formatter: '{a} <br/>{b}: {c} ({d}%)'
    },
    legend: {
      orient: 'vertical',
      left: 'left'
    },
    series: [{
      name: '维修状态',
      type: 'pie',
      radius: '50%',
      data: repairStatsData.value,
      emphasis: {
        itemStyle: {
          shadowBlur: 10,
          shadowOffsetX: 0,
          shadowColor: 'rgba(0, 0, 0, 0.5)'
        }
      }
    }]
  }
  
  repairStatsChartInstance.setOption(option)
}

// 处理快捷选项变化
const handleRangeChange = () => {
  if (selectedRange.value === 'custom') {
    // 自定义模式，不自动加载
    return
  }
  loadLabUsage()
}

const loadLabUsage = async () => {
  loading.value = true
  try {
    // 根据选择的日期类型计算开始和结束日期
    let startDate, endDate
    
    if (selectedRange.value === 'custom') {
      // 自定义日期范围
      const [start, end] = dateRange.value || []
      startDate = start
      endDate = end
    } else if (selectedRange.value === 'today') {
      // 今日
      startDate = new Date(selectedYear.value, selectedMonth.value - 1, selectedDay.value).toISOString().split('T')[0]
      endDate = startDate
    } else if (selectedRange.value === 'week') {
      // 本周（从本周一到本周日）
      const date = new Date(selectedYear.value, selectedMonth.value - 1, selectedDay.value)
      const dayOfWeek = date.getDay() || 7
      const monday = new Date(date)
      monday.setDate(date.getDate() - dayOfWeek + 1)
      const sunday = new Date(monday)
      sunday.setDate(monday.getDate() + 6)
      startDate = monday.toISOString().split('T')[0]
      endDate = sunday.toISOString().split('T')[0]
    } else if (selectedRange.value === 'month') {
      // 本月
      startDate = new Date(selectedYear.value, selectedMonth.value - 1, 1).toISOString().split('T')[0]
      endDate = new Date(selectedYear.value, selectedMonth.value, 0).toISOString().split('T')[0]
    } else if (selectedRange.value === 'year') {
      // 本年
      startDate = new Date(selectedYear.value, 0, 1).toISOString().split('T')[0]
      endDate = new Date(selectedYear.value, 11, 31).toISOString().split('T')[0]
    } else {
      // 默认：选中的年月日作为单日查询
      startDate = new Date(selectedYear.value, selectedMonth.value - 1, selectedDay.value).toISOString().split('T')[0]
      endDate = startDate
    }
    
    const res = await getLabUsage(startDate, endDate)
    labUsageData.value = res.labUsageData || []
    labUsageTableData.value = res.labUsageData || []
    updateLabUsageChart()
  } catch (error) {
    ElMessage.error('获取实验室使用率失败')
  } finally {
    loading.value = false
  }
}

const loadDeviceFailure = async () => {
  loading.value = true
  try {
    const res = await getDeviceFailure()
    const statusMap = {
      'AVAILABLE': '可用',
      'SCRAP': '报废',
      'REPAIR': '维修中'
    }
    deviceFailureTableData.value = (res.statusFailureData || []).map(item => ({
      status: statusMap[item.status] || item.status,
      count: item.count
    }))
    deviceFailureData.value = (res.statusFailureData || []).map(item => ({
      name: statusMap[item.status] || item.status,
      value: item.count
    }))
    updateDeviceFailureChart()
  } catch (error) {
    ElMessage.error('获取设备故障率失败')
  } finally {
    loading.value = false
  }
}

const loadAttendance = async () => {
  loading.value = true
  try {
    // 根据选择的日期类型计算开始和结束日期
    let startDate, endDate
    
    if (selectedRange.value === 'custom') {
      // 自定义日期范围
      const [start, end] = dateRange.value || []
      startDate = start
      endDate = end
    } else if (selectedRange.value === 'today') {
      // 今日
      startDate = new Date(selectedYear.value, selectedMonth.value - 1, selectedDay.value).toISOString().split('T')[0]
      endDate = startDate
    } else if (selectedRange.value === 'week') {
      // 本周（从本周一到本周日）
      const date = new Date(selectedYear.value, selectedMonth.value - 1, selectedDay.value)
      const dayOfWeek = date.getDay() || 7
      const monday = new Date(date)
      monday.setDate(date.getDate() - dayOfWeek + 1)
      const sunday = new Date(monday)
      sunday.setDate(monday.getDate() + 6)
      startDate = monday.toISOString().split('T')[0]
      endDate = sunday.toISOString().split('T')[0]
    } else if (selectedRange.value === 'month') {
      // 本月
      startDate = new Date(selectedYear.value, selectedMonth.value - 1, 1).toISOString().split('T')[0]
      endDate = new Date(selectedYear.value, selectedMonth.value, 0).toISOString().split('T')[0]
    } else if (selectedRange.value === 'year') {
      // 本年
      startDate = new Date(selectedYear.value, 0, 1).toISOString().split('T')[0]
      endDate = new Date(selectedYear.value, 11, 31).toISOString().split('T')[0]
    } else {
      // 默认：选中的年月日作为单日查询
      startDate = new Date(selectedYear.value, selectedMonth.value - 1, selectedDay.value).toISOString().split('T')[0]
      endDate = startDate
    }
    
    const res = await getAttendance(startDate, endDate)
    attendanceData.value = res.attendanceData || []
    attendanceTableData.value = res.attendanceData || []
    updateAttendanceChart()
  } catch (error) {
    ElMessage.error('获取学生出勤率失败')
  } finally {
    loading.value = false
  }
}

const loadRepairStats = async () => {
  loading.value = true
  try {
    const res = await getRepairStats()
    const statusMap = {
      'PENDING': '待处理',
      'PROCESSING': '处理中',
      'FIXED': '已修复',
      'CLOSED': '已关闭'
    }
    repairStatsTableData.value = Object.entries(res.statusCount || {}).map(([status, count]) => ({
      status: statusMap[status] || status,
      count
    }))
    repairStatsData.value = Object.entries(res.statusCount || {}).map(([status, count]) => ({
      name: statusMap[status] || status,
      value: count
    }))
    updateRepairStatsChart()
  } catch (error) {
    ElMessage.error('获取维修统计失败')
  } finally {
    loading.value = false
  }
}

const handleTabChange = (tab) => {
  switch (tab) {
    case 'lab-usage':
      loadLabUsage()
      break
    case 'device-failure':
      loadDeviceFailure()
      break
    case 'attendance':
      loadAttendance()
      break
    case 'repair-stats':
      loadRepairStats()
      break
    case 'ai-analysis':
      // AI 分析标签页，自动加载数据
      showAISuggestions()
      break
  }
}

// 获取使用率标签类型
const getUsageRateType = (usageRate) => {
  const rate = parseFloat(usageRate.replace('%', ''))
  if (rate > 80) return 'danger'
  if (rate > 60) return 'warning'
  if (rate > 30) return 'success'
  return 'info'
}

// 显示 AI 建议
const showAISuggestions = async () => {
  aiLoading.value = true
  
  try {
    const res = await getAIAnalysis()
    
    // 处理统计数据
    if (res.statistics) {
      aiStatsData.value = Object.entries(res.statistics).map(([labId, stat]) => ({
        labId: `实验室${labId}`,
        avgUsageRate: `${(stat.avg_usage_rate * 100).toFixed(1)}%`,
        totalUsers: stat.total_users || 0,
        totalFaults: stat.faults?.total_faults || 0,
        anomalyCount: stat.anomalies?.anomaly_count || 0
      }))
    }
    
    // 处理预测数据
    if (res.forecasts) {
      const dates = res.forecasts[1]?.map(f => {
        const date = new Date(f.ds)
        return date.toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' })
      }) || []
      
      aiForecastsData.value = dates.map((date, index) => ({
        date: `03-${date}`,
        lab1Forecast: res.forecasts[1]?.[index]?.yhat ? `${(res.forecasts[1][index].yhat * 100).toFixed(1)}%` : 'N/A',
        lab2Forecast: res.forecasts[2]?.[index]?.yhat ? `${(res.forecasts[2][index].yhat * 100).toFixed(1)}%` : 'N/A',
        lab3Forecast: res.forecasts[3]?.[index]?.yhat ? `${(res.forecasts[3][index].yhat * 100).toFixed(1)}%` : 'N/A'
      }))
    }
    
    // 处理建议
    aiSuggestions.value = res.suggestions || []
    
  } catch (error) {
    ElMessage.error('获取 AI 分析建议失败')
    console.error('AI 分析错误:', error)
  } finally {
    aiLoading.value = false
  }
}

const loadAllStats = () => {
  switch (activeTab.value) {
    case 'lab-usage':
      loadLabUsage()
      break
    case 'device-failure':
      loadDeviceFailure()
      break
    case 'attendance':
      loadAttendance()
      break
    case 'repair-stats':
      loadRepairStats()
      break
    case 'ai-analysis':
      showAISuggestions()
      break
  }
}

// 窗口大小变化时重新调整图表大小
const handleResize = () => {
  labUsageChartInstance?.resize()
  deviceFailureChartInstance?.resize()
  attendanceChartInstance?.resize()
  repairStatsChartInstance?.resize()
}

onMounted(() => {
  loadLabUsage()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  labUsageChartInstance?.dispose()
  deviceFailureChartInstance?.dispose()
  attendanceChartInstance?.dispose()
  repairStatsChartInstance?.dispose()
})
</script>

<style scoped>
.statistics {
  padding: 20px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.p-4 {
  padding: 16px;
}
.mb-4 {
  margin-bottom: 16px;
}
.mt-4 {
  margin-top: 16px;
}
.h-80 {
  height: 400px;
}
.ai-suggestions-card {
  background: linear-gradient(135deg, #fff7e6 0%, #ffffff 100%);
  border-left: 4px solid #faad14;
}
.ai-suggestions-content {
  max-height: 400px;
  overflow-y: auto;
}
.ml-2 {
  margin-left: 8px;
}
</style>
