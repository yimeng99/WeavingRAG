<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import IconSvg from '../components/ui/IconSvg.vue'

const router = useRouter()

// 统计数据
const stats = ref({
  totalKnowledge: 1428,
  totalGrowth: '+15.2%',
  activeUsers: 423,
  usersGrowth: '+9%',
  todayVisits: 1204,
  visitsGrowth: '+21%',
  pendingReview: 9
})

// 热门知识
const popularKnowledge = ref([
  {
    title: '微服务架构设计最佳实践（2025 企业版）',
    views: '2.8k',
    likes: 156,
    tag: '热门',
    tagType: 'hot'
  },
  {
    title: 'React 19 + 服务端组件架构实践',
    views: '2.1k',
    likes: 102,
    tag: '前沿',
    tagType: 'trending'
  },
  {
    title: '数据湖 vs 数据仓库：选型与融合指南',
    views: '1.5k',
    likes: 88,
    tag: '精选',
    tagType: 'featured'
  },
  {
    title: 'AI Agent 在企业知识库中的落地场景',
    views: '1.2k',
    likes: 74,
    tag: 'AI 专题',
    tagType: 'ai'
  }
])

// 实时动态
const activities = ref([
  { content: '技术部李明更新《K8s 最佳实践》', time: '1 小时前' },
  { content: '王芳创建「AI 工具集」智能分类', time: '3 小时前' },
  { content: '《设计系统 2.0》通过 AI 审核', time: '昨天' },
  { content: '知识库总访问量突破 12w+', time: '昨天' }
])

// 知识资产表格数据
const knowledgeAssets = ref([
  {
    title: '前端架构演进与微前端落地',
    category: '前端技术',
    author: '陈思琪',
    date: '2025-03-22',
    status: 'published'
  },
  {
    title: '数据中台建设方法论与实践精要',
    category: '数据治理',
    author: '刘子轩',
    date: '2025-03-20',
    status: 'reviewing'
  },
  {
    title: 'Kubernetes 故障排查手册（实战篇）',
    category: '云原生',
    author: '赵一鸣',
    date: '2025-03-18',
    status: 'published'
  },
  {
    title: '智能客服知识库构建标准流程',
    category: 'AI/客服',
    author: '运营部',
    date: '2025-03-15',
    status: 'published'
  }
])

const searchQuery = ref('')
const currentPage = ref(1)

const getStatusClass = (status) => {
  const classes = {
    published: 'status-published',
    reviewing: 'status-reviewing',
    draft: 'status-draft'
  }
  return classes[status] || classes.draft
}

const getStatusText = (status) => {
  const texts = {
    published: '已发布',
    reviewing: '审核中',
    draft: '草稿'
  }
  return texts[status] || '草稿'
}
</script>

<template>
  <div class="dashboard-page">
    <!-- 统计卡片 -->
    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-content">
          <p class="stat-label">知识总量</p>
          <p class="stat-value">{{ stats.totalKnowledge.toLocaleString() }}</p>
          <span class="stat-growth growth-positive">
            <IconSvg name="arrow-left" :size="8" color="#10b981" class="growth-icon" />
            {{ stats.totalGrowth }}
          </span>
        </div>
        <div class="stat-icon-wrapper">
          <IconSvg name="book-open" :size="20" color="#7c3aed" />
        </div>
      </div>
      
      <div class="stat-card">
        <div class="stat-content">
          <p class="stat-label">活跃用户</p>
          <p class="stat-value">{{ stats.activeUsers }}</p>
          <span class="stat-growth growth-positive">
            <IconSvg name="arrow-left" :size="8" color="#10b981" class="growth-icon" />
            {{ stats.usersGrowth }}
          </span>
        </div>
        <div class="stat-icon-wrapper">
          <IconSvg name="user" :size="20" color="#7c3aed" />
        </div>
      </div>
      
      <div class="stat-card">
        <div class="stat-content">
          <p class="stat-label">今日访问</p>
          <p class="stat-value">{{ stats.todayVisits.toLocaleString() }}</p>
          <span class="stat-growth growth-positive">
            <IconSvg name="arrow-left" :size="8" color="#7c3aed" class="growth-icon" />
            {{ stats.visitsGrowth }}
          </span>
        </div>
        <div class="stat-icon-wrapper">
          <IconSvg name="search" :size="20" color="#7c3aed" />
        </div>
      </div>
      
      <div class="stat-card">
        <div class="stat-content">
          <p class="stat-label">待审核</p>
          <p class="stat-value">{{ stats.pendingReview }}</p>
          <a href="#" class="stat-link">
            立即处理
            <IconSvg name="chevron-right" :size="8" color="#7c3aed" />
          </a>
        </div>
        <div class="stat-icon-wrapper">
          <IconSvg name="settings" :size="20" color="#7c3aed" />
        </div>
      </div>
    </div>

    <!-- 双栏布局 -->
    <div class="double-column">
      <!-- 左侧热门知识 -->
      <div class="popular-section card">
        <div class="card-header">
          <h2 class="card-title">
            <IconSvg name="book-open" :size="16" color="#7c3aed" class="title-icon" />
            热门知识 · 本周飙升
          </h2>
          <a href="#" class="card-link">
            全部榜单
            <IconSvg name="chevron-right" :size="10" color="#7c3aed" />
          </a>
        </div>
        <div class="knowledge-list">
          <div 
            v-for="(item, index) in popularKnowledge" 
            :key="index"
            class="knowledge-item"
          >
            <a href="#" class="knowledge-title">{{ item.title }}</a>
            <div class="knowledge-meta">
              <span class="meta-item">
                <IconSvg name="search" :size="10" color="#9ca3af" />
                {{ item.views }}阅读
              </span>
              <span class="meta-item">
                <IconSvg name="check" :size="10" color="#9ca3af" />
                {{ item.likes }}点赞
              </span>
              <span class="knowledge-tag">{{ item.tag }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧动态 + 待办 -->
      <div class="right-section">
        <!-- 实时动态 -->
        <div class="activity-section card">
          <div class="card-header-small">
            <h3 class="card-title-small">
              <IconSvg name="message-square" :size="14" color="#7c3aed" />
              实时动态
            </h3>
            <a href="#" class="card-link-small">全部</a>
          </div>
          <ul class="activity-list">
            <li 
              v-for="(activity, index) in activities" 
              :key="index"
              class="activity-item"
            >
              <div class="activity-dot"></div>
              <div class="activity-content">
                <p class="activity-text">{{ activity.content }}</p>
                <span class="activity-time">{{ activity.time }}</span>
              </div>
            </li>
          </ul>
        </div>
        
        <!-- 待处理事项 -->
        <div class="todo-section card">
          <h3 class="card-title-small">
            <IconSvg name="check" :size="14" color="#7c3aed" />
            待处理事项
          </h3>
          <p class="todo-count">6</p>
          <p class="todo-desc">待审核知识 · 3 条归档建议</p>
          <button class="todo-btn">
            <IconSvg name="sparkles" :size="10" color="#ffffff" />
            <span>立即处理</span>
          </button>
        </div>
      </div>
    </div>

    <!-- 知识资产表格 -->
    <div class="table-section card">
      <div class="table-header">
        <div class="table-title-section">
          <h2 class="card-title">
            <IconSvg name="book-open" :size="16" color="#7c3aed" />
            知识资产库
          </h2>
          <p class="table-desc">企业核心知识文档，智能检索与权限管理</p>
        </div>
        <div class="table-actions">
          <div class="search-box">
            <IconSvg name="search" :size="12" color="#9ca3af" class="search-icon" />
            <input 
              v-model="searchQuery"
              type="text" 
              placeholder="搜索标题、作者或标签" 
              class="search-input"
            >
          </div>
          <button class="filter-btn">
            <IconSvg name="settings" :size="10" color="#6b7280" />
            筛选
          </button>
        </div>
      </div>
      
      <div class="table-wrapper">
        <table class="data-table">
          <thead>
            <tr>
              <th class="th">标题</th>
              <th class="th">分类</th>
              <th class="th">作者</th>
              <th class="th">更新日期</th>
              <th class="th">状态</th>
              <th class="th text-right">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr 
              v-for="(item, index) in knowledgeAssets" 
              :key="index"
              class="table-row"
            >
              <td class="td">
                <a href="#" class="link-title">{{ item.title }}</a>
              </td>
              <td class="td">
                <span class="category-tag">{{ item.category }}</span>
              </td>
              <td class="td">{{ item.author }}</td>
              <td class="td">{{ item.date }}</td>
              <td class="td">
                <span :class="getStatusClass(item.status)">
                  {{ getStatusText(item.status) }}
                </span>
              </td>
              <td class="td text-right">
                <a href="#" class="action-link">
                  <IconSvg name="edit" :size="12" color="#7c3aed" />
                  编辑
                </a>
                <a href="#" class="action-icon">
                  <IconSvg name="trash" :size="12" color="#9ca3af" />
                </a>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      
      <!-- 分页器 -->
      <div class="pagination">
        <div class="pagination-info">显示 1-4 条，共 56 条记录</div>
        <div class="pagination-btns">
          <button class="pagination-btn" disabled>
            <IconSvg name="arrow-left" :size="10" color="#4b5563" />
          </button>
          <button 
            v-for="page in [1, 2, 3, 4, 5]" 
            :key="page"
            class="pagination-btn"
            :class="{ active: currentPage === page }"
          >
            {{ page }}
          </button>
          <button class="pagination-btn">...</button>
          <button class="pagination-btn">12</button>
          <button class="pagination-btn">
            <IconSvg name="chevron-right" :size="10" color="#4b5563" />
          </button>
        </div>
      </div>
    </div>

    <!-- 底部智能助手卡片 -->
    <div class="ai-assistant card">
      <div class="ai-content">
        <div class="ai-icon-wrapper">
          <IconSvg name="bot" :size="18" color="#7c3aed" />
        </div>
        <div class="ai-text">
          <p class="ai-title">AI 知识图谱助手</p>
          <p class="ai-desc">自动提取关联，智能推荐分类与标签</p>
        </div>
      </div>
      <button class="ai-btn">
        <IconSvg name="sparkles" :size="10" color="#ffffff" />
        <span>智能整理</span>
      </button>
    </div>
  </div>
</template>

<style scoped>
.dashboard-page {
  width: 100%;
}

/* 统计卡片网格 */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(1, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

@media (min-width: 640px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (min-width: 1024px) {
  .stats-grid {
    grid-template-columns: repeat(4, 1fr);
  }
}

.stat-card {
  background: #ffffff;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  border: 1px solid #f3f4f6;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  transition: all 0.2s cubic-bezier(0.2, 0, 0, 1);
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 16px -8px rgba(139, 92, 246, 0.12);
}

.stat-content {
  flex: 1;
}

.stat-label {
  font-size: 12px;
  font-weight: 500;
  color: #6b7280;
  margin: 0 0 4px 0;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: #1f2937;
  margin: 0 0 4px 0;
}

.stat-growth {
  display: inline-flex;
  align-items: center;
  font-size: 10px;
  font-weight: 600;
  padding: 2px 6px;
  border-radius: 9999px;
}

.growth-positive {
  color: #047857;
  background: #ecfdf5;
}

.growth-icon {
  margin-right: 2px;
}

.stat-icon-wrapper {
  width: 40px;
  height: 40px;
  background: #f3e8ff;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 10px;
  font-weight: 600;
  color: #7c3aed;
  text-decoration: none;
  margin-top: 4px;
}

/* 双栏布局 */
.double-column {
  display: grid;
  grid-template-columns: 1fr;
  gap: 20px;
  margin-bottom: 24px;
}

@media (min-width: 1024px) {
  .double-column {
    grid-template-columns: 2fr 1fr;
  }
}

/* 卡片通用样式 */
.card {
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  border: 1px solid #f3f4f6;
  overflow: hidden;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 20px;
  border-bottom: 1px solid #f3f4f6;
}

.card-header-small {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.card-title {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
}

.card-title-small {
  font-size: 12px;
  font-weight: 600;
  color: #1f2937;
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 0 0 8px 0;
}

.card-link {
  font-size: 12px;
  font-weight: 500;
  color: #7c3aed;
  text-decoration: none;
  display: flex;
  align-items: center;
  gap: 4px;
}

.card-link-small {
  font-size: 10px;
  font-weight: 500;
  color: #7c3aed;
  text-decoration: none;
}

/* 热门知识列表 */
.knowledge-list {
  padding: 0;
}

.knowledge-item {
  padding: 12px 20px;
  border-bottom: 1px solid #f9fafb;
  transition: background 0.15s ease;
  cursor: pointer;
}

.knowledge-item:last-child {
  border-bottom: none;
}

.knowledge-item:hover {
  background: rgba(139, 92, 246, 0.05);
}

.knowledge-title {
  font-size: 13px;
  font-weight: 600;
  color: #1f2937;
  text-decoration: none;
  display: block;
  margin-bottom: 6px;
  transition: color 0.15s ease;
}

.knowledge-title:hover {
  color: #7c3aed;
}

.knowledge-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 10px;
  color: #9ca3af;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 2px;
}

.knowledge-tag {
  color: #7c3aed;
  background: #f3e8ff;
  padding: 2px 8px;
  border-radius: 9999px;
  font-weight: 500;
}

/* 右侧区域 */
.right-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.activity-section {
  padding: 16px;
}

.activity-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.activity-item {
  display: flex;
  gap: 10px;
  padding: 8px 0;
}

.activity-dot {
  width: 6px;
  height: 6px;
  background: #c084fc;
  border-radius: 50%;
  margin-top: 4px;
  flex-shrink: 0;
}

.activity-content {
  flex: 1;
  min-width: 0;
}

.activity-text {
  font-size: 12px;
  color: #374151;
  margin: 0 0 4px 0;
  line-height: 1.4;
}

.activity-time {
  font-size: 10px;
  color: #9ca3af;
}

/* 待处理事项 */
.todo-section {
  padding: 16px;
  background: linear-gradient(135deg, rgba(249, 250, 251, 0.8), rgba(243, 244, 246, 0.5));
  border: 1px solid #e9d5ff;
}

.todo-count {
  font-size: 24px;
  font-weight: 700;
  color: #1f2937;
  margin: 8px 0 4px 0;
}

.todo-desc {
  font-size: 10px;
  color: #6b7280;
  margin: 0 0 12px 0;
}

.todo-btn {
  width: 100%;
  background: #7c3aed;
  color: #ffffff;
  border: none;
  padding: 8px 16px;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  box-shadow: 0 1px 2px rgba(124, 58, 237, 0.1);
}

.todo-btn:hover {
  background: #6b21a8;
  transform: translateY(-1px);
  box-shadow: 0 4px 8px rgba(124, 58, 237, 0.15);
}

/* 表格区域 */
.table-section {
  margin-bottom: 24px;
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 16px 20px;
  border-bottom: 1px solid #f3f4f6;
  flex-wrap: wrap;
  gap: 12px;
}

.table-title-section {
  flex: 1;
  min-width: 200px;
}

.table-desc {
  font-size: 10px;
  color: #9ca3af;
  margin: 4px 0 0 0;
}

.table-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

.search-box {
  position: relative;
}

.search-icon {
  position: absolute;
  left: 10px;
  top: 50%;
  transform: translateY(-50%);
}

.search-input {
  width: 224px;
  padding: 6px 10px 6px 32px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 12px;
  outline: none;
  transition: all 0.2s ease;
}

.search-input:focus {
  border-color: #c084fc;
  box-shadow: 0 0 0 3px rgba(168, 85, 247, 0.1);
}

.filter-btn {
  background: #ffffff;
  border: 1px solid #e5e7eb;
  padding: 6px 12px;
  border-radius: 8px;
  font-size: 12px;
  color: #6b7280;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  gap: 4px;
}

.filter-btn:hover {
  border-color: #c084fc;
  color: #7c3aed;
}

.table-wrapper {
  overflow-x: auto;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
}

.data-table thead {
  background: #f9fafb;
}

.th {
  padding: 10px 20px;
  text-align: left;
  font-size: 10px;
  font-weight: 600;
  color: #6b7280;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.th.text-right {
  text-align: right;
}

.table-row {
  transition: background 0.15s ease;
}

.table-row:hover {
  background: rgba(139, 92, 246, 0.05);
}

.td {
  padding: 10px 20px;
  font-size: 12px;
  color: #374151;
  border-bottom: 1px solid #f9fafb;
}

.link-title {
  font-size: 12px;
  font-weight: 600;
  color: #1f2937;
  text-decoration: none;
  transition: color 0.15s ease;
}

.link-title:hover {
  color: #7c3aed;
}

.category-tag {
  font-size: 10px;
  background: #f3e8ff;
  color: #7c3aed;
  padding: 2px 8px;
  border-radius: 9999px;
  font-weight: 500;
}

.status-published {
  font-size: 10px;
  background: #f0fdf4;
  color: #15803d;
  padding: 2px 8px;
  border-radius: 9999px;
  font-weight: 500;
}

.status-reviewing {
  font-size: 10px;
  background: #fef3c7;
  color: #b45309;
  padding: 2px 8px;
  border-radius: 9999px;
  font-weight: 500;
}

.status-draft {
  font-size: 10px;
  background: #f3f4f6;
  color: #6b7280;
  padding: 2px 8px;
  border-radius: 9999px;
  font-weight: 500;
}

.action-link {
  font-size: 12px;
  color: #7c3aed;
  text-decoration: none;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-right: 12px;
  transition: color 0.15s ease;
}

.action-link:hover {
  color: #6b21a8;
}

.action-icon {
  font-size: 12px;
  color: #9ca3af;
  text-decoration: none;
  transition: color 0.15s ease;
}

.action-icon:hover {
  color: #7c3aed;
}

/* 分页器 */
.pagination {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 20px;
  border-top: 1px solid #f3f4f6;
}

.pagination-info {
  font-size: 10px;
  color: #6b7280;
}

.pagination-btns {
  display: flex;
  gap: 6px;
  align-items: center;
}

/* AI 助手卡片 */
.ai-assistant {
  padding: 12px 16px;
  background: linear-gradient(90deg, rgba(249, 250, 251, 0.6), rgba(255, 255, 255, 0.8), rgba(249, 250, 251, 0.6));
  border: 1px solid #e9d5ff;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.ai-content {
  display: flex;
  align-items: center;
  gap: 12px;
}

.ai-icon-wrapper {
  width: 32px;
  height: 32px;
  background: #e9d5ff;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.ai-text {
  flex: 1;
}

.ai-title {
  font-size: 12px;
  font-weight: 600;
  color: #1f2937;
  margin: 0;
}

.ai-desc {
  font-size: 10px;
  color: #6b7280;
  margin: 2px 0 0 0;
}

.ai-btn {
  background: #7c3aed;
  color: #ffffff;
  border: none;
  padding: 6px 14px;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  gap: 6px;
  box-shadow: 0 1px 2px rgba(124, 58, 237, 0.1);
}

.ai-btn:hover {
  background: #6b21a8;
  transform: translateY(-1px);
  box-shadow: 0 4px 8px rgba(124, 58, 237, 0.15);
}
</style>
