import axios from 'axios'
import { message } from '../utils/message'

// 创建 axios 实例
const service = axios.create({
  baseURL: '/api/v0', // API 基础路径
  timeout: 30000, // 请求超时时间
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
service.interceptors.request.use(
  config => {
    // 从 localStorage 获取 token
    const token = localStorage.getItem('token')
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  error => {
    console.error('请求错误:', error)
    message.error(error.message || '请求错误')
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  response => {
    const res = response.data
    // 如果后端返回的是 { code, msg, data } 格式
    if (res.code !== undefined) {
      if (res.code === 200 || res.code === 0) {
        // 返回 data 数据
        return res.data
      } else {
        // 业务错误
        message.error(res.msg || '请求失败')
        return Promise.reject(new Error(res.msg || '请求失败'))
      }
    }
    // 如果不是标准格式，直接返回
    return res
  },
  error => {
    console.error('响应错误:', error)
    
    // 处理不同的 HTTP 状态码
    if (error.response) {
      const { status, data } = error.response
      
      switch (status) {
        case 400:
          message.error(data?.message || '请求参数错误')
          break
        case 401:
          message.error('未授权，请重新登录')
          // 清除 token 并跳转到登录页
          localStorage.removeItem('token')
          localStorage.removeItem('user')
          window.location.href = '/login'
          break
        case 403:
          message.error('拒绝访问')
          break
        case 404:
          message.error('请求资源不存在')
          break
        case 500:
          message.error('服务器内部错误')
          break
        case 502:
          message.error('网关错误')
          break
        case 503:
          message.error('服务不可用')
          break
        case 504:
          message.error('网关超时')
          break
        default:
          message.error(data?.message || '请求失败，请稍后重试')
      }
    } else if (error.request) {
      // 请求已发送但没有收到响应
      message.error('网络错误，请检查网络连接')
    } else {
      // 请求配置出错
      message.error(error.message || '请求配置错误')
    }
    
    return Promise.reject(error)
  }
)

// 导出请求方法
export const request = {
  /**
   * GET 请求
   * @param {string} url - 请求地址
   * @param {object} params - 请求参数
   * @param {object} config - 其他配置
   */
  get(url, params = {}, config = {}) {
    return service.get(url, { params, ...config })
  },

  /**
   * POST 请求
   * @param {string} url - 请求地址
   * @param {object} data - 请求数据
   * @param {object} config - 其他配置
   */
  post(url, data = {}, config = {}) {
    return service.post(url, data, config)
  },

  /**
   * PUT 请求
   * @param {string} url - 请求地址
   * @param {object} data - 请求数据
   * @param {object} config - 其他配置
   */
  put(url, data = {}, config = {}) {
    return service.put(url, data, config)
  },

  /**
   * DELETE 请求
   * @param {string} url - 请求地址
   * @param {object} config - 其他配置
   */
  delete(url, config = {}) {
    return service.delete(url, config)
  },

  /**
   * 上传文件
   * @param {string} url - 请求地址
   * @param {FormData} formData - FormData 对象
   * @param {object} config - 其他配置
   */
  upload(url, formData, config = {}) {
    return service.post(url, formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      },
      ...config
    })
  },

  /**
   * 下载文件
   * @param {string} url - 请求地址
   * @param {object} params - 请求参数
   * @param {string} filename - 保存的文件名
   */
  async download(url, params = {}, filename = 'download') {
    const response = await service.get(url, {
      params,
      responseType: 'blob'
    })
    
    // 创建下载链接
    const blob = new Blob([response.data])
    const link = document.createElement('a')
    link.href = window.URL.createObjectURL(blob)
    link.download = filename
    link.click()
    window.URL.revokeObjectURL(link.href)
  }
}

export default service
