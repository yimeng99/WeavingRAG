<script setup>
import { ref, computed, reactive } from 'vue'
import { useRouter } from 'vue-router'
import IconSvg from '../../components/ui/IconSvg.vue'
import StepIndicator from '../../components/ui/StepIndicator.vue'
import { message } from '../../utils/message'

const router = useRouter()

const currentStep = ref(1)
const totalSteps = 2
const loading = ref(false)

const form = reactive({
  name: '',
  description: '',
  owner: '',
  icon: 'code',
  iconColor: '#3b82f6',
  embeddingModel: 'text-embedding-ada-002',
  qaModel: 'GPT-4',
  hybridSearch: true,
  rerank: false,
  visibility: 'public',
  memberPermission: 'readonly',
  members: []
})

const icons = [
  { name: 'code', icon: 'code', color: '#3b82f6', gradient: 'from-blue-500 to-blue-700', label: '代码/技术' },
  { name: 'react', icon: 'code', color: '#06b6d4', gradient: 'from-cyan-500 to-cyan-700', label: '前端' },
  { name: 'database', icon: 'database', color: '#6366f1', gradient: 'from-indigo-500 to-indigo-700', label: '数据库' },
  { name: 'server', icon: 'settings', color: '#10b981', gradient: 'from-emerald-500 to-emerald-700', label: '运维' },
  { name: 'file', icon: 'file-text', color: '#f59e0b', gradient: 'from-amber-500 to-amber-700', label: '文档' },
  { name: 'ai', icon: 'brain', color: '#ec4899', gradient: 'from-pink-500 to-pink-700', label: 'AI/智能' },
  { name: 'product', icon: 'folder', color: '#a855f7', gradient: 'from-purple-500 to-purple-700', label: '产品' },
  { name: 'design', icon: 'sparkles', color: '#f43f5e', gradient: 'from-rose-500 to-rose-700', label: '设计' }
]
const embeddingModels = [
  { value: 'text-embedding-ada-002', label: 'text-embedding-ada-002 (OpenAI) - 1536 维' },
  { value: 'bge-large-zh', label: 'BAAI/bge-large-zh-v1.5 - 1024 维' },
  { value: 'text2vec-large', label: 'text2vec-large-chinese - 768 维' },
  { value: 'm3e-base', label: 'm3e-base - 768 维' }
]
const qaModels = [
  { value: 'GPT-4', label: 'GPT-4 (OpenAI)' },
  { value: 'GPT-3.5-Turbo', label: 'GPT-3.5-Turbo' },
  { value: 'Claude-3', label: 'Claude-3-Sonnet' },
  { value: 'Qwen-Max', label: '通义千问-Max' }
]

const selectedIcon = computed(() => icons.find(i => i.name === form.icon) || icons[0])

const selectIcon = (icon) => {
  form.icon = icon.name
  form.iconColor = icon.color
}

const nextStep = () => {
  if (currentStep.value === 1 && !form.name) {
    message.error('请输入知识库名称')
    return
  }
  if (currentStep.value < totalSteps) {
    currentStep.value++
  }
}

const prevStep = () => {
  if (currentStep.value > 1) {
    currentStep.value--
  }
}

const handleSubmit = async () => {
  if (!form.name) {
    message.error('请输入知识库名称')
    return
  }
  
  loading.value = true
  try {
    const res = await fetch('/api/v0/knowledge/bases', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        name: form.name,
        description: form.description,
        icon: form.icon,
        color: form.iconColor
      })
    })
    
    const data = await res.json()
    
    if (data.code === 200) {
      message.success('知识库创建成功')
      router.push('/knowledge')
    } else {
      message.error(data.msg || '创建失败')
    }
  } catch (error) {
    console.error('创建知识库失败:', error)
    message.error('创建失败')
  } finally {
    loading.value = false
  }
}

const goBack = () => {
  router.push('/knowledge')
}
</script>

<template>
  <div class="add-knowledge-page">
    <div class="page-container">
      <div class="page-header">
        <h2 class="page-title">
          <IconSvg name="folder" :size="20" color="#7c3aed" />
          创建知识库
        </h2>
        <p class="page-desc">创建一个新的知识库，配置基本信息、AI 模型和访问权限</p>
        <p class="page-tip">每个文档的切片策略可在上传时单独配置</p>
      </div>

      <StepIndicator 
        :currentStep="currentStep" 
        :totalSteps="totalSteps"
        :steps="['基本信息', '权限设置']"
      />

      <div class="form-container">
        <div v-show="currentStep === 1" class="step-content">
          <div class="form-card">
          <h3 class="section-title">
            <IconSvg name="info" :size="16" color="#7c3aed" />
            基本信息
          </h3>
          <p class="section-desc">设置知识库的基本属性和描述</p>
          
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
      </div>

      <div v-show="false" class="step-content">
          <div class="form-card">
          <h3 class="section-title">
            <IconSvg name="brain" :size="16" color="#7c3aed" />
            AI 模型配置
          </h3>
          <p class="section-desc">选择向量化模型和检索增强策略</p>
          
          <div class="form-row">
            <div class="form-group">
              <label class="form-label">向量化模型</label>
              <select v-model="form.embeddingModel" class="form-select">
                <option v-for="model in embeddingModels" :key="model.value" :value="model.value">
                  {{ model.label }}
                </option>
              </select>
              <p class="form-hint">用于将文本转换为向量，影响检索效果</p>
            </div>
            <div class="form-group">
              <label class="form-label">问答模型</label>
              <select v-model="form.qaModel" class="form-select">
                <option v-for="model in qaModels" :key="model.value" :value="model.value">
                  {{ model.label }}
                </option>
              </select>
              <p class="form-hint">用于生成回答和摘要</p>
            </div>
          </div>
          
          <div class="toggle-group">
            <div class="toggle-item">
              <div class="toggle-info">
                <p class="toggle-title">启用混合检索</p>
                <p class="toggle-desc">结合 BM25 关键词检索与向量检索，提高召回率</p>
              </div>
              <label class="toggle-switch">
                <input type="checkbox" v-model="form.hybridSearch">
                <span class="toggle-slider"></span>
              </label>
            </div>
            
            <div class="toggle-item">
              <div class="toggle-info">
                <p class="toggle-title">启用重排序 (Rerank)</p>
                <p class="toggle-desc">使用重排序模型优化检索结果相关性</p>
              </div>
              <label class="toggle-switch">
                <input type="checkbox" v-model="form.rerank">
                <span class="toggle-slider"></span>
              </label>
            </div>
          </div>
          
          <div class="tip-box">
            <IconSvg name="sparkles" :size="14" color="#7c3aed" />
            <p>每个文档的切片策略（固定长度、语义切片、标题层级）可在上传文章时单独配置</p>
          </div>
        </div>
      </div>

      <div v-show="currentStep === 3" class="step-content">
          <div class="form-card">
          <h3 class="section-title">
            <IconSvg name="lock" :size="16" color="#7c3aed" />
            访问权限
          </h3>
          <p class="section-desc">设置知识库的可见范围和访问权限</p>
          
          <div class="form-group">
            <label class="form-label">可见范围</label>
            <div class="radio-group">
              <label class="radio-item">
                <input type="radio" v-model="form.visibility" value="public">
                <span class="radio-label">公开</span>
                <span class="radio-desc">所有成员可查看和使用</span>
              </label>
              <label class="radio-item">
                <input type="radio" v-model="form.visibility" value="private">
                <span class="radio-label">私有</span>
                <span class="radio-desc">仅指定成员可访问</span>
              </label>
              <label class="radio-item">
                <input type="radio" v-model="form.visibility" value="department">
                <span class="radio-label">指定部门</span>
                <span class="radio-desc">仅特定部门成员可访问</span>
              </label>
            </div>
          </div>
          
          <div class="form-group">
            <label class="form-label">默认成员权限</label>
            <div class="permission-grid">
              <label class="permission-item" :class="{ active: form.memberPermission === 'readonly' }">
                <input type="radio" v-model="form.memberPermission" value="readonly">
                <div class="permission-content">
                  <p class="permission-title">只读</p>
                  <p class="permission-desc">仅可查看内容</p>
                </div>
              </label>
              <label class="permission-item" :class="{ active: form.memberPermission === 'edit' }">
                <input type="radio" v-model="form.memberPermission" value="edit">
                <div class="permission-content">
                  <p class="permission-title">可编辑</p>
                  <p class="permission-desc">可上传和编辑文档</p>
                </div>
              </label>
              <label class="permission-item" :class="{ active: form.memberPermission === 'manage' }">
                <input type="radio" v-model="form.memberPermission" value="manage">
                <div class="permission-content">
                  <p class="permission-title">可管理</p>
                  <p class="permission-desc">可管理成员和配置</p>
                </div>
              </label>
              <label class="permission-item" :class="{ active: form.memberPermission === 'full' }">
                <input type="radio" v-model="form.memberPermission" value="full">
                <div class="permission-content">
                  <p class="permission-title">完全控制</p>
                  <p class="permission-desc">所有操作权限</p>
                </div>
              </label>
            </div>
          </div>
        </div>
      </div>
      </div>

      <div class="form-footer">
        <button 
          v-if="currentStep > 1" 
          class="btn btn-outline" 
          @click="prevStep"
        >
          上一步
        </button>
        <div class="footer-right">
          <button class="btn btn-outline" @click="goBack">取消</button>
          <button 
            v-if="currentStep < totalSteps"
            class="btn btn-primary" 
            @click="nextStep"
          >
            下一步
          </button>
          <button 
            v-else
            class="btn btn-primary" 
            :disabled="loading"
            @click="handleSubmit"
          >
            {{ loading ? '创建中...' : '创建知识库' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>


.add-knowledge-page {
  width: 100%;
  padding: 0 $spacing-sm;
  overflow-y: auto;
}

.page-container {
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
  margin: 0 0 $spacing-xs 0;
}

.page-tip {
  font-size: 10px;
  color: $gray-400;
  margin: 0;
}

.step-content {
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateX(10px); }
  to { opacity: 1; transform: translateX(0); }
}

.form-container {
  margin-top: $spacing-lg;
}

.form-card {
  background: #fff;
  border-radius: $radius-lg;
  border: 1px solid $gray-100;
  overflow: hidden;
}

.form-section {
  background: #fff;
  border-radius: $radius-lg;
  border: 1px solid $gray-100;
  overflow: hidden;
}

.section-title {
  font-size: $font-size-base;
  font-weight: 600;
  color: $gray-800;
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  margin: 0;
  padding: $spacing-md $spacing-lg;
  background: linear-gradient(to right, $gray-50, #fff);
  border-bottom: 1px solid $gray-100;
}

.section-desc {
  font-size: 10px;
  color: $gray-500;
  margin: 0;
  padding: 0 $spacing-lg $spacing-md;
  background: linear-gradient(to right, $gray-50, #fff);
}

.form-group {
  padding: $spacing-md $spacing-lg;
  
  &:not(:last-child) {
    border-bottom: 1px solid $gray-100;
  }
}

.form-row {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: $spacing-lg;
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
.form-textarea,
.form-select {
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
.from-cyan-500 { background: linear-gradient(135deg, #06b6d4, #0891b2); }
.from-indigo-500 { background: linear-gradient(135deg, #6366f1, #4f46e5); }
.from-emerald-500 { background: linear-gradient(135deg, #10b981, #059669); }
.from-amber-500 { background: linear-gradient(135deg, #f59e0b, #d97706); }
.from-pink-500 { background: linear-gradient(135deg, #ec4899, #db2777); }
.from-purple-500 { background: linear-gradient(135deg, #a855f7, #7c3aed); }
.from-rose-500 { background: linear-gradient(135deg, #f43f5e, #be123c); }

.toggle-group {
  padding: $spacing-md $spacing-lg;
  border-bottom: 1px solid $gray-100;
}

.toggle-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: $spacing-sm 0;
  
  &:not(:last-child) {
    border-bottom: 1px solid $gray-100;
    margin-bottom: $spacing-sm;
    padding-bottom: $spacing-md;
  }
}

.toggle-info {
  flex: 1;
}

.toggle-title {
  font-size: $font-size-sm;
  font-weight: 500;
  color: $gray-800;
  margin: 0 0 2px 0;
}

.toggle-desc {
  font-size: 10px;
  color: $gray-500;
  margin: 0;
}

.toggle-switch {
  position: relative;
  display: inline-block;
  width: 44px;
  height: 24px;
  
  input {
    opacity: 0;
    width: 0;
    height: 0;
  }
  
  input:checked + .toggle-slider {
    background-color: $purple-600;
  }
  
  input:checked + .toggle-slider:before {
    transform: translateX(20px);
  }
}

.toggle-slider {
  position: absolute;
  cursor: pointer;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: #cbd5e1;
  transition: 0.3s;
  border-radius: 34px;
  
  &:before {
    position: absolute;
    content: "";
    height: 18px;
    width: 18px;
    left: 3px;
    bottom: 3px;
    background-color: white;
    transition: 0.3s;
    border-radius: 50%;
  }
}

.tip-box {
  margin: $spacing-md $spacing-lg;
  padding: $spacing-md;
  background: $purple-50;
  border-radius: $radius-md;
  display: flex;
  align-items: flex-start;
  gap: $spacing-sm;
  
  p {
    font-size: $font-size-xs;
    color: #6d28d9;
    margin: 0;
    line-height: 1.5;
  }
}

.radio-group {
  display: flex;
  flex-direction: column;
  gap: $spacing-sm;
}

.radio-item {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  padding: $spacing-sm;
  border-radius: $radius-md;
  cursor: pointer;
  transition: all 0.2s ease;
  
  &:hover {
    background: $gray-50;
  }
  
  input {
    accent-color: $purple-600;
  }
  
  .radio-label {
    font-size: $font-size-sm;
    font-weight: 500;
    color: $gray-700;
  }
  
  .radio-desc {
    font-size: 10px;
    color: $gray-400;
    margin-left: $spacing-sm;
  }
}

.permission-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: $spacing-md;
}

.permission-item {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  padding: $spacing-md;
  border: 1px solid $gray-200;
  border-radius: $radius-md;
  cursor: pointer;
  transition: all 0.2s ease;
  
  &:hover {
    border-color: $purple-300;
  }
  
  &.active {
    border-color: $purple-500;
    background: $purple-50;
  }
  
  input {
    accent-color: $purple-600;
  }
}

.permission-title {
  font-size: $font-size-xs;
  font-weight: 500;
  color: $gray-700;
  margin: 0;
}

.permission-desc {
  font-size: 10px;
  color: $gray-400;
  margin: 2px 0 0 0;
}

.form-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: $spacing-lg;
  margin-top: $spacing-lg;
  background: #fff;
  border-radius: $radius-lg;
  border: 1px solid $gray-100;
}

.modal-footer {
  display: flex;
  justify-content: space-between;
  padding: $spacing-lg $spacing-xl;
  border-top: 1px solid $gray-100;
  background: $gray-50;
  border-radius: 0 0 $radius-xl $radius-xl;
}

.footer-right {
  display: flex;
  gap: $spacing-md;
  margin-left: auto;
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