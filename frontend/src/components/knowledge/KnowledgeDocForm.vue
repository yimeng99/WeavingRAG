<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Plus, Delete, View } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()

const docId = route.params.id ? String(route.params.id) : null
const baseId = route.query.baseId ? String(route.query.baseId) : null
const isEdit = ref(!!docId)

const form = ref({
  title: '',
  content: '',
  type: 'text',
  tags: '',
  chunkingStrategy: 'intelligent',
  chunkSize: 1000
})
const loading = ref(false)
const submitting = ref(false)

// 切片配置
const chunkingStrategies = [
  { key: 'intelligent', name: '智能切分', desc: '在通用文档上的最优chunk切分方法，经过评测可在多数文档上获得最佳的检索效果' },
  { key: 'char', name: '按长度切分', desc: '适合对 Token 数量有严格要求的场景，比如使用上下文长度较小的模型时' },
  { key: 'page', name: '按页切分', desc: '适合每页传达独立主题的文档，要求不同页面的内容不会混杂在同一文本切片中' },
  { key: 'heading', name: '按标题切分', desc: '适合于用标题划分并传达独立主题的文档，要求不同级标题下的内容不会混杂在同一文本切片中' },
  { key: 'regex', name: '按照正则切分', desc: '依据设置的正则表达式，对知识库中的文本进行切分' },
  { key: 'separator', name: '按照符号切分', desc: '适用于根据特定标识符区分内容的文档，根据文档文件中特殊标识符进行切分' }
]

// 切片列表
const showChunksDialog = ref(false)
const chunks = ref([])
const loadingChunks = ref(false)

const loadDoc = async () => {
  if (!docId) return
  loading.value = true
  try {
    const response = await fetch(`/api/v0/knowledge/documents/${docId}`)
    if (response.ok) {
      const data = await response.json()
      form.value = {
        title: data.title || '',
        content: data.content || '',
        type: data.type || 'text',
        tags: data.tags || '',
        chunkingStrategy: data.chunkingStrategy || 'intelligent',
        chunkSize: data.chunkSize || 1000
      }
    } else {
      console.error('加载文档失败，状态码:', response.status)
      ElMessage.error('加载文档信息失败')
    }
  } catch (e) {
    console.error('加载文档异常:', e)
    ElMessage.error('加载文档信息失败')
  } finally {
    loading.value = false
  }
}

const handleBack = () => {
  if (baseId) {
    router.push({ path: '/knowledge', query: { baseId } })
  } else {
    router.push('/knowledge')
  }
}

const handleSubmit = async () => {
  if (!form.value.title.trim()) {
    ElMessage.warning('请输入文档标题')
    return
  }
  
  if (!baseId) {
    ElMessage.warning('缺少知识库ID')
    return
  }
  
  submitting.value = true
  try {
    const body = { 
      ...form.value, 
      knowledgeBaseId: baseId,
      docId: docId
    }
    const url = isEdit.value ? `/api/v0/knowledge/documents/${docId}` : '/api/v0/knowledge/documents'
    const method = isEdit.value ? 'PUT' : 'POST'
    
    const response = await fetch(url, {
      method,
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body)
    })
    
    if (response.ok) {
      ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
      router.push(`/knowledge?baseId=${baseId}`)
    } else {
      ElMessage.error(isEdit.value ? '更新失败' : '创建失败')
    }
  } catch (e) {
    ElMessage.error(isEdit.value ? '更新失败' : '创建失败')
  } finally {
    submitting.value = false
  }
}

const insertFormat = (before, after) => {
  form.value.content += before + after
}

// 查看切片
const viewChunks = async () => {
  if (!docId) {
    ElMessage.warning('文档尚未保存')
    return
  }
  
  showChunksDialog.value = true
  loadingChunks.value = true
  
  try {
    const response = await fetch(`/api/v0/knowledge/documents/${docId}/chunks`)
    if (response.ok) {
      chunks.value = await response.json()
    } else {
      ElMessage.error('加载切片失败')
    }
  } catch (e) {
    console.error('加载切片异常:', e)
    ElMessage.error('加载切片失败')
  } finally {
    loadingChunks.value = false
  }
}

// 重新分片
const rechunkDocument = async () => {
  if (!docId) {
    ElMessage.warning('文档尚未保存')
    return
  }
  
  if (!form.value.content || form.value.content.trim().length === 0) {
    ElMessage.warning('文档内容为空，无法分片')
    return
  }
  
  submitting.value = true
  try {
    const response = await fetch(`/api/v0/knowledge/documents/${docId}/rechunk`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        chunkingStrategy: form.value.chunkingStrategy,
        chunkSize: form.value.chunkSize
      })
    })
    
    if (response.ok) {
      const result = await response.json()
      if (result.success) {
        ElMessage.success(`重新分片成功，共生成 ${result.chunkCount} 个切片`)
        // 刷新切片列表
        viewChunks()
      } else {
        ElMessage.error(result.message || '重新分片失败')
      }
    } else {
      ElMessage.error('重新分片失败')
    }
  } catch (e) {
    console.error('重新分片异常:', e)
    ElMessage.error('重新分片失败')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadDoc()
})
</script>

<template>
  <div class="doc-form-page">
    <div class="page-header">
      <div class="header-left">
        <el-button :icon="ArrowLeft" text @click="handleBack">返回</el-button>
        <h2>{{ isEdit ? '编辑文档' : '添加文档' }}</h2>
      </div>
    </div>
    
    <div class="form-container" v-loading="loading">
      <el-card>
        <div class="editor-wrapper">
          <el-form :model="form" label-width="100px" class="doc-form">
            <el-form-item label="标题" required>
              <el-input 
                v-model="form.title" 
                placeholder="请输入文档标题" 
                maxlength="100" 
                show-word-limit 
                size="large"
              />
            </el-form-item>
            
            <el-form-item label="类型">
              <el-select v-model="form.type" style="width: 200px;">
                <el-option label="文本" value="text" />
                <el-option label="Markdown" value="markdown" />
                <el-option label="PDF" value="pdf" />
                <el-option label="Word" value="word" />
              </el-select>
            </el-form-item>
            
            <el-form-item label="标签">
              <el-input 
                v-model="form.tags" 
                placeholder="多个标签用逗号分隔" 
                style="max-width: 400px;"
              />
            </el-form-item>
          </el-form>
          
          <div class="content-editor-section">
            <div class="editor-toolbar">
              <el-button-group>
                <el-button size="small" @click="insertFormat('**', '**')">粗体</el-button>
                <el-button size="small" @click="insertFormat('*', '*')">斜体</el-button>
                <el-button size="small" @click="insertFormat('\n## ', '')">标题</el-button>
                <el-button size="small" @click="insertFormat('\n- ', '')">列表</el-button>
                <el-button size="small" @click="insertFormat('`', '`')">代码</el-button>
              </el-button-group>
              <span class="char-count">{{ form.content.length }} 字符</span>
            </div>
            <textarea 
              v-model="form.content" 
              class="content-editor"
              placeholder="在此输入文档内容..."
              @keydown.ctrl.s.prevent="handleSubmit"
              @keydown.meta.s.prevent="handleSubmit"
            ></textarea>
          </div>
          
          <div class="form-actions">
            <el-button type="primary" @click="handleSubmit" :loading="submitting">
              {{ isEdit ? '保存' : '创建' }}
            </el-button>
            <el-button v-if="isEdit" type="info" :icon="View" @click="viewChunks">查看切片</el-button>
            <el-button @click="handleBack">取消</el-button>
          </div>
        </div>
      </el-card>
      
      <!-- 切片配置卡片（仅在编辑时显示） -->
      <el-card v-if="isEdit" class="chunking-card" style="margin-top: 20px;">
        <template #header>
          <div class="card-header">
            <span>切片配置</span>
          </div>
        </template>
        
        <div class="chunking-section">
          <div class="section-title">
            <span>切片方式</span>
            <span class="required">*</span>
          </div>
          <div class="strategy-grid">
            <div 
              v-for="strategy in chunkingStrategies" 
              :key="strategy.key"
              class="strategy-card"
              :class="{ active: form.chunkingStrategy === strategy.key }"
              @click="form.chunkingStrategy = strategy.key"
            >
              <div class="strategy-header">
                <span class="strategy-name">{{ strategy.name }}</span>
                <div class="strategy-radio">
                  <div class="radio-inner" v-if="form.chunkingStrategy === strategy.key"></div>
                </div>
              </div>
              <div class="strategy-desc">{{ strategy.desc }}</div>
            </div>
          </div>
        </div>

        <div class="chunk-size-section">
          <div class="section-title">
            <span>最大分段长度</span>
            <span class="required">*</span>
          </div>
          <div class="slider-container">
            <el-slider v-model="form.chunkSize" :min="10" :max="6000" :step="10" show-stops />
            <div class="slider-labels">
              <span>10</span>
              <span>6000</span>
            </div>
          </div>
          <div class="chunk-size-input">
            <el-input-number v-model="form.chunkSize" :min="10" :max="6000" :step="10" />
          </div>
        </div>
        
        <div class="chunking-actions">
          <el-button type="warning" @click="rechunkDocument" :loading="submitting">
            重新分片
          </el-button>
        </div>
      </el-card>
    </div>
    
    <!-- 切片查看弹窗 -->
    <el-dialog 
      v-model="showChunksDialog" 
      title="文档切片" 
      width="800px"
      :close-on-click-modal="false"
    >
      <div v-loading="loadingChunks">
        <div v-if="chunks.length === 0" class="empty-chunks">
          <el-empty description="暂无切片数据" />
        </div>
        <div v-else class="chunks-list">
          <el-collapse>
            <el-collapse-item 
              v-for="(chunk, index) in chunks" 
              :key="chunk.id || index"
              :title="`切片 ${index + 1} (${chunk.content?.length || 0} 字符)`"
            >
              <div class="chunk-content">{{ chunk.content }}</div>
            </el-collapse-item>
          </el-collapse>
        </div>
      </div>
      <template #footer>
        <el-button @click="showChunksDialog = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.doc-form-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  
  .page-header {
    padding: 1rem 1.5rem;
    background: white;
    border-bottom: 1px solid #e4e7ed;
    flex-shrink: 0;
    
    .header-left {
      display: flex;
      align-items: center;
      gap: 1rem;
      
      h2 {
        margin: 0;
        font-size: 1.25rem;
      }
    }
  }
  
  .form-container {
    flex: 1;
    padding: 1.5rem;
    overflow-y: auto;
    
    .el-card {
      height: 100%;
      
      .editor-wrapper {
        display: flex;
        flex-direction: column;
        height: 100%;
        
        .doc-form {
          margin-bottom: 1rem;
        }
        
        .content-editor-section {
          display: flex;
          flex-direction: column;
          flex: 1;
          min-height: 400px;
          
          .editor-toolbar {
            display: flex;
            justify-content: space-between;
            align-items: center;
            padding: 0.5rem;
            background: #f5f7fa;
            border-radius: 8px 8px 0 0;
            border: 1px solid #e4e7ed;
            border-bottom: none;
            
            .char-count {
              color: #909399;
              font-size: 12px;
            }
          }
          
          .content-editor {
            flex: 1;
            width: 100%;
            padding: 1rem;
            border: 1px solid #e4e7ed;
            border-radius: 0 0 8px 8px;
            resize: none;
            font-family: 'Monaco', 'Menlo', 'Consolas', monospace;
            font-size: 14px;
            line-height: 1.8;
            color: #333;
            background: #fff;
            
            &:focus {
              outline: none;
              border-color: #667eea;
            }
            
            &::placeholder {
              color: #c0c4cc;
            }
          }
        }
        
        .form-actions {
          margin-top: 1rem;
          display: flex;
          gap: 0.75rem;
        }
      }
    }
    
    // 切片配置卡片
    .chunking-card {
      .card-header {
        font-weight: 600;
        font-size: 16px;
      }
      
      .chunking-section {
        margin-bottom: 1.5rem;
        
        .section-title {
          font-size: 14px;
          font-weight: 600;
          color: #1c1f23;
          margin-bottom: 1rem;
          display: flex;
          align-items: center;
          gap: 4px;
          
          .required {
            color: #f56c6c;
          }
        }
        
        .strategy-grid {
          display: grid;
          grid-template-columns: repeat(3, 1fr);
          gap: 1rem;
          
          .strategy-card {
            border: 1px solid #e4e7ed;
            border-radius: 8px;
            padding: 1rem;
            cursor: pointer;
            transition: all 0.3s;
            
            &:hover {
              border-color: #409EFF;
              box-shadow: 0 2px 12px rgba(64, 158, 255, 0.1);
            }
            
            &.active {
              border-color: #409EFF;
              background: #f0f9ff;
              
              .strategy-radio {
                border-color: #409EFF;
              }
            }
            
            .strategy-header {
              display: flex;
              justify-content: space-between;
              align-items: center;
              margin-bottom: 0.5rem;
              
              .strategy-name {
                font-weight: 600;
                font-size: 14px;
                color: #1c1f23;
              }
              
              .strategy-radio {
                width: 18px;
                height: 18px;
                border-radius: 50%;
                border: 2px solid #dcdfe6;
                display: flex;
                align-items: center;
                justify-content: center;
                
                .radio-inner {
                  width: 8px;
                  height: 8px;
                  border-radius: 50%;
                  background: #409EFF;
                }
              }
            }
            
            .strategy-desc {
              font-size: 12px;
              color: #606266;
              line-height: 1.5;
            }
          }
        }
      }
      
      .chunk-size-section {
        margin-bottom: 1.5rem;
        padding: 1rem;
        background: #fafbfc;
        border-radius: 8px;
        
        .section-title {
          font-size: 14px;
          font-weight: 600;
          color: #1c1f23;
          margin-bottom: 1rem;
          display: flex;
          align-items: center;
          gap: 4px;
          
          .required {
            color: #f56c6c;
          }
        }
        
        .slider-container {
          margin-bottom: 1rem;
          
          .slider-labels {
            display: flex;
            justify-content: space-between;
            margin-top: 0.5rem;
            font-size: 12px;
            color: #909399;
          }
        }
        
        .chunk-size-input {
          display: flex;
          justify-content: flex-end;
        }
      }
      
      .chunking-actions {
        display: flex;
        justify-content: flex-end;
        padding-top: 1rem;
        border-top: 1px solid #e4e7ed;
      }
    }
  }
}

// 切片弹窗样式
.chunks-list {
  max-height: 500px;
  overflow-y: auto;
  
  .chunk-content {
    white-space: pre-wrap;
    word-break: break-all;
    max-height: 200px;
    overflow-y: auto;
    padding: 10px;
    background: #f5f7fa;
    border-radius: 4px;
    font-size: 13px;
    line-height: 1.6;
  }
}

.empty-chunks {
  padding: 2rem;
}
</style>
