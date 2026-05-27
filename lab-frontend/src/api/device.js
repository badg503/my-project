import request from './request'

export function getBorrowPage(params) {
  return request.get('/device-borrow/page', { params })
}

export function approveBorrowApi(id, status) {
  return request.post('/device-borrow/approve', null, { params: { id, status } })
}

export function returnBorrowApi(id, returnRemark) {
  return request.post('/device-borrow/return', null, { params: { id, returnRemark } })
}
