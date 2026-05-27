import request from './request'
import axios from 'axios'

// 创建不带错误拦截的 axios 实例（用于批量预测，需要自定义错误处理）
const apiClient = axios.create({
  baseURL: '/api',
  timeout: 15000
})

apiClient.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

export function aiQa(question) {
  return request.post('/ai/qa', { question })
}

// 获取 BERT 问答阈值
export function getBertThreshold() {
  return request.get('/ai/qa-threshold')
}

// 更新 BERT 问答阈值
export function updateBertThreshold(threshold) {
  return request.post('/ai/qa-threshold', { threshold })
}

// 故障预测
export function faultPredict(deviceId) {
  return request.get('/ai/fault-predict', { params: { deviceId } })
}

// 安全检测
export function safetyDetect(labId) {
  return request.get('/ai/safety-detect', { params: { labId } })
}

// 批量故障预测
export function batchFaultPredict(labId) {
  // 如果 labId 为 null 或 undefined，发送空对象
  const body = labId ? { labId } : {}
  // 使用自定义 axios 实例，避免拦截器自动显示错误
  return apiClient.post('/ai/batch-fault-predict', body)
}

// 获取预测任务进度
export function getPredictionProgress(taskId) {
  return request.get(`/ai/prediction-progress/${taskId}`)
}

// 获取当天的预测结果
export function getTodayPredictions() {
  return request.get('/ai/today-predictions')
}

// 查询预测结果（分页，支持按触发类型和日期筛选）
export function getPredictionResults(labId, triggerType, date, current = 1, size = 20) {
  return request.get('/ai/prediction-results', {
    params: { labId, triggerType, date, current, size }
  })
}

// AI 数据分析（Prophet）
export function getAIAnalysis() {
  return request.get('/ai/analysis')
}

// AI 智能预约调度
export function getAISchedule() {
  return request.post('/ai/schedule', {})
}
