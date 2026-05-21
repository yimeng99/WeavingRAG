# 用户管理模块使用说明

## 功能特性

✅ 使用 Ant Design Vue Table 组件  
✅ 支持分页查询（前端 + 后端分页）  
✅ 支持按姓名/邮箱搜索  
✅ 支持按状态筛选  
✅ 新增用户  
✅ 编辑用户  
✅ 删除用户（单个/批量）  
✅ 统计卡片展示  

## 后端接口

### 1. 分页查询用户
```http
GET /api/v0/users/page?current=1&size=10&keyword=&status=
```

**参数说明：**
- `current`: 当前页码（默认 1）
- `size`: 每页大小（默认 10）
- `keyword`: 搜索关键词（可选，匹配姓名或邮箱）
- `status`: 状态筛选（可选，online/offline/pending）

**返回示例：**
```json
{
  "records": [
    {
      "id": 1,
      "username": "admin",
      "name": "张明远",
      "email": "ming.zhang@zhiku.com",
      "role": "admin",
      "department": "技术部",
      "status": "online",
      "lastLogin": "2025-03-26 09:23:00"
    }
  ],
  "total": 24,
  "size": 10,
  "current": 1
}
```

### 2. 创建用户
```http
POST /api/v0/users
Content-Type: application/json

{
  "name": "新用户",
  "email": "new@zhiku.com",
  "role": "reader",
  "department": "技术部"
}
```

### 3. 更新用户
```http
PUT /api/v0/users/{id}
Content-Type: application/json

{
  "name": "更新后的姓名",
  "email": "updated@zhiku.com",
  "role": "editor",
  "department": "产品部"
}
```

### 4. 删除用户
```http
DELETE /api/v0/users/{id}
```

### 5. 批量删除用户
```http
POST /api/v0/users/batch-delete
Content-Type: application/json

{
  "ids": [1, 2, 3]
}
```

## 数据库变更

执行以下 SQL 脚本添加必要字段：

```sql
-- 用户表新增字段
ALTER TABLE `user` 
ADD COLUMN `name` varchar(100) DEFAULT NULL COMMENT '姓名 (显示名称)' AFTER `username`,
ADD COLUMN `role` varchar(50) DEFAULT 'reader' COMMENT '角色 (admin:管理员，editor:编辑者，reviewer:审核者，reader:只读者)' AFTER `phone`,
ADD COLUMN `department` varchar(100) DEFAULT NULL COMMENT '部门' AFTER `role`,
ADD COLUMN `status` varchar(50) DEFAULT 'offline' COMMENT '状态 (online:在线，offline:离线，pending:待激活)' AFTER `department`,
ADD COLUMN `last_login` datetime DEFAULT NULL COMMENT '上次登录时间' AFTER `avatar`;
```

SQL 脚本位置：`database/alter_user_table.sql`

## 前端使用

### 访问路径
```
http://localhost:5173/users
```

### 主要组件
- **统计卡片区**: 展示总用户数、活跃成员、在线成员、待激活用户
- **搜索筛选栏**: 支持姓名/邮箱搜索、状态筛选
- **表格区**: 展示用户列表，支持分页、行选择
- **批量操作栏**: 选中用户后显示批量删除功能
- **新增/编辑模态框**: 用户表单

### 技术栈
- Vue 3 Composition API
- Ant Design Vue 组件库
- 自研 axios 封装（支持统一错误处理、token 自动携带）
- 全局 message/confirm 工具

## 快速开始

1. **执行数据库变更**
   ```bash
   mysql -u root -p your_database < database/alter_user_table.sql
   ```

2. **启动后端服务**
   ```bash
   cd lw-app
   mvn spring-boot:run
   ```

3. **启动前端服务**
   ```bash
   cd frontend
   npm run dev
   ```

4. **访问用户管理页面**
   - 打开浏览器访问：http://localhost:5173/users
   - 登录系统后点击左侧菜单"用户管理"

## API 封装

### 在组件中使用
```javascript
import { userApi } from '@/api/user'
import { message } from '@/utils/message'

// 获取用户列表
const loadUsers = async () => {
  const res = await userApi.getUsers({ current: 1, size: 10 })
  console.log(res.records)
}

// 创建用户
const handleAdd = async () => {
  await userApi.createUser({ name: '张三', email: 'test@example.com' })
  message.success('添加成功')
}

// 删除用户
const handleDelete = async (id) => {
  await userApi.deleteUser(id)
  message.success('删除成功')
}
```

### 使用全局工具
```javascript
// 在 Vue 组件中
this.$message.success('操作成功')
this.$confirm.show({
  title: '确认删除',
  content: '确定要删除吗？',
  onOk: async () => {
    // 确认操作
  }
})
```

## 注意事项

1. **密码加密**: 创建用户时，密码需要使用 BCrypt 加密
2. **角色权限**: 不同角色有不同的操作权限，需要在后端进行校验
3. **状态管理**: 用户状态变更（如激活、禁用）需要记录操作日志
4. **批量操作**: 批量删除时需要确认，避免误操作

## 后续优化

- [ ] 添加用户导入/导出功能
- [ ] 添加用户角色管理
- [ ] 添加用户登录日志
- [ ] 添加用户操作日志
- [ ] 支持自定义表格列显示
- [ ] 支持表格列排序
- [ ] 支持高级搜索（多条件组合）
