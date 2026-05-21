<script setup>
import { ref, onMounted, nextTick, computed, onUnmounted } from 'vue'
import IconSvg from '@/components/ui/IconSvg.vue'
import AIContentRenderer from '@/components/AIContentRenderer.vue'
import { useChat } from '@/composables/useChat'

const {
  messages,
  loading,
  selectedKb,
  selectedKbName,
  knowledgeBases,
  sendMessageStream,
  stopStreaming,
  selectKnowledgeBase,
  initWelcomeMessage
} = useChat()

const inputMessage = ref('')
const showKbDropdown = ref(false)
const chatContainer = ref(null)
const deepSearchEnabled = ref(true)
const webSearchEnabled = ref(false)
const useRealApi = ref(false)

const hotQuestions = [
  '如何部署微服务到 Kubernetes？',
  'React 19 有哪些新特性？',
  '数据库分库分表策略有哪些？'
]

const suggestions = computed(() => hotQuestions.slice(0, 3))

const recentUpdates = [
  'AI Agent 在企业知识库中的应用',
  'Kubernetes 1.28 新特性解读',
  '前端工程化最佳实践 2025'
]

const stats = ref({
  todayQuestions: 24,
  satisfactionRate: 92,
  avgResponse: 1.2
})

const scrollToBottom = async () => {
  await nextTick()
  if (chatContainer.value) {
    chatContainer.value.scrollTop = chatContainer.value.scrollHeight
  }
}

const toggleKbDropdown = () => {
  showKbDropdown.value = !showKbDropdown.value
}

const selectKb = (kb) => {
  selectKnowledgeBase(kb.id)
  showKbDropdown.value = false
}

const handleSendMessage = async () => {
  if (!inputMessage.value.trim() || loading.value) return
  await sendMessageStream(inputMessage.value, !useRealApi.value)
  inputMessage.value = ''
  await scrollToBottom()
}

const handleStopStreaming = () => {
  stopStreaming()
}

const handleKeydown = (e) => {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    handleSendMessage()
  }
}

const askQuestion = (question) => {
  inputMessage.value = question
  handleSendMessage()
}

onMounted(() => {
  initWelcomeMessage()
})

onUnmounted(() => {
  stopStreaming()
})
</script>

<template>
  <div class="chat-page">
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">
          <IconSvg name="brain" :size="20" color="#7c3aed" />
          AI 智能问答
        </h2>
        <p class="page-desc">基于企业知识库的智能问答助手，快速获取精准答案</p>
      </div>
      <div class="header-right">
        <div class="kb-selector" :class="{ open: showKbDropdown }">
          <button class="kb-selector-btn" @click="toggleKbDropdown">
            <IconSvg name="database" :size="14" color="#7c3aed" />
            <span>{{ selectedKbName }}</span>
            <IconSvg name="chevron-down" :size="10" color="#9ca3af" />
          </button>
          <div class="kb-dropdown" v-if="showKbDropdown">
            <div 
              v-for="kb in knowledgeBases" 
              :key="kb.id"
              class="kb-option"
              :class="{ active: selectedKb === kb.id }"
              @click="selectKb(kb)"
            >
              {{ kb.name }}
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="chat-main-wrapper">
      <div class="chat-container">
        <div class="chat-messages" ref="chatContainer">
          <div 
            v-for="msg in messages" 
            :key="msg.id"
            class="message"
            :class="{ 'user-message': msg.isUser }"
          >
            <div class="message-avatar" v-if="!msg.isUser">
              <IconSvg name="brain" :size="16" color="#7c3aed" />
            </div>
            
            <div class="message-content">
              <div class="message-bubble" :class="{ user: msg.isUser }">
                <p v-if="msg.isUser">{{ msg.content }}</p>
                <AIContentRenderer 
                  v-else
                  :content="msg.content"
                  :is-streaming="msg.isStreaming"
                  :references="msg.references"
                />
                <div class="message-suggestions" v-if="msg.showSuggestions">
                  <span 
                    v-for="(q, i) in suggestions" 
                    :key="i"
                    class="suggestion-tag"
                    @click="askQuestion(q)"
                  >
                    {{ q }}
                  </span>
                </div>
                <div class="message-footer" v-if="!msg.isUser && !msg.isStreaming">
                  <span class="footer-text">基于知识库生成 · AI 助手</span>
                </div>
              </div>
            </div>
            
            <div class="message-avatar user" v-if="msg.isUser">
              <span>张</span>
            </div>
          </div>
          
          <div class="message" v-if="loading && messages[messages.length - 1]?.isStreaming !== true">
            <div class="message-avatar">
              <IconSvg name="brain" :size="16" color="#7c3aed" />
            </div>
            <div class="message-content">
              <div class="message-bubble typing">
                <div class="typing-indicator">
                  <span></span>
                  <span></span>
                  <span></span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="chat-input-area">
          <div class="input-wrapper">
            <textarea 
              v-model="inputMessage"
              placeholder="输入你的问题..."
              @keydown="handleKeydown"
              rows="1"
            ></textarea>
            <button class="attach-btn" title="上传文件">
              <IconSvg name="paperclip" :size="16" color="#9ca3af" />
            </button>
          </div>
          <button 
            v-if="loading"
            class="stop-btn"
            @click="handleStopStreaming"
          >
            <IconSvg name="x" :size="14" color="#fff" />
            停止
          </button>
          <button 
            v-else
            class="send-btn" 
            :disabled="!inputMessage.trim()"
            @click="handleSendMessage"
          >
            <IconSvg name="send" :size="14" color="#fff" />
            发送
          </button>
        </div>
        
        <div class="input-footer">
          <div class="footer-options">
            <label class="option-item">
              <input type="checkbox" v-model="deepSearchEnabled">
              <span>深度检索</span>
            </label>
            <label class="option-item">
              <input type="checkbox" v-model="webSearchEnabled">
              <span>联网搜索</span>
            </label>
            <label class="option-item api-toggle">
              <input type="checkbox" v-model="useRealApi">
              <span>真实API</span>
            </label>
          </div>
          <p class="footer-hint">AI 生成内容仅供参考，请核实关键信息</p>
        </div>
      </div>
    </div>

    <div class="stats-cards">
      <div class="stat-card">
        <div class="card-header">
          <IconSvg name="fire" :size="14" color="#f59e0b" />
          <span>热门问题</span>
        </div>
        <div class="card-list">
          <div 
            v-for="(q, i) in hotQuestions" 
            :key="i"
            class="list-item"
            @click="askQuestion(q)"
          >
            {{ q }}
          </div>
        </div>
      </div>

      <div class="stat-card">
        <div class="card-header">
          <IconSvg name="clock" :size="14" color="#7c3aed" />
          <span>最近更新</span>
        </div>
        <div class="card-list">
          <div 
            v-for="(item, i) in recentUpdates" 
            :key="i"
            class="list-item"
          >
            {{ item }}
          </div>
        </div>
      </div>

      <div class="stat-card stats-summary">
        <div class="card-header">
          <IconSvg name="chart-bar" :size="14" color="#10b981" />
          <span>今日问答统计</span>
        </div>
        <div class="stats-grid">
          <div class="stat-item">
            <p class="stat-value">{{ stats.todayQuestions }}</p>
            <p class="stat-label">今日提问数</p>
          </div>
          <div class="stat-item">
            <p class="stat-value">{{ stats.satisfactionRate }}%</p>
            <p class="stat-label">满意率</p>
          </div>
          <div class="stat-item">
            <p class="stat-value">{{ stats.avgResponse }}s</p>
            <p class="stat-label">平均响应</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.chat-page {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  gap: $spacing-md;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  flex-wrap: wrap;
  gap: $spacing-sm;
}

.header-left {
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

.header-right {
  position: relative;
}

.kb-selector {
  position: relative;
  
  &.open .kb-dropdown {
    display: block;
  }
}

.kb-selector-btn {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  padding: 8px 12px;
  background: #fff;
  border: 1px solid $gray-200;
  border-radius: $radius-md;
  font-size: $font-size-sm;
  color: $gray-700;
  cursor: pointer;
  transition: all 0.2s ease;
  
  &:hover {
    border-color: $purple-300;
  }
}

.kb-dropdown {
  display: none;
  position: absolute;
  top: 100%;
  right: 0;
  margin-top: 4px;
  background: #fff;
  border: 1px solid $gray-200;
  border-radius: $radius-md;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  min-width: 160px;
  z-index: 10;
}

.kb-option {
  padding: 10px 12px;
  font-size: $font-size-sm;
  color: $gray-600;
  cursor: pointer;
  transition: all 0.2s ease;
  
  &:hover {
    background: $purple-50;
    color: $purple-600;
  }
  
  &.active {
    background: $purple-50;
    color: $purple-600;
    font-weight: 500;
  }
}

.chat-main-wrapper {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.chat-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: $radius-lg;
  border: 1px solid $gray-100;
  overflow: hidden;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: $spacing-lg;
  display: flex;
  flex-direction: column;
  gap: $spacing-md;
}

.message {
  display: flex;
  gap: $spacing-sm;
  animation: fadeIn 0.3s ease;
  
  &.user-message {
    flex-direction: row-reverse;
  }
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.message-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: $purple-100;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  
  &.user {
    background: $purple-600;
    color: #fff;
    font-size: $font-size-xs;
    font-weight: 600;
  }
}

.message-content {
  max-width: 80%;
}

.message-bubble {
  background: $gray-50;
  border-radius: $radius-lg;
  border-top-left-radius: 4px;
  padding: $spacing-md;
  
  &.user {
    background: $purple-600;
    color: #fff;
    border-radius: $radius-lg;
    border-top-right-radius: 4px;
    
    p {
      color: #fff;
    }
  }
  
  &.streaming {
    p {
      min-height: 24px;
    }
  }
  
  p {
    font-size: $font-size-sm;
    color: $gray-700;
    line-height: 1.6;
    margin: 0;
  }
  
  &.typing {
    padding: $spacing-md $spacing-lg;
  }
}

.message-suggestions {
  display: flex;
  flex-wrap: wrap;
  gap: $spacing-xs;
  margin-top: $spacing-sm;
}

.suggestion-tag {
  font-size: $font-size-xs;
  color: $purple-600;
  background: $purple-50;
  padding: 4px 10px;
  border-radius: 9999px;
  cursor: pointer;
  transition: all 0.2s ease;
  
  &:hover {
    background: $purple-100;
  }
}

.message-footer {
  margin-top: $spacing-sm;
  padding-top: $spacing-xs;
  border-top: 1px solid $gray-200;
  
  .footer-text {
    font-size: 10px;
    color: $gray-400;
  }
}

.typing-indicator {
  display: flex;
  gap: 4px;
  
  span {
    width: 8px;
    height: 8px;
    background: $purple-400;
    border-radius: 50%;
    animation: typing 1.4s infinite ease-in-out both;
    
    &:nth-child(1) { animation-delay: -0.32s; }
    &:nth-child(2) { animation-delay: -0.16s; }
  }
}

@keyframes typing {
  0%, 80%, 100% {
    transform: scale(0.6);
    opacity: 0.4;
  }
  40% {
    transform: scale(1);
    opacity: 1;
  }
}

.chat-input-area {
  display: flex;
  gap: $spacing-sm;
  padding: $spacing-md $spacing-lg;
  border-top: 1px solid $gray-100;
}

.input-wrapper {
  flex: 1;
  position: relative;
  display: flex;
  align-items: flex-end;
  
  textarea {
    flex: 1;
    padding: 10px 40px 10px 14px;
    border: 1px solid $gray-200;
    border-radius: $radius-lg;
    font-size: $font-size-sm;
    resize: none;
    min-height: 44px;
    max-height: 120px;
    line-height: 1.5;
    
    &:focus {
      border-color: $purple-400;
      outline: none;
    }
    
    &::placeholder {
      color: $gray-400;
    }
  }
  
  .attach-btn {
    position: absolute;
    right: 8px;
    bottom: 10px;
    width: 28px;
    height: 28px;
    border: none;
    background: transparent;
    border-radius: $radius-md;
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    
    &:hover {
      background: $gray-100;
    }
  }
}

.send-btn, .stop-btn {
  display: flex;
  align-items: center;
  gap: $spacing-xs;
  padding: 10px 20px;
  border: none;
  border-radius: $radius-lg;
  color: #fff;
  font-size: $font-size-sm;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.send-btn {
  background: $purple-600;
  
  &:hover:not(:disabled) {
    background: $purple-700;
  }
  
  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
}

.stop-btn {
  background: $gray-500;
  
  &:hover {
    background: $gray-600;
  }
}

.input-footer {
  display: flex;
  justify-content: space-between;
  padding: 0 $spacing-lg $spacing-md;
}

.footer-options {
  display: flex;
  gap: $spacing-md;
}

.option-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 10px;
  color: $gray-400;
  cursor: pointer;
  
  input {
    width: 12px;
    height: 12px;
    accent-color: $purple-600;
  }
  
  &.api-toggle {
    color: $purple-600;
    
    input:checked + span {
      color: $purple-700;
      font-weight: 500;
    }
  }
}

.footer-hint {
  font-size: 10px;
  color: $gray-400;
  margin: 0;
}

.stats-cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: $spacing-md;
}

.stat-card {
  background: #fff;
  border-radius: $radius-lg;
  border: 1px solid $gray-100;
  padding: $spacing-md;
  transition: all 0.2s cubic-bezier(0.2, 0, 0, 1);
  
  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 8px 16px -8px rgba(139, 92, 246, 0.12);
  }
}

.card-header {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  font-size: $font-size-xs;
  font-weight: 600;
  color: $gray-700;
  margin-bottom: $spacing-sm;
}

.card-list {
  display: flex;
  flex-direction: column;
  gap: $spacing-xs;
}

.list-item {
  font-size: $font-size-xs;
  color: $gray-600;
  padding: $spacing-xs 0;
  cursor: pointer;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  
  &:hover {
    color: $purple-600;
  }
}

.stats-summary {
  .stats-grid {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: $spacing-sm;
    text-align: center;
  }
  
  .stat-item {
    .stat-value {
      font-size: $font-size-lg;
      font-weight: 700;
      color: $gray-800;
      margin: 0;
    }
    
    .stat-label {
      font-size: 9px;
      color: $gray-400;
      margin: 2px 0 0 0;
    }
  }
}

@media (max-width: 1024px) {
  .stats-cards {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: stretch;
  }
  
  .header-right {
    width: 100%;
    
    .kb-selector-btn {
      width: 100%;
      justify-content: center;
    }
    
    .kb-dropdown {
      left: 0;
      right: 0;
      width: 100%;
    }
  }
  
  .message-content {
    max-width: 85%;
  }
  
  .stats-cards {
    grid-template-columns: 1fr;
  }
}
</style>