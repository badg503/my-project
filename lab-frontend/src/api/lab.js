import request from './request'

export function getLabList(params) {
  return request.get('/lab/list', { params })
}

export function getLabPage(params) {
  return request.get('/lab/page', { params })
}

export function getLabById(id) {
  return request.get(`/lab/${id}`)
}

export function addLab(data) {
  return request.post('/lab', data)
}

export function updateLab(data) {
  return request.put('/lab', data)
}

export function deleteLab(id) {
  return request.delete(`/lab/${id}`)
}

export function getDeviceList(labId) {
  return request.get('/device/list', { params: { labId } })
}

export function getDevicePage(params) {
  return request.get('/device/page', { params })
}

export function getDeviceById(id) {
  return request.get(`/device/${id}`)
}

export function addDevice(data) {
  return request.post('/device', data)
}

export function updateDevice(data) {
  return request.put('/device', data)
}

export function deleteDevice(id) {
  return request.delete(`/device/${id}`)
}

export function getDeviceRecords(params) {
  return request.get('/device/record/page', { params })
}

export function getRepairDesc(deviceId) {
  return request.get(`/device/repairDesc/${deviceId}`)
}
