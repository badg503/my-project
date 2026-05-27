import request from './request'

export function getMyChecks(params) {
  return request.get('/check/my', { params })
}

export function getCheckPage(params) {
  return request.get('/check/page', { params })
}

/**
 * 获取所有签到记录（考勤管理页面使用）
 */
export function getAllCheckRecords(params) {
  return request.get('/check/all', { params })
}

/**
 * 获取考勤统计数据
 */
export function getAllAttendanceStats() {
  return request.get('/check/stats')
}

export function signIn(labId, reserveId) {
  return request.post('/check/sign-in', null, { params: { labId, reserveId } })
}

export function signOut(checkId) {
  return request.post('/check/sign-out', null, { params: { checkId } })
}

/**
 * 签退前检查设备电源
 */
export function checkPowerBeforeSignOut(checkId) {
  return request.get('/check/check-power', { params: { checkId } })
}

/**
 * 强制签退（设备未关闭但学生坚持要签退）
 */
export function forceSignOut(checkId) {
  return request.post('/check/force-sign-out', null, { params: { checkId } })
}
