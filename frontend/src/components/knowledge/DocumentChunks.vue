<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import IconSvg from '../ui/IconSvg.vue'
import { message } from '../../utils/message'

const router = useRouter()
const route = useRoute()

const docId = route.params.id
const knowledgeBaseId = ref('')
const docTitle = ref('')
const chunks = ref([])
const loading = ref(false)
const searchText = ref('')

const pagination = ref({
  current: 1,
  pageSize: 20,
  total: 0,
  totalPages: 0
})

const pageSizeOptions = [10, 20, 50, 100]

const loadDocument = async () => {
  if (!docId) return
  try {
    const response = await fetch(`/api/v0/knowledge/documents/${docId}`)
    const data = await response.json()
    if (data.code === 200 && data.data) {
      docTitle.value = data.data.title || '未知文档'
      knowledgeBaseId.value = data.data.knowledgeBaseId || ''
    }
  } catch (e) {
    console.error('加载文档信息失败', e)
  }
}

const loadChunks = async () => {
  if (!docId || !knowledgeBaseId.value) return
  loading.value = true
  try {
    const params = new URLSearchParams({
      knowledgeBaseId: knowledgeBaseId.value,
      docId: docId,
      current: pagination.value.current,
      size: pagination.value.pageSize
    })
    const response = await fetch(`/api/v0/knowledge/chunk/list?${params}`)
    const data = await response.json()
    if (data.code === 200 && data.data) {
      chunks.value = data.data.records || []
      pagination.value.total = data.data.total || 0
      pagination.value.totalPages = data.data.totalPages || 0
    } else {
      message.error(data.msg || '加载切片失败')
    }
  } catch (e) {
    console.error('加载切片异常:', e)
    message.error('加载切片失败')
  } finally {
    loading.value = false
  }
}

const handleBack = () => {
  router.back()
}

const copyChunk = (content) => {
  navigator.clipboard.writeText(content)
  message.success('复制成功')
}

const handlePageChange = (page) => {
  if (page < 1 || page > pagination.value.totalPages) return
  pagination.value.current = page
  loadChunks()
}

const handlePageSizeChange = (size) => {
  pagination.value.pageSize = size
  pagination.value.current = 1
  loadChunks()
}

const visiblePages = computed(() => {
  const current = pagination.value.current
  const total = pagination.value.totalPages
  const pages = []

  if (total <= 7) {
    for (let i = 1; i <= total; i++) pages.push(i)
  } else {
    if (current <= 4) {
      for (let i = 1; i <= 5; i++) pages.push(i)
      pages.push('...')
      pages.push(total)
    } else if (current >= total - 3) {
      pages.push(1)
      pages.push('...')
      for (let i = total - 4; i <= total; i++) pages.push(i)
    } else {
      pages.push(1)
      pages.push('...')
      for (let i = current - 1; i <= current + 1; i++) pages.push(i)
      pages.push('...')
      pages.push(total)
    }
  }

  return pages
})

const handleSearch = () => {
  pagination.value.current = 1
  loadChunks()
}

const getStatusText = (status) => {
  return status === 1 ? '已向量化' : '未向量化'
}

const getStatusClass = (status) => {
  return status === 1 ? 'status-success' : 'status-pending'
}

const editChunk = (chunk) => {
  router.push(`/knowledge/chunks/${docId}/edit/${chunk.id}`)
}

const rechunkSingle = async (chunk) => {
  if (!chunk.id) {
    message.error('切片 ID 不存在')
    return
  }

  try {
    const response = await fetch(`/api/v0/knowledge/chunks/${chunk.id}/rechunk`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' }
    })
    const data = await response.json()

    if (data.code === 200) {
      message.success('重新切片成功')
      loadChunks()
    } else {
      message.error(data.msg || '重新切片失败')
    }
  } catch (e) {
    console.error('重新切片失败:', e)
    message.error('重新切片失败')
  }
}

const deleteChunk = async (chunk) => {
  if (!chunk.id) {
    message.error('切片 ID 不存在')
    return
  }

  if (!confirm('确定要删除该切片吗？此操作不可恢复。')) {
    return
  }

  try {
    const response = await fetch(`/api/v0/knowledge/chunks/${chunk.id}`, {
      method: 'DELETE'
    })
    const data = await response.json()

    if (data.code === 200) {
      message.success('删除成功')
      loadChunks()
    } else {
      message.error(data.msg || '删除失败')
    }
  } catch (e) {
    console.error('删除失败:', e)
    message.error('删除失败')
  }
}

onMounted(() => {
  loadDocument().then(() => {
    loadChunks()
  })
})
</script>

<template>
  <div class="chunks-page">
    <div class="page-header">
      <div class="header-left">
        <button class="back-btn" @click="handleBack">
          <IconSvg name="arrow-left" :size="16" color="#6b7280" />
        </button>
        <div class="header-info">
          <h2 class="page-title">文档切片</h2>
          <p class="page-subtitle" v-if="docTitle">{{ docTitle }}</p>
        </div>
      </div>
      <div class="header-right">
        <div class="search-box">
          <IconSvg name="search" :size="14" color="#9ca3af" />
          <input 
            v-model="searchText"
            type="text"
            placeholder="搜索切片内容..."
            @keyup.enter="handleSearch"
          >
        </div>
      </div>
    </div>

    <div class="page-content">
      <div class="stats-bar">
        <div class="stat-item">
          <span class="stat-label">总切片数</span>
          <span class="stat-value">{{ pagination.total }}</span>
        </div>
        <div class="stat-item">
          <span class="stat-label">已向量化</span>
          <span class="stat-value success">{{ chunks.filter(c => c.status === 1 || c.vectorId).length }}</span>
        </div>
        <div class="stat-item">
          <span class="stat-label">待处理</span>
          <span class="stat-value warning">{{ chunks.filter(c => c.status !== 1 && !c.vectorId).length }}</span>
        </div>
      </div>

      <div class="loading-state" v-if="loading">
        <IconSvg name="loader" :size="24" color="#7c3aed" class="spin" />
        <p>加载中...</p>
      </div>

      <div class="empty-state" v-else-if="chunks.length === 0">
        <IconSvg name="file" :size="40" color="#d1d5db" />
        <p>暂无切片数据</p>
      </div>

      <template v-else>
        <div class="chunks-list">
          <div
            v-for="(chunk, index) in chunks"
            :key="chunk.id || index"
            class="chunk-card"
          >
            <div class="chunk-header">
              <div class="chunk-info">
                <span class="chunk-index">切片 {{ chunk.chunkIndex !== undefined ? chunk.chunkIndex + 1 : (pagination.current - 1) * pagination.pageSize + index + 1 }}</span>
                <span class="chunk-divider">|</span>
                <span class="chunk-length">{{ chunk.content?.length || 0 }} 字符</span>
                <span 
                  class="chunk-status"
                  :class="getStatusClass(chunk.status)"
                >
                  {{ getStatusText(chunk.status) }}
                </span>
              </div>
              <div class="chunk-actions">
                <button class="action-btn" @click.stop="copyChunk(chunk.content)" title="复制">
                  <IconSvg name="download" :size="14" color="#9ca3af" />
                  复制
                </button>
                <button class="action-btn" @click.stop="editChunk(chunk)" title="编辑">
                  <IconSvg name="edit" :size="14" color="#9ca3af" />
                  编辑
                </button>
                <button class="action-btn warning" @click.stop="rechunkSingle(chunk)" title="重新切片">
                  <IconSvg name="scissors" :size="14" color="#9ca3af" />
                  重切
                </button>
                <button class="action-btn danger" @click.stop="deleteChunk(chunk)" title="删除">
                  <IconSvg name="trash" :size="14" color="#9ca3af" />
                  删除
                </button>
              </div>
            </div>
            <div class="chunk-content">
              {{ chunk.content }}
            </div>
          </div>
        </div>

        <div class="pagination-wrapper" v-if="pagination.totalPages > 1">
          <div class="page-size-selector">
            <span class="page-size-label">每页</span>
            <select v-model="pagination.pageSize" @change="handlePageSizeChange(pagination.pageSize)">
              <option v-for="size in pageSizeOptions" :key="size" :value="size">{{ size }}</option>
            </select>
            <span class="page-size-label">条</span>
          </div>
          <div class="pagination-controls">
            <button 
              class="page-btn prev-btn" 
              :disabled="pagination.current <= 1"
              @click="handlePageChange(pagination.current - 1)"
            >
              <IconSvg name="chevron-left" :size="14" color="#6b7280" />
            </button>
            <template v-for="(page, index) in visiblePages" :key="index">
              <span v-if="page === '...'" class="page-ellipsis">...</span>
              <button 
                v-else
                class="page-btn"
                :class="{ active: page === pagination.current }"
                @click="handlePageChange(page)"
              >
                {{ page }}
              </button>
            </template>
            <button 
              class="page-btn next-btn" 
              :disabled="pagination.current >= pagination.totalPages"
              @click="handlePageChange(pagination.current + 1)"
            >
              <IconSvg name="chevron-right" :size="14" color="#6b7280" />
            </button>
          </div>
          <span class="page-total">共 {{ pagination.total }} 条</span>
        </div>
      </template>
    </div>
  </div>
</template>

<style lang="scss" scoped>


.chunks-page {
  width: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.page-header {
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(8px);
  border-bottom: 1px solid $gray-100;
  padding: $spacing-sm $spacing-lg;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-left {
  display: flex;
  align-items: center;
  gap: $spacing-md;
}

.back-btn {
  width: 32px;
  height: 32px;
  border-radius: $radius-md;
  border: 1px solid $gray-200;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s ease;
  
  &:hover {
    background: $gray-50;
    border-color: $purple-300;
  }
}

.header-info {
  .page-title {
    font-size: $font-size-base;
    font-weight: 700;
    color: $gray-800;
    margin: 0;
  }
  
  .page-subtitle {
    font-size: $font-size-xs;
    color: $gray-500;
    margin: 2px 0 0 0;
  }
}

.header-right {
  display: flex;
  align-items: center;
  gap: $spacing-md;
  
  .search-box {
    position: relative;
    display: flex;
    align-items: center;
    
    svg {
      position: absolute;
      left: 10px;
    }
    
    input {
      padding: 8px 12px 8px 32px;
      border: 1px solid $gray-200;
      border-radius: $radius-md;
      font-size: $font-size-sm;
      width: 240px;
      
      &:focus {
        border-color: $purple-400;
        outline: none;
      }
      
      &::placeholder {
        color: $gray-400;
      }
    }
  }
}

.page-content {
  flex: 1;
  overflow-y: auto;
  padding: $spacing-lg;
}

.stats-bar {
  display: flex;
  gap: $spacing-lg;
  padding: $spacing-md $spacing-lg;
  background: #fff;
  border-radius: $radius-lg;
  border: 1px solid $gray-100;
  margin-bottom: $spacing-md;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  
  .stat-label {
    font-size: $font-size-xs;
    color: $gray-500;
  }
  
  .stat-value {
    font-size: $font-size-base;
    font-weight: 600;
    color: $gray-800;
    
    &.success {
      color: #10b981;
    }
    
    &.warning {
      color: #f59e0b;
    }
  }
}

.loading-state,
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: $spacing-xl * 2;
  gap: $spacing-md;
  
  .spin {
    animation: spin 1s linear infinite;
  }
  
  p {
    font-size: $font-size-sm;
    color: $gray-500;
    margin: 0;
  }
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.chunks-list {
  display: flex;
  flex-direction: column;
  gap: $spacing-md;
}

.chunk-card {
  background: #fff;
  border-radius: $radius-lg;
  border: 1px solid $gray-100;
  overflow: hidden;
  transition: all 0.2s ease;
  
  &:hover {
    border-color: $purple-200;
    box-shadow: 0 4px 12px rgba(139, 92, 246, 0.08);
  }
}

.chunk-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: $spacing-md $spacing-lg;
  border-bottom: 1px solid $gray-100;
  background: $gray-50;
}

.chunk-info {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  
  .chunk-index {
    font-size: $font-size-sm;
    font-weight: 600;
    color: $purple-600;
  }
  
  .chunk-divider {
    color: $gray-300;
  }
  
  .chunk-length {
    font-size: $font-size-xs;
    color: $gray-500;
  }
  
  .chunk-status {
    font-size: 10px;
    padding: 2px 8px;
    border-radius: 9999px;
    margin-left: $spacing-xs;
    
    &.status-success {
      background: #dcfce7;
      color: #166534;
    }
    
    &.status-pending {
      background: #fef3c7;
      color: #92400e;
    }
  }
}

.chunk-actions {
  display: flex;
  gap: $spacing-xs;
}

.action-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  border: none;
  background: transparent;
  border-radius: $radius-sm;
  font-size: $font-size-xs;
  color: $gray-500;
  cursor: pointer;
  transition: all 0.2s ease;
  
  &:hover {
    background: $purple-50;
    color: $purple-600;
  }
  
  &.warning:hover {
    background: #fef3c7;
    color: #d97706;
  }
  
  &.danger:hover {
    background: #fef2f2;
    color: #dc2626;
  }
}

.chunk-content {
  padding: $spacing-lg;
  font-size: 14px;
  line-height: 1.8;
  color: $gray-700;
  white-space: pre-wrap;
  word-break: break-word;
}

.pagination-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: $spacing-md;
  padding: $spacing-lg;
  background: #fff;
  border-radius: $radius-lg;
  border: 1px solid $gray-100;
  margin-top: $spacing-md;
}

.page-size-selector {
  display: flex;
  align-items: center;
  gap: 6px;
  
  .page-size-label {
    font-size: 13px;
    color: $gray-500;
  }
  
  select {
    padding: 4px 8px;
    border: 1px solid $gray-200;
    border-radius: $radius-sm;
    font-size: 13px;
    color: $gray-700;
    background: #fff;
    cursor: pointer;
    
    &:focus {
      border-color: $purple-400;
      outline: none;
    }
  }
}

.pagination-controls {
  display: flex;
  align-items: center;
  gap: 4px;
}

.page-btn {
  min-width: 32px;
  height: 32px;
  padding: 0 8px;
  border: 1px solid $gray-200;
  border-radius: $radius-sm;
  background: #fff;
  font-size: 13px;
  color: $gray-600;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  
  &:hover:not(:disabled) {
    border-color: $purple-300;
    color: $purple-600;
  }
  
  &.active {
    background: $purple-600;
    border-color: $purple-600;
    color: #fff;
  }
  
  &:disabled {
    opacity: 0.4;
    cursor: not-allowed;
  }
}

.page-ellipsis {
  padding: 0 4px;
  color: $gray-400;
}

.page-total {
  font-size: 13px;
  color: $gray-500;
}
</style>