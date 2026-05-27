import request from './request'

export function getMyReserves(params) {
  return request.get('/reserve/my', { params })
}

export function getReservePage(params) {
  return request.get('/reserve/page', { params })
}

export function addReserve(data) {
  return request.post('/reserve', data)
}

export function auditReserve(id, status, remark) {
  return request.post('/reserve/audit', null, { params: { id, status, remark } })
}

export function labAuditReserve(id, status, remark) {
  return request.post('/reserve/lab-audit', null, { params: { id, status, remark } })
}

export function cancelReserve(id) {
  return request.delete(`/reserve/${id}`)
}

export function updateReserve(data) {
  return request.put('/reserve', data)
}

export function deleteReserve(id) {
  return request.delete(`/reserve/delete/${id}`)
}

export function getAvailableSlots(params) {
  return request.get('/reserve/available-slots', { params })
}

export function getDeviceQuota(params) {
  return request.get('/reserve/device-quota', { params })
}
