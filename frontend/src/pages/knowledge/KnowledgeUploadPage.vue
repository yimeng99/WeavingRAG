<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import IconSvg from '../../components/ui/IconSvg.vue'
import StepIndicator from '../../components/ui/StepIndicator.vue'
import { message } from '../../utils/message'

const router = useRouter()
const route = useRoute()

const baseId = computed(() => route.query.baseId || route.params.baseId)

const currentStep = ref(1)
const totalSteps = 4
const stepLabels = ['选择文件', '切片配置', '上传处理', '完成']

const dragOver = ref(false)
const selectedFiles = ref([])
const uploading = ref(false)
const uploadProgress = ref(0)
const uploadStatus = ref('')

const form = ref({
  tags: '',
  chunkStrategy: 'fixed',
  chunkSize: 500,
  overlap: 50,
  semanticThreshold: 0.7,
  headingLevels: ['h1', 'h2'],
  embeddingModel: 'text-embedding-ada-002',
  indexType: 'hnsw',
  autoSummary: true,
  hybridSearch: false
})

const chunkStrategies = [
  { key: 'fixed', name: '固定长度切片', desc: '按固定字符数分割' },
  { key: 'semantic', name: '语义智能切片', desc: '基于段落语义边界' },
  { key: 'heading', name: '标题层级切片', desc: '按Markdown标题分割' }
]

const embeddingModels = [
  { value: 'text-embedding-ada-002', label: 'text-embedding-ada-002 (OpenAI)' },
  { value: 'bge-large-zh', label: 'BAAI/bge-large-zh-v1.5' },
  { value: 'text2vec-large', label: 'text2vec-large-chinese' }
]

const uploadedResults = ref([])
const showChunkPreview = ref(false)

const fileInput = ref(null)

const goBack = () => {
  router.back()
}

const prevStep = () => {
  if (currentStep.value > 1) {
    currentStep.value--
  }
}

const nextStep = () => {
  if (currentStep.value === 1 && selectedFiles.value.length === 0) {
    message.error('请先选择要上传的文件')
    return
  }
  
  if (currentStep.value < totalSteps) {
    currentStep.value++
  }
  
  if (currentStep.value === 3) {
    startUpload()
  }
}

const handleFileSelect = (event) => {
  const files = Array.from(event.target.files)
  addFiles(files)
  event.target.value = ''
}

const handleDrop = (event) => {
  event.preventDefault()
  dragOver.value = false
  const files = Array.from(event.dataTransfer.files)
  addFiles(files)
}

const addFiles = (files) => {
  const validFiles = files.filter(file => {
    const fileNameLower = file.name.toLowerCase()
    if (fileNameLower.endsWith('.md') || fileNameLower.endsWith('.docx') || fileNameLower.endsWith('.txt') || fileNameLower.endsWith('.pdf')) {
      const exists = selectedFiles.value.some(f => f.name === file.name && f.size === file.size)
      if (exists) {
        message.warning(`文件 "${file.name}" 已存在`)
        return false
      }
      return true
    }
    message.error(`文件 "${file.name}" 格式不支持，仅支持 .md, .docx, .txt, .pdf`)
    return false
  })
  
  selectedFiles.value = [...selectedFiles.value, ...validFiles]
}

const removeFile = (index) => {
  selectedFiles.value.splice(index, 1)
}

const clearFiles = () => {
  selectedFiles.value = []
}

const startUpload = async () => {
  if (selectedFiles.value.length === 0) {
    currentStep.value = 1
    return
  }
  
  uploading.value = true
  uploadProgress.value = 0
  uploadStatus.value = '准备上传...'
  uploadedResults.value = []
  
  const total = selectedFiles.value.length
  let successCount = 0
  let failCount = 0
  let totalChunks = 0
  
  for (let i = 0; i < selectedFiles.value.length; i++) {
    const file = selectedFiles.value[i]
    const progress = Math.round(((i + 0.5) / total) * 100)
    uploadProgress.value = progress
    uploadStatus.value = `正在上传: ${file.name} (${i + 1}/${total})`
    
    try {
      const formData = new FormData()
      formData.append('file', file)
      
      const uploadRes = await fetch('/api/v0/file/upload', {
        method: 'POST',
        body: formData
      })
      
      const uploadData = await uploadRes.json()
      
      if (uploadData.code === 200 && uploadData.data) {
        const progress = Math.round(((i + 0.8) / total) * 100)
        uploadProgress.value = progress
        uploadStatus.value = `正在处理: ${file.name}`
        
        const saveData = {
          knowledgeBaseId: baseId.value,
          fileName: uploadData.data.fileName,
          filePath: uploadData.data.filePath,
          fileUrl: uploadData.data.fileUrl || uploadData.data.url,
          fileType: uploadData.data.fileType,
          content: uploadData.data.content,
          tags: form.value.tags,
          chunkingStrategy: form.value.chunkStrategy,
          chunkSize: form.value.chunkSize,
          overlap: form.value.overlap,
          enableEmbedding: true
        }
        
        const saveRes = await fetch('/api/v0/knowledge/document/save', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(saveData)
        })
        
        const saveResult = await saveRes.json()
        
        if (saveResult.code === 200) {
          successCount++
          totalChunks += saveResult.data?.chunkCount || 0
          uploadedResults.value.push({
            fileName: file.name,
            status: 'success',
            chunkCount: saveResult.data?.chunkCount || 0
          })
        } else {
          failCount++
          uploadedResults.value.push({
            fileName: file.name,
            status: 'error',
            message: saveResult.msg || '保存失败'
          })
        }
      } else {
        failCount++
        uploadedResults.value.push({
          fileName: file.name,
          status: 'error',
          message: uploadData.msg || '上传失败'
        })
      }
    } catch (e) {
      console.error(`文件 ${file.name} 上传失败:`, e)
      failCount++
      uploadedResults.value.push({
        fileName: file.name,
        status: 'error',
        message: e.message || '上传异常'
      })
    }
  }
  
  uploadProgress.value = 100
  uploading.value = false
  currentStep.value = 4
  
  if (successCount > 0) {
    uploadStatus.value = `上传完成！成功 ${successCount} 个，共 ${totalChunks} 个切片`
    message.success(`成功上传 ${successCount} 个文件，共 ${totalChunks} 个切片`)
  }
  if (failCount > 0) {
    message.error(`${failCount} 个文件上传失败`)
  }
}

const handleFinish = () => {
  router.push(`/knowledge/${baseId.value}`)
}

const formatFileSize = (bytes) => {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

onMounted(() => {
  if (!baseId.value) {
    message.error('缺少知识库 ID 参数')
  }
})
</script>

<template>
  <div class="upload-page">
    <div class="page-header">
      <div class="header-left">
        <button class="back-btn" @click="goBack">
          <IconSvg name="arrow-left" :size="16" color="#6b7280" />
        </button>
        <div class="breadcrumb">
          <span class="breadcrumb-item">
            <IconSvg name="layers" :size="10" color="#a855f7" />
            工作台
          </span>
          <IconSvg name="chevron-right" :size="8" color="#d1d5db" />
          <span class="breadcrumb-item">知识库管理</span>
          <IconSvg name="chevron-right" :size="8" color="#d1d5db" />
          <span class="breadcrumb-current">上传文章</span>
        </div>
      </div>
    </div>

    <div class="page-content">
      <div class="page-title-section">
        <h2 class="page-title">
          <IconSvg name="upload" :size="20" color="#7c3aed" />
          上传文章
        </h2>
        <p class="page-desc">支持 Markdown (.md)、Word (.docx)、文本 (.txt)、PDF (.pdf) 格式，支持智能切片与向量索引配置</p>
      </div>

      <StepIndicator 
        :currentStep="currentStep"
        :totalSteps="totalSteps"
        :steps="stepLabels"
      />

      <div class="form-sections">
        <!-- 第一步：选择文件 -->
        <div class="form-card" v-show="currentStep === 1">
          <div class="card-header">
            <h3 class="card-title">
              <IconSvg name="file-upload" :size="16" color="#7c3aed" />
              选择文件
            </h3>
            <p class="card-desc">支持 .md, .docx, .txt, .pdf 格式，单个文件不超过 50MB</p>
          </div>
          <div class="card-body">
            <div class="dropzone"
              :class="{ 'drag-over': dragOver }"
              @click="() => fileInput?.click()"
              @dragover.prevent="dragOver = true"
              @dragleave="dragOver = false"
              @drop="handleDrop"
            >
              <IconSvg name="upload" :size="40" color="#9ca3af" />
              <p class="dropzone-text">点击或拖拽文件到此区域上传</p>
              <p class="dropzone-hint">支持 Markdown (.md)、Word (.docx)、文本 (.txt)、PDF (.pdf) 格式，可多选</p>
              <input 
                ref="fileInput"
                type="file"
                accept=".md,.docx,.txt,.pdf"
                multiple
                class="file-input"
                @change="handleFileSelect"
              >
            </div>
            
            <div class="file-list" v-if="selectedFiles.length > 0">
              <div class="file-list-header">
                <span class="file-count">已选择 {{ selectedFiles.length }} 个文件</span>
                <button class="clear-btn" @click="clearFiles">清空全部</button>
              </div>
              <div class="file-items">
                <div 
                  v-for="(file, index) in selectedFiles" 
                  :key="index"
                  class="file-item"
                >
                  <div class="file-details">
                    <IconSvg name="file" :size="16" color="#7c3aed" />
                    <span class="file-name">{{ file.name }}</span>
                    <span class="file-size">{{ formatFileSize(file.size) }}</span>
                  </div>
                  <button class="remove-btn" @click="removeFile(index)">
                    <IconSvg name="x" :size="14" color="#9ca3af" />
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 第二步：切片配置 -->
        <div class="form-card" v-show="currentStep === 2">
          <div class="card-header">
            <h3 class="card-title">
              <IconSvg name="scissors" :size="16" color="#7c3aed" />
              切片配置
            </h3>
            <p class="card-desc">将文档分割为适合向量检索的文本片段</p>
          </div>
          <div class="card-body">
            <div class="strategy-grid">
              <label 
                v-for="strategy in chunkStrategies"
                :key="strategy.key"
                class="strategy-card"
                :class="{ active: form.chunkStrategy === strategy.key }"
              >
                <input 
                  type="radio" 
                  v-model="form.chunkStrategy" 
                  :value="strategy.key"
                >
                <div class="strategy-content">
                  <p class="strategy-name">{{ strategy.name }}</p>
                  <p class="strategy-desc">{{ strategy.desc }}</p>
                </div>
              </label>
            </div>

            <div class="chunk-params" v-if="form.chunkStrategy === 'fixed'">
              <div class="param-row">
                <div class="param-group">
                  <label class="param-label">切片大小（字符）</label>
                  <input type="number" v-model="form.chunkSize" class="param-input">
                </div>
                <div class="param-group">
                  <label class="param-label">重叠大小（字符）</label>
                  <input type="number" v-model="form.overlap" class="param-input">
                  <p class="param-hint">保持上下文连贯性</p>
                </div>
              </div>
            </div>

            <div class="chunk-params" v-if="form.chunkStrategy === 'semantic'">
              <div class="param-group">
                <label class="param-label">语义相似度阈值</label>
                <input type="range" v-model="form.semanticThreshold" min="0" max="1" step="0.05" class="param-slider">
                <div class="slider-labels">
                  <span>宽松</span>
                  <span>{{ form.semanticThreshold }}</span>
                  <span>严格</span>
                </div>
              </div>
            </div>

            <div class="chunk-params" v-if="form.chunkStrategy === 'heading'">
              <div class="param-group">
                <label class="param-label">标题层级</label>
                <div class="checkbox-group">
                  <label class="checkbox-item">
                    <input type="checkbox" value="h1" v-model="form.headingLevels">
                    H1
                  </label>
                  <label class="checkbox-item">
                    <input type="checkbox" value="h2" v-model="form.headingLevels">
                    H2
                  </label>
                  <label class="checkbox-item">
                    <input type="checkbox" value="h3" v-model="form.headingLevels">
                    H3
                  </label>
                </div>
              </div>
            </div>
            
            <div class="param-group" style="margin-top: 16px;">
              <label class="param-label">标签（可选）</label>
              <input type="text" v-model="form.tags" placeholder="输入标签，用逗号分隔，将应用到所有文件" class="param-input">
            </div>
          </div>
        </div>

        <!-- 第三步：上传处理 -->
        <div class="form-card" v-show="currentStep === 3">
          <div class="card-header">
            <h3 class="card-title">
              <IconSvg name="upload" :size="16" color="#7c3aed" />
              上传处理
            </h3>
          </div>
          <div class="card-body">
            <div class="progress-section">
              <div class="progress-header">
                <span class="progress-title">上传进度</span>
                <span class="progress-percent">{{ uploadProgress }}%</span>
              </div>
              <div class="progress-bar">
                <div class="progress-fill" :style="{ width: uploadProgress + '%' }"></div>
              </div>
              <p class="progress-status">{{ uploadStatus }}</p>
            </div>
          </div>
        </div>

        <!-- 第四步：完成 -->
        <div class="form-card success-card" v-show="currentStep === 4">
          <div class="card-body">
            <div class="success-content">
              <div class="success-icon">
                <IconSvg name="check" :size="32" color="#10b981" />
              </div>
              <h3 class="success-title">上传完成！</h3>
              <p class="success-desc">{{ uploadStatus }}</p>
              
              <div class="result-list" v-if="uploadedResults.length > 0">
                <div 
                  v-for="(result, index) in uploadedResults" 
                  :key="index"
                  class="result-item"
                  :class="result.status"
                >
                  <div class="result-icon">
                    <IconSvg 
                      :name="result.status === 'success' ? 'check' : 'x'" 
                      :size="14" 
                      :color="result.status === 'success' ? '#10b981' : '#ef4444'" 
                    />
                  </div>
                  <div class="result-info">
                    <span class="result-name">{{ result.fileName }}</span>
                    <span class="result-detail" v-if="result.status === 'success'">
                      {{ result.chunkCount }} 个切片
                    </span>
                    <span class="result-detail error" v-else>
                      {{ result.message }}
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 操作按钮 -->
        <div class="form-actions">
          <div class="actions-left">
            <button 
              v-if="currentStep > 1 && currentStep < 4 && !uploading"
              class="btn btn-outline" 
              @click="prevStep"
            >
              上一步
            </button>
          </div>
          <div class="actions-right">
            <button 
              v-if="currentStep < 4"
              class="btn btn-primary" 
              :disabled="(currentStep === 1 && selectedFiles.length === 0) || uploading"
              @click="nextStep"
            >
              {{ uploading ? '处理中...' : (currentStep === 2 ? '开始上传' : '下一步') }}
            </button>
            <button 
              v-if="currentStep === 4"
              class="btn btn-primary" 
              @click="handleFinish"
            >
              返回知识库
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>


.upload-page {
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
  border: none;
  background: transparent;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  
  &:hover {
    background: $gray-50;
  }
}

.breadcrumb {
  display: flex;
  align-items: center;
  gap: $spacing-xs;
  font-size: $font-size-xs;
  
  .breadcrumb-item {
    color: $gray-500;
    display: flex;
    align-items: center;
    gap: 4px;
    
    &:hover {
      color: $purple-600;
    }
  }
  
  .breadcrumb-current {
    color: $gray-700;
    font-weight: 500;
  }
}

.page-content {
  flex: 1;
  overflow-y: auto;
  padding: $spacing-lg;
  max-width: 1000px;
  margin: 0 auto;
  width: 100%;
}

.page-title-section {
  margin-bottom: $spacing-lg;
  
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
}

.form-sections {
  display: flex;
  flex-direction: column;
  gap: $spacing-md;
}

.form-card {
  background: #fff;
  border-radius: $radius-lg;
  border: 1px solid $gray-100;
  overflow: hidden;
  transition: all 0.2s ease;
  
  &:hover {
    box-shadow: 0 4px 12px rgba(139, 92, 246, 0.08);
  }
}

.card-header {
  padding: $spacing-md $spacing-lg;
  background: linear-gradient(90deg, $gray-50, #fff);
  border-bottom: 1px solid $gray-100;
  
  .card-title {
    font-size: $font-size-base;
    font-weight: 600;
    color: $gray-800;
    display: flex;
    align-items: center;
    gap: $spacing-sm;
    margin: 0;
  }
  
  .card-desc {
    font-size: 10px;
    color: $gray-500;
    margin: 2px 0 0 0;
  }
}

.card-body {
  padding: $spacing-lg;
}

.dropzone {
  border: 2px dashed $gray-200;
  border-radius: $radius-lg;
  padding: $spacing-xl;
  text-align: center;
  cursor: pointer;
  transition: all 0.2s ease;
  
  &:hover, &.drag-over {
    border-color: $purple-400;
    background: #faf5ff;
  }
  
  .dropzone-text {
    font-size: $font-size-sm;
    color: $gray-600;
    margin: $spacing-sm 0 $spacing-xs 0;
  }
  
  .dropzone-hint {
    font-size: 10px;
    color: $gray-400;
    margin: 0;
  }
  
  .file-input {
    display: none;
  }
}

.file-list {
  margin-top: $spacing-md;
  
  .file-list-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: $spacing-sm;
    
    .file-count {
      font-size: $font-size-xs;
      font-weight: 500;
      color: $gray-600;
    }
    
    .clear-btn {
      font-size: $font-size-xs;
      color: $gray-500;
      background: none;
      border: none;
      cursor: pointer;
      
      &:hover {
        color: #ef4444;
      }
    }
  }
  
  .file-items {
    max-height: 200px;
    overflow-y: auto;
    border: 1px solid $gray-200;
    border-radius: $radius-md;
  }
}

.file-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: $spacing-sm $spacing-md;
  border-bottom: 1px solid $gray-100;
  
  &:last-child {
    border-bottom: none;
  }
  
  .file-details {
    display: flex;
    align-items: center;
    gap: $spacing-sm;
    
    .file-name {
      font-size: $font-size-sm;
      color: $gray-700;
      max-width: 300px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
    
    .file-size {
      font-size: $font-size-xs;
      color: $gray-400;
    }
  }
  
  .remove-btn {
    width: 24px;
    height: 24px;
    border: none;
    background: transparent;
    border-radius: $radius-sm;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    
    &:hover:not(:disabled) {
      background: $gray-100;
    }
    
    &:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }
  }
}

.strategy-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: $spacing-md;
}

.strategy-card {
  border: 1px solid $gray-200;
  border-radius: $radius-md;
  padding: $spacing-md;
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
    margin-right: $spacing-sm;
    accent-color: $purple-600;
  }
  
  .strategy-content {
    margin-top: $spacing-sm;
  }
  
  .strategy-name {
    font-size: $font-size-xs;
    font-weight: 500;
    color: $gray-700;
    margin: 0;
  }
  
  .strategy-desc {
    font-size: 10px;
    color: $gray-400;
    margin: 2px 0 0 0;
  }
}

.chunk-params {
  margin-top: $spacing-md;
  padding: $spacing-md;
  background: $gray-50;
  border-radius: $radius-md;
}

.param-row {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: $spacing-md;
}

.param-group {
  .param-label {
    display: block;
    font-size: $font-size-xs;
    font-weight: 500;
    color: $gray-700;
    margin-bottom: $spacing-xs;
  }
  
  .param-input, .param-select, .param-textarea {
    width: 100%;
    padding: 8px 12px;
    border: 1px solid $gray-200;
    border-radius: $radius-md;
    font-size: $font-size-sm;
    
    &:focus {
      border-color: $purple-400;
      outline: none;
    }
  }
  
  .param-textarea {
    resize: vertical;
    min-height: 60px;
  }
  
  .param-hint {
    font-size: 9px;
    color: $gray-400;
    margin: 4px 0 0 0;
  }
  
  .param-slider {
    width: 100%;
    accent-color: $purple-600;
  }
  
  .slider-labels {
    display: flex;
    justify-content: space-between;
    font-size: 10px;
    color: $gray-500;
    margin-top: 4px;
  }
}

.checkbox-group {
  display: flex;
  gap: $spacing-md;
  
  .checkbox-item {
    display: flex;
    align-items: center;
    gap: 4px;
    font-size: $font-size-xs;
    color: $gray-600;
    cursor: pointer;
    
    input {
      accent-color: $purple-600;
    }
  }
}

.preview-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-top: $spacing-md;
  padding: 0;
  border: none;
  background: transparent;
  font-size: $font-size-xs;
  font-weight: 500;
  color: $purple-600;
  cursor: pointer;
  
  &:hover {
    color: $purple-700;
  }
}

.chunk-preview {
  margin-top: $spacing-md;
  padding: $spacing-md;
  background: $gray-50;
  border-radius: $radius-md;
  max-height: 300px;
  overflow-y: auto;
  
  .preview-title {
    font-size: $font-size-xs;
    font-weight: 500;
    color: $gray-600;
    margin: 0 0 $spacing-sm 0;
  }
  
  .preview-list {
    display: flex;
    flex-direction: column;
    gap: $spacing-sm;
  }
  
  .preview-item {
    padding: $spacing-sm;
    background: #fff;
    border-radius: $radius-md;
    border-left: 2px solid $purple-400;
    font-size: $font-size-xs;
    color: $gray-500;
  }
}

.index-type-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: $spacing-md;
  margin-top: $spacing-md;
}

.index-card {
  border: 1px solid $gray-200;
  border-radius: $radius-md;
  padding: $spacing-md;
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
    margin-right: $spacing-sm;
    accent-color: $purple-600;
  }
  
  .index-content {
    margin-top: $spacing-xs;
  }
  
  .index-name {
    font-size: $font-size-xs;
    font-weight: 500;
    color: $gray-700;
    margin: 0;
  }
  
  .index-desc {
    font-size: 10px;
    color: $gray-400;
    margin: 2px 0 0 0;
  }
}

.toggle-section {
  margin-top: $spacing-md;
  border-top: 1px solid $gray-100;
  padding-top: $spacing-md;
}

.toggle-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: $spacing-sm 0;
  
  &:not(:last-child) {
    border-bottom: 1px solid $gray-100;
  }
  
  .toggle-info {
    .toggle-title {
      font-size: $font-size-sm;
      font-weight: 500;
      color: $gray-800;
      margin: 0;
    }
    
    .toggle-desc {
      font-size: 10px;
      color: $gray-500;
      margin: 2px 0 0 0;
    }
  }
}

.toggle-switch {
  position: relative;
  display: inline-block;
  width: 36px;
  height: 20px;
  
  input {
    opacity: 0;
    width: 0;
    height: 0;
  }
  
  input:checked + .toggle-slider {
    background-color: $purple-600;
  }
  
  input:checked + .toggle-slider:before {
    transform: translateX(16px);
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
    height: 14px;
    width: 14px;
    left: 3px;
    bottom: 3px;
    background-color: white;
    transition: 0.3s;
    border-radius: 50%;
  }
}

.progress-section {
  .progress-header {
    display: flex;
    justify-content: space-between;
    margin-bottom: $spacing-sm;
    
    .progress-title {
      font-size: $font-size-sm;
      font-weight: 500;
      color: $gray-700;
    }
    
    .progress-percent {
      font-size: $font-size-xs;
      color: $purple-600;
    }
  }
  
  .progress-bar {
    width: 100%;
    height: 8px;
    background: $gray-200;
    border-radius: 9999px;
    overflow: hidden;
    
    .progress-fill {
      height: 100%;
      background: $purple-600;
      border-radius: 9999px;
      transition: width 0.3s ease;
    }
  }
  
  .progress-status {
    font-size: $font-size-xs;
    color: $gray-500;
    margin: $spacing-sm 0 0 0;
  }
}

.success-card {
  .card-body {
    padding: $spacing-xl;
  }
  
  .success-content {
    text-align: center;
    
    .success-icon {
      width: 64px;
      height: 64px;
      border-radius: 50%;
      background: #dcfce7;
      display: flex;
      align-items: center;
      justify-content: center;
      margin: 0 auto $spacing-md;
    }
    
    .success-title {
      font-size: $font-size-lg;
      font-weight: 700;
      color: $gray-800;
      margin: 0 0 $spacing-xs 0;
    }
    
    .success-desc {
      font-size: $font-size-sm;
      color: $gray-500;
      margin: 0 0 $spacing-lg 0;
    }
    
    .result-list {
      text-align: left;
      max-height: 300px;
      overflow-y: auto;
      margin-bottom: $spacing-lg;
      border: 1px solid $gray-200;
      border-radius: $radius-md;
    }
    
    .result-item {
      display: flex;
      align-items: center;
      gap: $spacing-sm;
      padding: $spacing-sm $spacing-md;
      border-bottom: 1px solid $gray-100;
      
      &:last-child {
        border-bottom: none;
      }
      
      &.success {
        background: #f0fdf4;
      }
      
      &.error {
        background: #fef2f2;
      }
      
      .result-icon {
        width: 20px;
        height: 20px;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
      }
      
      .result-info {
        flex: 1;
        min-width: 0;
        
        .result-name {
          display: block;
          font-size: $font-size-sm;
          color: $gray-700;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }
        
        .result-detail {
          display: block;
          font-size: $font-size-xs;
          color: $gray-500;
          
          &.error {
            color: #ef4444;
          }
        }
      }
    }
  }
}

.form-actions {
  display: flex;
  justify-content: space-between;
  gap: $spacing-md;
  padding-top: $spacing-md;
  
  .actions-left {
    display: flex;
    gap: $spacing-md;
  }
  
  .actions-right {
    display: flex;
    gap: $spacing-md;
    margin-left: auto;
  }
}

.btn {
  display: inline-flex;
  align-items: center;
  gap: $spacing-xs;
  padding: 10px 16px;
  border-radius: $radius-md;
  font-size: $font-size-sm;
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

.btn-primary {
  background: $purple-600;
  border: none;
  color: #fff;
  
  &:hover:not(:disabled) {
    background: $purple-700;
  }
}

@media (max-width: 768px) {
  .strategy-grid {
    grid-template-columns: 1fr;
  }
  
  .index-type-grid {
    grid-template-columns: 1fr;
  }
  
  .param-row {
    grid-template-columns: 1fr;
  }
}
</style>