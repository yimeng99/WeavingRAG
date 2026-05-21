const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/components/LoginPage.vue'),
    meta: { title: '登录', requiresAuth: false }
  },
  {
    path: '/',
    redirect: '/dashboard'
  },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: () => import('@/pages/DashboardPage.vue'),
    meta: { title: '仪表板' }
  },
  {
    path: '/users',
    name: 'UserManagement',
    component: () => import('@/pages/user/UserManagement.vue'),
    meta: { title: '用户管理' }
  },
  {
    path: '/chat',
    name: 'Chat',
    component: () => import('@/components/ChatApp.vue'),
    meta: { title: 'AI 对话' }
  },
  {
    path: '/knowledge',
    name: 'Knowledge',
    component: () => import('@/components/knowledge/KnowledgeBase.vue'),
    meta: { title: '知识库管理' },
    children: [
      {
        path: '',
        name: 'KnowledgeList',
        component: () => import('@/pages/knowledge/KnowledgeCardList.vue'),
        meta: { title: '知识库列表' }
      },
      {
        path: ':id',
        name: 'KnowledgeDetail',
        component: () => import('@/pages/knowledge/KnowledgeDetailPage.vue'),
        meta: { title: '知识库详情' }
      },
      {
        path: 'new',
        name: 'KnowledgeNew',
        component: () => import('@/pages/knowledge/AddKnowledge.vue'),
        meta: { title: '新建知识库' }
      },
      {
        path: 'base/new',
        redirect: '/knowledge/new'
      },
      {
        path: 'base/edit/:id',
        name: 'KnowledgeBaseEdit',
        component: () => import('@/components/knowledge/KnowledgeBaseForm.vue'),
        meta: { title: '编辑知识库' }
      },
      {
        path: 'doc/new',
        name: 'KnowledgeDocNew',
        component: () => import('@/components/knowledge/KnowledgeDocForm.vue'),
        meta: { title: '添加文档' }
      },
      {
        path: 'doc/edit/:id',
        name: 'KnowledgeDocEdit',
        component: () => import('@/components/knowledge/KnowledgeDocForm.vue'),
        meta: { title: '编辑文档' }
      },
      {
        path: 'upload',
        name: 'KnowledgeUpload',
        component: () => import('@/pages/knowledge/KnowledgeUploadPage.vue'),
        meta: { title: '上传文件' }
      },
      {
        path: ':baseId/upload',
        name: 'KnowledgeUploadWithBase',
        component: () => import('@/pages/knowledge/KnowledgeUploadPage.vue'),
        meta: { title: '上传文件' }
      },
      {
        path: 'chunks/:id',
        name: 'DocumentChunks',
        component: () => import('@/components/knowledge/DocumentChunks.vue'),
        meta: { title: '文档分片' }
      },
      {
        path: 'search',
        name: 'KnowledgeSearch',
        component: () => import('@/pages/knowledge/SearchPage.vue'),
        meta: { title: '知识检索' }
      },
      {
        path: 'cards',
        name: 'KnowledgeCardListAlias',
        component: () => import('@/pages/knowledge/KnowledgeCardList.vue'),
        meta: { title: '知识库卡片' }
      }
    ]
  },
  {
    path: '/rag-chat',
    name: 'RAGChat',
    component: () => import('@/pages/knowledge/RAGChatPage.vue'),
    meta: { title: '知识库问答' }
  },
  {
    path: '/ai-chat',
    name: 'AIChat',
    component: () => import('@/pages/chat/AIChatPage.vue'),
    meta: { title: 'AI 智能问答' }
  },
  {
    path: '/ai-model',
    name: 'AIModel',
    component: () => import('@/pages/ai/AIModelPage.vue'),
    meta: { title: 'AI 模型配置' },
    children: [
      {
        path: '',
        name: 'AIModelList',
        component: () => import('@/pages/ai/AIModelPage.vue'),
        meta: { title: 'AI 模型配置' }
      },
      {
        path: 'provider/:id',
        name: 'AIModelProviderDetail',
        component: () => import('@/pages/ai/AIModelProviderDetail.vue'),
        meta: { title: '服务商详情' }
      },
      {
        path: 'model/:id',
        name: 'AIModelDetail',
        component: () => import('@/pages/ai/AIModelDetail.vue'),
        meta: { title: '模型详情' }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/pages/DashboardPage.vue'),
    meta: { title: '页面不存在' }
  }
]

export default routes