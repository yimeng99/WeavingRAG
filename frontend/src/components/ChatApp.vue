<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Search, Plus, Close, DataAnalysis, Document, Picture,
  User, Microphone, Paperclip, QuestionFilled, Share,
  UploadFilled, Loading, VideoPause, Connection, ArrowRight, Service, ArrowDown
} from '@element-plus/icons-vue'
import MarkdownRenderer from './MarkdownRenderer.vue'

// 从父组件接收用户信息
const props = defineProps({
  user: {
    type: Object,
    default: null
  }
})

// 状态管理
const messages = ref([])
const inputMessage = ref('')
const isLoading = ref(false)
const chatSessionId = ref(generateSessionId())
const eventSource = ref(null)
const selectedModel = ref('qwen3.5-flash') // 默认模型
const availableModels = ref([]) // 可选的模型列表，由后端返回
const enableAgent = ref(true) // 是否启用 Agent 模式
const isSidebarOpen = ref(true) // 侧边栏开关
const chatHistory = ref([]) // 历史对话列表
const useKnowledge = ref(false) // 是否启用知识库
const showKnowledgeBase = ref(false) // 显示知识库管理界面
const uploadedFiles = ref([]) // 已上传的文件列表
const isUploading = ref(false) // 是否正在上传文件
const inputAreaRef = ref(null) // 输入框引用
const isDragOver = ref(false) // 是否正在拖拽文件
const rating = ref(0) // 评分

// 生成会话 ID
function generateSessionId() {
  return 'session_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9)
}

// 加载可用的模型列表
const loadAvailableModels = async () => {
  try {
    const response = await fetch('/api/v0/models')
    if (response.ok) {
      const models = await response.json()
      // 转换数据格式
      availableModels.value = models.map((m) => ({
        value: m.value,
        name: m.name,
        description: m.label?.split(' - ')[1] || m.description || m.label
      }))
      // 设置默认模型为第一个
      if (models.length > 0 && !selectedModel.value) {
        selectedModel.value = models[0].value
      }
    }
  } catch (error) {
    console.error('加载模型列表失败:', error)
  }
}

// 组件挂载时加载模型列表
onMounted(() => {
  loadAvailableModels()
  loadChatHistoryFromServer() // 从服务器加载历史记录
})

// 自动滚动到底部
const messagesContainer = ref(null)
const scrollToBottom = async () => {
  await nextTick()
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

// 发送消息
const sendMessage = async () => {
  if ((!inputMessage.value.trim() && uploadedFiles.value.length === 0) || isLoading.value) return

  const userMessage = inputMessage.value.trim()
  
  // 构建消息内容（包含文件信息）
  let messageContent = userMessage
  if (uploadedFiles.value.length > 0) {
    const filesInfo = uploadedFiles.value.map(f => `[文件：${f.name}]`).join(' ')
    messageContent = userMessage ? `${userMessage}\n\n${filesInfo}` : filesInfo
  }
  
  // 如果是第一条消息，创建新的会话记录
  const isFirstMessage = messages.value.length === 0
  if (isFirstMessage) {
    // 生成新的会话 ID
    chatSessionId.value = generateSessionId()
    
    // 创建会话标题
    const title = userMessage.length > 30 ? userMessage.substring(0, 30) + '...' : userMessage
    
    // 添加到左侧会话列表
    const newSession = {
      sessionId: chatSessionId.value,
      title: title,
      model: selectedModel.value,
      messageCount: 1,
      preview: userMessage,
      createTime: new Date().toISOString(),
      updateTime: new Date().toISOString()
    }
    
    // 添加到列表最前面
    chatHistory.value.unshift(newSession)
  }
  
  // 添加用户消息到列表
  messages.value.push({
    role: 'user',
    content: messageContent,
    timestamp: new Date(),
    hasFiles: uploadedFiles.value.length > 0
  })

  // 清空输入框和文件列表
  inputMessage.value = ''
  uploadedFiles.value = []
  isLoading.value = true

  try {
    // 添加助手消息占位
    const assistantMessageIndex = messages.value.length
    messages.value.push({
      role: 'assistant',
      content: '',
      timestamp: new Date(),
      isStreaming: true
    })

    await scrollToBottom()

    // 根据是否启用 Agent 模式选择接口
    const url = enableAgent.value ? `/api/v0/agent/chat` : `/api/v0/chat/completion`
    
    const response = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        chatSessionId: chatSessionId.value,
        prompt: userMessage,
        modelName: selectedModel.value, // 发送选择的模型
        useKnowledge: useKnowledge.value // 发送知识库开关状态
      })
    })

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`)
    }

    // 读取 SSE 流式响应
    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''
    let currentEvent = null

    while (true) {
      const { done, value } = await reader.read()
      
      if (done) {
        break
      }

      const chunk = decoder.decode(value, { stream: true })
      buffer += chunk
      const lines = buffer.split('\n')
      buffer = lines.pop() || '' // 保留最后一行可能不完整的部分

      for (const line of lines) {
        const trimmedLine = line.trim()
        if (!trimmedLine) continue // 跳过空行
        
        // 解析 SSE 格式：event: xxx 或 data: {...}
        if (trimmedLine.startsWith('event:')) {
          // 记录事件类型
          currentEvent = trimmedLine.slice(6).trim()
        } else if (trimmedLine.startsWith('data:')) {
          const data = trimmedLine.slice(5) // 注意这里是 5 不是 6，因为 event: 后面没有空格
          
          // 检查是否是结束标记
          if (data === '[DONE]') {
            break
          }
          
          try {
            const parsedData = JSON.parse(data)
            
            // 只在 token 事件时处理
            if (currentEvent === 'token' && parsedData.token !== undefined) {
              // token 数据 - 确保是字符串
              const token = String(parsedData.token)
              if (messages.value[assistantMessageIndex]) {
                messages.value[assistantMessageIndex].content += token
                await scrollToBottom()
              }
            }
          } catch (e) {
            console.error('解析 SSE 数据失败:', e, '数据:', data)
            // 如果不是 JSON，直接显示原始数据 (兼容旧格式)
            if (currentEvent === 'token') {
              if (messages.value[assistantMessageIndex]) {
                messages.value[assistantMessageIndex].content += String(data)
                await scrollToBottom()
              }
            }
          }
        }
      }
    }

    // 标记流结束
    if (messages.value[assistantMessageIndex]) {
      messages.value[assistantMessageIndex].isStreaming = false
    }

  } catch (error) {
    console.error('发送消息失败:', error)
    messages.value.push({
      role: 'system',
      content: `错误：${error.message}`,
      timestamp: new Date()
    })
  } finally {
    isLoading.value = false
  }
}

// 处理文件上传
const handleFileUpload = async (event) => {
  const files = Array.from(event.target.files)
  if (files.length === 0) return
  
  isUploading.value = true
  
  try {
    // 这里可以调用后端 API 上传文件，暂时先在前端存储
    for (const file of files) {
      // 检查文件大小（限制 10MB）
      if (file.size > 10 * 1024 * 1024) {
        ElMessage.warning(`文件 ${file.name} 超过 10MB 限制`)
        continue
      }
      
      // 读取文件内容（用于预览或后续处理）
      const reader = new FileReader()
      reader.onload = (e) => {
        uploadedFiles.value.push({
          name: file.name,
          type: file.type,
          size: file.size,
          content: e.target.result,
          uploadTime: new Date()
        })
      }
      reader.readAsDataURL(file)
    }
    
    ElMessage.success(`成功添加 ${files.length} 个文件`)
  } catch (error) {
    console.error('文件上传失败:', error)
    ElMessage.error('文件上传失败')
  } finally {
    isUploading.value = false
  }
}

// 移除已上传的文件
const removeFile = (index) => {
  uploadedFiles.value.splice(index, 1)
}

// 处理拖拽上传
const handleDrop = async (event) => {
  event.preventDefault()
  const files = Array.from(event.dataTransfer.files)
  if (files.length === 0) return
  
  // 复用文件上传逻辑
  const fakeEvent = { target: { files } }
  await handleFileUpload(fakeEvent)
}

// 阻止拖拽默认行为
const handleDragOver = (event) => {
  event.preventDefault()
}

// 处理拖拽进入
const handleDragEnter = (event) => {
  event.preventDefault()
  isDragOver.value = true
}

// 处理拖拽离开
const handleDragLeave = () => {
  isDragOver.value = false
}

// 停止生成
const stopGeneration = () => {
  if (eventSource.value) {
    eventSource.value.close()
    eventSource.value = null
  }
  isLoading.value = false
  
  // 标记最后一个助手消息为已停止
  const lastAssistantMessage = messages.value.slice().reverse().find(m => m.role === 'assistant')
  if (lastAssistantMessage && lastAssistantMessage.isStreaming) {
    lastAssistantMessage.isStreaming = false
    lastAssistantMessage.content += ' [已停止]'
  }
}

// 开始新对话
const startNewChat = () => {
  chatSessionId.value = generateSessionId()
  messages.value = []
}

// 从后端加载历史对话
const loadChatHistoryFromServer = async () => {
  try {
    const response = await fetch('/api/v0/sessions')
    if (response.ok) {
      const sessions = await response.json()
      chatHistory.value = sessions.map(session => ({
        sessionId: session.sessionId,
        title: session.title,
        model: session.model,
        messageCount: session.messageCount,
        lastTime: session.updateTime,
        preview: session.preview
      }))
    }
  } catch (e) {
    console.error('从服务器加载历史记录失败:', e)
  }
}

// 加载历史对话
const loadChat = async (chat) => {
  // 先保存当前对话
  if (messages.value.length > 0) {
    // TODO: 保存到后端
  }
  
  // 切换到选中的对话
  chatSessionId.value = chat.sessionId
  selectedModel.value = chat.model || 'qwen3.5-flash'
  
  // 从后端加载历史消息
  try {
    const response = await fetch(`/api/v0/sessions/${chat.sessionId}/messages`)
    if (response.ok) {
      const historyMessages = await response.json()
      console.log('加载到的历史消息:', historyMessages)
      // 将后端返回的消息转换为前端格式
      messages.value = historyMessages.map(msg => {
        let content = msg.content
        
        // 如果 content 是对象，尝试转换为字符串
        if (typeof content === 'object') {
          try {
            content = JSON.stringify(content)
          } catch (e) {
            content = String(content)
          }
        } else if (content === null || content === undefined) {
          content = ''
        } else {
          // 确保是字符串
          content = String(content)
        }
        
        // 处理 UserMessage 的 toString() 格式："UserMessage { text = "..." }"
        if (msg.role === 'user' && content.includes('UserMessage')) {
          const match = content.match(/text\s*=\s*"([^"]+)"/)
          if (match) {
            content = match[1]
          }
        }
        return {
          role: msg.role,
          content: content,
          timestamp: new Date(msg.timestamp || Date.now()),
          isStreaming: false
        }
      })
      console.log(`加载历史消息成功：${messages.value.length} 条`)
    } else {
      console.error('加载历史消息失败:', response.status)
      messages.value = []
    }
  } catch (error) {
    console.error('加载历史消息出错:', error)
    messages.value = []
  }
  
  // 在移动端自动关闭侧边栏
  if (window.innerWidth < 768) {
    isSidebarOpen.value = false
  }
}

// 删除历史对话
const deleteChat = async (sessionId, event) => {
  event.stopPropagation()
  
  try {
    const response = await fetch(`/api/v0/sessions/${sessionId}`, {
      method: 'DELETE'
    })
    
    if (response.ok) {
      chatHistory.value = chatHistory.value.filter(h => h.sessionId !== sessionId)
      // 如果删除的是当前对话，开始新对话
      if (sessionId === chatSessionId.value) {
        startNewChat()
      }
    }
  } catch (e) {
    console.error('删除会话失败:', e)
  }
}

// 退出登录
const handleLogout = async () => {
  try {
    const token = localStorage.getItem('token')
    await fetch('/api/v0/auth/logout', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`
      }
    })
  } catch (e) {
    console.error('退出登录失败:', e)
  } finally {
    // 清除本地数据
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    location.reload() // 刷新页面
  }
}

// 格式化时间
const formatTime = (date) => {
  if (!date) return ''
  const d = new Date(date)
  // 检查是否是有效日期
  if (isNaN(d.getTime())) return ''
  
  const hours = d.getHours().toString().padStart(2, '0')
  const minutes = d.getMinutes().toString().padStart(2, '0')
  return `${hours}:${minutes}`
}

// 格式化日期显示
const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const now = new Date()
  const diff = now - date
  
  // 今天
  if (diff < 24 * 60 * 60 * 1000) {
    return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }
  
  // 7 天内
  if (diff < 7 * 24 * 60 * 60 * 1000) {
    const days = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
    return days[date.getDay()]
  }
  
  // 更久之前
  return date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}

// 全局函数：复制代码块
window.copyCodeBlock = async (button) => {
  try {
    const pre = button.parentElement.querySelector('pre')
    const code = pre.querySelector('code')
    const text = code.textContent
    await navigator.clipboard.writeText(text)
    
    // 显示复制成功提示
    const originalIcon = button.innerHTML
    button.innerHTML = '<span class="copy-icon">✓</span><span class="copy-text" style="color: #67c23a;">已复制</span>'
    setTimeout(() => {
      button.innerHTML = originalIcon
    }, 2000)
  } catch (error) {
    console.error('复制代码失败:', error)
    ElMessage.error('复制失败')
  }
}

// 复制消息内容
const copyMessage = async (content) => {
  try {
    await navigator.clipboard.writeText(content)
    ElMessage.success('复制成功')
  } catch (error) {
    console.error('复制失败:', error)
    ElMessage.error('复制失败')
  }
}

// 重新生成回复
const regenerateResponse = async (message, index) => {
  // 找到对应的用户消息
  const userMessage = messages.value.slice(0, index).reverse().find(m => m.role === 'user')
  if (!userMessage) return
  
  // 删除当前的助手消息
  messages.value.splice(index, 1)
  
  // 重新发送消息
  inputMessage.value = userMessage.content.split('\n\n')[0] // 提取原始消息内容
  await sendMessage()
}

// 分享消息
const shareMessage = (content) => {
  // 复制到剪贴板
  copyMessage(content)
}

// 处理模型选择
const handleModelSelect = (modelValue) => {
  selectedModel.value = modelValue
  console.log('切换到模型:', modelValue)
  // 可以添加提示，告诉用户模型已切换
  ElMessage.success(`已切换到 ${availableModels.find(m => m.value === modelValue)?.name || modelValue}`)
}
</script>

<template>
  <div class="deepsider-container">
    <!-- 左侧导航栏 -->
    <aside class="left-sidebar">
      <div class="sidebar-top">
        <!-- 用户头像 -->
        <div class="user-avatar">
          <el-avatar :src="user?.avatar || 'https://api.dicebear.com/7.x/avataaars/svg?seed=' + (user?.username || 'user')" size="large" />
        </div>
        
        <!-- 搜索框 -->
        <div class="search-box">
          <el-icon><Search /></el-icon>
          <input type="text" placeholder="搜索" />
        </div>
        
        <!-- 新建对话按钮 -->
        <el-button @click="startNewChat" class="new-chat-btn" :icon="Plus">
          新建对话
        </el-button>
      </div>
      
      <!-- 历史对话列表 -->
      <div class="history-section">
        <div class="section-title">今天</div>
        <div
          v-for="chat in chatHistory"
          :key="chat.sessionId"
          class="history-item"
          :class="{ active: chat.sessionId === chatSessionId }"
          @click="loadChat(chat)"
        >
          <span class="history-text">{{ chat.title }}</span>
          <el-button
            @click.stop="deleteChat(chat.sessionId, $event)"
            class="delete-btn"
            :icon="Close"
            text
            size="small"
          />
        </div>
      </div>
      
      <!-- 底部功能菜单 -->
      <div class="sidebar-bottom">
        <div class="menu-item">
          <el-icon size="20"><DataAnalysis /></el-icon>
          <span>数据分析</span>
        </div>
        <div class="menu-item">
          <el-icon size="20"><Service /></el-icon>
          <span>翻译</span>
        </div>
        <div class="menu-item">
          <el-icon size="20"><Document /></el-icon>
          <span>PDF</span>
        </div>
        <div class="menu-item">
          <el-icon size="20"><Picture /></el-icon>
          <span>OCR</span>
        </div>
      </div>
      
      <!-- 用户信息 -->
      <div class="sidebar-footer">
        <div class="footer-text">
          <span>免费用户对话保留 30 天，会员保留 3 年</span>
          <el-icon @click="handleLogout" class="logout-icon"><Close /></el-icon>
        </div>
      </div>
    </aside>

    <!-- 主聊天区域 -->
    <main class="main-chat">
      <!-- 空状态欢迎界面 -->
      <div v-if="messages.length === 0" class="welcome-section">
        <div class="logo-section">
          <div class="logo-icon">💎</div>
          <h1 class="logo-text">DeepSider</h1>
        </div>
        <h2 class="welcome-text">👋 嗨，接下来如何帮助你呢？</h2>
        
        <!-- 功能卡片 -->
        <div class="feature-cards">
          <div class="feature-card">
            <div class="card-icon">🔬</div>
            <div class="card-content">
              <h3>Deep Research</h3>
              <p>整合信息，提炼知识，快速搞懂复杂课题</p>
            </div>
          </div>
          
          <div class="feature-card">
            <div class="card-icon">📊</div>
            <div class="card-content">
              <h3>数据分析</h3>
              <p>躺平神器！数据全部丢进去，结论快到碗里来</p>
            </div>
          </div>
          
          <div class="feature-card">
            <div class="card-icon">🌐</div>
            <div class="card-content">
              <h3>AI 翻译</h3>
              <p>语言开挂 buff！多模型文本/语音实时翻译，又快又准</p>
            </div>
          </div>
        </div>
      </div>
      
      <!-- 消息列表 -->
      <div v-else class="messages-wrapper">
        <div class="messages-container" ref="messagesContainer">
          <div 
            v-for="(message, index) in messages" 
            :key="index" 
            class="message"
            :class="message.role"
          >
            <div class="message-avatar">
              <el-avatar v-if="message.role === 'user'" :icon="User" size="large" />
              <el-avatar v-else class="ai-avatar" size="large">💎</el-avatar>
            </div>
            <div class="message-body">
              <div class="message-header">
                <div class="header-left">
                  <span class="sender-name">{{ message.role === 'user' ? (user?.username || '你') : 'GPT-4o' }}</span>
                  <span class="message-time">{{ formatTime(message.timestamp) }}</span>
                </div>
                <!-- 消息操作栏 - 右上角 -->
                <div v-if="!message.isStreaming" class="message-actions">
                  <button class="action-btn" @click="copyMessage(message.content)" title="复制">
                    <el-icon size="14"><Document /></el-icon>
                  </button>
                  <button v-if="message.role === 'assistant'" class="action-btn" @click="regenerateResponse(message, index)" title="重新生成">
                    <el-icon size="14"><Connection /></el-icon>
                  </button>
                  <button class="action-btn" @click="shareMessage(message.content)" title="分享">
                    <el-icon size="14"><Share /></el-icon>
                  </button>
                </div>
              </div>
              <div class="message-content">
                <MarkdownRenderer v-if="message.content" :content="message.content" />
                <div v-if="message.isStreaming" class="typing-indicator">
                  <span></span><span></span><span></span>
                </div>
              </div>
            </div>
          </div>

          <div v-if="isLoading && messages[messages.length - 1]?.role !== 'assistant'" class="message assistant">
            <div class="message-avatar">
              <el-avatar class="ai-avatar" size="large">💎</el-avatar>
            </div>
            <div class="message-body">
              <div class="message-header">
                <span class="sender-name">GPT-4o</span>
              </div>
              <div class="message-content">
                <div class="typing-indicator">
                  <span></span><span></span><span></span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 底部输入区域 -->
      <div class="bottom-input-section">
        <!-- 输入框 -->
        <div 
          class="input-container" 
          @drop="handleDrop" 
          @dragover="handleDragOver"
          @dragenter="handleDragEnter"
          @dragleave="handleDragLeave"
          :class="{ 'drag-over': isDragOver }"
        >
          <!-- 顶部工具栏：模型选择 -->
          <div class="input-toolbar-top">
            <div class="model-select-btn">
              <el-dropdown trigger="click" @command="handleModelSelect">
                <div class="model-trigger">
                  <span class="model-name">{{ availableModels.find(m => m.value === selectedModel)?.name || 'Qwen3.5-Flash' }}</span>
                  <el-icon class="dropdown-icon"><ArrowDown /></el-icon>
                </div>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item
                      v-for="model in availableModels"
                      :key="model.value"
                      :command="model.value"
                      :class="{ active: model.value === selectedModel }"
                    >
                      <div class="model-dropdown-item">
                        <div class="model-item-title">{{ model.name }}</div>
                        <div class="model-item-desc">{{ model.description }}</div>
                      </div>
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
            <div class="toolbar-top-right">
              <!-- Agent 模式开关 -->
              <el-switch
                v-model="enableAgent"
                size="small"
                active-text="Agent"
                inactive-text="普通"
                style="margin-right: 8px;"
              />
              <el-button class="tool-btn" :icon="Connection" text title="联网搜索" />
              <el-button class="tool-btn" :icon="Microphone" text title="语音输入" />
            </div>
          </div>
          
          <!-- 文件预览 -->
          <div v-if="uploadedFiles.length > 0" class="file-preview-mini">
            <div v-for="(file, index) in uploadedFiles" :key="index" class="file-chip">
              <span>{{ file.name }}</span>
              <el-icon @click="removeFile(index)" class="remove-icon"><Close /></el-icon>
            </div>
          </div>
          
          <el-input
            ref="inputAreaRef"
            v-model="inputMessage"
            @keydown.enter.exact.prevent="sendMessage"
            placeholder="输入内容开始聊天（Shift + Enter 换行）..."
            :disabled="isLoading || isUploading"
            type="textarea"
            :rows="3"
            resize="none"
            class="chat-input"
          />
          
          <!-- 底部工具栏 -->
          <div class="input-toolbar">
            <div class="toolbar-left">
              <label for="file-upload" class="tool-btn" title="上传文件">
                <el-icon><Paperclip /></el-icon>
                <input 
                  id="file-upload" 
                  type="file" 
                  multiple 
                  @change="handleFileUpload"
                  :disabled="isLoading || isUploading"
                  style="display: none;"
                />
              </label>
              <el-button class="tool-btn" :icon="Connection" text title="联网搜索" />
              <el-button class="tool-btn" :icon="Microphone" text title="语音输入" />
            </div>
            <div class="toolbar-right">
              <el-button 
                @click="sendMessage" 
                type="primary"
                :disabled="(!inputMessage.trim() && uploadedFiles.length === 0) || isLoading || isUploading"
                :icon="ArrowRight"
                :loading="isLoading || isUploading"
                circle
                class="send-btn"
              />
            </div>
          </div>
        </div>
        
        <!-- 底部提示 -->
        <div class="input-footer">
          <div class="footer-left">
            <el-button text size="small" :icon="QuestionFilled">反馈</el-button>
            <el-button text size="small" :icon="Share">分享</el-button>
          </div>
          <div class="footer-right">
            <span>打分</span>
            <el-rate v-model="rating" disabled size="small" />
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<style scoped lang="scss">
// 定义 SCSS 变量
$primary-gradient: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
$primary-color: #667eea;
$secondary-color: #764ba2;
$danger-color: #ff4444;

// 背景色
$bg-white: #ffffff;
$bg-light: #f9f9f9;
$bg-lighter: #f0f0f0;
$bg-gray: #f6f8fa;

// 边框色
$border-light: #e5e5e5;
$border-lighter: #f0f0f0;

// 文字色
$text-primary: #333333;
$text-secondary: #666666;
$text-muted: #999999;

// 尺寸
$spacing-xs: 4px;
$spacing-sm: 8px;
$spacing-md: 12px;
$spacing-lg: 16px;
$spacing-xl: 24px;
$spacing-2xl: 48px;  // 增加边距值

// 圆角
$radius-sm: 8px;
$radius-md: 12px;
$radius-lg: 16px;
$radius-full: 50%;

// 过渡
$transition-fast: 0.2s;
$transition-normal: 0.3s;

// DeepSider 风格样式
.deepsider-container {
  display: flex;
  width: 100%;
  height: 100vh;  // 使用 vh 单位确保占满视口
  background: $bg-white;
  position: absolute;  // 绝对定位确保铺满
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
}

// 左侧导航栏
.left-sidebar {
  width: 260px;
  background: $bg-light;
  border-right: 1px solid $border-light;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  padding-top: 64px;
}

.sidebar-top {
  padding: $spacing-lg;
  border-bottom: 1px solid $border-light;
  
  .user-avatar {
    margin-bottom: $spacing-md;
  }
  
  .search-box {
    display: flex;
    align-items: center;
    gap: $spacing-sm;
    background: $bg-lighter;
    border-radius: $radius-sm;
    padding: $spacing-sm $spacing-md;
    margin-bottom: $spacing-md;
    
    input {
      border: none;
      background: transparent;
      outline: none;
      flex: 1;
      font-size: 14px;
    }
  }
  
  .new-chat-btn {
    width: 100%;
    justify-content: flex-start;
    background: $bg-white;
    border: 1px solid $border-light;
    color: $text-primary;
    border-radius: $radius-sm;
    
    &:hover {
      background: $bg-lighter;
      border-color: darken($border-light, 5%);
    }
  }
}

// 历史对话列表
.history-section {
  flex: 1;
  overflow-y: auto;
  padding: $spacing-md $spacing-sm;
  
  .section-title {
    font-size: 13px;
    color: $text-muted;
    padding: $spacing-sm;
    margin-bottom: $spacing-xs;
  }
  
  .history-item {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 10px $spacing-md;
    margin-bottom: $spacing-xs;
    border-radius: $radius-sm;
    cursor: pointer;
    transition: all $transition-fast;
    
    &:hover {
      background: $bg-lighter;
      
      .delete-btn {
        opacity: 1;
      }
    }
    
    &.active {
      background: #e6f4ff;
    }
    
    .history-text {
      flex: 1;
      font-size: 14px;
      color: $text-primary;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
    
    .delete-btn {
      opacity: 0;
      transition: opacity $transition-fast;
    }
  }
}

// 底部菜单
.sidebar-bottom {
  padding: $spacing-sm;
  border-top: 1px solid $border-light;
  
  .menu-item {
    display: flex;
    align-items: center;
    gap: $spacing-md;
    padding: $spacing-md;
    border-radius: $radius-sm;
    cursor: pointer;
    transition: all $transition-fast;
    color: $text-secondary;
    font-size: 14px;
    
    &:hover {
      background: $bg-lighter;
      color: $text-primary;
    }
  }
}

// 底部信息
.sidebar-footer {
  padding: $spacing-md;
  background: $bg-light;
  border-top: 1px solid $border-light;
  
  .footer-text {
    display: flex;
    align-items: center;
    justify-content: space-between;
    font-size: 12px;
    color: $text-muted;
    
    .logout-icon {
      cursor: pointer;
      transition: color $transition-fast;
      
      &:hover {
        color: $danger-color;
      }
    }
  }
}

// 主聊天区域
.main-chat {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: $bg-white;
  height: 100%;  // 确保占满父容器高度
  min-width: 0;  // 防止内容溢出
  padding: $spacing-xl 0;  // 上下留白
}

// 欢迎界面
.welcome-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: $spacing-2xl;
  width: 100%;
  height: 100%;
  max-width: 1200px;  // 限制最大宽度
  margin: 0 auto;  // 居中
  
  .logo-section {
    display: flex;
    align-items: center;
    gap: $spacing-md;
    margin-bottom: $spacing-2xl;
    
    .logo-icon {
      font-size: 48px;
    }
    
    .logo-text {
      font-size: 32px;
      font-weight: 600;
      color: $text-primary;
      margin: 0;
    }
  }
  
  .welcome-text {
    font-size: 20px;
    color: $text-secondary;
    margin-bottom: $spacing-2xl;
  }
  
  // 功能卡片
  .feature-cards {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
    gap: $spacing-md;
    max-width: 900px;
    width: 100%;
    
    .feature-card {
      display: flex;
      gap: $spacing-lg;
      padding: $spacing-lg;
      background: $bg-white;
      border: 1px solid $border-light;
      border-radius: $radius-md;
      cursor: pointer;
      transition: all $transition-fast;
      
      &:hover {
        border-color: $primary-color;
        box-shadow: 0 4px 12px rgba(102, 126, 234, 0.1);
        transform: translateY(-2px);
      }
      
      .card-icon {
        font-size: 32px;
      }
      
      .card-content {
        h3 {
          font-size: 16px;
          font-weight: 600;
          color: $text-primary;
          margin: 0 0 $spacing-sm 0;
        }
        
        p {
          font-size: 13px;
          color: $text-muted;
          margin: 0;
          line-height: 1.5;
        }
      }
    }
  }
}

// 消息列表
.messages-wrapper {
  flex: 1;
  overflow: hidden;
}

.messages-container {
  height: 100%;
  overflow-y: auto;
  padding: $spacing-xl $spacing-2xl;  // 增加左右内边距
  
  // 响应式：小屏幕减少边距
  @media (max-width: 768px) {
    padding: $spacing-lg $spacing-xl;
  }
}

.message {
  // 鼠标移入消息时显示操作按钮
  &:hover .message-actions {
    opacity: 1;
  }
}
  
.message {
  display: flex;
  gap: $spacing-lg;
  margin-bottom: $spacing-xl;
  padding: $spacing-lg 0;
  animation: fadeIn $transition-normal ease;
  max-width: 900px;  // 限制最大宽度
  margin-left: auto;  // 居中显示
  margin-right: auto;
  
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
  
  &:last-child {
    margin-bottom: 0;
  }
  
  &.user {
    .message-body {
      background: transparent;  // 去掉背景色
      border-radius: 0;
      padding: 0;
      max-width: 100%;
      
      .message-content {
        color: $text-primary;
        padding-left: $spacing-md;  // 添加左边距
        padding-top: $spacing-xs;  // 添加上边距，与头像对齐
      }
    }
  }
  
  &.assistant {
    .message-body {
      background: transparent;
      padding: 0;
      width: 100%;
    }
  }
  
  .message-avatar {
    flex-shrink: 0;
    position: relative;
    
    .ai-avatar {
      background: linear-gradient(135deg, #8B5CF6 0%, #7C3AED 100%);
    }
  }
  
  .message-body {
    flex: 1;
    min-width: 0;
    
    .message-header {
      display: flex;
      align-items: flex-start;
      justify-content: space-between;
      gap: $spacing-md;
      margin-bottom: $spacing-sm;
      
      // 用户消息头部不需要调整
      .header-left {
        display: flex;
        align-items: center;
        gap: $spacing-sm;
        
        .sender-name {
          font-weight: 600;
          font-size: 15px;
          color: $text-primary;
        }
        
        .message-time {
          font-size: 12px;
          color: $text-muted;
        }
      }
      
      .message-actions {
        display: flex;
        gap: $spacing-xs;
        flex-shrink: 0;
        opacity: 0;  // 默认隐藏
        transition: opacity $transition-fast;
        
        .action-btn {
          display: flex;
          align-items: center;
          justify-content: center;
          width: 28px;
          height: 28px;
          background: transparent;
          border: none;
          border-radius: 4px;
          color: $text-muted;
          cursor: pointer;
          transition: all $transition-fast;
          padding: 0;
          
          &:hover {
            background: rgba(0, 0, 0, 0.05);
            color: $text-primary;
          }
          
          // 用户消息中的按钮样式
          .user & {
            color: $text-secondary;
            
            &:hover {
              background: rgba(255, 255, 255, 0.6);
              color: $text-primary;
            }
          }
        }
      }
      
      // 鼠标移入时显示操作按钮
      &:hover .message-actions {
        opacity: 1;
      }
    }
  }
  
  .message-content {
    width: 100%;
    line-height: 1.8;
    font-size: 15px;
    
    .user & {
      color: $text-primary;
    }
  }
  
  // 移除消息内容下方的操作栏
  .message-actions {
    display: none;  // 隐藏原来的底部操作栏
  }
}

// 底部输入区域
.bottom-input-section {
  padding: $spacing-lg $spacing-2xl;  // 增加左右内边距
  background: $bg-white;
  border-top: 1px solid $border-light;
  max-width: 900px;  // 与消息宽度一致
  margin: 0 auto;  // 居中
  width: 100%;
}

.model-selector-wrapper {
  display: flex;
  justify-content: center;
  margin-bottom: $spacing-md;
}

.input-container {
  position: relative;
  background: $bg-light;
  border: 1px solid $border-light;
  border-radius: $radius-lg;
  padding: $spacing-md;
  transition: all $transition-normal;
  
  &:focus-within {
    border-color: $primary-color;
    box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.1);
    background: $bg-white;
  }
  
  &.drag-over {
    border-color: $primary-color;
    background: rgba(102, 126, 234, 0.05);
  }
  
  .file-preview-mini {
    display: flex;
    flex-wrap: wrap;
    gap: $spacing-sm;
    margin-bottom: $spacing-md;
    
    .file-chip {
      display: flex;
      align-items: center;
      gap: $spacing-sm;
      padding: $spacing-sm $spacing-md;
      background: #e6f4ff;
      border-radius: $radius-lg;
      font-size: 13px;
      color: $text-primary;
      
      .remove-icon {
        cursor: pointer;
        color: $text-muted;
        transition: color $transition-fast;
        
        &:hover {
          color: $danger-color;
        }
      }
    }
  }
}

.chat-input {
  margin-bottom: $spacing-md;
  
  :deep(.el-textarea__inner) {
    border: none !important;
    box-shadow: none !important;
    background: transparent !important;
   padding: $spacing-sm 0 !important;
   font-size: 15px !important;
   resize: none !important;
    line-height: 1.6 !important;
  }
}

.input-toolbar-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: $spacing-md;
  padding-bottom: $spacing-md;
  border-bottom: 1px solid $border-lighter;
  
  .model-select-btn {
    .model-trigger {
      display: flex;
      align-items: center;
      gap: $spacing-xs;
     padding: $spacing-xs $spacing-md;
      border-radius: $radius-md;
      cursor: pointer;
      transition: all $transition-fast;
      background: $bg-lighter;
      
      &:hover {
        background: darken($bg-lighter, 3%);
      }
      
      .model-name {
      font-size: 14px;
      color: $text-primary;
      font-weight: 500;
      }
      
      .dropdown-icon {
      color: $text-muted;
      font-size: 14px;
        transition: transform $transition-fast;
      }
    }
  }
  
  .toolbar-top-right {
    display: flex;
    align-items: center;
    gap: $spacing-sm;
  }
}

// 模型下拉框样式
.model-dropdown-item {
  padding: $spacing-xs 0;
  
  .model-item-title {
  font-size: 14px;
  font-weight: 500;
  color: $text-primary;
    margin-bottom: 2px;
  }
  
  .model-item-desc {
  font-size: 12px;
  color: $text-muted;
  }
}

.input-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  
  .toolbar-left {
    display: flex;
    gap: $spacing-sm;
    
    .tool-btn {
      color: $text-muted;
      transition: all $transition-fast;
      
      &:hover:not(:disabled) {
        color: $primary-color;
        background: rgba(102, 126, 234, 0.1);
      }
    }
  }
  
  .toolbar-right {
    .send-btn {
      background: $primary-gradient !important;
      width: 40px;
      height: 40px;
      border: none !important;
      
      &:hover:not(:disabled) {
        transform: scale(1.1);
        box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
      }
    }
  }
}

// 输入框底部
.input-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: $spacing-md;
  padding-top: $spacing-md;
  border-top: 1px solid $border-lighter;
  
  .footer-left {
    display: flex;
    gap: $spacing-md;
    
    .el-button {
      color: $text-muted;
      font-size: 13px;
    }
  }
  
  .footer-right {
    display: flex;
    align-items: center;
    gap: $spacing-sm;
    font-size: 13px;
    color: $text-muted;
  }
}

// 打字动画
.typing-indicator {
  display: flex;
  gap: 4px;
  padding: $spacing-sm 0;
  
  span {
    width: 8px;
    height: 8px;
    background: $primary-color;
    border-radius: $radius-full;
    animation: bounce 1.4s infinite ease-in-out;
    
    &:nth-child(1) {
      animation-delay: -0.32s;
    }
    
    &:nth-child(2) {
      animation-delay: -0.16s;
    }
  }
}

@keyframes bounce {
  0%, 80%, 100% {
    transform: scale(0);
  }
  40% {
    transform: scale(1);
  }
}

// 滚动条
.messages-container {
  &::-webkit-scrollbar {
    width: 6px;
  }
  
  &::-webkit-scrollbar-track {
    background: $bg-lighter;
  }
  
  &::-webkit-scrollbar-thumb {
    background: darken($bg-lighter, 15%);
   border-radius: 3px;
    
    &:hover {
      background: darken($bg-lighter, 20%);
    }
  }
}
</style>