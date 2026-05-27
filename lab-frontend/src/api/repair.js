import request from './request'

export function getMyRepairs(params) {
  return request.get('/repair/my', { params })
}

export function getRepairPage(params) {
  return request.get('/repair/page', { params })
}

export function addRepair(data) {
  return request.post('/repair', data)
}

export function handleRepair(id, status, repairRemark) {
  return request.post('/repair/handle', null, { params: { id, status, repairRemark } })
}

export function updateRepair(data) {
  return request.put('/repair', data)
}

export function cancelRepair(id) {
  return request.delete(`/repair/${id}`)
}

export function checkDeviceRepairStatus(deviceId) {
  return request.get(`/repair/check-device/${deviceId}`)
}

export function getPendingRepairDevices() {
  return request.get('/repair/pending-devices')
}
