import request from './request'

export function getDashboard() {
  return request.get('/statistics/dashboard')
}

export function getLabUsage(startDate, endDate) {
  return request.get('/statistics/lab-usage', {
    params: {
      startDate: startDate,
      endDate: endDate
    }
  })
}

export function getDeviceFailure() {
  return request.get('/statistics/device-failure')
}

export function getAttendance(startDate, endDate) {
  return request.get('/statistics/attendance', {
    params: {
      startDate: startDate,
      endDate: endDate
    }
  })
}

export function getRepairStats() {
  return request.get('/statistics/repair-stats')
}
