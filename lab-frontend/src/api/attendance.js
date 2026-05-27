import request from './request'

export function getAttendanceByTask(taskId, params) {
  return request.get(`/attendance/task/${taskId}`, { params })
}

export function saveAttendance(data) {
  return request.post('/attendance', data)
}

export function updateAttendance(data) {
  return request.put('/attendance', data)
}

export function getAttendanceStats(taskId) {
  return request.get(`/attendance/stats/${taskId}`)
}

export function getAllAttendance(params) {
  return request.get('/attendance/all', { params })
}

export function getAllAttendanceStats() {
  return request.get('/attendance/stats/all')
}
