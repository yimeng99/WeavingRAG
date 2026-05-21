<script setup>
import { ref } from 'vue'
import AppSidebar from './AppSidebar.vue'
import AppNavbar from './AppNavbar.vue'

const sidebarCollapsed = ref(false)
</script>

<template>
  <div class="app-layout">
    <!-- 侧边栏 -->
    <AppSidebar 
      v-model:collapsed="sidebarCollapsed"
    />
    
    <!-- 右侧主内容区 -->
    <div 
      class="main-content"
      :class="{ 'sidebar-collapsed': sidebarCollapsed }"
    >
      <!-- 顶部导航栏 -->
      <AppNavbar />
      
      <!-- 主内容区域 -->
      <div class="content-wrapper">
        <div class="content-container">
          <router-view></router-view>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.app-layout {
  display: flex;
  height: 100vh;
  overflow: hidden;
  background: #f9fafb;
}

.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  width: 100%;
  margin-left: 256px;
  transition: margin-left 0.3s ease;
}

.main-content.sidebar-collapsed {
  margin-left: 64px;
}

.content-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  width: 100%;
}

.content-container {
  width: 100%;
  padding: 24px;
}

@media (min-width: 768px) {
  .content-container {
    padding: 28px;
  }
}

@media (min-width: 1280px) {
  .content-container {
    padding: 32px;
  }
}

/* 精致卡片悬浮效果 */
:global(.card-refined) {
  transition: all 0.2s cubic-bezier(0.2, 0, 0, 1);
}

:global(.card-refined:hover) {
  transform: translateY(-1px);
  box-shadow: 0 8px 16px -8px rgba(139, 92, 246, 0.12);
}

/* 表格行 hover */
:global(.data-table tbody tr) {
  transition: background 0.15s ease;
}

:global(.data-table tbody tr:hover) {
  background: rgba(139, 92, 246, 0.05);
}

/* 分页器 */
:global(.pagination-btn) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 32px;
  height: 32px;
  padding: 0 8px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
  transition: all 0.15s ease;
  background-color: white;
  border: 1px solid #e5e7eb;
  color: #4b5563;
  cursor: pointer;
}

:global(.pagination-btn:hover:not(.active):not(:disabled)) {
  background-color: #faf5ff;
  border-color: #c084fc;
  color: #7c3aed;
}

:global(.pagination-btn.active) {
  background-color: #7c3aed;
  border-color: #7c3aed;
  color: white;
}

:global(.pagination-btn:disabled) {
  opacity: 0.4;
  cursor: not-allowed;
}

/* 滚动条 */
.content-wrapper::-webkit-scrollbar {
  width: 6px;
}

.content-wrapper::-webkit-scrollbar-track {
  background: #f3f4f6;
  border-radius: 3px;
}

.content-wrapper::-webkit-scrollbar-thumb {
  background: #c084fc;
  border-radius: 3px;
}

.content-wrapper::-webkit-scrollbar-thumb:hover {
  background: #a855f7;
}
</style>
