import { request } from '../utils/request'

/**
 * 用户管理相关 API
 */
export const userApi = {
  /**
   * 获取用户列表（分页）
   * @param {object} params - 查询参数 { current, size, keyword, status }
   */
  getUsers(params) {
    return request.get('/users/page', params)
  },

  /**
   * 获取所有用户（不分页）
   */
  getAllUsers() {
    return request.get('/users')
  },

  /**
   * 获取用户详情
   */
  getUser(id) {
    return request.get(`/users/${id}`)
  },

  /**
   * 创建用户
   */
  createUser(data) {
    return request.post('/users', data)
  },

  /**
   * 更新用户
   */
  updateUser(data) {
    return request.put('/users', data)
  },

  /**
   * 删除用户
   */
  deleteUser(id) {
    return request.delete(`/users/${id}`)
  },

  /**
   * 批量删除用户
   */
  batchDeleteUsers(ids) {
    return request.post('/users/batch-delete', { ids })
  },

  /**
   * 更新用户角色
   */
  updateRole(id, role) {
    return request.put(`/users/${id}/role`, { role })
  },

  /**
   * 更新用户状态
   */
  updateStatus(id, status) {
    return request.put(`/users/${id}/status`, { status })
  },

  /**
   * 邀请用户
   */
  inviteUser(data) {
    return request.post('/users/invite', data)
  },

  /**
   * 获取当前用户信息
   */
  getCurrentUser() {
    return request.get('/users/current')
  },

  /**
   * 更新当前用户信息
   */
  updateCurrentUser(data) {
    return request.put('/users/current', data)
  },

  /**
   * 修改密码
   */
  changePassword(data) {
    return request.post('/users/change-password', data)
  }
}

export default userApi
