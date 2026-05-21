<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import IconSvg from '../../components/ui/IconSvg.vue'
import { message, confirm } from '../../utils/message'

const router = useRouter()
const route = useRoute()

const loading = ref(false)
const knowledgeBases = ref([])
const total = ref(0)
const pagination = ref({
  current: 1,
  pageSize: 12,
  totalPages: 0
})

const pageSizeOptions = [12, 24, 36, 48]

const stats = ref({
  totalDocs: 0,
  totalGrowth: '+8.3%',
  published: 0,
  publishedPercent: '76.5%',
  reviewing: 0,
  draft: 0
})

const searchQuery = ref('')
const sortBy = ref('recent')
const selectAll = ref(false)

const iconColors = [
  { from: '#3b82f6', to: '#1d4ed8' },
  { from: '#06b6d4', to: '#0891b2' },
  { from: '#6366f1', to: '#4f46e5' },
  { from: '#10b981', to: '#059669' },
  { from: '#ec4899', to: '#db2777' },
  { from: '#f59e0b', to: '#d97706' }
]

const getIconColor = (index) => {
  return iconColors[index % iconColors.length]
}

const categoryIcons = {
  '前端技术': 'code',
  '后端架构': 'database',
  '云原生': 'cloud',
  '数据治理': 'chart-bar',
  'AI/机器学习': 'brain',
  '运维保障': 'settings',
  'default': 'folder'
}

const getCategoryIcon = (category) => {
  return categoryIcons[category] || categoryIcons['default']
}

const selectedCount = computed(() => {
  return knowledgeBases.value.filter(d => d.selected).length
})

const getStatusClass = (status) => {
  const classes = {
    published: 'bg-green-50 text-green-700',
    reviewing: 'bg-amber-50 text-amber-700',
    draft: 'bg-gray-100 text-gray-600',
    recommended: 'bg-purple-50 text-purple-700'
  }
  return classes[status] || classes.published
}

const toggleSelectAll = () => {
  knowledgeBases.value.forEach(doc => {
    doc.selected = selectAll.value
  })
}

const toggleSelect = () => {
  const allSelected = knowledgeBases.value.every(d => d.selected)
  selectAll.value = allSelected
}

const loadKnowledgeBases = async () => {
  loading.value = true
  try {
    const res = await fetch('/api/v0/knowledge/bases')
    const data = await res.json()
    
    if (data.code === 200) {
      let list = data.data || []
      
      if (searchQuery.value) {
        list = list.filter(item => 
          item.name?.toLowerCase().includes(searchQuery.value.toLowerCase())
        )
      }
      
      knowledgeBases.value = list.map((item, index) => ({
        ...item,
        selected: false,
        status: item.status === 1 ? 'published' : 'draft',
        statusText: item.status === 1 ? '已发布' : '草稿',
        iconColor: getIconColor(index)
      }))
      total.value = knowledgeBases.value.length
      pagination.value.totalPages = Math.ceil(total.value / pagination.value.pageSize)
      
      stats.value.totalDocs = total.value
      stats.value.published = knowledgeBases.value.filter(d => d.status === 'published').length
      stats.value.draft = knowledgeBases.value.filter(d => d.status === 'draft').length
    }
  } catch (error) {
    console.error('加载知识库列表失败:', error)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  loadKnowledgeBases()
}

const createNew = () => {
  router.push('/knowledge/new')
}

const viewDetail = (id) => {
  router.push(`/knowledge/${id}`)
}

const editItem = (doc, event) => {
  event.stopPropagation()
  router.push(`/knowledge/base/edit/${doc.id}`)
}

const deleteItem = async (doc, event) => {
  event.stopPropagation()

  return new Promise((resolve) => {
    confirm.show({
      title: '删除确认',
      content: '确定要删除该知识库吗？删除后无法恢复。',
      okText: '删除',
      cancelText: '取消',
      okButtonProps: { danger: true },
      onOk: async () => {
        try {
          const res = await fetch(`/api/v0/knowledge/bases/${doc.id}`, {
            method: 'DELETE'
          })
          const data = await res.json()

          if (data.code === 200) {
            message.success('删除成功')
            loadKnowledgeBases()
          } else {
            message.error(data.msg || '删除失败')
          }
        } catch (error) {
          console.error('删除失败:', error)
          message.error('删除失败')
        }
        resolve(true)
      },
      onCancel: () => {
        resolve(false)
      }
    })
  })
}

const handlePageChange = (page) => {
  if (page < 1 || page > pagination.value.totalPages) return
  pagination.value.current = page
}

const handlePageSizeChange = (size) => {
  pagination.value.pageSize = size
  pagination.value.current = 1
  pagination.value.totalPages = Math.ceil(total.value / size)
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

const paginatedKnowledgeBases = computed(() => {
  const start = (pagination.value.current - 1) * pagination.value.pageSize
  const end = start + pagination.value.pageSize
  return knowledgeBases.value.slice(start, end)
})

onMounted(loadKnowledgeBases)

watch(() => route.path, (newPath) => {
  if (newPath === '/knowledge') {
    loadKnowledgeBases()
  }
})
</script>

<template>
  <div class="knowledge-card-page">
    <div class="page-header">
      <h2 class="page-title">
        <IconSvg name="book-open" :size="20" color="#7c3aed" />
        知识库管理
      </h2>
      <p class="page-desc">管理企业所有知识文档，支持编辑、归档、发布与权限设置</p>
    </div>

    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-content">
          <p class="stat-label">全部文档</p>
          <p class="stat-value">{{ stats.totalDocs.toLocaleString() }}</p>
          <span class="stat-growth growth-positive">{{ stats.totalGrowth }}</span>
        </div>
        <div class="stat-icon stat-icon-purple">
          <IconSvg name="file-text" :size="18" color="#7c3aed" />
        </div>
      </div>
      
      <div class="stat-card">
        <div class="stat-content">
          <p class="stat-label">已发布</p>
          <p class="stat-value">{{ stats.published }}</p>
          <span class="stat-growth growth-success">{{ stats.publishedPercent }}</span>
        </div>
        <div class="stat-icon stat-icon-green">
          <IconSvg name="check" :size="18" color="#10b981" />
        </div>
      </div>
      
      <div class="stat-card">
        <div class="stat-content">
          <p class="stat-label">审核中</p>
          <p class="stat-value">{{ stats.reviewing }}</p>
          <a href="#" class="stat-link">待处理</a>
        </div>
        <div class="stat-icon stat-icon-amber">
          <IconSvg name="loader" :size="18" color="#f59e0b" />
        </div>
      </div>
      
      <div class="stat-card">
        <div class="stat-content">
          <p class="stat-label">草稿箱</p>
          <p class="stat-value">{{ stats.draft }}</p>
          <span class="stat-growth growth-gray">待完善</span>
        </div>
        <div class="stat-icon stat-icon-gray">
          <IconSvg name="edit" :size="18" color="#6b7280" />
        </div>
      </div>
    </div>

    <div class="filter-card">
      <div class="filter-content">
        <div class="filter-left">
          <div class="search-box">
            <IconSvg name="search" :size="12" color="#9ca3af" />
            <input 
              v-model="searchQuery"
              type="text" 
              placeholder="搜索知识库名称..."
              @keyup.enter="handleSearch"
            >
          </div>
          <button class="btn-primary-sm" @click="handleSearch">
            <IconSvg name="search" :size="12" color="#fff" />
            搜索
          </button>
        </div>
        <div class="filter-right">
          <button class="btn-purple-light" @click="createNew">
            <IconSvg name="plus" :size="12" color="#7c3aed" />
            新建知识库
          </button>
        </div>
      </div>
    </div>

    <div class="knowledge-grid" v-if="!loading">
      <div 
        v-for="(doc, index) in paginatedKnowledgeBases" 
        :key="doc.id"
        class="knowledge-card"
        :class="{ 'card-recommended': doc.recommended }"
        @click="viewDetail(doc.id)"
      >
        <div class="card-body">
          <div class="card-header">
            <div 
              class="card-icon"
              :style="{ background: `linear-gradient(135deg, ${doc.iconColor?.from}, ${doc.iconColor?.to})` }"
            >
              <IconSvg :name="getCategoryIcon(doc.category)" :size="18" color="#fff" />
            </div>
            <div class="card-header-content">
              <div class="card-header-top">
                <h3 class="card-title">{{ doc.name }}</h3>
                <button class="more-btn" @click.stop>
                  <IconSvg name="more-vertical" :size="14" color="#9ca3af" />
                </button>
              </div>
            </div>
          </div>
          
          <p class="card-summary">{{ doc.description || '暂无描述' }}</p>
          
          <div class="card-meta">
            <span class="meta-item">
              <IconSvg name="folder" :size="10" color="#9ca3af" />
              {{ doc.category || '未分类' }}
            </span>
            <span class="meta-item">
              <IconSvg name="user" :size="10" color="#9ca3af" />
              {{ doc.creator || '管理员' }}
            </span>
            <span class="meta-item">
              <IconSvg name="calendar" :size="10" color="#9ca3af" />
              {{ doc.updateTime?.split('T')[0] || '-' }}
            </span>
          </div>
          
          <div class="card-footer">
            <div class="footer-actions">
              <button class="action-btn" @click="editItem(doc, $event)" title="编辑">
                <IconSvg name="edit" :size="14" color="#9ca3af" />
              </button>
              <button class="action-btn" @click="deleteItem(doc, $event)" title="删除">
                <IconSvg name="trash" :size="14" color="#9ca3af" />
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="empty-state" v-if="!loading && knowledgeBases.length === 0">
      <IconSvg name="folder" :size="48" color="#d1d5db" />
      <p>暂无知识库</p>
      <button class="btn-primary" @click="createNew">
        <IconSvg name="plus" :size="14" color="#fff" />
        新建知识库
      </button>
    </div>

    <div class="pagination-card" v-if="knowledgeBases.length > 0">
      <div class="pagination-info">
        显示 {{ (pagination.current - 1) * pagination.pageSize + 1 }}-{{ Math.min(pagination.current * pagination.pageSize, total) }} 条，共 {{ total }} 条
      </div>
      <div class="pagination-controls">
        <div class="page-size-selector">
          <span class="page-size-label">每页</span>
          <select v-model="pagination.pageSize" @change="handlePageSizeChange(pagination.pageSize)">
            <option v-for="size in pageSizeOptions" :key="size" :value="size">{{ size }}</option>
          </select>
          <span class="page-size-label">条</span>
        </div>
        <div class="pagination-btns">
          <button 
            class="pagination-btn" 
            :disabled="pagination.current <= 1"
            @click="handlePageChange(pagination.current - 1)"
          >
            <IconSvg name="chevron-left" :size="12" color="#4b5563" />
          </button>
          <template v-for="(page, index) in visiblePages" :key="index">
            <span v-if="page === '...'" class="page-ellipsis">...</span>
            <button 
              v-else
              class="pagination-btn"
              :class="{ active: page === pagination.current }"
              @click="handlePageChange(page)"
            >
              {{ page }}
            </button>
          </template>
          <button 
            class="pagination-btn" 
            :disabled="pagination.current >= pagination.totalPages"
            @click="handlePageChange(pagination.current + 1)"
          >
            <IconSvg name="chevron-right" :size="12" color="#4b5563" />
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>


.knowledge-card-page {
  width: 100%;
}

.page-header {
  margin-bottom: $spacing-lg;
}

.page-title {
  font-size: $font-size-lg;
  font-weight: 700;
  color: $gray-800;
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  margin: 0 0 $spacing-xs 0;
}

.page-desc {
  font-size: $font-size-xs;
  color: $gray-500;
  margin: 0;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(1, 1fr);
  gap: $spacing-md;
  margin-bottom: $spacing-lg;
  
  @media (min-width: 640px) {
    grid-template-columns: repeat(2, 1fr);
  }
  
  @media (min-width: 1024px) {
    grid-template-columns: repeat(4, 1fr);
  }
}

.stat-card {
  background: #fff;
  border-radius: $radius-xl;
  padding: $spacing-md;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  border: 1px solid $gray-100;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  transition: all 0.2s ease;
  
  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 20px 25px -12px rgba(139, 92, 246, 0.15);
  }
}

.stat-content {
  flex: 1;
}

.stat-label {
  font-size: $font-size-xs;
  font-weight: 500;
  color: $gray-500;
  margin: 0 0 $spacing-xs 0;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: $gray-800;
  margin: 0 0 $spacing-xs 0;
}

.stat-growth {
  display: inline-flex;
  align-items: center;
  font-size: 10px;
  font-weight: 600;
  padding: 2px 6px;
  border-radius: 9999px;
  
  &.growth-positive {
    color: #047857;
    background: #ecfdf5;
  }
  
  &.growth-success {
    color: #16a34a;
    background: #f0fdf4;
  }
  
  &.growth-gray {
    color: $gray-500;
    background: $gray-100;
  }
}

.stat-link {
  display: inline-flex;
  font-size: 10px;
  font-weight: 600;
  color: #d97706;
  text-decoration: none;
}

.stat-icon {
  width: 40px;
  height: 40px;
  border-radius: $radius-xl;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  
  &.stat-icon-purple { background: $purple-50; }
  &.stat-icon-green { background: #f0fdf4; }
  &.stat-icon-amber { background: #fffbeb; }
  &.stat-icon-gray { background: $gray-100; }
}

.filter-card {
  background: #fff;
  border-radius: $radius-xl;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  border: 1px solid $gray-100;
  padding: $spacing-md;
  margin-bottom: $spacing-lg;
  transition: all 0.2s ease;
  
  &:hover {
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  }
}

.filter-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: $spacing-md;
}

.filter-left {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
}

.search-box {
  position: relative;
  display: flex;
  align-items: center;
  
  svg {
    position: absolute;
    left: 12px;
  }
  
  input {
    width: 280px;
    padding: 8px 12px 8px 36px;
    border: 1px solid $gray-200;
    border-radius: $radius-md;
    font-size: $font-size-xs;
    outline: none;
    transition: all 0.2s ease;
    
    &:focus {
      border-color: $purple-300;
      box-shadow: 0 0 0 3px rgba(124, 58, 237, 0.1);
    }
  }
}

.btn-primary-sm {
  display: flex;
  align-items: center;
  gap: $spacing-xs;
  padding: 8px 16px;
  border-radius: $radius-md;
  font-size: $font-size-xs;
  font-weight: 500;
  background: $purple-600;
  border: none;
  color: #fff;
  cursor: pointer;
  transition: all 0.2s ease;
  
  &:hover {
    background: $purple-700;
  }
}

.filter-right {
  display: flex;
  gap: $spacing-sm;
  align-items: center;
}

.btn-purple-light {
  display: flex;
  align-items: center;
  gap: $spacing-xs;
  padding: 6px 12px;
  border-radius: $radius-md;
  font-size: $font-size-xs;
  background: $purple-50;
  border: 1px solid $purple-100;
  color: $purple-600;
  cursor: pointer;
  transition: all 0.2s ease;
  
  &:hover {
    background: $purple-100;
  }
}

.batch-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: $spacing-md;
  flex-wrap: wrap;
  gap: $spacing-md;
}

.batch-left {
  display: flex;
  align-items: center;
  gap: $spacing-md;
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  font-size: $font-size-xs;
  color: $gray-500;
  
  input {
    width: 14px;
    height: 14px;
    accent-color: $purple-600;
  }
}

.batch-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  font-size: $font-size-xs;
  color: $gray-500;
  background: none;
  border: none;
  cursor: pointer;
  transition: all 0.2s ease;
  border-radius: $radius-sm;
  
  &:hover:not(:disabled) {
    color: $purple-600;
    background: $purple-50;
  }
  
  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
}

.batch-right {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
}

.sort-label {
  font-size: 10px;
  color: $gray-400;
}

select {
  padding: 4px 8px;
  border: 1px solid $gray-200;
  border-radius: $radius-sm;
  font-size: $font-size-xs;
  background: #fff;
  color: $gray-700;
  outline: none;
}

.knowledge-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
  gap: $spacing-lg;
  margin-bottom: $spacing-lg;
  
  @media (max-width: 768px) {
    grid-template-columns: 1fr;
  }
}

.knowledge-card {
  background: #fff;
  border-radius: $radius-xl;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  border: 1px solid $gray-100;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.2s ease;
  
  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 20px 25px -12px rgba(139, 92, 246, 0.15);
    border-color: $purple-200;
  }
  
  &.card-recommended {
    background: linear-gradient(135deg, rgba(249, 250, 251, 0.5), rgba(255, 255, 255, 0.8));
    border: 1px solid $purple-100;
  }
}

.card-body {
  padding: $spacing-lg;
}

.card-header {
  display: flex;
  gap: $spacing-md;
  margin-bottom: $spacing-md;
}

.card-icon {
  width: 48px;
  height: 48px;
  border-radius: $radius-xl;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.card-header-content {
  flex: 1;
  min-width: 0;
}

.card-header-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: $spacing-sm;
}

.card-title {
  font-size: $font-size-base;
  font-weight: 700;
  color: $gray-800;
  margin: 0;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
  transition: color 0.2s ease;
  
  &:hover {
    color: $purple-600;
  }
}

.more-btn {
  background: none;
  border: none;
  padding: 6px;
  color: $gray-400;
  cursor: pointer;
  border-radius: $radius-sm;
  transition: all 0.2s ease;
  
  &:hover {
    background: $purple-50;
    color: $purple-500;
  }
}

.card-header-bottom {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  padding: 2px 10px;
  border-radius: 9999px;
  font-size: 10px;
  font-weight: 500;
  
  &.bg-green-50 { background: #f0fdf4; color: #16a34a; }
  &.bg-amber-50 { background: #fffbeb; color: #d97706; }
  &.bg-gray-100 { background: #f3f4f6; color: #6b7280; }
  &.bg-purple-50 { background: #faf5ff; color: #7c3aed; }
}

.card-checkbox {
  width: 14px;
  height: 14px;
  accent-color: $purple-600;
}

.card-summary {
  font-size: $font-size-xs;
  color: $gray-500;
  margin: 0 0 $spacing-md 0;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-meta {
  display: flex;
  flex-wrap: wrap;
  gap: $spacing-md;
  margin-bottom: $spacing-md;
  padding-bottom: $spacing-md;
  border-bottom: 1px solid $gray-100;
}

.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 10px;
  color: $gray-400;
}

.card-footer {
  display: flex;
  justify-content: flex-end;
  align-items: center;
}

.footer-actions {
  display: flex;
  gap: 4px;
}

.action-btn {
  background: none;
  border: none;
  padding: 6px;
  color: $gray-400;
  cursor: pointer;
  transition: all 0.2s ease;
  border-radius: $radius-sm;
  
  &:hover {
    background: $purple-50;
    color: $purple-500;
  }
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px;
  color: $gray-400;
  
  p {
    margin: $spacing-md 0;
    font-size: $font-size-sm;
  }
}

.btn-primary {
  display: flex;
  align-items: center;
  gap: $spacing-xs;
  padding: 8px 16px;
  background: $purple-600;
  color: #fff;
  border: none;
  border-radius: $radius-md;
  font-size: $font-size-xs;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  
  &:hover {
    background: $purple-700;
  }
}

.pagination-card {
  background: #fff;
  border-radius: $radius-xl;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  border: 1px solid $gray-100;
  padding: $spacing-md;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: $spacing-md;
}

.pagination-info {
  font-size: $font-size-xs;
  color: $gray-500;
}

.pagination-controls {
  display: flex;
  align-items: center;
  gap: $spacing-md;
}

.page-size-selector {
  display: flex;
  align-items: center;
  gap: 6px;
  
  .page-size-label {
    font-size: $font-size-xs;
    color: $gray-500;
  }
  
  select {
    padding: 4px 8px;
    border: 1px solid $gray-200;
    border-radius: $radius-sm;
    font-size: $font-size-xs;
    color: $gray-700;
    background: #fff;
    cursor: pointer;
    
    &:focus {
      border-color: $purple-400;
      outline: none;
    }
  }
}

.pagination-btns {
  display: flex;
  gap: 4px;
  align-items: center;
}

.pagination-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 32px;
  height: 32px;
  padding: 0 8px;
  border-radius: $radius-sm;
  font-size: $font-size-xs;
  font-weight: 500;
  background: #fff;
  border: 1px solid $gray-200;
  color: $gray-600;
  cursor: pointer;
  transition: all 0.15s ease;
  
  &:hover:not(.active):not(:disabled) {
    background: $purple-50;
    border-color: $purple-300;
    color: $purple-700;
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
</style>