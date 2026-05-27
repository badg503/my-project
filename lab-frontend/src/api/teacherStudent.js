import request from './request'

export function getMyStudents() {
  return request.get('/teacher-student/my-students')
}

export function getAvailableStudents(params) {
  return request.get('/teacher-student/available-students', { params })
}

export function assignStudent(studentId) {
  return request.post('/teacher-student/assign', { studentId })
}

export function removeStudent(id) {
  return request.delete(`/teacher-student/${id}`)
}

export function getTeacherStudentsPage(params) {
  return request.get('/teacher-student/page', { params })
}

export function getAllTeachers() {
  return request.get('/teacher-student/admin/teachers')
}

export function getAllStudentsForAdmin() {
  return request.get('/teacher-student/admin/students')
}

export function getTeacherStudents(teacherId) {
  return request.get(`/teacher-student/admin/teacher/${teacherId}/students`)
}

export function adminAssignStudent(teacherId, studentId) {
  return request.post('/teacher-student/admin/assign', { teacherId, studentId })
}

export function adminRemoveStudent(id) {
  return request.delete(`/teacher-student/admin/${id}`)
}
