import request from './request'

export function getTaskList(params) {
  return request.get('/task/list', { params })
}

export function getMyTasks(params) {
  return request.get('/task/my', { params })
}

export function getTaskById(id) {
  return request.get(`/task/${id}`)
}

export function addTask(data) {
  return request.post('/task', data)
}

export function updateTask(data) {
  return request.put('/task', data)
}

export function deleteTask(id) {
  return request.delete(`/task/${id}`)
}
