<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import IconSvg from '../components/ui/IconSvg.vue'

const route = useRoute()
const router = useRouter()

const breadcrumbs = computed(() => {
  const crumbs = []
  
  if (route.path === '/dashboard') {
    crumbs.push({ name: '工作台', path: '/' })
    crumbs.push({ name: '仪表板', path: '/dashboard' })
  } else if (route.path.startsWith('/knowledge')) {
    crumbs.push({ name: '工作台', path: '/' })
    crumbs.push({ name: '知识库管理', path: '/knowledge' })
    if (route.path.includes('/rag-chat')) {
      crumbs.push({ name: 'RAG 问答', path: '/rag-chat' })
    }
  } else if (route.path === '/rag-chat') {
    crumbs.push({ name: '工作台', path: '/' })
    crumbs.push({ name: 'RAG 问答', path: '/rag-chat' })
  } else {
    crumbs.push({ name: '工作台', path: '/' })
    crumbs.push({ name: route.meta.title || '当前页面', path: route.path })
  }
  
  return crumbs
})

const handleNewKnowledge = () => {
  router.push('/knowledge/upload')
}
</script>

<template>
  <header class="navbar">
    <div class="navbar-left">
      <!-- 面包屑导航 -->
      <nav class="breadcrumbs">
        <ol class="breadcrumb-list">
          <li 
            v-for="(crumb, index) in breadcrumbs" 
            :key="crumb.path"
            class="breadcrumb-item"
          >
            <template v-if="index > 0">
              <IconSvg name="chevron-right" :size="10" color="#d1d5db" class="breadcrumb-separator" />
            </template>
            <a 
              v-if="index < breadcrumbs.length - 1"
              :href="crumb.path"
              @click.prevent="router.push(crumb.path)"
              class="breadcrumb-link"
            >
              <template v-if="index === 0">
                <IconSvg name="book-open" :size="10" color="#FF8200" class="breadcrumb-icon" />
              </template>
              {{ crumb.name }}
            </a>
            <span 
              v-else 
              class="breadcrumb-current"
            >
              {{ crumb.name }}
            </span>
          </li>
        </ol>
      </nav>
    </div>
    
    <div class="navbar-right">
      <!-- 通知按钮 -->
      <button class="icon-btn">
        <IconSvg name="message-square" :size="18" />
        <span class="notification-dot"></span>
      </button>
      
      <!-- 新建按钮 -->
      <button 
        @click="handleNewKnowledge"
        class="primary-btn"
      >
        <IconSvg name="plus" :size="12" color="#ffffff" />
        <span>新建知识</span>
      </button>
    </div>
  </header>
</template>

<style scoped>
.navbar {
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(8px);
  border-bottom: 1px solid #f3f4f6;
  position: sticky;
  top: 0;
  z-index: 20;
  padding: 10px 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-shrink: 0;
}

.navbar-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.breadcrumbs {
  font-size: 12px;
}

.breadcrumb-list {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  list-style: none;
  margin: 0;
  padding: 0;
}

.breadcrumb-item {
  display: inline-flex;
  align-items: center;
}

.breadcrumb-separator {
  margin: 0 8px;
}

.breadcrumb-link {
  color: #6b7280;
  text-decoration: none;
  transition: color 0.2s ease;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.breadcrumb-link:hover {
  color: #FF8200;
}

.breadcrumb-icon {
  display: inline-flex;
}

.breadcrumb-current {
  color: #374151;
  font-weight: 500;
}

.navbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.icon-btn {
  background: none;
  border: none;
  padding: 8px;
  color: #6b7280;
  cursor: pointer;
  position: relative;
  transition: color 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
}

.icon-btn:hover {
  color: #FF8200;
}

.notification-dot {
  position: absolute;
  top: 6px;
  right: 6px;
  width: 6px;
  height: 6px;
  background: #FF8200;
  border-radius: 50%;
  border: 2px solid #ffffff;
}

.primary-btn {
  background: #FF8200;
  color: #ffffff;
  border: none;
  padding: 6px 14px;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  gap: 6px;
  box-shadow: 0 1px 2px rgba(124, 58, 237, 0.1);
}

.primary-btn:hover {
  background: #E67600;
  transform: translateY(-1px);
  box-shadow: 0 4px 8px rgba(255, 130, 0, 0.15);
}
</style>
