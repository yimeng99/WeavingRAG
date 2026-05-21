<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import IconSvg from '../components/ui/IconSvg.vue'

const route = useRoute()
const router = useRouter()

const props = defineProps({
  collapsed: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:collapsed'])

const currentUser = ref({
  name: '张明远',
  email: 'ming@zhiku.com',
  avatar: '张'
})

const menuItems = ref([
  { 
    path: '/dashboard', 
    name: '仪表板', 
    icon: 'home',
    badge: '新'
  },
  { 
    path: '/knowledge', 
    name: '知识库管理', 
    icon: 'book-open'
  },
  {
    path: '/ai-chat',
    name: 'AI 智能问答',
    icon: 'message-circle'
  },
  {
    path: '/ai-model',
    name: 'AI 模型配置',
    icon: 'cpu'
  },
  { 
    path: '/tags', 
    name: '分类与标签', 
    icon: 'search'
  },
  { 
    path: '/users', 
    name: '用户管理', 
    icon: 'user'
  },
  { 
    path: '/analytics', 
    name: '洞察分析', 
    icon: 'settings'
  },
  { 
    path: '/settings', 
    name: '系统配置', 
    icon: 'settings'
  }
])

const isActive = (path) => {
  return route.path === path || route.path.startsWith(path + '/')
}

const handleNavigate = (path) => {
  router.push(path)
}

const handleLogout = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  router.push('/login')
}
</script>

<template>
  <aside 
    class="sidebar"
    :class="{ collapsed }"
  >
    <!-- Logo 区 -->
    <div class="logo-section">
      <div class="logo-icon">
        <IconSvg name="book-open" :size="16" color="#ffffff" />
      </div>
      <div v-show="!collapsed" class="logo-text">
        <h1>LLM Weaving</h1>
        <p>企业知识中枢</p>
      </div>
    </div>

    <!-- 导航菜单 -->
    <div class="menu-section">
      <nav class="menu-items">
        <a 
          v-for="item in menuItems" 
          :key="item.path"
          href="#"
          @click.prevent="handleNavigate(item.path)"
          class="menu-item"
          :class="{ active: isActive(item.path) }"
        >
          <IconSvg 
            :name="item.icon" 
            :size="16" 
            class="menu-icon"
            :color="isActive(item.path) ? '#7c3aed' : '#9ca3af'"
          />
          <span v-show="!collapsed" class="menu-text">{{ item.name }}</span>
          <span 
            v-if="item.badge && !collapsed" 
            class="menu-badge"
          >
            {{ item.badge }}
          </span>
        </a>
      </nav>
    </div>

    <!-- 底部用户卡片 -->
    <div class="user-section">
      <div class="user-card">
        <div class="user-avatar">
          {{ currentUser.avatar }}
        </div>
        <div v-show="!collapsed" class="user-info">
          <p class="user-name">{{ currentUser.name }}</p>
          <p class="user-email">{{ currentUser.email }}</p>
        </div>
        <button 
          v-show="!collapsed"
          @click="handleLogout"
          class="logout-btn"
          title="退出登录"
        >
          <IconSvg name="log-out" :size="16" />
        </button>
      </div>
      <button 
        v-show="collapsed"
        @click="handleLogout"
        class="logout-btn-collapsed"
        title="退出登录"
      >
        <IconSvg name="log-out" :size="16" color="#9ca3af" />
      </button>
    </div>

    <!-- 折叠按钮 -->
    <button 
      @click="emit('update:collapsed', !collapsed)"
      class="collapse-btn"
    >
      <IconSvg 
        :name="collapsed ? 'chevron-right' : 'arrow-left'" 
        :size="12" 
        color="#6b7280"
      />
    </button>
  </aside>
</template>

<style scoped>
.sidebar {
  position: fixed;
  left: 0;
  top: 0;
  height: 100%;
  min-height: 100vh;
  background: #ffffff;
  border-right: 1px solid #f3f4f6;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  z-index: 20;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  transition: width 0.3s ease;
  width: 256px;
}

.sidebar.collapsed {
  width: 64px;
}

.logo-section {
  padding: 20px 20px;
  border-bottom: 1px solid #f3f4f6;
  display: flex;
  align-items: center;
  gap: 10px;
  overflow: hidden;
  flex-shrink: 0;
}

.logo-icon {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: linear-gradient(135deg, #a855f7 0%, #7c3aed 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 4px rgba(168, 85, 247, 0.2);
  flex-shrink: 0;
}

.logo-text h1 {
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 0.5px;
  background: linear-gradient(90deg, #7c3aed, #6b21a8);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin: 0;
  white-space: nowrap;
}

.logo-text p {
  font-size: 10px;
  color: #9ca3af;
  margin: 0;
}

.menu-section {
  flex: 1;
  overflow-y: auto;
  padding: 20px 12px;
}

.menu-section::-webkit-scrollbar {
  width: 4px;
}

.menu-section::-webkit-scrollbar-track {
  background: transparent;
}

.menu-section::-webkit-scrollbar-thumb {
  background: #e5e7eb;
  border-radius: 2px;
}

.menu-items {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.menu-item {
  display: flex;
  align-items: center;
  padding: 12px 12px;
  font-size: 14px;
  font-weight: 500;
  border-radius: 8px;
  text-decoration: none;
  color: #6b7280;
  transition: all 0.2s ease;
  cursor: pointer;
  gap: 10px;
  min-height: 44px;
}

.menu-item:hover {
  background: rgba(168, 85, 247, 0.05);
  color: #7c3aed;
}

.menu-item.active {
  background: linear-gradient(90deg, rgba(168, 85, 247, 0.08), rgba(168, 85, 247, 0.02));
  border-right: 2px solid #7c3aed;
  color: #7c3aed;
}

.menu-icon {
  flex-shrink: 0;
}

.menu-text {
  white-space: nowrap;
  flex: 1;
}

.menu-badge {
  font-size: 10px;
  background: #f3e8ff;
  color: #7c3aed;
  padding: 2px 6px;
  border-radius: 9999px;
  font-weight: 600;
}

.user-section {
  border-top: 1px solid #f3f4f6;
  padding: 16px;
  flex-shrink: 0;
}

.user-card {
  background: rgba(249, 250, 251, 0.8);
  border-radius: 8px;
  padding: 12px;
  display: flex;
  align-items: center;
  gap: 10px;
  transition: background 0.2s ease;
  cursor: pointer;
}

.user-card:hover {
  background: #f3f4f6;
}

.user-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: linear-gradient(135deg, #a855f7, #7c3aed);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  flex-shrink: 0;
}

.user-info {
  flex: 1;
  min-width: 0;
}

.user-name {
  font-size: 12px;
  font-weight: 600;
  color: #374151;
  margin: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user-email {
  font-size: 10px;
  color: #9ca3af;
  margin: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.logout-btn {
  background: none;
  border: none;
  padding: 6px;
  color: #9ca3af;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
}

.logout-btn:hover {
  color: #dc2626;
  background: rgba(220, 38, 38, 0.1);
}

.logout-btn-collapsed {
  width: 100%;
  background: rgba(249, 250, 251, 0.8);
  border: none;
  padding: 10px;
  color: #9ca3af;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  margin-top: 8px;
}

.logout-btn-collapsed:hover {
  background: rgba(220, 38, 38, 0.1);
}

.logout-btn-collapsed:hover :deep(svg) {
  stroke: #dc2626 !important;
}

.collapse-btn {
  position: absolute;
  right: -12px;
  top: 80px;
  width: 24px;
  height: 24px;
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 50%;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s ease;
}

.collapse-btn:hover {
  border-color: #c084fc;
  transform: scale(1.05);
}
</style>
