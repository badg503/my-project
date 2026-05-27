import request from './request'

export function getUserPage(params) {
  return request.get('/user/page', { params })
}

export function addUser(data) {
  return request.post('/user', data)
}

export function updateUser(data) {
  return request.put('/user', data)
}

export function deleteUser(id) {
  return request.delete(`/user/${id}`)
}

export function resetPassword(userId, newPassword) {
  return request.post('/user/reset-password', null, { params: { userId, newPassword } })
}

export function updateProfile(data) {
  return request.put('/user/profile', data)
}

export function changePassword(data) {
  return request.put('/user/change-password', data)
}

export function getTeachers() {
  return request.get('/user/teachers')
}

export function getStudents() {
  return request.get('/user/students')
}

export function assignStudents(teacherId, studentIds) {
  return request.post('/user/assign-students', { teacherId, studentIds })
}

export function unassignStudents(teacherId, studentIds) {
  return request.post('/user/unassign-students', { teacherId, studentIds })
}

export function getAssignedStudents(teacherId) {
  return request.get('/user/assigned-students', { params: { teacherId } })
}

export function getClasses() {
  return request.get('/user/classes')
}

export function getDepartments() {
  return request.get('/user/departments')
}
