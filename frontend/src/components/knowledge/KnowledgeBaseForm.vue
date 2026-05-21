<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import IconSvg from '../ui/IconSvg.vue'
import { message } from '../../utils/message'

const router = useRouter()
const route = useRoute()

const baseId = route.params.id || null
const isEdit = ref(!!baseId)

const form = ref({
  name: '',
  description: '',
  icon: 'folder',
  iconColor: '#3b82f6'
})

const loading = ref(false)
const submitting = ref(false)

const icons = [
  { name: 'code', icon: 'code', color: '#3b82f6', gradient: 'from-blue-500 to-blue-700', label: '代码/技术' },
  { name: 'database', icon: 'database', color: '#6366f1', gradient: 'from-indigo-500 to-indigo-700', label: '数据库' },
  { name: 'server', icon: 'settings', color: '#10b981', gradient: 'from-emerald-500 to-emerald-700', label: '运维' },
  { name: 'file', icon: 'file-text', color: '#f59e0b', gradient: 'from-amber-500 to-amber-700', label: '文档' },
  { name: 'ai', icon: 'brain', color: '#ec4899', gradient: 'from-pink-500 to-pink-700', label: 'AI/智能' },
  { name: 'product', icon: 'folder', color: '#a855f7', gradient: 'from-purple-500 to-purple-700', label: '产品' },
  { name: 'design', icon: 'sparkles', color: '#f43f5e', gradient: 'from-rose-500 to-rose-700', label: '设计' }
]

const selectedIcon = ref(icons[5])

const selectIcon = (icon) => {
  form.value.icon = icon.name
  form.value.iconColor = icon.color
  selectedIcon.value = icon
}

const loadBase = async () => {
  if (!baseId) return
  loading.value = true
  try {
    const res = await fetch(`/api/v0/knowledge/bases/${baseId}`)
    const data = await res.json()
    
    if (data.code === 200 && data.data) {
      form.value = {
        name: data.data.name || '',
        description: data.data.description || '',
        icon: data.data.icon || 'folder',
        iconColor: data.data.color || '#3b82f6'
      }
      const foundIcon = icons.find(i => i.name === form.value.icon)
      if (foundIcon) selectedIcon.value = foundIcon
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

const handleBack = () => {
  router.push('/knowledge')
}

const handleSubmit = async () => {
  if (!form.value.name.trim()) {
    message.error('请输入知识库名称')
    return
  }
  
  submitting.value = true
  try {
    const url = isEdit.value ? `/api/v0/knowledge/bases/${baseId}` : '/api/v0/knowledge/bases'
    const method = isEdit.value ? 'PUT' : 'POST'
    
    const res = await fetch(url, {
      method,
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        name: form.value.name,
        description: form.value.description,
        icon: form.value.icon,
        color: form.value.iconColor
      })
    })
    
    const data = await res.json()
    
    if (data.code === 200) {
      message.success(isEdit.value ? '更新成功' : '创建成功')
      router.push('/knowledge')
    } else {
      message.error(data.msg || (isEdit.value ? '更新失败' : '创建失败'))
    }
  } catch (e) {
    console.error('保存失败:', e)
    message.error(isEdit.value ? '更新失败' : '创建失败')
  } finally {
    submitting.value = false
  }
}

onMounted(loadBase)
</script>

<template>
  <div class="edit-knowledge-page">
    <div class="page-container">
      <div class="page-header">
        <h2 class="page-title">
          <IconSvg name="edit" :size="20" color="#7c3aed" />
          {{ isEdit ? '编辑知识库' : '新建知识库' }}
        </h2>
        <p class="page-desc">{{ isEdit ? '修改知识库的基本信息' : '创建一个新的知识库' }}</p>
      </div>

      <div class="form-card" v-if="!loading">
        <div class="form-group">
          <label class="form-label required">知识库名称</label>
          <input 
            v-model="form.name"
            type="text" 
            placeholder="例如：技术文档库、产品知识库"
            class="form-input"
          >
          <p class="form-hint">2-50 个字符，支持中文、英文、数字、下划线</p>
        </div>
        
        <div class="form-group">
          <label class="form-label">知识库描述</label>
          <textarea 
            v-model="form.description"
            rows="3" 
            placeholder="简要描述这个知识库的用途和内容范围"
            class="form-textarea"
          ></textarea>
        </div>
        
        <div class="form-group">
          <label class="form-label">知识库图标</label>
          <div class="icon-selector">
            <div 
              class="icon-preview"
              :class="selectedIcon.gradient"
            >
              <IconSvg :name="selectedIcon.icon" :size="20" color="#fff" />
            </div>
            <div class="icon-list">
              <button 
                v-for="icon in icons" 
                :key="icon.name"
                type="button"
                class="icon-btn"
                :class="[icon.gradient, { active: form.icon === icon.name }]"
                @click="selectIcon(icon)"
                :title="icon.label"
              >
                <IconSvg :name="icon.icon" :size="14" color="#fff" />
              </button>
            </div>
          </div>
          <p class="form-hint">选择一个图标来代表这个知识库</p>
        </div>
      </div>

      <div class="loading-state" v-if="loading">
        <IconSvg name="loader" :size="24" color="#7c3aed" class="spin" />
        <p>加载中...</p>
      </div>

      <div class="form-footer">
        <button class="btn btn-outline" @click="handleBack">取消</button>
        <button 
          class="btn btn-primary" 
          :disabled="submitting || loading"
          @click="handleSubmit"
        >
          {{ submitting ? '保存中...' : (isEdit ? '保存' : '创建') }}
        </button>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>


.edit-knowledge-page {
  width: 100%;
  padding: 0 $spacing-sm;
  overflow-y: auto;
}

.page-container {
  max-width: 600px;
  margin: 0 auto;
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

.form-card {
  background: #fff;
  border-radius: $radius-lg;
  border: 1px solid $gray-100;
  overflow: hidden;
}

.form-group {
  padding: $spacing-md $spacing-lg;
  
  &:not(:last-child) {
    border-bottom: 1px solid $gray-100;
  }
}

.form-label {
  display: block;
  font-size: $font-size-xs;
  font-weight: 500;
  color: $gray-700;
  margin-bottom: $spacing-sm;
  
  &.required::after {
    content: ' *';
    color: #ef4444;
  }
}

.form-input,
.form-textarea {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid $gray-200;
  border-radius: $radius-md;
  font-size: $font-size-sm;
  color: $gray-700;
  transition: all 0.2s ease;
  
  &:focus {
    border-color: $purple-400;
    box-shadow: 0 0 0 3px rgba(124, 58, 237, 0.1);
  }
  
  &::placeholder {
    color: $gray-400;
  }
}

.form-textarea {
  resize: vertical;
  min-height: 80px;
}

.form-hint {
  font-size: 10px;
  color: $gray-400;
  margin: $spacing-xs 0 0 0;
}

.icon-selector {
  display: flex;
  align-items: center;
  gap: $spacing-md;
}

.icon-preview {
  width: 48px;
  height: 48px;
  border-radius: $radius-lg;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.icon-list {
  display: flex;
  flex-wrap: wrap;
  gap: $spacing-sm;
}

.icon-btn {
  width: 36px;
  height: 36px;
  border-radius: $radius-md;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
  
  &:hover {
    transform: scale(1.1);
  }
  
  &.active {
    box-shadow: 0 0 0 2px #fff, 0 0 0 4px $purple-500;
  }
}

.from-blue-500 { background: linear-gradient(135deg, #3b82f6, #1d4ed8); }
.from-indigo-500 { background: linear-gradient(135deg, #6366f1, #4f46e5); }
.from-emerald-500 { background: linear-gradient(135deg, #10b981, #059669); }
.from-amber-500 { background: linear-gradient(135deg, #f59e0b, #d97706); }
.from-pink-500 { background: linear-gradient(135deg, #ec4899, #db2777); }
.from-purple-500 { background: linear-gradient(135deg, #a855f7, #7c3aed); }
.from-rose-500 { background: linear-gradient(135deg, #f43f5e, #be123c); }

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: $spacing-xl;
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

.form-footer {
  display: flex;
  justify-content: flex-end;
  gap: $spacing-md;
  padding: $spacing-lg;
  margin-top: $spacing-lg;
  background: #fff;
  border-radius: $radius-lg;
  border: 1px solid $gray-100;
}

.btn {
  padding: 10px 20px;
  border-radius: $radius-md;
  font-size: $font-size-sm;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  
  &:disabled {
    opacity: 0.6;
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

.btn-primary {
  background: $purple-600;
  border: none;
  color: #fff;
  
  &:hover:not(:disabled) {
    background: $purple-700;
  }
}
</style>