<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import IconSvg from '../../components/ui/IconSvg.vue'
import ConfirmDialog from '../../components/ui/ConfirmDialog.vue'
import { message } from '../../utils/message'

const router = useRouter()
const route = useRoute()

const knowledgeBaseId = computed(() => route.params.id)

const knowledgeBase = ref(null)
const documents = ref([])
const loading = ref(false)
const docLoading = ref(false)

const searchQuery = ref('')
const currentCategory = ref('all')
const selectAll = ref(false)
const selectedDocs = ref([])

const pagination = ref({
  current: 1,
  pageSize: 10,
  total: 0,
  totalPages: 0
})

const categories = [
  { key: 'all', label: '全部文章' },
  { key: 'published', label: '已发布' },
  { key: 'reviewing', label: '审核中' },
  { key: 'draft', label: '草稿' }
]

const stats = ref({
  total: 0
})

const confirmVisible = ref(false)
const confirmTitle = ref('')
const confirmContent = ref('')
const confirmType = ref('warning')
const confirmCallback = ref(null)

const loadKnowledgeBase = async () => {
  if (!knowledgeBaseId.value) return
  loading.value = true
  try {
    const res = await fetch(`/api/v0/knowledge/bases/${knowledgeBaseId.value}`)
    const data = await res.json()
    
    if (data.code === 200 && data.data) {
      knowledgeBase.value = data.data
    } else {
      message.error('加载知识库信息失败')
    }
  } catch (e) {
    console.error('加载知识库信息失败:', e)
    message.error('加载知识库信息失败')
  } finally {
    loading.value = false
  }
}

const loadDocuments = async () => {
  if (!knowledgeBaseId.value) return
  docLoading.value = true
  try {
    const params = new URLSearchParams({
      current: pagination.value.current,
      size: pagination.value.pageSize,
      knowledgeBaseId: knowledgeBaseId.value
    })
    
    if (currentCategory.value !== 'all') {
      const statusMap = { published: 1, reviewing: 0, draft: 0 }
      if (statusMap[currentCategory.value] !== undefined) {
        params.append('status', statusMap[currentCategory.value])
      }
    }
    
    const res = await fetch(`/api/v0/knowledge/documents/page?${params}`)
    const data = await res.json()
    
    if (data.code === 200 && data.data) {
      const records = data.data.records || []
      
      if (searchQuery.value) {
        const query = searchQuery.value.toLowerCase()
        documents.value = records.filter(doc => 
          doc.title?.toLowerCase().includes(query) ||
          doc.tags?.toLowerCase().includes(query)
        )
      } else {
        documents.value = records
      }
      
      pagination.value.total = data.data.total || 0
      pagination.value.totalPages = Math.ceil(pagination.value.total / pagination.value.pageSize)
      
      stats.value = {
        total: pagination.value.total
      }
    }
  } catch (e) {
    console.error('加载文档列表失败:', e)
    message.error('加载文档列表失败')
  } finally {
    docLoading.value = false
  }
}

const getStatusBadge = (status, chunkCount) => {
  if (status === 1) return { class: 'badge-success', text: '已发布' }
  if (status === 0 && chunkCount > 0) return { class: 'badge-warning', text: '审核中' }
  return { class: 'badge-default', text: '草稿' }
}

const handleSearch = () => {
  pagination.value.current = 1
  loadDocuments()
}

const handleCategoryChange = (category) => {
  currentCategory.value = category
  pagination.value.current = 1
  loadDocuments()
}

const toggleSelectAll = () => {
  if (selectAll.value) {
    selectedDocs.value = documents.value.map(d => d.docId)
  } else {
    selectedDocs.value = []
  }
}

const toggleSelect = (docId) => {
  const index = selectedDocs.value.indexOf(docId)
  if (index > -1) {
    selectedDocs.value.splice(index, 1)
  } else {
    selectedDocs.value.push(docId)
  }
  selectAll.value = selectedDocs.value.length === documents.value.length
}

const goBack = () => {
  router.push('/knowledge')
}

const editKnowledgeBase = () => {
  router.push(`/knowledge/base/edit/${knowledgeBaseId.value}`)
}

const uploadDocument = () => {
  router.push(`/knowledge/upload?baseId=${knowledgeBaseId.value}`)
}

const viewDocument = (doc) => {
  router.push(`/knowledge/chunks/${doc.docId}`)
}

const editDocument = (doc) => {
  router.push(`/knowledge/doc/edit/${doc.docId}`)
}

const showConfirm = (title, content, type, callback) => {
  confirmTitle.value = title
  confirmContent.value = content
  confirmType.value = type
  confirmCallback.value = callback
  confirmVisible.value = true
}

const handleConfirm = () => {
  if (confirmCallback.value) {
    confirmCallback.value()
  }
}

const deleteDocument = (doc) => {
  showConfirm(
    '删除确认',
    '确定要删除该文档吗？删除后将同时删除向量库中的数据，无法恢复。',
    'danger',
    async () => {
      try {
        const res = await fetch(`/api/v0/knowledge/documents/${doc.docId}`, {
          method: 'DELETE'
        })
        const data = await res.json()
        
        if (data.code === 200) {
          message.success('删除成功')
          loadDocuments()
        } else {
          message.error(data.msg || '删除失败')
        }
      } catch (e) {
        console.error('删除失败:', e)
        message.error('删除失败')
      }
    }
  )
}

const batchDelete = () => {
  if (selectedDocs.value.length === 0) return
  
  showConfirm(
    '批量删除确认',
    `确定要删除选中的 ${selectedDocs.value.length} 个文档吗？删除后将同时删除向量库中的数据，无法恢复。`,
    'danger',
    async () => {
      let successCount = 0
      let failCount = 0
      
      for (const docId of selectedDocs.value) {
        try {
          const res = await fetch(`/api/v0/knowledge/documents/${docId}`, {
            method: 'DELETE'
          })
          const data = await res.json()
          
          if (data.code === 200) {
            successCount++
          } else {
            failCount++
          }
        } catch (e) {
          failCount++
        }
      }
      
      if (successCount > 0) {
        message.success(`成功删除 ${successCount} 个文档`)
        selectedDocs.value = []
        selectAll.value = false
        loadDocuments()
      }
      if (failCount > 0) {
        message.error(`${failCount} 个文档删除失败`)
      }
    }
  )
}

const rechunkDocument = (doc) => {
  showConfirm(
    '重新切片确认',
    '确定要重新切片该文档吗？这将删除现有切片并重新生成。',
    'warning',
    async () => {
      try {
        const res = await fetch(`/api/v0/knowledge/documents/${doc.docId}/rechunk`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            chunkingStrategy: 'intelligent',
            chunkSize: 500,
            overlap: 50
          })
        })
        const data = await res.json()
        
        if (data.code === 200) {
          message.success(`重新切片成功，共 ${data.data?.chunkCount || 0} 个切片`)
          loadDocuments()
        } else {
          message.error(data.msg || '重新切片失败')
        }
      } catch (e) {
        console.error('重新切片失败:', e)
        message.error('重新切片失败')
      }
    }
  )
}

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN')
}

const formatViews = (num) => {
  if (!num) return '0'
  if (num >= 1000) return (num / 1000).toFixed(1) + 'k'
  return num.toString()
}

const handlePageChange = (page) => {
  if (page < 1 || page > pagination.value.totalPages) return
  pagination.value.current = page
  loadDocuments()
}

const handlePageSizeChange = (size) => {
  pagination.value.pageSize = size
  pagination.value.current = 1
  loadDocuments()
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

const pageSizeOptions = [10, 20, 50, 100]

onMounted(() => {
  loadKnowledgeBase()
  loadDocuments()
})

watch(knowledgeBaseId, () => {
  loadKnowledgeBase()
  loadDocuments()
})
</script>

<template>
  <div class="knowledge-detail-page">
    <div class="page-header">
      <div class="header-left">
        <button class="back-btn" @click="goBack">
          <IconSvg name="arrow-left" :size="16" color="#6b7280" />
        </button>
        <div class="kb-info" v-if="knowledgeBase">
          <div class="kb-icon" :style="{ background: knowledgeBase.color || '#3b82f6' }">
            <IconSvg :name="knowledgeBase.icon || 'folder'" :size="16" color="#fff" />
          </div>
          <div class="kb-meta">
            <h2 class="kb-name">{{ knowledgeBase.name }}</h2>
            <p class="kb-desc">{{ knowledgeBase.description || '暂无描述' }}</p>
          </div>
        </div>
        <div class="kb-info" v-else>
          <div class="kb-icon" style="background: #3b82f6">
            <IconSvg name="folder" :size="16" color="#fff" />
          </div>
          <div class="kb-meta">
            <h2 class="kb-name">加载中...</h2>
          </div>
        </div>
      </div>
      <div class="header-right">
        <button class="btn btn-primary" @click="uploadDocument">
          <IconSvg name="upload" :size="14" color="#fff" />
          上传文章
        </button>
      </div>
    </div>

    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-content">
          <p class="stat-label">总文章数</p>
          <p class="stat-value">{{ stats.total }}</p>
          <span class="stat-tag tag-success">共 {{ knowledgeBase?.docCount || 0 }} 篇</span>
        </div>
        <div class="stat-icon stat-icon-purple">
          <IconSvg name="file-text" :size="16" color="#7c3aed" />
        </div>
      </div>
    </div>

    <div class="filter-bar">
      <div class="category-tabs">
        <button 
          v-for="cat in categories" 
          :key="cat.key"
          class="category-tab"
          :class="{ active: currentCategory === cat.key }"
          @click="handleCategoryChange(cat.key)"
        >
          {{ cat.label }}
        </button>
      </div>
      <div class="search-bar">
        <div class="search-input">
          <IconSvg name="search" :size="14" color="#9ca3af" />
          <input 
            v-model="searchQuery"
            type="text"
            placeholder="搜索标题或标签..."
            @keyup.enter="handleSearch"
          >
        </div>
        <button class="btn btn-outline-sm" @click="handleSearch">
          <IconSvg name="filter" :size="12" color="#6b7280" />
          筛选
        </button>
      </div>
    </div>

    <div class="document-table">
      <table>
        <thead>
          <tr>
            <th class="col-check">
              <label class="checkbox-label">
                <input 
                  type="checkbox" 
                  :checked="selectAll"
                  @change="toggleSelectAll"
                >
              </label>
            </th>
            <th class="col-title">标题</th>
            <th class="col-type">类型</th>
            <th class="col-status">状态</th>
            <th class="col-date">更新日期</th>
            <th class="col-actions">操作</th>
          </tr>
        </thead>
        <tbody v-if="!docLoading">
          <tr 
            v-for="doc in documents" 
            :key="doc.docId"
            class="doc-row"
            :class="{ selected: selectedDocs.includes(doc.docId) }"
            @click="viewDocument(doc)"
          >
            <td class="col-check" @click.stop>
              <label class="checkbox-label">
                <input 
                  type="checkbox"
                  :checked="selectedDocs.includes(doc.docId)"
                  @change="toggleSelect(doc.docId)"
                >
              </label>
            </td>
            <td class="col-title">
              <div class="doc-title-info">
                <a class="doc-title">{{ doc.title }}</a>
                <p class="doc-summary" v-if="doc.content">{{ doc.content.substring(0, 80) }}...</p>
              </div>
            </td>
            <td class="col-type">
              <span class="type-badge">{{ doc.type || '文本' }}</span>
            </td>
            <td class="col-status">
              <span 
                class="status-badge"
                :class="getStatusBadge(doc.status, doc.chunkCount).class"
              >
                {{ getStatusBadge(doc.status, doc.chunkCount).text }}
              </span>
            </td>
            <td class="col-date">{{ formatDate(doc.updateTime) }}</td>
            <td class="col-actions" @click.stop>
              <button class="action-btn" @click="viewDocument(doc)" title="查看切片">
                <IconSvg name="eye" :size="14" color="#9ca3af" />
              </button>
              <button class="action-btn" @click="rechunkDocument(doc)" title="重新切片">
                <IconSvg name="scissors" :size="14" color="#9ca3af" />
              </button>
              <button class="action-btn" @click="deleteDocument(doc)" title="删除">
                <IconSvg name="trash" :size="14" color="#9ca3af" />
              </button>
            </td>
          </tr>
          <tr v-if="documents.length === 0">
            <td colspan="6" class="empty-cell">
              <div class="empty-state">
                <IconSvg name="file" :size="32" color="#d1d5db" />
                <p>暂无文档</p>
                <button class="btn btn-primary" @click="uploadDocument">
                  <IconSvg name="upload" :size="14" color="#fff" />
                  上传文章
                </button>
              </div>
            </td>
          </tr>
        </tbody>
        <tbody v-else>
          <tr>
            <td colspan="6" class="loading-cell">
              <div class="loading-state">
                <IconSvg name="loader" :size="20" color="#7c3aed" class="spin" />
                <p>加载中...</p>
              </div>
            </td>
          </tr>
        </tbody>
      </table>

      <div class="table-footer" v-if="documents.length > 0">
        <div class="batch-actions">
          <span class="selected-count">已选择 <strong>{{ selectedDocs.length }}</strong> 项</span>
          <div class="action-divider"></div>
          <button class="batch-btn" :disabled="selectedDocs.length === 0" @click="batchDelete">
            <IconSvg name="trash" :size="12" color="#6b7280" />
            批量删除
          </button>
        </div>
        <div class="pagination-wrapper">
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
      </div>
    </div>

    <div class="upload-tip">
      <div class="tip-icon">
        <IconSvg name="upload" :size="16" color="#7c3aed" />
      </div>
      <div class="tip-content">
        <p class="tip-title">批量上传文章</p>
        <p class="tip-desc">支持 Markdown、Word、PDF 格式，一键导入知识库</p>
      </div>
      <button class="btn btn-primary" @click="uploadDocument">开始上传</button>
    </div>

    <ConfirmDialog 
      v-model="confirmVisible"
      :title="confirmTitle"
      :content="confirmContent"
      :type="confirmType"
      @confirm="handleConfirm"
    />
  </div>
</template>

<style lang="scss" scoped>


.knowledge-detail-page {
  width: 100%;
  padding: 0 $spacing-sm;
  overflow-y: auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: $spacing-md $spacing-sm;
  background: #fff;
  border-radius: $radius-lg;
  border: 1px solid $gray-100;
  margin-bottom: $spacing-md;
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

.kb-info {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
}

.kb-icon {
  width: 40px;
  height: 40px;
  border-radius: $radius-lg;
  display: flex;
  align-items: center;
  justify-content: center;
}

.kb-meta {
  .kb-name {
    font-size: $font-size-base;
    font-weight: 700;
    color: $gray-800;
    margin: 0;
  }
  
  .kb-desc {
    font-size: $font-size-xs;
    color: $gray-500;
    margin: 2px 0 0 0;
  }
}

.header-right {
  display: flex;
  gap: $spacing-sm;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(1, 1fr);
  gap: $spacing-md;
  margin-bottom: $spacing-md;
}

.stat-card {
  background: #fff;
  border-radius: $radius-lg;
  border: 1px solid $gray-100;
  padding: $spacing-md;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  transition: all 0.2s ease;
  
  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba(139, 92, 246, 0.08);
  }
}

.stat-content {
  .stat-label {
    font-size: $font-size-xs;
    color: $gray-500;
    margin: 0;
  }
  
  .stat-value {
    font-size: $font-size-xl;
    font-weight: 700;
    color: $gray-800;
    margin: 4px 0;
  }
  
  .stat-tag {
    font-size: 10px;
    padding: 2px 6px;
    border-radius: 9999px;
    background: #dcfce7;
    color: #166534;
  }
}

.stat-icon {
  width: 32px;
  height: 32px;
  border-radius: $radius-md;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f3e8ff;
}

.filter-bar {
  background: #fff;
  border-radius: $radius-lg;
  border: 1px solid $gray-100;
  padding: $spacing-md;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: $spacing-md;
  margin-bottom: $spacing-md;
}

.category-tabs {
  display: flex;
  gap: $spacing-sm;
}

.category-tab {
  padding: 6px 12px;
  border-radius: $radius-md;
  border: 1px solid $gray-200;
  background: #fff;
  font-size: $font-size-xs;
  font-weight: 500;
  color: $gray-600;
  cursor: pointer;
  transition: all 0.2s ease;
  
  &:hover {
    background: $purple-50;
    border-color: $purple-300;
    color: $purple-600;
  }
  
  &.active {
    background: $purple-600;
    border-color: $purple-600;
    color: #fff;
  }
}

.search-bar {
  display: flex;
  gap: $spacing-sm;
}

.search-input {
  position: relative;
  display: flex;
  align-items: center;
  
  svg {
    position: absolute;
    left: 10px;
  }
  
  input {
    padding: 6px 10px 6px 32px;
    border: 1px solid $gray-200;
    border-radius: $radius-md;
    font-size: $font-size-xs;
    width: 200px;
    
    &:focus {
      border-color: $purple-400;
      outline: none;
    }
  }
}

.btn {
  display: inline-flex;
  align-items: center;
  gap: $spacing-xs;
  padding: 8px 12px;
  border-radius: $radius-md;
  font-size: $font-size-xs;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  
  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
}

.btn-outline {
  background: #fff;
  border: 1px solid $gray-200;
  color: $gray-600;
  
  &:hover:not(:disabled) {
    background: $gray-50;
    border-color: $gray-300;
  }
}

.btn-outline-sm {
  padding: 6px 10px;
  background: #fff;
  border: 1px solid $gray-200;
  color: $gray-600;
  
  &:hover:not(:disabled) {
    background: $gray-50;
    border-color: $purple-300;
    color: $purple-600;
  }
}

.btn-primary {
  background: $purple-600;
  border: none;
  color: #fff;
  
  &:hover:not(:disabled) {
    background: $purple-700;
  }
}

.document-table {
  background: #fff;
  border-radius: $radius-lg;
  border: 1px solid $gray-100;
  overflow: hidden;
  margin-bottom: $spacing-md;
  
  table {
    width: 100%;
    border-collapse: collapse;
  }
  
  th {
    padding: 12px 16px;
    text-align: center;
    font-size: 12px;
    font-weight: 600;
    color: $gray-600;
    background: $gray-50;
    border-bottom: 1px solid $gray-100;
    text-transform: uppercase;
  }
  
td {
    padding: 12px 16px;
    border-bottom: 1px solid $gray-100;
    text-align: center;
    font-size: 13px;
    color: $gray-700;
  }
  
  .col-check { width: 40px; }
  .col-title { min-width: 200px; text-align: left; }
  .col-type { width: 80px; }
  .col-status { width: 80px; }
  .col-date { width: 100px; }
  .col-actions { 
    width: 140px; 
    text-align: center;
    white-space: nowrap;
  }
}

.checkbox-label {
  display: flex;
  align-items: center;
  
  input[type="checkbox"] {
    width: 14px;
    height: 14px;
    border-radius: 3px;
    border: 1px solid $gray-300;
    accent-color: $purple-600;
    cursor: pointer;
  }
}

.doc-row {
  cursor: pointer;
  transition: all 0.2s ease;
  
  &:hover {
    background: #faf5ff;
  }
  
  &.selected {
    background: #f3e8ff;
  }
}

.doc-title-info {
  .doc-title {
    font-size: 14px;
    font-weight: 500;
    color: $gray-800;
    cursor: pointer;
    
    &:hover {
      color: $purple-600;
    }
  }
  
  .doc-summary {
    font-size: 12px;
    color: $gray-400;
    margin: 2px 0 0 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.type-badge {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 9999px;
  background: $purple-50;
  color: $purple-700;
}

.status-badge {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 9999px;
  
  &.badge-success { background: #dcfce7; color: #166534; }
  &.badge-warning { background: #fef3c7; color: #92400e; }
  &.badge-default { background: $gray-100; color: $gray-600; }
}

.action-btn {
  width: 28px;
  height: 28px;
  border-radius: $radius-md;
  border: none;
  background: transparent;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s ease;
  
  &:hover {
    background: $gray-100;
    
    svg { color: $purple-500 !important; }
  }
}

.empty-cell, .loading-cell {
  padding: $spacing-xl !important;
}

.empty-state, .loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: $spacing-md;
  
  p {
    font-size: 14px;
    color: $gray-500;
    margin: 0;
  }
  
  .spin {
    animation: spin 1s linear infinite;
  }
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.table-footer {
  padding: $spacing-md $spacing-lg;
  border-top: 1px solid $gray-100;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.batch-actions {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  
  .selected-count {
    font-size: 13px;
    color: $gray-500;
    
    strong { color: $gray-700; }
  }
  
  .action-divider {
    width: 1px;
    height: 16px;
    background: $gray-200;
  }
  
  .batch-btn {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    padding: 4px 8px;
    font-size: 13px;
    color: $gray-500;
    background: transparent;
    border: none;
    cursor: pointer;
    
    &:hover:not(:disabled) {
      color: $purple-600;
    }
    
    &:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }
  }
}

.pagination {
  .page-info {
    font-size: 13px;
    color: $gray-500;
  }
}

.pagination-wrapper {
  display: flex;
  align-items: center;
  gap: $spacing-md;
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

.upload-tip {
  background: linear-gradient(95deg, #f3e8ff 0%, #fff 50%, #f3e8ff 100%);
  border-radius: $radius-lg;
  border: 1px solid #e9d5ff;
  padding: $spacing-md;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: $spacing-md;
}

.tip-icon {
  width: 32px;
  height: 32px;
  border-radius: $radius-md;
  background: #f3e8ff;
  display: flex;
  align-items: center;
  justify-content: center;
}

.tip-content {
  .tip-title {
    font-size: 13px;
    font-weight: 600;
    color: $gray-800;
    margin: 0;
  }
  
  .tip-desc {
    font-size: 12px;
    color: $gray-500;
    margin: 2px 0 0 0;
  }
}

@media (max-width: 768px) {
  .filter-bar {
    flex-direction: column;
    align-items: stretch;
  }

  .search-bar {
    width: 100%;

    .search-input input {
      width: 100%;
    }
  }
}
</style>