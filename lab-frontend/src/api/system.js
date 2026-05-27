import request from './request'

export function getAnnouncements() {
  return request.get('/system/announcements')
}

export function getAnnouncementPage(params) {
  return request.get('/system/announcements/page', { params })
}

export function addAnnouncement(data) {
  return request.post('/system/announcements', data)
}

export function updateAnnouncement(data) {
  return request.put('/system/announcements', data)
}

export function deleteAnnouncement(id) {
  return request.delete(`/system/announcements/${id}`)
}

export function getAiConfigs() {
  return request.get('/system/ai-config')
}

export function updateAiConfig(data) {
  return request.put('/system/ai-config', data)
}

export function addAiConfig(data) {
  return request.post('/system/ai-config', data)
}

export function deleteAiConfig(id) {
  return request.delete(`/system/ai-config/${id}`)
}

export function backupDatabase() {
  return request.post('/database-backup/backup')
}

// ==================== 操作日志 ====================
export function getOperationLogPage(params) {
  return request.get('/operation-log/page', { params })
}

export function deleteLog(id) {
  return request.delete(`/operation-log/${id}`)
}

export function clearLogs() {
  return request.delete('/operation-log/clear')
}

// ==================== 数据库备份 ====================
export function getBackupStatus() {
  return request.get('/database-backup/status')
}

export function getBackupList() {
  return request.get('/database-backup/list')
}

export function deleteBackup(id) {
  return request.delete(`/database-backup/${id}`)
}

// ==================== 系统参数 ====================
export function getSystemParams() {
  return request.get('/system-config/list')
}

export function saveSystemParams(params) {
  return request.post('/system-config/update', params)
}
