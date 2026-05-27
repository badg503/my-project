import request from './request'

export function getMyReports(params) {
  return request.get('/report/my', { params })
}

export function getReportsByTask(taskId, params) {
  return request.get(`/report/task/${taskId}`, { params })
}

export function submitReport(data) {
  return request.post('/report', data)
}

export function updateReport(id, data) {
  return request.put(`/report/${id}`, data)
}

export function deleteReport(id) {
  return request.delete(`/report/${id}`)
}

export function gradeReport(id, score, remark) {
  return request.post('/report/grade', null, { params: { id, score, remark } })
}
