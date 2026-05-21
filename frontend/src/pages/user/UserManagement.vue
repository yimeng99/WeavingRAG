<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { message, confirm } from '../../utils/message'
import { userApi } from '../../api/user'
import IconSvg from '../../components/ui/IconSvg.vue'

// 表格数据
const dataSource = ref([])
const loading = ref(false)
const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
  pageSizeOptions: ['10', '20', '50', '100']
})

// 搜索和筛选
const searchQuery = ref('')
const selectedStatus = ref('')

// 选中的行
const selectedRowKeys = ref([])

const columns = [
  { title: '用户名', dataIndex: 'username', key: 'name', width: 150 },
  { title: '手机号', dataIndex: 'phone', key: 'phone', width: 150 },
  { title: '邮箱', dataIndex: 'email', key: 'email', width: 200 },
  { title: '账号状态', dataIndex: 'status', key: 'status', width: 120 },
  { title: '操作', key: 'action', width: 120, fixed: 'right' }
]

// 统计数据
const stats = ref({
  totalUsers: 0,
  monthGrowth: '+0',
  activeUsers: 0,
  onlineUsers: 0
})

// 新增用户模态框
const showAddUserModal = ref(false)
const addUserForm = reactive({
  username: '',
  phone: '',
  email: '',
  password: ''
})

// 编辑用户模态框
const showEditModal = ref(false)
const editForm = reactive({
  id: null,
  username: '',
  phone: '',
  email: '',
  status: 'offline'
})

// 行选择配置
const rowSelection = computed(() => ({
  selectedRowKeys: selectedRowKeys.value,
  onChange: (keys) => {
    selectedRowKeys.value = keys
  }
}))

// 获取状态文本
const getStatusText = (status) => {
  const statusMap = {
    online: '在线',
    offline: '离线',
    pending: '待激活'
  }
  return statusMap[status] || '未知'
}

// 加载用户列表
const loadUsers = async () => {
  loading.value = true
  try {
    const params = {
      current: pagination.current,
      size: pagination.pageSize
    }
    
    if (searchQuery.value) {
      params.keyword = searchQuery.value
    }
    if (selectedStatus.value) {
      params.status = selectedStatus.value
    }
    
    const res = await userApi.getUsers(params)
    dataSource.value = res.records || res.list || []
    pagination.total = res.total || 0
    stats.value.totalUsers = res.total || 0
  } catch (error) {
    console.error('加载用户列表失败:', error)
    message.error('加载用户列表失败')
  } finally {
    loading.value = false
  }
}

// 分页变化
const handleTableChange = (pag) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  loadUsers()
}

// 搜索
const handleSearch = () => {
  pagination.current = 1
  loadUsers()
}

// 重置搜索
const handleReset = () => {
  searchQuery.value = ''
  selectedStatus.value = ''
  pagination.current = 1
  loadUsers()
}

// 新增用户
const handleAddUser = async () => {
  if (!addUserForm.username || !addUserForm.email) {
    message.error('请填写完整信息')
    return
  }
  
  try {
    const userData = {
      username: addUserForm.username,
      phone: addUserForm.phone,
      email: addUserForm.email
    }
    if (addUserForm.password) {
      userData.password = addUserForm.password
    }
    
    await userApi.createUser(userData)
    message.success('用户添加成功')
    showAddUserModal.value = false
    addUserForm.username = ''
    addUserForm.phone = ''
    addUserForm.email = ''
    addUserForm.password = ''
    loadUsers()
  } catch (error) {
    console.error('添加用户失败:', error)
  }
}

// 编辑用户
const handleEdit = (record) => {
  editForm.id = record.id
  editForm.username = record.username
  editForm.phone = record.phone
  editForm.email = record.email
  editForm.status = record.status
  showEditModal.value = true
}

// 保存编辑
const handleUpdateUser = async () => {
  if (!editForm.username || !editForm.email) {
    message.error('请填写完整信息')
    return
  }
  
  try {
    await userApi.updateUser({
      id: editForm.id,
      username: editForm.username,
      phone: editForm.phone,
      email: editForm.email,
      status: editForm.status
    })
    message.success('用户更新成功')
    showEditModal.value = false
    loadUsers()
  } catch (error) {
    console.error('更新用户失败:', error)
  }
}

// 删除用户
const handleDelete = (record) => {
  confirm.show({
    title: '确认删除',
    content: `确定要删除用户「${record.username}」吗？删除后该用户将无法登录系统，此操作不可恢复。`,
    okText: '确认删除',
    cancelText: '取消',
    okType: 'danger',
    onOk: async () => {
      try {
        await userApi.deleteUser(record.id)
        message.success('删除成功')
        loadUsers()
      } catch (error) {
        console.error('删除用户失败:', error)
        message.error('删除失败')
      }
    }
  })
}

// 批量删除
const handleBatchDelete = () => {
  message.warning('批量删除功能已禁用')
}

// 获取角色标签颜色
const getRoleTagColor = (role) => {
  const colors = {
    admin: 'purple',
    editor: 'blue',
    reviewer: 'orange',
    reader: 'green'
  }
  return colors[role] || 'default'
}

// 获取状态样式类
const getStatusClass = (status) => {
  const classes = {
    online: 'status-online',
    offline: 'status-offline',
    pending: 'status-pending'
  }
  return classes[status] || classes.offline
}

// 获取状态点样式类
const getStatusDotClass = (status) => {
  const classes = {
    online: 'dot-green',
    offline: 'dot-gray',
    pending: 'dot-amber'
  }
  return classes[status] || classes.offline
}

const handleStatusChange = async (record, checked) => {
  const newStatus = checked ? 'online' : 'offline'
  try {
    await userApi.updateStatus(record.id, newStatus)
    record.status = newStatus
    message.success(`账号已${checked ? '启用' : '禁用'}`)
  } catch (error) {
    console.error('更新状态失败:', error)
    message.error('更新状态失败')
  }
}

// 初始化
onMounted(() => {
  loadUsers()
})
</script>

<template>
  <div class="user-management-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">
          <IconSvg name="user" :size="20" color="#7c3aed" class="title-icon" />
          用户管理
        </h2>
        <p class="page-desc">管理企业成员信息、角色权限、部门归属及账号状态</p>
      </div>
    </div>

    <!-- 统计卡片区 -->
    <div class="stats-grid">
      <div class="stat-card-wrapper">
        <div class="stat-card">
          <div class="stat-content">
            <p class="stat-label">总用户数</p>
            <p class="stat-value">{{ stats.totalUsers }}</p>
            <span class="stat-growth growth-positive">
              <IconSvg name="arrow-left" :size="8" color="#10b981" class="growth-icon" />
              {{ stats.monthGrowth }} 本月
            </span>
          </div>
          <div class="stat-icon-wrapper stat-icon-purple">
            <IconSvg name="user" :size="20" color="#7c3aed" />
          </div>
        </div>
      </div>
      
      <div class="stat-card-wrapper">
        <div class="stat-card">
          <div class="stat-content">
            <p class="stat-label">活跃成员</p>
            <p class="stat-value">{{ stats.activeUsers }}</p>
            <span class="stat-growth growth-success">
              本周活跃
            </span>
          </div>
          <div class="stat-icon-wrapper stat-icon-green">
            <IconSvg name="check" :size="20" color="#16a34a" />
          </div>
        </div>
      </div>
      
      <div class="stat-card-wrapper">
        <div class="stat-card">
          <div class="stat-content">
            <p class="stat-label">在线成员</p>
            <p class="stat-value">{{ stats.onlineUsers }}</p>
            <span class="stat-growth growth-purple">
              当前在线
            </span>
          </div>
          <div class="stat-icon-wrapper stat-icon-blue">
            <div class="online-dot"></div>
          </div>
        </div>
      </div>
    </div>

    <!-- 表格区域 -->
    <div class="table-section card">
      <!-- 工具栏 -->
      <div class="toolbar">
        <div class="toolbar-content">
          <div class="toolbar-left">
            <div class="search-input-wrapper">
              <IconSvg name="search" :size="14" color="#9ca3af" class="search-icon" />
              <a-input
                v-model:value="searchQuery"
                placeholder="搜索姓名或邮箱..."
                class="search-input"
                @pressEnter="handleSearch"
              />
            </div>
            <a-select
              v-model:value="selectedStatus"
              placeholder="全部状态"
              class="filter-select"
              allow-clear
            >
              <a-select-option value="">全部状态</a-select-option>
              <a-select-option value="online">在线</a-select-option>
              <a-select-option value="offline">离线</a-select-option>
              <a-select-option value="pending">待激活</a-select-option>
            </a-select>
            <button class="btn-search" @click="handleSearch">
              <IconSvg name="search" :size="14" color="#ffffff" />
              查询
            </button>
            <button class="btn-reset" @click="handleReset">
              <IconSvg name="refresh-cw" :size="14" color="#6b7280" />
              重置
            </button>
          </div>
          <div class="toolbar-right">
            <button class="btn-secondary">
              <IconSvg name="download" :size="14" color="#4b5563" />
              导出
            </button>
            <button class="btn-primary" @click="showAddUserModal = true">
              <IconSvg name="plus" :size="14" color="#ffffff" />
              新增用户
            </button>
          </div>
        </div>
      </div>

      <a-table
        :columns="columns"
        :data-source="dataSource"
        :loading="loading"
        :row-key="(record) => record.id"
        :pagination="false"
        class="user-table"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'name'">
            <span class="cell-text">{{ record.username || '-' }}</span>
          </template>
          <template v-else-if="column.key === 'phone'">
            <span class="cell-text">{{ record.phone || '-' }}</span>
          </template>
          <template v-else-if="column.key === 'email'">
            <span class="cell-text">{{ record.email || '-' }}</span>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-switch
              :checked="record.status === 'online'"
              checked-children="启用"
              un-checked-children="禁用"
              @change="(checked) => handleStatusChange(record, checked)"
            />
          </template>
          <template v-else-if="column.key === 'action'">
            <div class="action-buttons">
              <button class="action-btn" @click="handleEdit(record)">
                <IconSvg name="edit" :size="14" color="#7c3aed" />
              </button>
              <button class="action-btn danger" @click="handleDelete(record)">
                <IconSvg name="trash" :size="14" color="#dc2626" />
              </button>
            </div>
          </template>
        </template>
      </a-table>

      <div class="pagination-wrapper">
        <div class="pagination-info">
          显示 {{ (pagination.current - 1) * pagination.pageSize + 1 }}-{{ Math.min(pagination.current * pagination.pageSize, pagination.total) }} 条，共 {{ pagination.total }} 条
        </div>
        <div class="pagination-controls">
          <button 
            class="pagination-btn"
            :disabled="pagination.current <= 1"
            @click="handleTableChange({ current: pagination.current - 1 })"
          >
            上一页
          </button>
          <span class="pagination-current">{{ pagination.current }} / {{ Math.ceil(pagination.total / pagination.pageSize) || 1 }}</span>
          <button 
            class="pagination-btn"
            :disabled="pagination.current >= Math.ceil(pagination.total / pagination.pageSize)"
            @click="handleTableChange({ current: pagination.current + 1 })"
          >
            下一页
          </button>
        </div>
      </div>

      <!-- 批量操作栏 -->
      <div class="batch-bar" v-if="selectedRowKeys.length > 0">
        <span class="selected-text">已选择 {{ selectedRowKeys.length }} 项</span>
        <div class="batch-actions">
          <button class="btn-secondary" @click="selectedRowKeys = []">
            <IconSvg name="x" :size="14" color="#6b7280" />
            取消选择
          </button>
          <button class="btn-danger" @click="handleBatchDelete">
            <IconSvg name="trash" :size="14" color="#dc2626" />
            批量删除
          </button>
        </div>
      </div>
    </div>

    <!-- 新增用户模态框 -->
    <a-modal
      v-model:open="showAddUserModal"
      title="新增用户"
      :width="480"
      :maskClosable="false"
      okText="确定"
      cancelText="取消"
      :okButtonProps="{ style: { background: 'linear-gradient(135deg, #9333ea 0%, #7c3aed 100%)', border: 'none' } }"
      @ok="handleAddUser"
    >
      <a-form :model="addUserForm" layout="vertical">
        <a-form-item label="用户名" required>
          <a-input
            v-model:value="addUserForm.username"
            placeholder="请输入用户名"
            maxlength="50"
          />
        </a-form-item>
        <a-form-item label="手机号">
          <a-input
            v-model:value="addUserForm.phone"
            placeholder="请输入手机号"
            maxlength="20"
          />
        </a-form-item>
        <a-form-item label="邮箱" required>
          <a-input
            v-model:value="addUserForm.email"
            type="email"
            placeholder="member@company.com"
            maxlength="100"
          />
        </a-form-item>
        <a-form-item label="初始密码">
          <a-input
            v-model:value="addUserForm.password"
            type="password"
            placeholder="请输入初始密码 (可选，默认：123456)"
            maxlength="50"
          />
        </a-form-item>
      </a-form>
      <a-alert
        type="info"
        show-icon
        class="form-tip"
        message="添加用户后，系统会自动发送激活邮件到用户邮箱"
      />
    </a-modal>

    <!-- 编辑用户模态框 -->
    <a-modal
      v-model:open="showEditModal"
      title="编辑用户"
      :width="480"
      :maskClosable="false"
      okText="确定"
      cancelText="取消"
      :okButtonProps="{ style: { background: 'linear-gradient(135deg, #9333ea 0%, #7c3aed 100%)', border: 'none' } }"
      @ok="handleUpdateUser"
    >
      <a-form :model="editForm" layout="vertical">
        <a-form-item label="用户名" required>
          <a-input
            v-model:value="editForm.username"
            placeholder="请输入用户名"
            maxlength="50"
          />
        </a-form-item>
        <a-form-item label="手机号">
          <a-input
            v-model:value="editForm.phone"
            placeholder="请输入手机号"
            maxlength="20"
          />
        </a-form-item>
        <a-form-item label="邮箱" required>
          <a-input
            v-model:value="editForm.email"
            type="email"
            placeholder="member@company.com"
            maxlength="100"
          />
        </a-form-item>
        <a-form-item label="账号状态">
          <a-select v-model:value="editForm.status" style="width: 100%">
            <a-select-option value="online">启用</a-select-option>
            <a-select-option value="offline">禁用</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<style lang="scss" scoped>


.user-management-page {
  width: 100%;
  height: 100%;
}

.page-header {
  margin-bottom: $spacing-xl;

  .header-left {
    .page-title {
      display: flex;
      align-items: center;
      gap: $spacing-sm;
      font-size: $font-size-2xl;
      font-weight: 700;
      color: $gray-800;
      margin: 0 0 $spacing-sm 0;
      
      .title-icon {
        filter: drop-shadow(0 2px 4px rgba(124, 58, 237, 0.3));
      }
    }

    .page-desc {
      font-size: $font-size-sm;
      color: $gray-500;
      margin: 0;
    }
  }
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(1, 1fr);
  gap: 20px;
  margin-bottom: 20px;

  @media (min-width: 640px) {
    grid-template-columns: repeat(2, 1fr);
  }

  @media (min-width: 1024px) {
    grid-template-columns: repeat(3, 1fr);
  }
}

.table-section {
  background: #ffffff;
  border-radius: $radius-lg;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
  overflow: hidden;
  padding: $spacing-md $spacing-lg;

  .toolbar {
    padding-top: $spacing-md;
  }

  .toolbar-content {
    padding: $spacing-md 0;
    border-bottom: 1px solid $gray-100;
  }

  .toolbar-content {
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-wrap: nowrap;
    gap: $spacing-md;
    width: 100%;
  }

  .toolbar-left {
    display: flex;
    flex-wrap: nowrap;
    gap: $spacing-md;
    align-items: center;
  }

  .search-input-wrapper {
    position: relative;
    width: 280px;

    .search-icon {
      position: absolute;
      left: 12px;
      top: 50%;
      transform: translateY(-50%);
      z-index: 1;
    }

    :deep(.ant-input) {
      padding-left: 36px;
      border: 1px solid $gray-200;
      border-radius: $radius-md;
      transition: all 0.2s ease;

      &:hover {
        border-color: $purple-300;
      }

      &:focus {
        border-color: $purple-500;
        box-shadow: 0 0 0 3px rgba(124, 58, 237, 0.1);
      }
    }
  }

  .search-input {
    width: 100%;
  }

  .filter-select {
    width: 140px;
  }

  .toolbar-right {
    display: flex;
    gap: $spacing-sm;
    flex-shrink: 0;
  }

  .user-table {
    width: 100%;

    :deep(.ant-table) {
      background: transparent;
      font-size: $font-size-base;
    }

    :deep(.ant-table-thead > tr > th) {
      background: #ffffff;
      font-weight: 600;
      color: $gray-500;
      font-size: $font-size-sm;
      padding: 10px 16px;
      border-bottom: 1px solid $gray-100;
    }

    :deep(.ant-table-tbody > tr > td) {
      padding: 10px 16px;
      color: $gray-700;
      border-bottom: 1px solid $gray-100;
    }

    :deep(.ant-table-tbody > tr:hover > td) {
      background: $gray-50;
    }

    :deep(.ant-table-footer) {
      padding: 0;
      background: #ffffff;
    }
  }
}

.user-info {
  .cell-text {
    color: $gray-700;
    font-size: $font-size-base;
  }
}

.modal {
  :deep(.ant-modal-header) {
    padding: 16px 24px;
    border-bottom: 1px solid $gray-100;
    border-radius: $radius-lg $radius-lg 0 0;
  }

  :deep(.ant-modal-title) {
    font-size: $font-size-lg;
    font-weight: 600;
    color: $gray-800;
  }

  :deep(.ant-modal-body) {
    padding: $spacing-xl;
  }

  :deep(.ant-modal-footer) {
    padding: 12px 24px;
    border-top: 1px solid $gray-100;
  }

  :deep(.ant-btn-primary) {
    background: linear-gradient(135deg, $purple-600, $purple-700);
    border: none;
    box-shadow: 0 2px 4px rgba(124, 58, 237, 0.3);

    &:hover {
      background: linear-gradient(135deg, $purple-500, $purple-600);
    }
  }
}

.form-tip {
  margin-top: $spacing-lg;
  border: 1px solid $purple-200;
  background: $purple-50;
}

.pagination-wrapper {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: $spacing-md 0;
  margin-top: $spacing-md;
}

.pagination-info {
  font-size: $font-size-sm;
  color: $gray-500;
}

.pagination-controls {
  display: flex;
  align-items: center;
  gap: $spacing-md;
}

.pagination-btn {
  padding: 6px 12px;
  border: 1px solid $gray-200;
  border-radius: $radius-md;
  background: #fff;
  font-size: $font-size-xs;
  color: $gray-600;
  cursor: pointer;
  transition: all 0.2s ease;
  
  &:hover:not(:disabled) {
    border-color: $purple-300;
    color: $purple-600;
  }
  
  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
}

.pagination-current {
  font-size: $font-size-sm;
  color: $gray-700;
  font-weight: 500;
}
</style>
