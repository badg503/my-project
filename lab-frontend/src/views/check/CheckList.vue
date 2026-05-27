<template>
  <div>
    <h2 class="page-title">考勤签到</h2>
    
    <!-- 待签到提醒 -->
    <el-alert
      v-if="needSignInReserve"
      title=" 待签到提醒"
      type="warning"
      :closable="false"
      show-icon
      class="mb-4"
    >
      <template #default>
        <div class="alert-content">
          <p><strong>您有待签到的预约，请及时签到！</strong></p>
          <p>
            实验室：{{ needSignInReserve.labName }} | 
            预约时间：{{ needSignInReserve.timeSlotStart }} - {{ needSignInReserve.timeSlotEnd }}
          </p>
          <el-button type="primary" size="small" @click="handleSignIn(needSignInReserve)">立即签到</el-button>
        </div>
      </template>
    </el-alert>
    
    <!-- 信息栏提醒 -->
    <el-alert
      v-if="currentReserve || list.length > 0"
      title="📌 签到与签退须知"
      type="info"
      :closable="false"
      class="mb-4"
    >
      <template #default>
        <div class="notice-content">
          <p><strong>【签到规则】</strong></p>
          <p>1. 请在预约开始时间后 10 分钟内完成签到，超过 10 分钟未签到将视为<strong>【迟到】</strong></p>
          <p>2. 签到成功后方可使用实验室和设备</p>
          <el-divider />
          <p><strong>【签退规则】</strong></p>
          <p>1. 请在预约结束前 5 分钟内完成签退，<strong style="color: #F56C6C;">最后 5 分钟内签退视为【正常签退】</strong></p>
          <p>2. <strong style="color: #F56C6C;">⚠️ 提前签退（不在最后 5 分钟内）将视为【早退】</strong></p>
          <p>3. 签退前请务必关闭所有实验设备电源，确保设备安全</p>
          <p>4. 签退时系统将二次确认设备关闭情况，请如实反馈</p>
          <el-divider v-if="currentReserve" />
          <p v-if="currentReserve"><strong>当前预约信息：</strong></p>
          <p v-if="currentReserve">预约时间：{{ currentReserve.timeSlotStart }} - {{ currentReserve.timeSlotEnd }}</p>
          <p v-if="currentReserve">最晚签到时间：{{ getLateTime(currentReserve.timeSlotStart) }}（超过视为迟到）</p>
          <p v-if="currentReserve">正常签退时段：{{ getNormalSignOutTime(currentReserve.timeSlotEnd) }}（最后 5 分钟）</p>
        </div>
      </template>
    </el-alert>
    
    <el-card style="margin-bottom:16px">
      <el-form inline>
        <el-form-item label="实验室名称">
          <el-input v-model="searchForm.labName" placeholder="请输入实验室名称" style="width:200px" clearable />
        </el-form-item>
        <el-button type="primary" @click="load">查询</el-button>
      </el-form>
    </el-card>
    <el-table :data="filteredList" stripe v-loading="loading">
      <el-table-column prop="labId" label="实验室ID" width="100" />
      <el-table-column prop="labName" label="实验室名称" width="180" />
      <el-table-column prop="reserveTime" label="预约时间" width="250" />
      <el-table-column prop="checkInTime" label="签到时间" width="170" />
      <el-table-column prop="checkOutTime" label="签退时间" width="170" />
      <el-table-column prop="status" label="状态" width="180">
        <template #default="{ row }">
          <div style="display: flex; flex-direction: column; gap: 4px;">
            <!-- 签到状态 -->
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ getStatusText(row.status) }}
            </el-tag>
            <!-- 签退状态 -->
            <el-tag v-if="row.checkOutStatus" :type="getCheckOutStatusType(row.checkOutStatus)" size="small">
              {{ getCheckOutStatusText(row.checkOutStatus) }}
            </el-tag>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button 
            v-if="row.status === 'ABSENT'" 
            type="primary" 
            size="small" 
            @click="handleSignIn(row)"
          >
            签到
          </el-button>
          <el-button 
            v-if="row.status === 'PRESENT' && !row.checkOutTime" 
            type="success" 
            size="small" 
            @click="signOut(row.id)"
          >
            签退
          </el-button>
          <!-- 已完成的记录不显示任何内容 -->
        </template>
      </el-table-column>
    </el-table>
    <el-pagination 
      v-model:current-page="page.current" 
      v-model:page-size="page.size" 
      :total="page.total" 
      layout="total, prev, pager, next" 
      @current-change="load" 
      style="margin-top:16px" 
    />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { getMyChecks, signIn as apiSignIn, signOut as apiSignOut, checkPowerBeforeSignOut, forceSignOut } from '@/api/check'
import { ElMessage, ElMessageBox } from 'element-plus'

const list = ref([])
const loading = ref(false)
const page = reactive({ current: 1, size: 10, total: 0 })
const searchForm = ref({ labName: '' })
const currentReserve = ref(null)
const needSignInReserve = ref(null) // 待签到的预约

const filteredList = computed(() => {
  if (!searchForm.value.labName) {
    return list.value
  }
  return list.value.filter(item => 
    item.labName && item.labName.includes(searchForm.value.labName)
  )
})

async function load() {
  loading.value = true
  try {
    const res = await getMyChecks({ current: page.current, size: page.size })
    list.value = res.records || []
    page.total = res.total || 0
    
    // 找到第一个未签到的预约，显示待签到提醒
    const needSignIn = list.value.find(item => item.status === 'ABSENT')
    if (needSignIn) {
      needSignInReserve.value = needSignIn
      currentReserve.value = needSignIn
    } else if (list.value.length > 0) {
      // 如果没有待签到的预约，显示第一条记录用于提示
      currentReserve.value = list.value[0]
    }
  } finally {
    loading.value = false
  }
}

function getStatusType(status) {
  const map = {
    PRESENT: 'success',
    ABSENT: 'warning',
    LATE: 'danger',
    ABSENCE: 'danger'
  }
  return map[status] || 'info'
}

function getStatusText(status) {
  const map = {
    PRESENT: '已签到',
    ABSENT: '未签到',
    LATE: '迟到',
    ABSENCE: '缺勤'
  }
  return map[status] || status
}

function getCheckOutStatusType(status) {
  const map = {
    NORMAL: 'success',
    EARLY_LEAVE: 'danger',
    ABSENCE: 'danger'
  }
  return map[status] || 'info'
}

function getCheckOutStatusText(status) {
  const map = {
    NORMAL: '正常签退',
    EARLY_LEAVE: '早退',
    ABSENCE: '缺勤'
  }
  return map[status] || status
}

// 计算最晚签到时间（预约开始时间 + 10 分钟）
function getLateTime(startTime) {
  if (!startTime) return ''
  const [hours, minutes] = startTime.split(':').map(Number)
  const date = new Date()
  date.setHours(hours, minutes + 10, 0, 0)
  return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

// 计算正常签退时段（预约结束时间 - 5 分钟）
function getNormalSignOutTime(endTime) {
  if (!endTime) return ''
  const [hours, minutes] = endTime.split(':').map(Number)
  const date = new Date()
  date.setHours(hours, minutes - 5, 0, 0)
  return `${date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })} - ${endTime}`
}

async function handleSignIn(row) {
  try {
    await apiSignIn(row.labId, row.reserveId)
    ElMessage.success('签到成功')
    load()
  } catch (error) {
    ElMessage.error(error.message || '签到失败')
  }
}

async function signOut(checkId) {
  try {
    // 1. 第一次提醒：提示关闭设备
    await ElMessageBox.confirm(
      '⚠️ 签退前请确认：\n\n1. 已关闭所有实验设备电源\n2. 已整理好实验台\n3. 已带走个人物品\n\n请确保设备已完全关闭后再进行签退操作！',
      '设备关闭确认',
      {
        confirmButtonText: '已关闭设备',
        cancelButtonText: '再检查一下',
        type: 'warning',
        distinguishCancelAndClose: true
      }
    )
    
    // 2. 系统检测：检查设备电源状态
    const checkResult = await checkPowerBeforeSignOut(checkId)
    
    if (checkResult.code === 200 && checkResult.data) {
      const result = checkResult.data
      
      if (result.success && !result.hasUnclosedDevices) {
        // 所有设备已关闭，直接签退
        await ElMessageBox.confirm(
          '✅ 系统检测：所有设备已关闭电源\n\n确认要签退吗？',
          '签退确认',
          {
            confirmButtonText: '确认签退',
            cancelButtonText: '取消',
            type: 'success'
          }
        )
        await apiSignOut(checkId)
        ElMessage.success('签退成功')
        load()
      } else if (result.hasUnclosedDevices) {
        // 有设备未关闭
        let deviceList = ''
        if (result.unclosedDevices && result.unclosedDevices.length > 0) {
          deviceList = result.unclosedDevices.map(d => 
            `• 设备 ID: ${d.deviceId} (电流：${d.current.toFixed(2)}A)`
          ).join('\n')
        }
        
        // 3. 第二次提醒：显示未关闭设备，要求学生现场检查
        await ElMessageBox.confirm(
          `⚠️ 检测到以下设备未关闭电源：\n\n${deviceList}\n\n` +
          '请立即现场检查并关闭设备！\n\n' +
          '如果确认已经关闭，可以选择强制签退（系统将发送邮件提醒）',
          '设备未关闭警告',
          {
            confirmButtonText: '我已现场检查，强制签退',
            cancelButtonText: '去关闭设备',
            type: 'error',
            distinguishCancelAndClose: true
          }
        )
        
        // 学生选择强制签退
        await ElMessageBox.confirm(
          '❗ 最后确认：\n\n' +
          '系统已发送邮件给：\n' +
          '• 您本人（提醒返回关闭电源）\n' +
          '• 您的指导教师（进行安全教育）\n' +
          '• 实验室管理员（查看是否返回关闭）\n\n' +
          '确认要强制签退吗？',
          '强制签退确认',
          {
            confirmButtonText: '确认强制签退',
            cancelButtonText: '返回检查',
            type: 'warning'
          }
        )
        
        // 执行强制签退（发送邮件）
        await forceSignOut(checkId)
        ElMessage.warning('签退成功，系统已发送邮件提醒')
        load()
      } else {
        // 其他情况，直接签退
        await apiSignOut(checkId)
        ElMessage.success('签退成功')
        load()
      }
    } else {
      // 检测失败，直接签退
      await apiSignOut(checkId)
      ElMessage.success('签退成功')
      load()
    }
  } catch (error) {
    if (error === 'cancel' || error.action === 'cancel') {
      ElMessage.info('已取消签退，请先检查设备')
    } else {
      ElMessage.error(error.message || '签退失败')
    }
  }
}

onMounted(() => { load() })
</script>

<style scoped>
.page-title { margin-bottom: 16px; font-size: 18px; }

.notice-content p {
  margin: 8px 0;
  line-height: 1.6;
}

.notice-content strong {
  color: #F56C6C;
}

.alert-content {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.alert-content p {
  margin: 4px 0;
}

.alert-content strong {
  color: #E6A23C;
  font-size: 16px;
}
</style>
