import { request } from './request'

/**
 * 认证相关 API
 */
export const authApi = {
  /**
   * 用户登录
   * @param {string} username - 用户名
   * @param {string} password - 密码
   */
  login(username, password) {
    return request.post('/auth/login', { username, password })
  },

  /**
   * 用户登出
   */
  logout() {
    return request.post('/auth/logout')
  },

  /**
   * 刷新 token
   */
  refreshToken() {
    return request.post('/auth/refresh')
  },

  /**
   * 获取当前用户信息
   */
  getCurrentUser() {
    return request.get('/auth/user')
  },

  /**
   * 发送验证码
   */
  sendCode(email) {
    return request.post('/auth/send-code', { email })
  },

  /**
   * 重置密码
   */
  resetPassword(email, code, newPassword) {
    return request.post('/auth/reset-password', { email, code, newPassword })
  }
}

export default authApi
