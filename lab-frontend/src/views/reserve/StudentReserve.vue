<template>
  <div class="student-reserve">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>📅 预约实验室</span>
        </div>
      </template>

      <!-- 步骤条 -->
      <el-steps :active="currentStep" finish-status="success" align-center class="mb-6">
        <el-step title="选择实验室" />
        <el-step title="选择设备" />
        <el-step title="选择日期" />
        <el-step title="填写信息" />
      </el-steps>

      <!-- 步骤 1: 选择实验室 -->
      <div v-show="currentStep === 0" class="step-content">
        <h3 class="step-title">选择实验室</h3>
        <el-table 
          :data="labs" 
          stripe 
          v-loading="labsLoading" 
          @current-change="handleLabChange"
          :row-class-name="rowClassName"
          highlight-current-row
          current-row-key="id"
        >
          <el-table-column type="radio" width="50" :selectable="labSelectable" />
          <el-table-column prop="name" label="实验室名称" />
          <el-table-column prop="location" label="位置" />
          <el-table-column label="开放时间" width="180">
            <template #default="{ row }">
              {{ row.openTimeStart }} - {{ row.openTimeEnd }}
            </template>
          </el-table-column>
          <el-table-column prop="capacity" label="容量" width="80" />
          <el-table-column label="状态" width="80">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'danger'">
                {{ row.status === 1 ? '可用' : '不可用' }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
        <div class="step-footer">
          <el-button type="primary" @click="currentStep = 1" :disabled="!selectedLab">
            下一步
          </el-button>
        </div>
      </div>

      <!-- 步骤 2: 选择设备 -->
      <div v-show="currentStep === 1" class="step-content">
        <h3 class="step-title">选择设备</h3>
        <el-alert
          v-if="!selectedLab"
          title="请先选择实验室"
          type="warning"
          :closable="false"
          class="mb-4"
        />
        <el-table 
          v-else 
          :data="labDevices" 
          stripe 
          v-loading="devicesLoading"
          @selection-change="handleDeviceSelection"
        >
          <el-table-column type="selection" width="55" :selectable="selectable" />
          <el-table-column prop="name" label="设备名称" />
          <el-table-column prop="deviceType" label="设备类型" width="120">
            <template #default="{ row }">
              {{ getDeviceTypeName(row.deviceType) }}
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 'AVAILABLE' ? 'success' : 'danger'">
                {{ row.status === 'AVAILABLE' ? '可用' : '不可用' }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
        <div class="step-footer">
          <el-button @click="currentStep = 0">上一步</el-button>
          <el-button 
            type="primary" 
            @click="currentStep = 2" 
            :disabled="selectedDevices.length === 0"
          >
            下一步
          </el-button>
        </div>
      </div>

      <!-- 步骤 3: 选择日期（可视化时间轴） -->
      <div v-show="currentStep === 2" class="step-content">
        <h3 class="step-title">
          选择预约日期
          <el-tag v-if="reserveForm.reserveDate" type="success" class="ml-2">
            已选：{{ reserveForm.reserveDate }}
          </el-tag>
        </h3>

        <el-alert
          v-if="!selectedLab || selectedDevices.length === 0"
          title="请先选择实验室和设备"
          type="warning"
          :closable="false"
          class="mb-4"
        />

        <div v-else>
          <!-- 显示实验室开放时间 -->
          <el-alert 
            :title="`实验室开放时间：${selectedLab.openTimeStart} - ${selectedLab.openTimeEnd}`" 
            type="info" 
            :closable="false"
            show-icon
            class="mb-4"
          />
          
          <el-date-picker
            v-model="reserveForm.reserveDate"
            type="date"
            placeholder="选择预约日期"
            :disabled-date="disabledDate"
            value-format="YYYY-MM-DD"
            style="width: 100%"
            @change="loadAvailableSlots"
          />

          <div v-loading="slotsLoading" class="timeline-container">
            <div v-if="availableSlots.length > 0" class="timeline-wrapper">
              <!-- 提示信息 -->
              <el-alert 
                title="请选择连续时间段" 
                type="info" 
                :closable="false"
                show-icon
                class="mb-4"
              >
                <template #default>
                  每个时段 1 小时，最少选择 1 个时段，最多选择 4 个连续时段（总时长不超过 4 小时）
                  <span v-if="selectedSlots.length > 0" class="ml-4">
                    已选择：{{ selectedSlots.length }} 个时段（{{ selectedSlots.length * 60 }}分钟）
                  </span>
                </template>
              </el-alert>
              
              <!-- 时间轴 -->
              <div class="timeline">
                <div
                  v-for="(slot, index) in availableSlots"
                  :key="index"
                  class="timeline-slot"
                  :class="{
                    'available': slot.isAvailable,
                    'unavailable': !slot.isAvailable,
                    'selected': isSlotSelected(slot)
                  }"
                  @click="selectSlot(slot)"
                >
                  <div class="time-label">{{ slot.timeSlotStart }}</div>
                  <div class="slot-bar">
                    <div class="slot-indicator"></div>
                  </div>
                  <div class="slot-status">
                    <el-icon v-if="slot.isAvailable" color="#67C23A"><i-ep-check /></el-icon>
                    <el-icon v-else color="#F56C6C"><i-ep-close /></el-icon>
                  </div>
                </div>
              </div>

              <!-- 图例 -->
              <div class="timeline-legend">
                <div class="legend-item">
                  <div class="legend-box available"></div>
                  <span>可预约</span>
                </div>
                <div class="legend-item">
                  <div class="legend-box unavailable"></div>
                  <span>已满</span>
                </div>
                <div class="legend-item">
                  <div class="legend-box selected"></div>
                  <span>已选择</span>
                </div>
              </div>
            </div>

            <el-empty v-else-if="reserveForm.reserveDate" description="暂无可用时段" />
            <el-empty v-else description="请选择日期查看可用时段" />
          </div>
        </div>

        <div class="step-footer">
          <el-button @click="currentStep = 1">上一步</el-button>
          <el-button type="primary" @click="currentStep = 3" :disabled="selectedSlots.length === 0">
            下一步
          </el-button>
        </div>
      </div>

      <!-- 步骤 4: 填写预约信息 -->
      <div v-show="currentStep === 3" class="step-content">
        <h3 class="step-title">填写预约信息</h3>
        <el-form :model="reserveForm" label-width="100px">
          <el-form-item label="实验室">
            <el-input :value="selectedLab?.name" disabled />
          </el-form-item>
          <el-form-item label="预约日期">
            <el-input :value="reserveForm.reserveDate" disabled />
          </el-form-item>
          <el-form-item label="时间段">
            <el-input 
              :value="getTimeSlotDisplay()" 
              disabled 
            />
          </el-form-item>
          <el-form-item label="预约目的" required>
            <el-input
              v-model="reserveForm.purpose"
              type="textarea"
              :rows="3"
              placeholder="请输入预约目的（不超过 200 字）"
              maxlength="200"
              show-word-limit
            />
          </el-form-item>
          <el-form-item label="已选设备">
            <el-tag
              v-for="device in selectedDevices"
              :key="device.id"
              closable
              @close="removeDevice(device.id)"
              class="mr-2"
            >
              {{ device.name }}
            </el-tag>
            <el-button size="small" @click="currentStep = 1" v-if="selectedDevices.length === 0">
              返回选择设备
            </el-button>
          </el-form-item>
        </el-form>

        <div class="step-footer">
          <el-button @click="handleBackToStep2">上一步</el-button>
          <el-button type="primary" @click="submitReserve" :loading="submitting">提交预约</el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { getLabPage, getDeviceList } from '@/api/lab'
import { getAvailableSlots, addReserve } from '@/api/reserve'
import { ElMessage } from 'element-plus'

const router = useRouter()
const currentStep = ref(0)
const labsLoading = ref(false)
const devicesLoading = ref(false)
const slotsLoading = ref(false)
const submitting = ref(false)

// 实验室列表
const labs = ref([])
const selectedLab = ref(null)

// 设备列表
const labDevices = ref([])
const selectedDevices = ref([])

// 预约表单
const reserveForm = reactive({
  reserveDate: '',
  timeSlotStart: '',
  timeSlotEnd: '',
  purpose: '',
  deviceIds: []
})

// 可用时段
const availableSlots = ref([])
const selectedSlots = ref([]) // 改为数组，支持选择多个时段

// 最大选择时段数（4 个时段 = 4 小时）
const MAX_SLOTS = 4

// 加载实验室列表
async function loadLabs() {
  labsLoading.value = true
  try {
    const res = await getLabPage({ current: 1, size: 100, status: 1 })
    labs.value = res.records || []
  } catch (error) {
    ElMessage.error('加载实验室列表失败')
  } finally {
    labsLoading.value = false
  }
}

// 选择实验室
function handleLabChange(row) {
  selectedLab.value = row
  // 重置后续步骤
  labDevices.value = []
  selectedDevices.value = []
  availableSlots.value = []
  selectedSlots.value = []
  reserveForm.reserveDate = ''
}

// 行类名
function rowClassName({ row }) {
  return selectedLab.value && selectedLab.value.id === row.id ? 'selected-row' : ''
}

// 判断实验室是否可选（只允许选择可用实验室）
function labSelectable(row) {
  return row.status === 1
}

// 加载实验室设备
async function loadLabDevices() {
  if (!selectedLab.value) return

  devicesLoading.value = true
  try {
    const res = await getDeviceList(selectedLab.value.id)
    labDevices.value = res || []
  } catch (error) {
    ElMessage.error('加载设备列表失败')
  } finally {
    devicesLoading.value = false
  }
}

// 选择设备
function handleDeviceSelection(selection) {
  selectedDevices.value = selection
}

// 判断设备是否可选（只允许选择可用设备）
function selectable(row) {
  return row.status === 'AVAILABLE'
}

// 获取设备类型名称
function getDeviceTypeName(type) {
  const typeMap = {
    'PRECISE_MANUAL': '精密型',
    'NON_PRECISE_MANUAL': '非精密型',
    'AUTO_BATCH': '自动批量型',
    'MULTI_USER': '多用户型'
  }
  return typeMap[type] || '精密型'
}

// 禁用过去的日期
function disabledDate(date) {
  return date.getTime() < Date.now() - 86400000
}

// 加载可用时段
async function loadAvailableSlots() {
  if (!selectedLab.value || !reserveForm.reserveDate) return

  slotsLoading.value = true
  try {
    const deviceIds = selectedDevices.value.map(d => d.id)
    const res = await getAvailableSlots({
      labId: selectedLab.value.id,
      reserveDate: reserveForm.reserveDate,
      deviceIds: deviceIds.length > 0 ? deviceIds : undefined
    })
    availableSlots.value = res || []
    selectedSlots.value = [] // 重置选择的时段
  } catch (error) {
    ElMessage.error('加载时段列表失败')
  } finally {
    slotsLoading.value = false
  }
}

// 判断时段是否已选中
function isSlotSelected(slot) {
  return selectedSlots.value.some(s => s.slotIndex === slot.slotIndex)
}

// 选择时段（支持选择多个连续时段）
function selectSlot(slot) {
  if (!slot.isAvailable) return

  const index = selectedSlots.value.findIndex(s => s.slotIndex === slot.slotIndex)
  
  // 如果已经选中，则取消选中
  if (index !== -1) {
    selectedSlots.value.splice(index, 1)
  } else {
    // 检查是否超过最大时段数
    if (selectedSlots.value.length >= MAX_SLOTS) {
      ElMessage.warning(`最多只能选择${MAX_SLOTS}个连续时段（总时长不超过 4 小时）`)
      return
    }
    
    // 添加新选中的时段
    selectedSlots.value.push(slot)
    
    // 按 slotIndex 排序，确保连续性
    selectedSlots.value.sort((a, b) => a.slotIndex - b.slotIndex)
  }
  
  // 更新表单的时间段
  if (selectedSlots.value.length > 0) {
    const firstSlot = selectedSlots.value[0]
    const lastSlot = selectedSlots.value[selectedSlots.value.length - 1]
    reserveForm.timeSlotStart = firstSlot.timeSlotStart
    reserveForm.timeSlotEnd = lastSlot.timeSlotEnd
  }
}

// 获取时间段显示文本
function getTimeSlotDisplay() {
  if (selectedSlots.value.length === 0) return ''
  const firstSlot = selectedSlots.value[0]
  const lastSlot = selectedSlots.value[selectedSlots.value.length - 1]
  return `${firstSlot.timeSlotStart} - ${lastSlot.timeSlotEnd} (${selectedSlots.value.length}个时段，${selectedSlots.value.length * 45}分钟)`
}

// 移除设备
function removeDevice(deviceId) {
  selectedDevices.value = selectedDevices.value.filter(d => d.id !== deviceId)
}

// 提交预约
async function submitReserve() {
  if (!reserveForm.purpose || reserveForm.purpose.trim() === '') {
    ElMessage.warning('请填写预约目的')
    return
  }

  submitting.value = true
  try {
    await addReserve({
      labId: selectedLab.value.id,
      reserveDate: reserveForm.reserveDate,
      timeSlotStart: reserveForm.timeSlotStart,
      timeSlotEnd: reserveForm.timeSlotEnd,
      purpose: reserveForm.purpose,
      deviceIds: selectedDevices.value.map(d => d.id).join(',')
    })
    ElMessage.success('预约成功，等待审核')
    router.push('/lab-reserve')
  } catch (error) {
    ElMessage.error(error.message || '预约失败')
  } finally {
    submitting.value = false
  }
}

// 监听步骤变化
function watchStep(newStep) {
  // 步骤 1：选择实验室后，加载设备
  if (newStep === 1) {
    loadLabDevices()
  }
  // 步骤 2：选择设备后，加载时段
  if (newStep === 2) {
    if (selectedLab.value && reserveForm.reserveDate) {
      loadAvailableSlots()
    }
  }
}

// 从第 4 步返回第 3 步时，重新加载时段
function handleBackToStep2() {
  currentStep.value = 2
  // 重新加载时段，确保显示最新的开放时间
  if (selectedLab.value && reserveForm.reserveDate) {
    loadAvailableSlots()
  }
}

// 监听 currentStep 变化
import { watch } from 'vue'
watch(currentStep, watchStep)

onMounted(() => {
  loadLabs()
})
</script>

<style scoped>
.student-reserve {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 18px;
  font-weight: bold;
}

.mb-6 {
  margin-bottom: 24px;
}

.mb-4 {
  margin-bottom: 16px;
}

.ml-2 {
  margin-left: 8px;
}

.mr-2 {
  margin-right: 8px;
}

.step-content {
  padding: 20px 0;
}

.step-title {
  font-size: 16px;
  font-weight: bold;
  margin-bottom: 16px;
  color: #303133;
}

.step-footer {
  margin-top: 24px;
  text-align: center;
}

/* 选中的实验室行样式 */
:deep(.el-table .selected-row) {
  background-color: #ecf5ff !important;
  box-shadow: 0 0 0 2px #409EFF inset;
}

:deep(.el-table .selected-row:hover) {
  background-color: #d9ecff !important;
}

:deep(.el-table__row:hover) {
  background-color: #f5f7fa;
}

.timeline-container {
  margin-top: 20px;
  padding: 20px;
  background: #f5f7fa;
  border-radius: 4px;
}

.timeline-wrapper {
  overflow-x: auto;
}

.timeline {
  display: flex;
  gap: 8px;
  padding: 16px;
  background: #fff;
  border-radius: 4px;
  min-width: max-content;
}

.timeline-slot {
  width: 100px;
  cursor: pointer;
  transition: all 0.3s;
  padding: 12px 8px;
  background: #fff;
  border: 2px solid #e4e7ed;
  border-radius: 8px;
  text-align: center;
}

.timeline-slot:hover {
  transform: translateY(-2px);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.timeline-slot.available {
  border-color: #67C23A;
  background: #f0f9ff;
}

.timeline-slot.unavailable {
  border-color: #F56C6C;
  background: #fef0f0;
  cursor: not-allowed;
}

.timeline-slot.selected {
  border-color: #409EFF;
  background: #ecf5ff;
  box-shadow: 0 0 0 2px #409EFF;
}

.time-label {
  font-size: 12px;
  color: #606266;
  margin-bottom: 8px;
}

.slot-bar {
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.slot-indicator {
  width: 4px;
  height: 30px;
  border-radius: 2px;
}

.timeline-slot.available .slot-indicator {
  background: #67C23A;
}

.timeline-slot.unavailable .slot-indicator {
  background: #F56C6C;
}

.timeline-slot.selected .slot-indicator {
  background: #409EFF;
}

.slot-status {
  margin-top: 8px;
  font-size: 12px;
}

.timeline-legend {
  display: flex;
  gap: 16px;
  margin-top: 16px;
  padding: 12px;
  background: #fff;
  border-radius: 4px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #606266;
}

.legend-box {
  width: 16px;
  height: 16px;
  border-radius: 4px;
  border: 2px solid #e4e7ed;
}

.legend-box.available {
  background: #f0f9ff;
  border-color: #67C23A;
}

.legend-box.unavailable {
  background: #fef0f0;
  border-color: #F56C6C;
}

.legend-box.selected {
  background: #ecf5ff;
  border-color: #409EFF;
}
</style>
