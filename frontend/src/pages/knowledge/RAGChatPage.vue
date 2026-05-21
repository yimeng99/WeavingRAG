<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()

const chatContainer = ref(null)
const questionInput = ref('')
const loading = ref(false)
const selectedBaseId = ref('')
const threshold = ref(0.3)
const topK = ref(10)
const enableRerank = ref(true)

const knowledgeBases = ref([])
const conversations = ref([])
const searchResults = ref([])
const stats = ref({
  todayCount: 0,
  accuracy: 0,
  avgResponseTime: 0,
  docCount: 0
})

const charCount = computed(() => questionInput.value.length)

const handleBack = () => {
  router.push({ path: '/knowledge', query: { baseId: selectedBaseId.value } })
}

const selectKnowledgeBase = (kb) => {
  knowledgeBases.value.forEach(k => {
    k.selected = k.id === kb.id
  })
  selectedBaseId.value = kb.id
  router.replace({ path: '/rag-chat', query: { baseId: kb.id } })
}

const scrollToBottom = () => {
  setTimeout(() => {
    if (chatContainer.value) {
      chatContainer.value.scrollTop = chatContainer.value.scrollHeight
    }
  }, 100)
}

const handleSubmit = async () => {
  const question = questionInput.value.trim()
  if (!question) {
    ElMessage.warning('请输入问题')
    return
  }

  if (charCount.value > 1000) {
    ElMessage.warning('问题长度不能超过 1000 字')
    return
  }

  conversations.value.push({
    role: 'user',
    content: question,
    timestamp: new Date()
  })

  questionInput.value = ''
  loading.value = true
  scrollToBottom()

  try {
    const startTime = Date.now()
    
    // 构建 URL 参数
    const urlParams = new URLSearchParams({
      query: question,
      knowledgeBaseId: selectedBaseId.value,
      maxResults: topK.value.toString()
    })
    
    // 使用 SSE 流式接口（GET 请求，参数通过 URL 传递）
    const eventSource = new EventSource(`/api/v0/knowledge/rag/chat/stream?${urlParams.toString()}`)
    
    // 创建一个空的助手消息用于流式更新
    const assistantMessageIndex = conversations.value.length
    conversations.value.push({
      role: 'assistant',
      content: '思考中...',
      sources: [],
      responseTime: 0,
      timestamp: new Date(),
      isLLM: true,
      isStreaming: true
    })
    
    let answer = ''
    let searchResultsData = []
    
    eventSource.onmessage = (event) => {
      // 处理默认消息
      console.log('SSE message:', event.data)
    }
    
    eventSource.addEventListener('search_results', (event) => {
      console.log('检索结果:', event.data)
      try {
        searchResultsData = JSON.parse(event.data)
        // 更新检索结果面板
        searchResults.value = searchResultsData.map(r => ({
          title: r.title || '未知文档',
          content: r.content,
          score: (r.score * 100).toFixed(1),
          docId: r.docId,
          chunkIndex: r.chunkIndex
        }))
      } catch (e) {
        console.error('解析检索结果失败:', e)
      }
    })
    
    eventSource.addEventListener('token', (event) => {
      // 接收文本片段
      answer += event.data
      if (conversations.value[assistantMessageIndex]) {
        conversations.value[assistantMessageIndex].content = answer
        scrollToBottom()
      }
    })
    
    eventSource.addEventListener('start', (event) => {
      console.log('开始生成回答')
      if (conversations.value[assistantMessageIndex]) {
        conversations.value[assistantMessageIndex].content = ''
      }
    })
    
    eventSource.addEventListener('complete', (event) => {
      console.log('回答完成')
      const endTime = Date.now()
      const responseTime = ((endTime - startTime) / 1000).toFixed(1)
      
      if (conversations.value[assistantMessageIndex]) {
        conversations.value[assistantMessageIndex].isStreaming = false
        conversations.value[assistantMessageIndex].responseTime = responseTime
        conversations.value[assistantMessageIndex].hasKnowledge = searchResultsData.length > 0
      }
      
      stats.value.todayCount += 1
      stats.value.avgResponseTime = ((stats.value.avgResponseTime + parseFloat(responseTime)) / 2).toFixed(1)
      
      eventSource.close()
      loading.value = false
      scrollToBottom()
    })
    
    eventSource.addEventListener('error', (event) => {
      console.error('SSE error:', event.data)
      if (conversations.value[assistantMessageIndex]) {
        conversations.value[assistantMessageIndex].content = '抱歉，生成回答时出现错误。'
        conversations.value[assistantMessageIndex].isStreaming = false
      }
      eventSource.close()
      loading.value = false
    })
    
    eventSource.onerror = (error) => {
      console.error('SSE 连接错误:', error)
      if (conversations.value[assistantMessageIndex]) {
        conversations.value[assistantMessageIndex].content = '抱歉，连接服务器失败。'
        conversations.value[assistantMessageIndex].isStreaming = false
      }
      eventSource.close()
      loading.value = false
    }
    
  } catch (error) {
    console.error('问答失败:', error)
    ElMessage.error('回答失败，请稍后重试')
    conversations.value.push({
      role: 'assistant',
      content: '抱歉，系统出现错误，请稍后重试。',
      timestamp: new Date()
    })
    loading.value = false
  }
}

const viewSource = (result) => {
  if (result.docId) {
    window.open(`/knowledge/chunks/${result.docId}?baseId=${selectedBaseId.value}`, '_blank')
  }
}

const loadStats = () => {
  stats.value = {
    todayCount: 128,
    accuracy: 96,
    avgResponseTime: 4.2,
    docCount: 5800
  }
}

const loadKnowledgeBases = async () => {
  try {
    const response = await fetch('/api/v0/knowledge/bases')
    if (response.ok) {
      const data = await response.json()
      knowledgeBases.value = (data.list || []).map(kb => ({
        id: kb.id,
        name: kb.name,
        selected: false
      }))
      
      const baseId = route.query.baseId
      if (baseId) {
        selectedBaseId.value = baseId
        const kb = knowledgeBases.value.find(k => k.id === baseId)
        if (kb) {
          kb.selected = true
        }
      } else if (knowledgeBases.value.length > 0) {
        selectedBaseId.value = knowledgeBases.value[0].id
        knowledgeBases.value[0].selected = true
      }
    }
  } catch (error) {
    console.error('加载知识库失败:', error)
  }
}

onMounted(() => {
  loadStats()
  loadKnowledgeBases()
})
</script>

<template>
  <div class="rag-chat-page">
    <!-- 顶部导航栏 -->
    <header class="page-header">
      <div class="header-left">
        <el-button :icon="ArrowLeft" text @click="handleBack" class="back-btn">
          <template #icon>
            <svg viewBox="0 0 24 24" width="16" height="16" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round" stroke-linejoin="round">
              <path d="M19 12H5M12 19l-7-7 7-7"/>
            </svg>
          </template>
        </el-button>
        <svg class="logo-icon" viewBox="0 0 24 24" width="24" height="24" fill="currentColor">
          <path d="M15.5 14h-.79l-.28-.27A6.471 6.471 0 0 0 16 9.5 6.5 6.5 0 1 0 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z"/>
        </svg>
        <h1>RAG 知识库问答系统</h1>
      </div>
      <div class="header-right">
        <div class="user-avatar">U</div>
      </div>
    </header>

    <main class="page-content">
      <!-- 左侧边栏 - 知识库列表 -->
      <aside class="sidebar-left">
        <div class="sidebar-header">
          <h2>知识库</h2>
        </div>
        
        <div class="kb-list">
          <div 
            v-for="kb in knowledgeBases" 
            :key="kb.id"
            class="kb-item"
            :class="{ active: kb.selected }"
            @click="selectKnowledgeBase(kb)"
          >
            <span>{{ kb.name }}</span>
            <svg v-if="kb.selected" viewBox="0 0 24 24" width="16" height="16" fill="currentColor">
              <path d="M20 12A8 8 0 1 0 8.46 4.05 8 8 0 0 0 20 12z"/>
            </svg>
            <svg v-else class="circle-icon" viewBox="0 0 24 24" width="16" height="16" fill="currentColor">
              <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8z"/>
            </svg>
          </div>
        </div>
        
        <div class="search-settings">
          <h3>检索设置</h3>
          <div class="setting-item">
            <span class="label">相似度阈值</span>
            <el-slider v-model="threshold" :min="0" :max="1" :step="0.1" size="small" />
          </div>
          <div class="setting-item">
            <span class="label">返回结果数</span>
            <el-select v-model="topK" size="small">
              <el-option :value="5" label="5" />
              <el-option :value="10" label="10" />
              <el-option :value="15" label="15" />
              <el-option :value="20" label="20" />
            </el-select>
          </div>
          <div class="setting-item checkbox">
            <el-checkbox v-model="enableRerank" size="small">启用重排序</el-checkbox>
          </div>
        </div>
      </aside>

      <!-- 中间区域 - 问答对话 -->
      <section class="chat-section">
        <div class="chat-container" ref="chatContainer">
          <!-- 欢迎消息 -->
          <div class="welcome-message" v-if="conversations.length === 0">
            <div class="message assistant">
              <div class="avatar robot-avatar">
                <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor">
                  <path d="M12 2a2 2 0 0 1 2 2c0 .74-.4 1.39-1 1.73V7h1a7 7 0 0 1 7 7h1a1 1 0 0 1 1 1v3a1 1 0 0 1-1 1h-1v1a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-1H2a1 1 0 0 1-1-1v-3a1 1 0 0 1 1-1h1v-1a7 7 0 0 1 7-7h1V5.73c-.6-.34-1-.99-1-1.73a2 2 0 0 1 2-2M7.5 13A2.5 2.5 0 0 0 5 15.5 2.5 2.5 0 0 0 7.5 18a2.5 2.5 0 0 0 2.5-2.5A2.5 2.5 0 0 0 7.5 13m9 0a2.5 2.5 0 0 0-2.5 2.5 2.5 2.5 0 0 0 2.5 2.5 2.5 2.5 0 0 0 2.5-2.5 2.5 2.5 0 0 0-2.5-2.5z"/>
                </svg>
              </div>
              <div class="message-content">
                <p>您好！我是 RAG 知识库问答助手，我可以基于知识库内容回答您的问题。请输入您想查询的问题，我会为您提供精准的答案。</p>
              </div>
            </div>
          </div>

          <!-- 对话列表 -->
          <div class="conversation-list">
            <div 
              v-for="(msg, index) in conversations" 
              :key="index"
              class="message"
              :class="msg.role"
            >
              <div class="avatar" :class="msg.role">
                <div v-if="msg.role === 'assistant'" class="icon-svg">
                  <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor">
                    <path d="M12 2a2 2 0 0 1 2 2c0 .74-.4 1.39-1 1.73V7h1a7 7 0 0 1 7 7h1a1 1 0 0 1 1 1v3a1 1 0 0 1-1 1h-1v1a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-1H2a1 1 0 0 1-1-1v-3a1 1 0 0 1 1-1h1v-1a7 7 0 0 1 7-7h1V5.73c-.6-.34-1-.99-1-1.73a2 2 0 0 1 2-2M7.5 13A2.5 2.5 0 0 0 5 15.5 2.5 2.5 0 0 0 7.5 18a2.5 2.5 0 0 0 2.5-2.5A2.5 2.5 0 0 0 7.5 13m9 0a2.5 2.5 0 0 0-2.5 2.5 2.5 2.5 0 0 0 2.5 2.5 2.5 2.5 0 0 0 2.5-2.5 2.5 2.5 0 0 0-2.5-2.5z"/>
                  </svg>
                </div>
                <div v-else class="icon-svg">
                  <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor">
                    <path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/>
                  </svg>
                </div>
              </div>
              <div class="message-content">
                <p>{{ msg.content }}</p>
                <div class="message-meta" v-if="msg.responseTime">
                  <span>响应时间：{{ msg.responseTime }}s</span>
                  <span v-if="msg.hasKnowledge">· 基于知识库</span>
                  <span v-else-if="msg.isLLM">· AI 模型生成</span>
                  <span v-if="msg.sources && msg.sources.length > 0">· {{ msg.sources.length }} 条参考</span>
                </div>
              </div>
            </div>

            <!-- 加载状态 -->
            <div class="message assistant" v-if="loading">
              <div class="avatar robot-avatar">
                <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor">
                  <path d="M12 2a2 2 0 0 1 2 2c0 .74-.4 1.39-1 1.73V7h1a7 7 0 0 1 7 7h1a1 1 0 0 1 1 1v3a1 1 0 0 1-1 1h-1v1a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-1H2a1 1 0 0 1-1-1v-3a1 1 0 0 1 1-1h1v-1a7 7 0 0 1 7-7h1V5.73c-.6-.34-1-.99-1-1.73a2 2 0 0 1 2-2M7.5 13A2.5 2.5 0 0 0 5 15.5 2.5 2.5 0 0 0 7.5 18a2.5 2.5 0 0 0 2.5-2.5A2.5 2.5 0 0 0 7.5 13m9 0a2.5 2.5 0 0 0-2.5 2.5 2.5 2.5 0 0 0 2.5 2.5 2.5 2.5 0 0 0 2.5-2.5 2.5 2.5 0 0 0-2.5-2.5z"/>
                </svg>
              </div>
              <div class="message-content loading">
                <div class="loading-dots">
                  <div class="dot"></div>
                  <div class="dot"></div>
                  <div class="dot"></div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 输入区域 -->
        <div class="input-section">
          <el-form @submit.prevent="handleSubmit">
            <div class="input-wrapper">
              <el-input
                v-model="questionInput"
                type="textarea"
                :rows="3"
                placeholder="请输入您的问题..."
                resize="none"
                :maxlength="1000"
              />
              <span class="char-count">{{ charCount }}/1000</span>
            </div>
            
            <div class="input-actions">
              <div class="left-actions">
                <el-button circle class="icon-btn-small">
                  <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor">
                    <path d="M16.5 6v11.5c0 2.21-1.79 4-4 4s-4-1.79-4-4V5c0-1.38 1.12-2.5 2.5-2.5s2.5 1.12 2.5 2.5v10.5c0 .55-.45 1-1 1s-1-.45-1-1V6H10v9.5a2.5 2.5 0 0 0 5 0V5c0-2.21-1.79-4-4-4S7 2.79 7 5v12.5c0 3.04 2.46 5.5 5.5 5.5s5.5-2.46 5.5-5.5V6h-1.5z"/>
                  </svg>
                </el-button>
                <el-button circle class="icon-btn-small">
                  <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor">
                    <path d="M13 3c-4.97 0-9 4.03-9 9H1l3.89 3.89.07.14L9 12H6c0-3.87 3.13-7 7-7s7 3.13 7 7-3.13 7-7 7c-1.93 0-3.68-.79-4.94-2.06l-1.42 1.42C8.27 19.99 10.51 21 13 21c4.97 0 9-4.03 9-9s-4.03-9-9-9zm-1 5v5l4.28 2.54.72-1.21-3.5-2.08V8H12z"/>
                  </svg>
                </el-button>
              </div>
              <el-button 
                type="primary" 
                :loading="loading"
                @click="handleSubmit"
                class="send-btn"
              >
                <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor">
                  <path d="M3.4 20.4L22.1 12 3.4 3.6 2 5l14 7-14 7z"/>
                </svg>
                发送
              </el-button>
            </div>
          </el-form>
        </div>
      </section>

      <!-- 右侧边栏 - 检索结果 -->
      <aside class="sidebar-right">
        <div class="search-results-panel">
          <div class="panel-header">
            <h2>检索结果</h2>
            <span class="result-count">{{ searchResults.length }} 条相关文档</span>
          </div>
          
          <div class="results-list">
            <div 
              v-for="(result, index) in searchResults" 
              :key="index"
              class="result-item"
              @click="viewSource(result)"
            >
              <div class="result-header">
                <h3>{{ result.title }}</h3>
                <el-tag size="small" type="success">{{ result.score }}%</el-tag>
              </div>
              <p class="result-content">{{ result.content }}</p>
              <el-button text size="small" class="view-btn">
                查看全文
                <svg viewBox="0 0 24 24" width="14" height="14" fill="currentColor">
                  <path d="M10 6L8.59 7.41 13.17 12l-4.58 4.59L10 18l6-6z"/>
                </svg>
              </el-button>
            </div>
            
            <div class="empty-results" v-if="searchResults.length === 0">
              <el-empty :description="loading ? '检索中...' : '暂无检索结果'" size="small" />
            </div>
          </div>
        </div>
        
        <div class="stats-panel">
          <h2>问答统计</h2>
          <div class="stats-grid">
            <div class="stat-item">
              <div class="stat-value primary">{{ stats.todayCount }}</div>
              <div class="stat-label">今日问答数</div>
            </div>
            <div class="stat-item">
              <div class="stat-value success">{{ stats.accuracy }}%</div>
              <div class="stat-label">回答准确率</div>
            </div>
            <div class="stat-item">
              <div class="stat-value">{{ stats.avgResponseTime }}s</div>
              <div class="stat-label">平均响应时间</div>
            </div>
            <div class="stat-item">
              <div class="stat-value primary">{{ (stats.docCount / 1000).toFixed(1) }}k</div>
              <div class="stat-label">知识库文档数</div>
            </div>
          </div>
        </div>
      </aside>
    </main>

    <footer class="page-footer">
      <p>© 2025 RAG 知识库问答系统 | 数据安全 | 隐私政策</p>
    </footer>
  </div>
</template>

<style scoped lang="scss">
.rag-chat-page {
  width: 100%;
  height: 100%;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: #f8fafc;

  .page-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 12px 24px;
    background-color: #ffffff;
    border-bottom: 1px solid #e2e8f0;
    flex-shrink: 0;

    .header-left {
      display: flex;
      align-items: center;
      gap: 12px;

      .back-btn {
        padding: 6px 8px;
        
        :deep(svg) {
          color: #64748b;
        }
      }

      .logo-icon {
        color: #3b82f6;
      }

      h1 {
        margin: 0;
        font-size: 20px;
        font-weight: 600;
        color: #1e293b;
      }
    }

    .header-right {
      display: flex;
      align-items: center;
      gap: 16px;

      .user-avatar {
        width: 32px;
        height: 32px;
        border-radius: 50%;
        background-color: #3b82f6;
        color: #ffffff;
        display: flex;
        align-items: center;
        justify-content: center;
        font-weight: 600;
        font-size: 14px;
      }
    }
  }

  .page-content {
    flex: 1;
    display: grid;
    grid-template-columns: 280px 1fr 320px;
    gap: 16px;
    padding: 16px;
    overflow: hidden;
  }

  .sidebar-left,
  .sidebar-right {
    background-color: #ffffff;
    border-radius: 8px;
    padding: 16px;
    overflow-y: auto;
    border: 1px solid #e2e8f0;
  }

  .sidebar-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 16px;

    h2 {
      margin: 0;
      font-size: 16px;
      font-weight: 600;
    }
  }

  .kb-list {
    .kb-item {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 10px 12px;
      margin-bottom: 8px;
      border-radius: 6px;
      cursor: pointer;
      transition: all 0.2s;

      &:hover {
        background-color: #f1f5f9;
      }

      &.active {
        background-color: #eff6ff;
        color: #3b82f6;

        svg {
          color: #3b82f6;
        }
      }

      .circle-icon {
        font-size: 12px;
      }
    }
  }

  .search-settings {
    margin-top: 24px;

    h3 {
      margin: 0 0 12px 0;
      font-size: 14px;
      font-weight: 500;
      color: #64748b;
    }

    .setting-item {
      margin-bottom: 16px;

      .label {
        display: block;
        font-size: 13px;
        color: #475569;
        margin-bottom: 8px;
      }

      &.checkbox {
        display: flex;
        align-items: center;
      }
    }
  }

  .chat-section {
    display: flex;
    flex-direction: column;
    background-color: #ffffff;
    border-radius: 8px;
    overflow: hidden;
    border: 1px solid #e2e8f0;
  }

  .chat-container {
    flex: 1;
    overflow-y: auto;
    padding: 20px;
    display: flex;
    flex-direction: column;
  }

  .welcome-message {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .conversation-list {
    display: flex;
    flex-direction: column;
  }

  .message {
    display: flex;
    gap: 12px;
    margin-bottom: 24px;
    max-width: 80%;

    &.user {
      flex-direction: row-reverse;
      margin-left: auto;

      .message-content {
        background-color: #eff6ff;
        color: #1e293b;
      }
    }

    &.assistant {
      .message-content {
        background-color: #f1f5f9;
        color: #1e293b;
      }
    }

    .avatar {
      width: 32px;
      height: 32px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;

      &.user {
        background-color: #e2e8f0;
        color: #64748b;
      }

      &.robot-avatar {
        background-color: #eff6ff;
        color: #3b82f6;
      }

      .icon-svg {
        display: flex;
        align-items: center;
        justify-content: center;
      }
    }

    .message-content {
      padding: 12px 16px;
      border-radius: 12px;
      font-size: 14px;
      line-height: 1.6;
      white-space: pre-wrap;

      .message-meta {
        margin-top: 8px;
        font-size: 12px;
        color: #64748b;
      }

      &.loading {
        padding: 16px;
      }
    }
  }

  .loading-dots {
    display: flex;
    gap: 4px;

    .dot {
      width: 8px;
      height: 8px;
      border-radius: 50%;
      background-color: #3b82f6;
      animation: pulse 1.4s infinite ease-in-out;

      &:nth-child(1) {
        animation-delay: -0.32s;
      }

      &:nth-child(2) {
        animation-delay: -0.16s;
      }
    }
  }

  @keyframes pulse {
    0%, 80%, 100% {
      transform: scale(0);
      opacity: 0.5;
    }
    40% {
      transform: scale(1);
      opacity: 1;
    }
  }

  .input-section {
    padding: 16px;
    border-top: 1px solid #e2e8f0;

    .input-wrapper {
      position: relative;
      margin-bottom: 12px;

      .char-count {
        position: absolute;
        right: 12px;
        bottom: 8px;
        font-size: 12px;
        color: #94a3b8;
      }
    }

    .input-actions {
      display: flex;
      justify-content: space-between;
      align-items: center;

      .left-actions {
        display: flex;
        gap: 8px;
      }
    }
  }

  .search-results-panel,
  .stats-panel {
    margin-bottom: 16px;

    .panel-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 12px;

      h2 {
        margin: 0;
        font-size: 16px;
        font-weight: 600;
      }

      .result-count {
        font-size: 12px;
        color: #64748b;
      }
    }
  }

  .results-list {
    .result-item {
      padding: 12px;
      border: 1px solid #e2e8f0;
      border-radius: 6px;
      margin-bottom: 12px;
      cursor: pointer;
      transition: all 0.2s;

      &:hover {
        border-color: #3b82f6;
        box-shadow: 0 2px 8px rgba(59, 130, 246, 0.1);
      }

      .result-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 8px;

        h3 {
          margin: 0;
          font-size: 14px;
          font-weight: 500;
        }
      }

      .result-content {
        margin: 0 0 8px 0;
        font-size: 13px;
        color: #64748b;
        display: -webkit-box;
        -webkit-line-clamp: 3;
        -webkit-box-orient: vertical;
        overflow: hidden;
      }

      .view-btn {
        padding: 0;
        font-size: 12px;
        color: #3b82f6;
      }
    }

    .empty-results {
      padding: 20px 0;
    }
  }

  .stats-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;

    .stat-item {
      background-color: #f8fafc;
      padding: 12px;
      border-radius: 6px;
      text-align: center;

      .stat-value {
        font-size: 20px;
        font-weight: 700;
        margin-bottom: 4px;

        &.primary {
          color: #3b82f6;
        }

        &.success {
          color: #10b981;
        }
      }

      .stat-label {
        font-size: 12px;
        color: #64748b;
      }
    }
  }

  .page-footer {
    padding: 12px 24px;
    background-color: #ffffff;
    border-top: 1px solid #e2e8f0;
    text-align: center;
    font-size: 12px;
    color: #64748b;
  }
}
</style>
