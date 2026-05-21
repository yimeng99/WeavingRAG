import request from '@/utils/request'

// ==================== Provider ====================

export function listProviders(params) {
  return request({
    url: '/ai/providers',
    method: 'get',
    params
  })
}

export function getProvider(id) {
  return request({
    url: `/ai/providers/${id}`,
    method: 'get'
  })
}

export function createProvider(data) {
  return request({
    url: '/ai/providers',
    method: 'post',
    data
  })
}

export function updateProvider(id, data) {
  return request({
    url: `/ai/providers/${id}`,
    method: 'put',
    data
  })
}

export function deleteProvider(id) {
  return request({
    url: `/ai/providers/${id}`,
    method: 'delete'
  })
}

// ==================== Model ====================

export function listModels(params) {
  return request({
    url: '/ai/models',
    method: 'get',
    params
  })
}

export function getModel(id) {
  return request({
    url: `/ai/models/${id}`,
    method: 'get'
  })
}

export function getModelByIdentifier(identifier) {
  return request({
    url: `/ai/models/identifier/${identifier}`,
    method: 'get'
  })
}

export function createModel(data) {
  return request({
    url: '/ai/models',
    method: 'post',
    data
  })
}

export function updateModel(id, data) {
  return request({
    url: `/ai/models/${id}`,
    method: 'put',
    data
  })
}

export function deleteModel(id) {
  return request({
    url: `/ai/models/${id}`,
    method: 'delete'
  })
}

// ==================== Service Config ====================

export function listServiceConfigs(params) {
  return request({
    url: '/ai/services',
    method: 'get',
    params
  })
}

export function getActiveServiceConfig(modelIdentifier) {
  return request({
    url: `/ai/services/active/${modelIdentifier}`,
    method: 'get'
  })
}

export function createServiceConfig(data) {
  return request({
    url: '/ai/services',
    method: 'post',
    data
  })
}

export function updateServiceConfig(id, data) {
  return request({
    url: `/ai/services/${id}`,
    method: 'put',
    data
  })
}

export function deleteServiceConfig(id) {
  return request({
    url: `/ai/services/${id}`,
    method: 'delete'
  })
}