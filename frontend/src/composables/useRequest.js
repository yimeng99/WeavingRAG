import { ref } from 'vue'
import { request } from '../utils/request'

/**
 * 在 Vue 组件中使用的请求 hooks
 * 使用示例:
 * const { loading, error, data } = useRequest()
 * await data.get('/api')
 */
export function useRequest() {
  const loading = ref(false)
  const error = ref(null)
  const data = ref(null)

  const execute = async (promise) => {
    loading.value = true
    error.value = null
    try {
      data.value = await promise
      return data.value
    } catch (err) {
      error.value = err
      throw err
    } finally {
      loading.value = false
    }
  }

  return {
    loading,
    error,
    data,
    get: (url, params, config) => execute(request.get(url, params, config)),
    post: (url, data, config) => execute(request.post(url, data, config)),
    put: (url, data, config) => execute(request.put(url, data, config)),
    delete: (url, config) => execute(request.delete(url, config)),
    upload: (url, formData, config) => execute(request.upload(url, formData, config))
  }
}
