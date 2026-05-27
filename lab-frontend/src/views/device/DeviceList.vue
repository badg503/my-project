<template>
  <div>
    <h2 class="page-title">设备管理</h2>
    <el-form inline style="margin-bottom:12px">
      <el-select v-model="query.labId" placeholder="实验室" clearable style="width:160px">
        <el-option v-for="l in labs" :key="l.id" :label="l.name" :value="l.id" />
      </el-select>
      <el-select v-model="query.status" placeholder="状态" clearable style="width:120px">
        <el-option label="可用" value="AVAILABLE" />
        <el-option label="维修中" value="REPAIR" />
        <el-option label="暂不可用" value="UNAVAILABLE" />
        <el-option label="报废" value="SCRAP" />
      </el-select>
      <el-select v-model="query.deviceType" placeholder="设备类型" clearable style="width:160px">
        <el-option label="精密/手动操作类" value="PRECISE_MANUAL" />
        <el-option label="非精密/手动操作类" value="NON_PRECISE_MANUAL" />
        <el-option label="自动/批量类" value="AUTO_BATCH" />
        <el-option label="多人协作类" value="MULTI_USER" />
      </el-select>
      <el-button type="primary" @click="load">查询</el-button>
      <el-button @click="openEdit(null)">新增设备</el-button>
      <el-button type="info" @click="openDeviceRecords">设备记录</el-button>
      <el-button type="warning" @click="batchFaultPredict" :loading="batchPredictLoading">
        <el-icon><Warning /></el-icon>
        故障预测
      </el-button>
    </el-form>
    <el-table :data="sortedList" stripe v-loading="loading">
      <el-table-column prop="id" label="设备ID" width="80" />
      <el-table-column label="实验室" width="180">
        <template #default="{ row }">
          {{ row.labId }} - {{ getLabName(row.labId) }}
        </template>
      </el-table-column>
      <el-table-column prop="name" label="设备名称" />
      <el-table-column label="设备故障描述" show-overflow-tooltip>
        <template #default="{ row }">
          <span v-if="row.status === 'REPAIR'">{{ row.repairDesc || '-' }}</span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column prop="model" label="型号" width="120" />
      <el-table-column prop="deviceType" label="设备类型" width="160">
        <template #default="{ row }">
          {{ getDeviceTypeText(row.deviceType) }}
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 'AVAILABLE' ? 'success' : row.status === 'REPAIR' ? 'warning' : row.status === 'UNAVAILABLE' ? 'info' : 'danger'">
            {{ row.status === 'AVAILABLE' ? '可用' : row.status === 'REPAIR' ? '维修中' : row.status === 'UNAVAILABLE' ? '暂不可用' : '报废' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination v-model:current-page="page.current" v-model:page-size="page.size" :page-sizes="[10, 20, 30, 40, 50]" :total="page.total" layout="total, sizes, prev, pager, next, jumper" @size-change="handleSizeChange" @current-change="handleCurrentChange" style="margin-top:16px" />
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑设备' : '新增设备'" width="500px" @close="form = {}">
      <el-form :model="form" label-width="100px">
        <el-form-item label="所属实验室"><el-select v-model="form.labId" placeholder="请选择" style="width:100%"><el-option v-for="l in labs" :key="l.id" :label="l.name" :value="l.id" /></el-select></el-form-item>
        <el-form-item label="设备名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="型号"><el-input v-model="form.model" /></el-form-item>
        <el-form-item label="设备类型">
          <el-select v-model="form.deviceType" placeholder="请选择" style="width:100%">
            <el-option label="精密/手动操作类" value="PRECISE_MANUAL" />
            <el-option label="非精密/手动操作类" value="NON_PRECISE_MANUAL" />
            <el-option label="自动/批量类" value="AUTO_BATCH" />
            <el-option label="多人协作类" value="MULTI_USER" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width:100%">
            <el-option label="可用" value="AVAILABLE" />
            <el-option label="维修中" value="REPAIR" />
            <el-option label="暂不可用" value="UNAVAILABLE" />
            <el-option label="报废" value="SCRAP" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="deviceRecordsVisible" title="设备记录" width="900px">
      <el-form inline style="margin-bottom:12px">
        <el-input v-model="recordsQuery.keyword" placeholder="设备 ID/设备名称" clearable style="width:200px" />
        <el-select v-model="recordsQuery.type" placeholder="记录类型" clearable style="width:120px">
          <el-option label="借用" value="BORROW" />
          <el-option label="报修" value="REPAIR" />
          <el-option label="报废" value="SCRAP" />
          <el-option label="设备不可用" value="LAB_UNAVAILABLE" />
          <el-option label="已取消" value="CANCELLED" />
        </el-select>
        <el-button type="primary" @click="loadDeviceRecords">查询</el-button>
      </el-form>
      <el-table :data="deviceRecords" stripe v-loading="recordsLoading">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="deviceId" label="设备 ID" width="80" />
        <el-table-column prop="deviceName" label="设备名称" width="120" />
        <el-table-column prop="userId" label="用户 ID" width="80" />
        <el-table-column prop="userName" label="用户名称" width="100" />
        <el-table-column prop="type" label="记录类型" width="90">
          <template #default="{ row }">
            <el-tag :type="getRecordTypeTag(row.type)">{{ getRecordTypeText(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" show-overflow-tooltip />
        <el-table-column prop="createTime" label="创建时间" width="160" />
      </el-table>
      <el-pagination v-model:current-page="recordsPage.current" v-model:page-size="recordsPage.size" :total="recordsPage.total" layout="total, prev, pager, next" @current-change="loadDeviceRecords" style="margin-top:16px" />
      <template #footer>
        <el-button @click="deviceRecordsVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 批量故障预测进度对话框 -->
    <el-dialog v-model="progressVisible" title="故障预测进行中" width="500px" :close-on-click-modal="false" :show-close="false">
      <div class="progress-content">
        <p>正在预测设备故障概率...</p>
        
        <el-progress 
          :percentage="progressPercent"
          :status="progressStatus"
        />
        
        <div class="progress-info">
          <p>设备进度：{{ processedDevices }} / {{ totalDevices }}</p>
          <p v-if="estimatedTime > 0">预估时间：{{ estimatedTime }} 秒</p>
          <p v-if="remainingTime > 0">
            剩余时间：{{ remainingTime }} 秒
          </p>
        </div>
        
        <p class="tips">
          💡 提示：预测过程中请勿关闭页面
        </p>
      </div>
    </el-dialog>

    <!-- 批量故障预测结果对话框 -->
    <el-dialog v-model="batchPredictVisible" title="设备故障预测结果" width="900px" :close-on-click-modal="false">
      <div v-loading="batchPredictLoading" element-loading-text="正在对所有设备进行故障预测，请稍候...">
        <el-alert
          v-if="batchPredictResult"
          :title="`检测完成！共预测 ${batchPredictResult.total} 台设备`"
          :type="batchPredictResult.faultyCount > 0 ? 'warning' : 'success'"
          show-icon
          :closable="false"
          style="margin-bottom: 20px"
        >
          <template #default>
            <div style="font-size: 14px; line-height: 1.8;">
              <p style="margin: 0 0 10px 0;">
                <strong>检测结果：</strong>
                <span v-if="batchPredictResult.faultyCount > 0" style="color: #F56C6C;">
                  发现 <strong>{{ batchPredictResult.faultyCount }}</strong> 台疑似故障设备
                </span>
                <span v-else style="color: #67C23A;">
                  所有设备运行正常，暂无故障风险
                </span>
              </p>
              <p style="margin: 0;">
                <el-tag type="success" size="small">正常设备：{{ batchPredictResult.normalCount }} 台</el-tag>
                <el-tag v-if="batchPredictResult.faultyCount > 0" type="danger" size="small" style="margin-left: 10px;">
                  疑似故障：{{ batchPredictResult.faultyCount }} 台
                </el-tag>
              </p>
            </div>
          </template>
        </el-alert>
        
        <el-table 
          v-if="batchPredictResult && batchPredictResult.faultyCount > 0" 
          :data="batchPredictResult.faultyDevices" 
          stripe 
          border
          :default-sort="{prop: 'faultProb', order: 'descending'}"
        >
          <el-table-column prop="deviceId" label="设备 ID" width="100" />
          <el-table-column prop="deviceName" label="设备名称" width="200" />
          <el-table-column label="实验室" width="180">
            <template #default="{ row }">
              {{ row.labId }} - {{ getLabName(row.labId) }}
            </template>
          </el-table-column>
          <el-table-column prop="faultProb" label="故障概率" width="120" sortable>
            <template #default="{ row }">
              <el-tag :type="row.faultProb >= 0.8 ? 'danger' : row.faultProb >= 0.6 ? 'warning' : 'info'">
                {{ (row.faultProb * 100).toFixed(2) }}%
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="threshold" label="预警阈值" width="100">
            <template #default="{ row }">
              {{ (row.threshold * 100).toFixed(2) }}%
            </template>
          </el-table-column>
          <el-table-column prop="message" label="分析结果" show-overflow-tooltip />
          <el-table-column prop="suggestion" label="建议" show-overflow-tooltip />
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="viewDeviceDetail(row.deviceId)">查看详情</el-button>
            </template>
          </el-table-column>
        </el-table>
        
        <el-empty v-else-if="batchPredictResult" description="没有发现疑似故障的设备，所有设备运行正常" />
      </div>
      <template #footer>
        <el-button @click="batchPredictVisible = false">关闭</el-button>
        <el-button type="primary" @click="exportFaultReport" v-if="batchPredictResult && batchPredictResult.faultyCount > 0">
          导出报告
        </el-button>
      </template>
    </el-dialog>

    <!-- 预测结果查看对话框 -->
    <el-dialog v-model="predictionResultsVisible" title="故障预测结果" width="1200px" :close-on-click-modal="false">
      
      <!-- 筛选器 -->
      <div style="margin-bottom: 15px; display: flex; gap: 10px; align-items: center;">
        <el-input 
          v-model="searchDeviceName" 
          placeholder="搜索设备名称..." 
          clearable
          style="width: 200px"
          prefix-icon="Search"
        />
        <el-select 
          v-model="searchFaultLevel" 
          placeholder="故障等级筛选" 
          clearable
          style="width: 120px"
        >
          <el-option label="极高 (≥90%)" value="极高" />
          <el-option label="高 (60%-90%)" value="高" />
          <el-option label="中 (10%-60%)" value="中" />
          <el-option label="低 (<10%)" value="低" />
        </el-select>
        <el-select 
          v-model="searchStatus" 
          placeholder="状态筛选" 
          clearable
          style="width: 100px"
        >
          <el-option label="正常" value="正常" />
          <el-option label="故障" value="故障" />
          <el-option label="无数据" value="无数据" />
        </el-select>
        <el-button @click="resetFilters" size="small">重置</el-button>
        
        <!-- 统计信息 -->
        <el-tag type="info" style="margin-left: auto">
          总计：{{ manualResults.length }} 台设备
        </el-tag>
        <el-tag type="danger" style="margin-left: 5px">
          极高：{{ highRiskCount }} 台
        </el-tag>
        <el-tag type="warning" style="margin-left: 5px">
          高：{{ mediumRiskCount }} 台
        </el-tag>
        <el-tag style="margin-left: 5px">
          中：{{ lowRiskCount }} 台
        </el-tag>
        <el-tag type="success" style="margin-left: 5px">
          低：{{ normalRiskCount }} 台
        </el-tag>
      </div>
      
      <!-- 定时预测结果（今天） -->
      <div style="margin-bottom: 20px;">
        <el-alert 
          title="定时预测结果（今日）"
          type="info"
          :closable="false"
          show-icon
          style="margin-bottom: 10px"
        />
        <el-table :data="filteredScheduledResults" v-loading="resultsLoading" stripe size="small">
          <el-table-column prop="device_name" label="设备名称" width="180" />
          <el-table-column label="实验室" width="160">
            <template #default="{ row }">
              {{ getLabName(row.lab_id) }}
            </template>
          </el-table-column>
          <el-table-column label="故障概率" width="100">
            <template #default="{ row }">
              <el-tag 
                :type="getFaultProbabilityType(row.fault_probability)" 
                size="small">
                {{ (row.fault_probability * 100).toFixed(1) }}%
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="故障等级" width="80">
            <template #default="{ row }">
              <el-tag 
                :type="getFaultLevelType(row.fault_probability)" 
                size="small">
                {{ getFaultLevel(row.fault_probability) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="80">
            <template #default="{ row }">
              <el-tag :type="row.warning ? 'warning' : row.is_faulty === 1 ? 'danger' : 'success'" size="small">
                <span v-if="row.warning">无数据</span>
                <span v-else-if="row.is_faulty === 1">故障</span>
                <span v-else>正常</span>
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="message" label="分析结果" show-overflow-tooltip />
          <el-table-column prop="predict_time" label="预测时间" width="160" />
        </el-table>
        <el-empty v-if="filteredScheduledResults.length === 0 && scheduledResults.length > 0" description="没有符合条件的定时预测结果" />
      </div>
      
      <!-- 手动预测结果（最新） -->
      <div>
        <el-alert 
          title="手动预测结果（最新）"
          type="success"
          :closable="false"
          show-icon
          style="margin-bottom: 10px"
        />
        <el-table :data="filteredManualResults" v-loading="resultsLoading" stripe size="small">
          <el-table-column prop="device_name" label="设备名称" width="180" />
          <el-table-column label="实验室" width="160">
            <template #default="{ row }">
              {{ getLabName(row.lab_id) }}
            </template>
          </el-table-column>
          <el-table-column label="故障概率" width="100">
            <template #default="{ row }">
              <el-tag 
                :type="getFaultProbabilityType(row.fault_probability)" 
                size="small">
                {{ (row.fault_probability * 100).toFixed(1) }}%
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="故障等级" width="80">
            <template #default="{ row }">
              <el-tag 
                :type="getFaultLevelType(row.fault_probability)" 
                size="small">
                {{ getFaultLevel(row.fault_probability) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="80">
            <template #default="{ row }">
              <el-tag :type="row.warning ? 'warning' : row.is_faulty === 1 ? 'danger' : 'success'" size="small">
                <span v-if="row.warning">无数据</span>
                <span v-else-if="row.is_faulty === 1">故障</span>
                <span v-else>正常</span>
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="message" label="分析结果" show-overflow-tooltip />
          <el-table-column prop="predict_time" label="预测时间" width="160" />
        </el-table>
        <el-empty v-if="filteredManualResults.length === 0 && manualResults.length > 0" description="没有符合条件的手动预测结果" />
      </div>
      
      <template #footer>
        <el-button @click="predictionResultsVisible = false">关闭</el-button>
      </template>
    </el-dialog>

  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { getDevicePage, addDevice, updateDevice, getDeviceRecords, getRepairDesc } from '@/api/lab'
import { getLabList } from '@/api/lab'
import { faultPredict, getTodayPredictions, batchFaultPredict as apiBatchFaultPredict, getPredictionProgress, getPredictionResults } from '@/api/ai'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Warning } from '@element-plus/icons-vue'

const list = ref([])
const labs = ref([])
const loading = ref(false)
const query = reactive({ labId: null, status: '', deviceType: '' })
const page = reactive({ current: 1, size: 10, total: 0 })
const dialogVisible = ref(false)
const form = ref({})

const deviceRecordsVisible = ref(false)
const deviceRecords = ref([])
const recordsLoading = ref(false)
const recordsQuery = reactive({ keyword: '', type: '' })
const recordsPage = reactive({ current: 1, size: 10, total: 0 })

// 故障预测相关
const batchPredictVisible = ref(false)
const batchPredictLoading = ref(false)
const batchPredictResult = ref(null)

// 预测结果对话框相关
const predictionResultsVisible = ref(false)
const resultsLoading = ref(false)
const scheduledResults = ref([])  // 定时预测结果
const manualResults = ref([])      // 手动预测结果

// 筛选器相关
const searchDeviceName = ref('')
const searchFaultLevel = ref('')
const searchStatus = ref('')

// 计算属性：筛选后的结果
const filteredScheduledResults = computed(() => {
  return scheduledResults.value.filter(item => {
    // 设备名称筛选
    if (searchDeviceName.value && !item.device_name.includes(searchDeviceName.value)) {
      return false
    }
    // 故障等级筛选
    if (searchFaultLevel.value && getFaultLevel(item.fault_probability) !== searchFaultLevel.value) {
      return false
    }
    // 状态筛选
    const status = item.warning ? '无数据' : item.is_faulty === 1 ? '故障' : '正常'
    if (searchStatus.value && status !== searchStatus.value) {
      return false
    }
    return true
  })
})

const filteredManualResults = computed(() => {
  return manualResults.value.filter(item => {
    // 设备名称筛选
    if (searchDeviceName.value && !item.device_name.includes(searchDeviceName.value)) {
      return false
    }
    // 故障等级筛选
    if (searchFaultLevel.value && getFaultLevel(item.fault_probability) !== searchFaultLevel.value) {
      return false
    }
    // 状态筛选
    const status = item.warning ? '无数据' : item.is_faulty === 1 ? '故障' : '正常'
    if (searchStatus.value && status !== searchStatus.value) {
      return false
    }
    return true
  })
})

// 统计信息
const highRiskCount = computed(() => {
  return manualResults.value.filter(item => item.fault_probability >= 0.9).length
})

const mediumRiskCount = computed(() => {
  return manualResults.value.filter(item => item.fault_probability >= 0.6 && item.fault_probability < 0.9).length
})

const lowRiskCount = computed(() => {
  return manualResults.value.filter(item => item.fault_probability >= 0.1 && item.fault_probability < 0.6).length
})

const normalRiskCount = computed(() => {
  return manualResults.value.filter(item => item.fault_probability < 0.1).length
})

function resetFilters() {
  searchDeviceName.value = ''
  searchFaultLevel.value = ''
  searchStatus.value = ''
}

// 进度相关
const progressVisible = ref(false)
const progressPercent = ref(0)
const progressStatus = ref('')
const processedDevices = ref(0)
const totalDevices = ref(0)
const estimatedTime = ref(0)
const remainingTime = ref(0)
let progressTimer = null
let currentTaskId = null

const sortedList = computed(() => {
  return [...list.value].sort((a, b) => a.id - b.id)
})

function getLabName(labId) {
  const lab = labs.value.find(l => l.id === labId)
  return lab ? lab.name : '未知实验室'
}

// 获取故障概率标签类型
function getFaultProbabilityType(probability) {
  if (!probability || isNaN(probability)) return 'info'
  if (probability >= 0.8) return 'danger'
  if (probability >= 0.6) return 'warning'
  return 'info'
}

// 获取故障等级（高/中/低/正常）
function getFaultLevel(probability) {
  if (!probability || isNaN(probability)) return '无数据'
  if (probability >= 0.9) return '极高'
  if (probability >= 0.6) return '高'
  if (probability >= 0.1) return '中'
  return '低'
}

// 获取故障等级标签类型
function getFaultLevelType(probability) {
  if (!probability || isNaN(probability)) return 'info'
  if (probability >= 0.9) return 'danger'  // 极高 - 红色
  if (probability >= 0.6) return 'warning' // 高 - 橙色
  if (probability >= 0.1) return ''        // 中 - 默认
  return 'success'                         // 低 - 绿色
}

function getRecordTypeTag(type) {
  const map = { BORROW: 'success', REPAIR: 'warning', SCRAP: 'danger', LAB_UNAVAILABLE: 'info', CANCELLED: 'danger' }
  return map[type] || 'info'
}

function getRecordTypeText(type) {
  const map = { BORROW: '借用', REPAIR: '报修', SCRAP: '报废', LAB_UNAVAILABLE: '设备不可用', CANCELLED: '已取消' }
  return map[type] || type
}

function getDeviceTypeText(deviceType) {
  const map = { 
    PRECISE_MANUAL: '精密/手动操作类', 
    NON_PRECISE_MANUAL: '非精密/手动操作类', 
    AUTO_BATCH: '自动/批量类', 
    MULTI_USER: '多人协作类' 
  }
  return map[deviceType] || ''
}

// 故障预测相关函数
async function batchFaultPredict() {
  // 不再强制要求选择实验室，不传 labId 则预测所有设备
  const selectedLabId = query.labId
  
  batchPredictLoading.value = true
  
  try {
    // 调用批量预测 API
    const result = await apiBatchFaultPredict(selectedLabId || null)
    
    if (result.code === 200 && result.data) {
      const data = result.data
      
      // 等待预测完成（轮询检查进度）
      const taskId = data.taskId
      if (taskId) {
        await waitForPredictionComplete(taskId)
      }
      
      // 预测完成后打开结果对话框
      await openPredictionResults()
    } else {
      // 即使返回失败，也打开对话框查看结果
      console.warn('预测返回失败:', result.message)
      await openPredictionResults()
    }
  } catch (e) {
    console.error('批量故障预测异常:', e)
    // 即使出错也打开对话框，让用户看到现有结果
    await openPredictionResults()
  } finally {
    batchPredictLoading.value = false
  }
}

// 等待预测完成
async function waitForPredictionComplete(taskId) {
  return new Promise((resolve, reject) => {
    const maxAttempts = 30 // 最多尝试 30 次
    const interval = 2000 // 每 2 秒检查一次
    let attempts = 0
    
    const checkProgress = async () => {
      try {
        attempts++
        const progressResult = await getPredictionProgress(taskId)
        
        if (progressResult.code === 200 && progressResult.data) {
          const progress = progressResult.data
          
          // 检查是否完成
          if (progress.status === 'COMPLETED' || progress.status === 'FAILED') {
            resolve(progress)
            return
          }
          
          // 如果超过最大尝试次数，也认为完成
          if (attempts >= maxAttempts) {
            console.log('⏰ 达到最大等待次数，认为预测已完成')
            resolve(progress)
            return
          }
          
          // 继续等待
          setTimeout(checkProgress, interval)
        } else {
          reject(new Error('查询进度失败'))
        }
      } catch (e) {
        console.error('查询预测进度失败:', e)
        // 查询失败也继续等待
        if (attempts >= maxAttempts) {
          resolve(null)
        } else {
          setTimeout(checkProgress, interval)
        }
      }
    }
    
    // 开始检查
    checkProgress()
  })
}

// 打开预测结果查看对话框
async function openPredictionResults() {
  predictionResultsVisible.value = true
  await loadPredictionResults()
}

// 加载预测结果
async function loadPredictionResults() {
  resultsLoading.value = true
  
  try {
    // 设备管理页面显示所有实验室的预测结果，不限制 labId
    // 获取今天的日期
    const today = new Date().toISOString().split('T')[0]
    
    // 获取定时预测结果（今天）- 查询所有实验室
    const scheduledResult = await getPredictionResults(null, 'SCHEDULED', today, 1, 100)
    if (scheduledResult && scheduledResult.records) {
      scheduledResults.value = scheduledResult.records
    } else {
      scheduledResults.value = []
    }
    
    // 获取手动预测结果（最新）- 查询所有实验室
    const manualResult = await getPredictionResults(null, 'MANUAL', undefined, 1, 100)
    if (manualResult && manualResult.records) {
      manualResults.value = manualResult.records
    } else {
      manualResults.value = []
    }
    
    // 检查是否完全没有数据
    if (scheduledResults.value.length === 0 && manualResults.value.length === 0) {
      // 显示空数据提示
      ElMessage.info('暂无预测数据，请点击"故障预测"按钮进行预测')
    }
  } catch (e) {
    console.error('查询预测结果失败:', e)
    ElMessage.error('查询预测结果失败：' + e.message)
  } finally {
    resultsLoading.value = false
  }
}

function viewDeviceDetail(deviceId) {
  // 关闭预测对话框，跳转到设备详情或编辑页面
  batchPredictVisible.value = false
  const device = list.value.find(d => d.id === deviceId)
  if (device) {
    openEdit(device)
  }
}

function exportFaultReport() {
  ElMessage.info('导出功能开发中...')
  // TODO: 实现导出功能
}

async function load() {
  loading.value = true
  try {
    const res = await getDevicePage({ 
      current: page.current, 
      size: page.size, 
      labId: query.labId || undefined, 
      status: query.status || undefined,
      deviceType: query.deviceType || undefined
    })
    list.value = res.records || []
    page.total = res.total || 0
    
    // 为维修中的设备加载故障描述
    await loadRepairDescriptions()
  } finally {
    loading.value = false
  }
}

// 加载维修设备的故障描述
async function loadRepairDescriptions() {
  try {
    // 找出所有维修中的设备
    const repairDevices = list.value.filter(device => device.status === 'REPAIR')
    
    // 并行获取每个设备的故障描述
    const promises = repairDevices.map(async (device) => {
      try {
        const repairDesc = await getRepairDesc(device.id)
        // axios 拦截器已经解包了 data，直接返回字符串
        device.repairDesc = repairDesc || '-'
      } catch (e) {
        console.error(`获取设备 ${device.id} 故障描述失败:`, e)
        device.repairDesc = '-'
      }
    })
    
    await Promise.all(promises)
  } catch (e) {
    console.error('加载故障描述失败:', e)
  }
}

async function initLabs() {
  labs.value = await getLabList()
}

function openEdit(row) {
  form.value = row ? { ...row } : { labId: null, name: '', model: '', deviceType: '', status: 'AVAILABLE' }
  dialogVisible.value = true
}

async function submit() {
  if (form.value.id) await updateDevice(form.value)
  else await addDevice(form.value)
  ElMessage.success('保存成功')
  dialogVisible.value = false
  load()
}

async function openDeviceRecords() {
  deviceRecordsVisible.value = true
  loadDeviceRecords()
}

async function loadDeviceRecords() {
  recordsLoading.value = true
  try {
    const res = await getDeviceRecords({
      current: recordsPage.current,
      size: recordsPage.size,
      keyword: recordsQuery.keyword || undefined,
      type: recordsQuery.type || undefined
    })
    deviceRecords.value = res.records || []
    recordsPage.total = res.total || 0
  } finally {
    recordsLoading.value = false
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

// 加载当天的故障预测结果
async function loadTodayPredictions() {
  try {
    const result = await getTodayPredictions()
    
    if (result.code === 200 && result.data) {
      const { tasks, count } = result.data
      
      if (count > 0) {
        // 获取最新的预测任务
        const latestTask = tasks[0]
        
        // 获取该任务的详细预测结果
        // 这里需要根据实际后端接口调整，暂时显示任务信息
        ElMessage.success(`今日已完成 ${count} 次故障预测，共检测 ${latestTask.totalDevices || 0} 台设备`)
        
        // 可以在这里打开预测结果对话框展示详情
        // batchPredictVisible.value = true
      }
    }
  } catch (e) {
    // 静默失败，不影响页面加载
    console.error('加载今日预测失败:', e)
  }
}

onMounted(() => { 
  initLabs()
  load()
  // 加载当天的故障预测结果
  loadTodayPredictions()
})
</script>

<style scoped>
.progress-content {
  text-align: center;
  padding: 20px;
}

.progress-info {
  margin-top: 20px;
  color: #666;
  font-size: 14px;
  line-height: 1.8;
}

.tips {
  margin-top: 15px;
  color: #909399;
  font-size: 12px;
}

.page-title { margin-bottom: 16px; font-size: 18px; }
</style>
