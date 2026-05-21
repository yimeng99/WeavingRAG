<script setup>
import { computed, ref } from 'vue'
import hljs from 'highlight.js'

const props = defineProps({
  content: {
    type: String,
    default: ''
  },
  isStreaming: {
    type: Boolean,
    default: false
  },
  references: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['reference-click'])

const copiedCode = ref(null)

const renderedContent = computed(() => {
  if (!props.content) return ''
  
  let html = props.content
  
  html = html.replace(/```(\w*)\n([\s\S]*?)```/g, (match, lang, code) => {
    const language = lang || 'plaintext'
    let highlightedCode = code.trim()
    
    try {
      if (hljs.getLanguage(language)) {
        highlightedCode = hljs.highlight(code.trim(), { language }).value
      }
    } catch (e) {
      highlightedCode = escapeHtml(code.trim())
    }
    
    const codeId = `code-${Math.random().toString(36).substr(2, 9)}`
    return `<div class="code-wrapper" data-code-id="${codeId}" data-code="${encodeURIComponent(code.trim())}">
      <div class="code-header">
        <span class="code-lang">${language}</span>
        <button class="copy-btn" data-code-id="${codeId}">复制</button>
      </div>
      <pre class="code-block"><code class="hljs language-${language}">${highlightedCode}</code></pre>
    </div>`
  })
  
  html = html.replace(/`([^`]+)`/g, '<code class="inline-code">$1</code>')
  
  html = html.replace(/^### (.+)$/gm, '<h3 class="md-h3">$1</h3>')
  html = html.replace(/^## (.+)$/gm, '<h2 class="md-h2">$1</h2>')
  html = html.replace(/^# (.+)$/gm, '<h1 class="md-h1">$1</h1>')
  
  html = html.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
  html = html.replace(/\*([^*]+)\*/g, '<em>$1</em>')
  
  html = html.replace(/\[([^\]]+)\]\(([^)]+)\)/g, '<a href="$2" target="_blank" rel="noopener">$1</a>')
  
  html = html.replace(/^[-*] (.+)$/gm, '<li class="md-li">$1</li>')
  html = html.replace(/(<li class="md-li">.*<\/li>\n?)+/g, '<ul class="md-ul">$&</ul>')
  
  html = html.replace(/^\d+\. (.+)$/gm, '<li class="md-li">$1</li>')
  html = html.replace(/(<li class="md-li">.*<\/li>\n?)+/g, (match) => {
    if (match.includes('<ul')) return match
    return `<ol class="md-ol">${match}</ol>`
  })
  
  html = html.replace(/\n\n/g, '</p><p class="md-p">')
  html = html.replace(/\n/g, '<br>')
  
  if (!html.startsWith('<')) {
    html = `<p class="md-p">${html}</p>`
  }
  
  return html
})

function escapeHtml(text) {
  const div = document.createElement('div')
  div.textContent = text
  return div.innerHTML
}

function handleCopy(event) {
  const target = event.target
  if (target.classList.contains('copy-btn')) {
    const wrapper = target.closest('.code-wrapper')
    const code = decodeURIComponent(wrapper.dataset.code)
    
    navigator.clipboard.writeText(code).then(() => {
      const originalText = target.textContent
      target.textContent = '已复制'
      target.classList.add('copied')
      
      setTimeout(() => {
        target.textContent = originalText
        target.classList.remove('copied')
      }, 2000)
    }).catch(() => {
      target.textContent = '复制失败'
      setTimeout(() => {
        target.textContent = '复制'
      }, 2000)
    })
  }
}

function handleReferenceClick(ref) {
  emit('reference-click', ref)
}
</script>

<template>
  <div class="ai-content-renderer" @click="handleCopy">
    <div class="content-body" v-html="renderedContent"></div>
    
    <span v-if="isStreaming" class="streaming-cursor">▊</span>
    
    <div v-if="references && references.length" class="references-section">
      <div class="references-header">
        <svg class="ref-icon" viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5L14.5 2z"/>
          <polyline points="14 2 14 8 20 8"/>
        </svg>
        <span>参考文档</span>
      </div>
      <div class="references-list">
        <div 
          v-for="(ref, index) in references" 
          :key="ref.id || index"
          class="reference-item"
          @click="handleReferenceClick(ref)"
        >
          <span class="ref-index">[{{ index + 1 }}]</span>
          <span class="ref-title">{{ ref.title || ref.name || '未知文档' }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.ai-content-renderer {
  line-height: 1.7;
  font-size: 14px;
  color: #374151;
}

.content-body {
  :deep(.md-p) {
    margin: 0 0 12px;
    
    &:last-child {
      margin-bottom: 0;
    }
  }
  
  :deep(.md-h1) {
    font-size: 20px;
    font-weight: 700;
    margin: 16px 0 8px;
    color: #1f2937;
  }
  
  :deep(.md-h2) {
    font-size: 18px;
    font-weight: 600;
    margin: 14px 0 6px;
    color: #1f2937;
  }
  
  :deep(.md-h3) {
    font-size: 16px;
    font-weight: 600;
    margin: 12px 0 4px;
    color: #374151;
  }
  
  :deep(.code-wrapper) {
    margin: 12px 0;
    border-radius: 8px;
    overflow: hidden;
    background: #1e1e1e;
  }
  
  :deep(.code-header) {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 8px 12px;
    background: #2d2d2d;
    border-bottom: 1px solid #404040;
  }
  
  :deep(.code-lang) {
    font-size: 12px;
    color: #9ca3af;
    text-transform: lowercase;
  }
  
  :deep(.copy-btn) {
    font-size: 12px;
    color: #9ca3af;
    background: transparent;
    border: 1px solid #4b5563;
    border-radius: 4px;
    padding: 2px 8px;
    cursor: pointer;
    transition: all 0.2s;
    
    &:hover {
      color: #fff;
      border-color: #6b7280;
    }
    
    &.copied {
      color: #10b981;
      border-color: #10b981;
    }
  }
  
  :deep(.code-block) {
    margin: 0;
    padding: 12px 16px;
    overflow-x: auto;
    
    code {
      font-family: 'JetBrains Mono', 'Fira Code', 'Consolas', monospace;
      font-size: 13px;
      line-height: 1.5;
      background: transparent;
    }
  }
  
  :deep(.inline-code) {
    background: #f3f4f6;
    padding: 2px 6px;
    border-radius: 4px;
    font-family: 'Consolas', 'Monaco', monospace;
    font-size: 13px;
    color: #7c3aed;
  }
  
  :deep(a) {
    color: #7c3aed;
    text-decoration: none;
    
    &:hover {
      text-decoration: underline;
    }
  }
  
  :deep(.md-ul), :deep(.md-ol) {
    margin: 8px 0;
    padding-left: 20px;
  }
  
  :deep(.md-li) {
    margin: 4px 0;
  }
  
  :deep(strong) {
    font-weight: 600;
    color: #1f2937;
  }
  
  :deep(em) {
    font-style: italic;
  }
}

.streaming-cursor {
  display: inline-block;
  color: #7c3aed;
  animation: blink 1s infinite;
  margin-left: 2px;
  font-weight: bold;
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}

.references-section {
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px solid #e5e7eb;
}

.references-header {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 600;
  color: #7c3aed;
  margin-bottom: 8px;
  
  .ref-icon {
    opacity: 0.8;
  }
}

.references-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.reference-item {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  font-size: 12px;
  color: #6b7280;
  padding: 6px 8px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
  
  &:hover {
    background: #f9fafb;
    color: #7c3aed;
    
    .ref-title {
      color: #7c3aed;
    }
  }
  
  .ref-index {
    color: #9ca3af;
    flex-shrink: 0;
  }
  
  .ref-title {
    color: #4b5563;
    transition: color 0.2s;
  }
}
</style>