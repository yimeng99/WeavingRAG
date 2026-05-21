import { ref, computed, readonly, shallowRef } from 'vue'
import { knowledgeApi } from '@/api/knowledge'

export function useChat() {
  const messages = ref([])
  const loading = shallowRef(false)
  const selectedKb = shallowRef('all')
  const currentEventSource = shallowRef(null)

  const knowledgeBases = [
    { id: 'all', name: '全部知识库' },
    { id: 'tech', name: '技术文档库' },
    { id: 'product', name: '产品手册库' },
    { id: 'operation', name: '运营资料库' }
  ]

  const selectedKbName = computed(() => 
    knowledgeBases.find(kb => kb.id === selectedKb.value)?.name || '全部知识库'
  )

  const simulateAIResponse = (question) => {
    return new Promise((resolve) => {
      setTimeout(() => {
        let answer = ''
        if (question.includes('微服务') || question.includes('架构')) {
          answer = '微服务架构的核心设计原则包括：单一职责原则、高内聚低耦合、边界上下文、数据独立性、去中心化治理。每个服务应该有独立的数据库，服务间通过轻量级通信协议交互。建议采用领域驱动设计（DDD）来划分服务边界。'
        } else if (question.includes('Kubernetes') || question.includes('k8s')) {
          answer = 'Kubernetes 部署应用通常需要：1. 编写 Dockerfile 构建镜像 2. 创建 Deployment 定义文件 3. 创建 Service 暴露服务 4. 使用 kubectl apply 部署。建议使用 Helm 管理复杂应用，使用 Namespace 隔离不同环境。'
        } else if (question.includes('React')) {
          answer = 'React 19 引入了多项新特性：Server Components（服务端组件）、Actions（表单操作）、useOptimistic 钩子、新的资源加载 API 等。这些特性显著提升了性能和开发体验，特别是服务端组件可以大幅减少客户端 JavaScript 体积。'
        } else {
          answer = '感谢您的提问！根据企业知识库的文档，我为您找到了相关信息。建议您查看技术文档库中的详细资料，或者尝试更具体的问题描述。我会持续学习并提供更精准的答案。'
        }
        resolve(answer)
      }, 1000)
    })
  }

  const closeEventSource = () => {
    if (currentEventSource.value) {
      currentEventSource.value.close()
      currentEventSource.value = null
    }
  }

  const sendMessageStream = async (question, useMock = false) => {
    if (!question.trim() || loading.value) return false
    
    closeEventSource()
    
    const userMsgId = Date.now()
    messages.value.push({
      id: userMsgId,
      content: question.trim(),
      isUser: true,
      time: new Date()
    })
    
    loading.value = true
    
    const aiMsgId = userMsgId + 1
    const aiMessage = {
      id: aiMsgId,
      content: '',
      isUser: false,
      time: new Date(),
      showSuggestions: messages.value.length <= 2,
      isStreaming: true
    }
    messages.value.push(aiMessage)
    
    if (useMock) {
      try {
        const answer = await simulateAIResponse(question)
        const msgIndex = messages.value.findIndex(m => m.id === aiMsgId)
        if (msgIndex !== -1) {
          messages.value[msgIndex].content = answer
          messages.value[msgIndex].isStreaming = false
        }
        return true
      } catch (e) {
        const msgIndex = messages.value.findIndex(m => m.id === aiMsgId)
        if (msgIndex !== -1) {
          messages.value[msgIndex].content = '抱歉，我遇到了一些问题，请稍后再试。'
          messages.value[msgIndex].isStreaming = false
        }
        return false
      } finally {
        loading.value = false
      }
    }

    const kbId = selectedKb.value === 'all' ? null : selectedKb.value
    const url = knowledgeApi.ragChatStreamUrl(question.trim(), kbId)
    
    const eventSource = new EventSource(url)
    currentEventSource.value = eventSource
    
    eventSource.onmessage = (event) => {
      const msgIndex = messages.value.findIndex(m => m.id === aiMsgId)
      if (msgIndex === -1) return
      
      try {
        const data = JSON.parse(event.data)
        if (data.content) {
          messages.value[msgIndex].content += data.content
        }
        if (data.done) {
          messages.value[msgIndex].isStreaming = false
          loading.value = false
          closeEventSource()
        }
        if (data.references) {
          messages.value[msgIndex].references = data.references
        }
      } catch (e) {
        messages.value[msgIndex].content += event.data
      }
    }
    
    eventSource.addEventListener('token', (event) => {
      const msgIndex = messages.value.findIndex(m => m.id === aiMsgId)
      if (msgIndex === -1) return
      messages.value[msgIndex].content += event.data
    })
    
    eventSource.addEventListener('complete', (event) => {
      const msgIndex = messages.value.findIndex(m => m.id === aiMsgId)
      if (msgIndex !== -1) {
        messages.value[msgIndex].isStreaming = false
      }
      loading.value = false
      closeEventSource()
    })
    
    eventSource.addEventListener('start', () => {
      const msgIndex = messages.value.findIndex(m => m.id === aiMsgId)
      if (msgIndex !== -1) {
        messages.value[msgIndex].content = ''
      }
    })
    
    eventSource.onerror = (error) => {
      const msgIndex = messages.value.findIndex(m => m.id === aiMsgId)
      if (msgIndex !== -1) {
        if (!messages.value[msgIndex].content) {
          messages.value[msgIndex].content = '抱歉，连接出现问题，请稍后再试。'
        }
        messages.value[msgIndex].isStreaming = false
      }
      loading.value = false
      closeEventSource()
    }
    
    eventSource.addEventListener('complete', () => {
      const msgIndex = messages.value.findIndex(m => m.id === aiMsgId)
      if (msgIndex !== -1) {
        messages.value[msgIndex].isStreaming = false
      }
      loading.value = false
      closeEventSource()
    })

    return true
  }

  const sendMessage = async (question) => {
    return sendMessageStream(question, true)
  }

  const stopStreaming = () => {
    closeEventSource()
    loading.value = false
  }

  const selectKnowledgeBase = (kbId) => {
    selectedKb.value = kbId
  }

  const initWelcomeMessage = () => {
    if (messages.value.length === 0) {
      messages.value.push({
        id: 1,
        content: '你好！我是知库 AI 助手，可以帮你解答关于企业知识库的任何问题。试试问我：',
        isUser: false,
        time: new Date(),
        showSuggestions: true
      })
    }
  }

  const formatTime = (date) => {
    return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }

  return {
    messages: readonly(messages),
    loading: readonly(loading),
    selectedKb: readonly(selectedKb),
    selectedKbName,
    knowledgeBases,
    sendMessage,
    sendMessageStream,
    stopStreaming,
    selectKnowledgeBase,
    initWelcomeMessage,
    formatTime
  }
}