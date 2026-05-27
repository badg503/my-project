import request from './request'

export function getAnnouncementList() {
  return request.get('/announcement/list')
}

export function getAnnouncementPage(params) {
  return request.get('/announcement/page', { params })
}

export function getAnnouncementById(id) {
  return request.get(`/announcement/${id}`)
}

export function addAnnouncement(data) {
  return request.post('/announcement', data)
}

export function updateAnnouncement(data) {
  return request.put('/announcement', data)
}

export function deleteAnnouncement(id) {
  return request.delete(`/announcement/${id}`)
}
