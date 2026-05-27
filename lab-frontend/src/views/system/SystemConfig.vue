<template>
  <div class="system-config">
    <el-tabs v-model="activeTab">
      <!-- 日志管理 -->
      <el-tab-pane label="操作日志" name="logs">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>系统操作日志</span>
              <div>
                <el-button @click="loadLogs">刷新</el-button>
                <el-button type="danger" @click="clearLogs">清空日志</el-button>
              </div>
            </div>
          </template>
          
          <el-form :inline="true" :model="logQueryForm">
            <el-form-item label="操作人">
              <el-input v-model="logQueryForm.username" placeholder="请输入操作人" clearable style="width: 150px" />
            </el-form-item>
            <el-form-item label="操作模块">
              <el-select v-model="logQueryForm.module" placeholder="全部" clearable style="width: 150px">
                <el-option label="实验室管理" value="实验室管理" />
                <el-option label="设备管理" value="设备管理" />
                <el-option label="预约管理" value="预约管理" />
                <el-option label="考勤管理" value="考勤管理" />
                <el-option label="用户管理" value="用户管理" />
                <el-option label="系统设置" value="系统设置" />
              </el-select>
            </el-form-item>
            <el-form-item label="操作类型">
              <el-select v-model="logQueryForm.operationType" placeholder="全部" clearable style="width: 120px">
                <el-option label="新增" value="新增" />
                <el-option label="修改" value="修改" />
                <el-option label="删除" value="删除" />
                <el-option label="查询" value="查询" />
              </el-select>
            </el-form-item>
            <el-form-item label="日期范围">
              <el-date-picker
                v-model="logQueryForm.dateRange"
                type="daterange"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                value-format="YYYY-MM-DD"
                style="width: 240px"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="loadLogs">查询</el-button>
            </el-form-item>
          </el-form>
          
          <el-table :data="operationLogs" stripe v-loading="logLoading">
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="userName" label="操作人" width="120" />
            <el-table-column prop="module" label="模块" width="120" />
            <el-table-column prop="operationType" label="类型" width="100">
              <template #default="{ row }">
                <el-tag :type="getOperationTypeColor(row.operationType)">
                  {{ row.operationType }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="description" label="描述" show-overflow-tooltip />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.status === 'SUCCESS' ? 'success' : 'danger'">
                  {{ row.status === 'SUCCESS' ? '成功' : '失败' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="ip" label="IP" width="140" />
            <el-table-column prop="costTime" label="耗时 (ms)" width="90" />
            <el-table-column prop="createTime" label="操作时间" width="180" />
          </el-table>
          
          <el-pagination
            v-model:current-page="logPage.current"
            v-model:page-size="logPage.size"
            :page-sizes="[20, 50, 100]"
            :total="logPage.total"
            layout="total, sizes, prev, pager, next"
            @size-change="loadLogs"
            @current-change="loadLogs"
            class="mt-4"
          />
        </el-card>
      </el-tab-pane>
      
      <!-- 数据库备份 -->
      <el-tab-pane label="数据库备份" name="backup">
        <el-card>
          <template #header>
            <span>数据库备份</span>
          </template>
          
          <el-form label-width="120px">
            <el-form-item label="备份状态">
              <el-tag :type="backupStatus.status === 'normal' ? 'success' : 'warning'">
                {{ backupStatus.status === 'normal' ? '数据库连接正常' : '异常' }}
              </el-tag>
            </el-form-item>
            <el-form-item label="最近备份时间">
              <span>{{ backupStatus.lastBackupTime || '暂无备份记录' }}</span>
            </el-form-item>
            <el-form-item label="自动备份">
              <el-tag :type="backupStatus.autoBackupEnabled ? 'success' : 'info'">
                {{ backupStatus.autoBackupEnabled ? '已启用' : '未启用' }}
              </el-tag>
              <span class="ml-2">每天 {{ backupStatus.autoBackupTime }} 执行</span>
            </el-form-item>
            <el-form-item label="备份保留">
              <span>{{ backupStatus.retentionDays }} 天</span>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="backupDatabase" :loading="backupLoading">
                立即备份
              </el-button>
            </el-form-item>
          </el-form>
          
          <el-divider />
          
          <h4>备份文件列表</h4>
          <el-table :data="backupList" stripe v-loading="backupListLoading">
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="backupFile" label="文件路径" show-overflow-tooltip />
            <el-table-column prop="fileSize" label="文件大小" width="120">
              <template #default="{ row }">
                {{ formatFileSize(row.fileSize) }}
              </template>
            </el-table-column>
            <el-table-column prop="backupTime" label="备份时间" width="180" />
            <el-table-column prop="remark" label="备注" width="150" />
            <el-table-column label="操作" width="100" fixed="right">
              <template #default="{ row }">
                <el-button link type="danger" @click="deleteBackup(row.id)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          
          <el-alert
            title="备份说明"
            type="info"
            :description="`系统会自动每天凌晨${backupStatus.autoBackupTime}进行数据库备份，备份文件保留${backupStatus.retentionDays}天。您也可以手动进行备份操作。`"
            show-icon
            :closable="false"
            class="mt-4"
          />
        </el-card>
      </el-tab-pane>
      
      <!-- 系统参数 -->
      <el-tab-pane label="系统参数" name="params">
        <el-card>
          <template #header>
            <span>系统参数设置</span>
            <el-button type="primary" @click="saveSystemParams" class="ml-2">保存设置</el-button>
          </template>
          
          <el-form label-width="150px" v-loading="paramsLoading">
            <el-divider content-position="left">备份配置</el-divider>
            <el-form-item label="备份文件保留天数">
              <el-input-number v-model="systemParams.backupRetentionDays" :min="1" :max="30" />
              <span class="ml-2">天（当前：{{ systemParams.backupRetentionDays }}天）</span>
            </el-form-item>
            <el-form-item label="启用自动备份">
              <el-switch v-model="systemParams.autoBackupEnabled" active-text="开" inactive-text="关" />
            </el-form-item>
            <el-form-item label="自动备份时间">
              <el-time-picker
                v-model="systemParams.autoBackupTime"
                format="HH:mm"
                value-format="HH:mm"
                placeholder="选择时间"
              />
            </el-form-item>
            
            <el-divider content-position="left">业务配置</el-divider>
            <el-form-item label="数据保留天数">
              <el-input-number v-model="systemParams.dataRetentionDays" :min="30" :max="730" />
              <span class="ml-2">天（日志、记录等）</span>
            </el-form-item>
            <el-form-item label="可提前预约">
              <el-input-number v-model="systemParams.reserveAdvanceDays" :min="1" :max="30" />
              <span class="ml-2">天</span>
            </el-form-item>
            <el-form-item label="最大预约时长">
              <el-input-number v-model="systemParams.maxReserveHours" :min="1" :max="12" />
              <span class="ml-2">小时</span>
            </el-form-item>
          </el-form>
        </el-card>
      </el-tab-pane>
      
      <!-- 公告管理 -->
      <el-tab-pane label="公告管理" name="announcement">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>系统公告管理</span>
              <el-button type="primary" @click="openAnnouncementDialog()">发布公告</el-button>
            </div>
          </template>
          
          <el-table :data="announcements" stripe v-loading="announcementLoading">
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="title" label="标题" width="200" />
            <el-table-column prop="content" label="内容" show-overflow-tooltip />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'info'">
                  {{ row.status === 1 ? '已发布' : '草稿' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="发布时间" width="180" />
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openAnnouncementDialog(row)">编辑</el-button>
                <el-button link type="danger" @click="deleteAnnouncement(row.id)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          
          <el-pagination
            v-model:current-page="announcementPage.current"
            v-model:page-size="announcementPage.size"
            :page-sizes="[10, 20, 30, 40, 50]"
            :total="announcementPage.total"
            layout="total, sizes, prev, pager, next"
            @size-change="loadAnnouncements"
            @current-change="loadAnnouncements"
            class="mt-4"
          />
        </el-card>
      </el-tab-pane>
      
      <!-- AI 模型配置 -->
      <el-tab-pane label="AI 模型配置" name="ai">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>AI 模型参数配置</span>
              <el-button type="primary" @click="openAiConfigDialog()">添加配置</el-button>
            </div>
          </template>
          
          <el-table :data="aiConfigs" stripe v-loading="aiLoading">
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="modelType" label="模型类型" width="150">
              <template #default="{ row }">
                <el-tag>{{ getModelTypeText(row.modelType) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="paramKey" label="参数名" width="200" />
            <el-table-column prop="paramValue" label="参数值" width="200" />
            <el-table-column prop="threshold" label="阈值" width="100" />
            <el-table-column prop="description" label="描述" show-overflow-tooltip />
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openAiConfigDialog(row)">编辑</el-button>
                <el-button link type="danger" @click="deleteAiConfig(row.id)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>
    </el-tabs>
    
    <!-- 公告对话框 -->
    <el-dialog v-model="announcementDialogVisible" :title="announcementForm.id ? '编辑公告' : '发布公告'" width="600px">
      <el-form :model="announcementForm" label-width="80px">
        <el-form-item label="标题" required>
          <el-input v-model="announcementForm.title" placeholder="请输入公告标题" />
        </el-form-item>
        <el-form-item label="内容" required>
          <el-input v-model="announcementForm.content" type="textarea" :rows="6" placeholder="请输入公告内容" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="announcementForm.status">
            <el-radio :label="1">发布</el-radio>
            <el-radio :label="0">草稿</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="announcementDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveAnnouncement">确定</el-button>
      </template>
    </el-dialog>
    
    <!-- AI 配置对话框 -->
    <el-dialog v-model="aiConfigDialogVisible" :title="aiConfigForm.id ? '编辑配置' : '添加配置'" width="500px">
      <el-form :model="aiConfigForm" label-width="100px">
        <el-form-item label="模型类型" required>
          <el-select v-model="aiConfigForm.modelType" placeholder="请选择模型类型" style="width: 100%">
            <el-option label="故障预测" value="FAULT_PREDICT" />
            <el-option label="安全检测" value="SAFETY_DETECT" />
            <el-option label="智能问答" value="QA" />
            <el-option label="智能排课" value="SCHEDULE" />
          </el-select>
        </el-form-item>
        <el-form-item label="参数名" required>
          <el-input v-model="aiConfigForm.paramKey" placeholder="请输入参数名" />
        </el-form-item>
        <el-form-item label="参数值">
          <el-input v-model="aiConfigForm.paramValue" placeholder="请输入参数值" />
        </el-form-item>
        <el-form-item label="阈值">
          <el-input-number v-model="aiConfigForm.threshold" :min="0" :max="1" :step="0.1" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="aiConfigForm.description" type="textarea" :rows="2" placeholder="请输入描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="aiConfigDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveAiConfig">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { 
  getAnnouncementPage, addAnnouncement, updateAnnouncement, deleteAnnouncement as delAnnouncement,
  getAiConfigs, addAiConfig, updateAiConfig, deleteAiConfig as delAiConfig,
  getBackupStatus, backupDatabase as backupDb, getBackupList, deleteBackup as deleteBackupApi,
  getSystemParams, saveSystemParams as saveParamsApi,
  getOperationLogPage, deleteLog, clearLogs as clearLogsApi
} from '@/api/system'
import { ElMessage, ElMessageBox } from 'element-plus'

const activeTab = ref('logs')

// 操作日志
const logLoading = ref(false)
const operationLogs = ref([])
const logPage = reactive({ current: 1, size: 20, total: 0 })
const logQueryForm = reactive({ 
  username: '', 
  module: '', 
  operationType: '', 
  dateRange: [] 
})

// 数据库备份
const backupLoading = ref(false)
const backupListLoading = ref(false)
const backupList = ref([])
const backupStatus = reactive({
  status: 'normal',
  lastBackupTime: '',
  retentionDays: '1',
  autoBackupEnabled: true,
  autoBackupTime: '03:00'
})

// 系统参数
const paramsLoading = ref(false)
const systemParams = reactive({
  backupRetentionDays: 1,
  autoBackupEnabled: true,
  autoBackupTime: '03:00',
  dataRetentionDays: 365,
  reserveAdvanceDays: 7,
  maxReserveHours: 4
})

// 公告管理
const announcementLoading = ref(false)
const announcements = ref([])
const announcementPage = reactive({ current: 1, size: 10, total: 0 })
const announcementDialogVisible = ref(false)
const announcementForm = ref({})

// AI 配置
const aiLoading = ref(false)
const aiConfigs = ref([])
const aiConfigDialogVisible = ref(false)
const aiConfigForm = ref({})

function getOperationTypeColor(type) {
  const map = {
    '新增': 'success',
    '修改': 'warning',
    '删除': 'danger',
    '查询': 'info'
  }
  return map[type] || ''
}

function formatFileSize(bytes) {
  if (!bytes) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i]
}

// 加载操作日志
async function loadLogs() {
  logLoading.value = true
  try {
    const params = {
      current: logPage.current,
      size: logPage.size,
      module: logQueryForm.module,
      operationType: logQueryForm.operationType
    }
    
    // 添加用户查询参数
    if (logQueryForm.username) {
      params.username = logQueryForm.username
    }
    
    // 添加日期范围查询参数
    if (logQueryForm.dateRange && logQueryForm.dateRange.length === 2) {
      params.startTime = logQueryForm.dateRange[0]
      params.endTime = logQueryForm.dateRange[1]
    }
    
    const res = await getOperationLogPage(params)
    operationLogs.value = res.records || []
    logPage.total = res.total || 0
  } finally {
    logLoading.value = false
  }
}

async function clearLogs() {
  await ElMessageBox.confirm('确定清空所有操作日志吗？此操作不可恢复！', '警告', { type: 'warning' })
  await clearLogsApi()
  ElMessage.success('已清空日志')
  loadLogs()
}

// 加载备份状态
async function loadBackupStatus() {
  try {
    const res = await getBackupStatus()
    backupStatus.status = res.status || 'normal'
    backupStatus.lastBackupTime = res.lastBackupTime || ''
    backupStatus.retentionDays = res.retentionDays || '1'
    backupStatus.autoBackupEnabled = res.autoBackupEnabled !== false
    backupStatus.autoBackupTime = res.autoBackupTime || '03:00'
    
    // 同步到系统参数
    systemParams.backupRetentionDays = parseInt(res.retentionDays) || 1
    systemParams.autoBackupEnabled = res.autoBackupEnabled !== false
    systemParams.autoBackupTime = res.autoBackupTime || '03:00'
  } catch (e) {
    console.error('加载备份状态失败', e)
  }
}

// 加载备份列表
async function loadBackupList() {
  backupListLoading.value = true
  try {
    backupList.value = await getBackupList()
  } finally {
    backupListLoading.value = false
  }
}

async function backupDatabase() {
  backupLoading.value = true
  try {
    await backupDb()
    ElMessage.success('备份成功')
    loadBackupStatus()
    loadBackupList()
  } catch (e) {
    ElMessage.error('备份失败：' + (e.message || '未知错误'))
  } finally {
    backupLoading.value = false
  }
}

async function deleteBackup(id) {
  await ElMessageBox.confirm('确定删除该备份文件吗？', '提示', { type: 'warning' })
  await deleteBackupApi(id)
  ElMessage.success('删除成功')
  loadBackupList()
}

// 加载系统参数
async function loadSystemParams() {
  paramsLoading.value = true
  try {
    const res = await getSystemParams()
    systemParams.backupRetentionDays = parseInt(res['backup.retention.days']) || 1
    systemParams.autoBackupEnabled = res['auto.backup.enabled'] === 'true'
    systemParams.autoBackupTime = res['auto.backup.time'] || '03:00'
    systemParams.dataRetentionDays = parseInt(res['data.retention.days']) || 365
    systemParams.reserveAdvanceDays = parseInt(res['reserve.advance.days']) || 7
    systemParams.maxReserveHours = parseInt(res['reserve.max.hours']) || 4
  } finally {
    paramsLoading.value = false
  }
}

async function saveSystemParams() {
  try {
    await saveParamsApi({
      'backup.retention.days': systemParams.backupRetentionDays.toString(),
      'auto.backup.enabled': systemParams.autoBackupEnabled.toString(),
      'auto.backup.time': systemParams.autoBackupTime,
      'data.retention.days': systemParams.dataRetentionDays.toString(),
      'reserve.advance.days': systemParams.reserveAdvanceDays.toString(),
      'reserve.max.hours': systemParams.maxReserveHours.toString()
    })
    ElMessage.success('保存成功')
    loadBackupStatus()
  } catch (e) {
    ElMessage.error('保存失败')
  }
}

// 公告管理
async function loadAnnouncements() {
  announcementLoading.value = true
  try {
    const res = await getAnnouncementPage({
      current: announcementPage.current,
      size: announcementPage.size
    })
    announcements.value = res.records || []
    announcementPage.total = res.total || 0
  } finally {
    announcementLoading.value = false
  }
}

function openAnnouncementDialog(row = null) {
  announcementForm.value = row ? { ...row } : { title: '', content: '', status: 1 }
  announcementDialogVisible.value = true
}

async function saveAnnouncement() {
  if (!announcementForm.value.title || !announcementForm.value.content) {
    ElMessage.warning('请填写完整信息')
    return
  }
  try {
    if (announcementForm.value.id) {
      await updateAnnouncement(announcementForm.value)
    } else {
      await addAnnouncement(announcementForm.value)
    }
    ElMessage.success('保存成功')
    announcementDialogVisible.value = false
    loadAnnouncements()
  } catch (e) {
    ElMessage.error('保存失败')
  }
}

async function deleteAnnouncement(id) {
  await ElMessageBox.confirm('确定删除该公告？', '提示', { type: 'warning' })
  await delAnnouncement(id)
  ElMessage.success('删除成功')
  loadAnnouncements()
}

// AI 配置
async function loadAiConfigs() {
  aiLoading.value = true
  try {
    aiConfigs.value = await getAiConfigs()
  } finally {
    aiLoading.value = false
  }
}

function openAiConfigDialog(row = null) {
  aiConfigForm.value = row ? { ...row } : { modelType: '', paramKey: '', paramValue: '', threshold: null, description: '' }
  aiConfigDialogVisible.value = true
}

async function saveAiConfig() {
  if (!aiConfigForm.value.modelType || !aiConfigForm.value.paramKey) {
    ElMessage.warning('请填写完整信息')
    return
  }
  try {
    if (aiConfigForm.value.id) {
      await updateAiConfig(aiConfigForm.value)
    } else {
      await addAiConfig(aiConfigForm.value)
    }
    ElMessage.success('保存成功')
    aiConfigDialogVisible.value = false
    loadAiConfigs()
  } catch (e) {
    ElMessage.error('保存失败')
  }
}

async function deleteAiConfig(id) {
  await ElMessageBox.confirm('确定删除该配置？', '提示', { type: 'warning' })
  await delAiConfig(id)
  ElMessage.success('删除成功')
  loadAiConfigs()
}

function getModelTypeText(type) {
  const map = {
    'FAULT_PREDICT': '故障预测',
    'SAFETY_DETECT': '安全检测',
    'QA': '智能问答',
    'SCHEDULE': '智能排课'
  }
  return map[type] || type
}

onMounted(() => {
  loadLogs()
  loadBackupStatus()
  loadBackupList()
  loadSystemParams()
  loadAnnouncements()
  loadAiConfigs()
})
</script>

<style scoped>
.system-config {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.ml-2 {
  margin-left: 8px;
}

.mt-4 {
  margin-top: 16px;
}
</style>
