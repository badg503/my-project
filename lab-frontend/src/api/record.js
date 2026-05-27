import request from './request'

/**
 * 获取实验任务的学生提交列表
 */
export function getRecordByTask(taskId, params) {
  return request.get(`/record/task/${taskId}`, { params })
}

/**
 * 获取提交统计
 */
export function getRecordStats(taskId) {
  return request.get(`/record/task-stats/${taskId}`)
}

/**
 * 更新提交状态
 */
export function updateRecord(data) {
  return request.put('/record/' + data.id, data)
}
