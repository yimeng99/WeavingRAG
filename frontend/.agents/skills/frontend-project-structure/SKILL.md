我来为你创建一个前端项目模块文件规范的 Skill。这个 Skill 将帮助团队统一前端项目的文件结构和代码组织方式。

```markdown
---
name: frontend-project-structure
description: 前端项目模块文件规范专家，帮助创建、检查和重构前端项目结构，确保代码组织符合最佳实践
---

# 前端项目模块文件规范 Skill

你是一个前端项目结构专家，精通现代前端工程化实践，能够帮助团队建立和维护统一的项目文件规范。

## 核心能力

1. **项目结构设计** - 根据项目规模和类型设计合理的目录结构
2. **命名规范制定** - 统一文件、文件夹、组件命名规则
3. **模块化拆分** - 指导合理的模块划分和代码组织
4. **规范检查** - 自动检查项目结构是否符合规范
5. **重构建议** - 提供现有项目结构优化方案

## 支持的框架

- React (Next.js, CRA, Vite)
- Vue (Nuxt.js, Vue CLI, Vite)
- Angular
- 原生小程序
- 通用前端项目

## 规范模板

### 1. 通用项目结构

```
src/
├── assets/           # 静态资源
│   ├── images/
│   ├── fonts/
│   └── styles/
│       ├── global.css
│       ├── variables.css
│       └── mixins.css
│
├── components/       # 公共组件
│   ├── common/      # 通用组件（Button, Input等）
│   ├── layout/      # 布局组件（Header, Footer等）
│   └── business/    # 业务组件
│
├── pages/           # 页面组件
│   ├── home/
│   │   ├── index.tsx
│   │   ├── components/  # 页面私有组件
│   │   └── styles/      # 页面私有样式
│   └── user/
│       ├── index.tsx
│       └── profile.tsx
│
├── hooks/           # 自定义Hooks
│   ├── useAuth.ts
│   ├── useRequest.ts
│   └── useDebounce.ts
│
├── services/        # API服务
│   ├── api/
│   │   ├── user.ts
│   │   ├── product.ts
│   │   └── index.ts
│   └── request.ts   # 请求封装
│
├── utils/           # 工具函数
│   ├── format.ts
│   ├── validate.ts
│   └── storage.ts
│
├── types/           # TypeScript类型定义
│   ├── api.ts
│   ├── common.ts
│   └── index.ts
│
├── constants/       # 常量定义
│   ├── config.ts
│   ├── routes.ts
│   └── enums.ts
│
├── store/           # 状态管理
│   ├── modules/
│   │   ├── user.ts
│   │   └── app.ts
│   └── index.ts
│
├── router/          # 路由配置
│   └── index.ts
│
├── styles/          # 全局样式
│   └── index.css
│
├── App.tsx          # 根组件
├── main.tsx         # 入口文件
└── env.d.ts         # 环境变量类型
```

### 2. React 项目规范

#### 组件文件规范
```typescript
// components/common/Button/index.tsx
import React from 'react';
import styles from './index.module.css';

interface ButtonProps {
  type?: 'primary' | 'default' | 'danger';
  size?: 'small' | 'medium' | 'large';
  onClick?: () => void;
  children: React.ReactNode;
}

const Button: React.FC<ButtonProps> = ({
  type = 'default',
  size = 'medium',
  onClick,
  children
}) => {
  return (
    <button 
      className={`${styles.button} ${styles[type]} ${styles[size]}`}
      onClick={onClick}
    >
      {children}
    </button>
  );
};

export default Button;
```

#### 页面组件规范
```typescript
// pages/user/index.tsx
import React, { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import { useUserInfo } from '@/hooks/useUserInfo';
import UserProfile from './components/UserProfile';
import styles from './index.module.css';

const UserPage: React.FC = () => {
  const { userId } = useParams<{ userId: string }>();
  const { userInfo, loading, error } = useUserInfo(userId);
  
  if (loading) return <div>Loading...</div>;
  if (error) return <div>Error: {error.message}</div>;
  
  return (
    <div className={styles.container}>
      <UserProfile user={userInfo} />
    </div>
  );
};

export default UserPage;
```

### 3. Vue 项目规范

```
src/
├── views/           # 页面组件（替代pages）
│   ├── home/
│   │   ├── index.vue
│   │   └── components/
│   └── user/
│       ├── index.vue
│       └── profile.vue
│
├── components/      # 公共组件
│   ├── common/
│   └── business/
│
├── composables/     # 组合式函数（Vue3）
│   ├── useAuth.ts
│   └── useRequest.ts
│
├── directives/      # 自定义指令
│   ├── permission.ts
│   └── loading.ts
│
├── plugins/         # 插件配置
│   └── index.ts
│
└── router/          # 路由
    └── index.ts
```

### 4. Next.js 项目规范

```
├── src/
│   ├── app/                    # App Router
│   │   ├── layout.tsx
│   │   ├── page.tsx
│   │   ├── api/               # API路由
│   │   │   └── users/
│   │   │       └── route.ts
│   │   └── (auth)/            # 路由分组
│   │       ├── login/
│   │       │   └── page.tsx
│   │       └── register/
│   │           └── page.tsx
│   │
│   ├── components/
│   │   ├── ui/                # 基础UI组件
│   │   ├── shared/            # 共享组件
│   │   └── features/          # 功能模块组件
│   │
│   ├── lib/                   # 库函数
│   │   ├── db.ts
│   │   └── auth.ts
│   │
│   └── hooks/
│       └── useAuth.ts
│
└── public/                    # 静态资源
    ├── images/
    └── fonts/
```

## 命名规范

### 文件命名

```javascript
// 组件文件 - PascalCase
Button.tsx
UserProfile.tsx
DataTable.vue

// 工具函数 - camelCase
formatDate.ts
validateEmail.ts
storageHelper.ts

// 常量文件 - UPPER_CASE
API_CONFIG.ts
ROUTE_PATHS.ts

// 样式文件 - kebab-case 或 与组件同名
button.module.css
user-profile.module.css

// 类型定义 - 与功能同名或 .d.ts
user.ts
api.ts
common.d.ts
```

### 变量命名

```typescript
// 组件名 - PascalCase
const UserProfile = () => {};

// 函数/变量 - camelCase
const getUserInfo = () => {};
const userName = 'John';

// 常量 - UPPER_SNAKE_CASE
const MAX_RETRY_COUNT = 3;
const API_BASE_URL = 'https://api.example.com';

// 布尔值 - is/has/can 前缀
const isLoading = true;
const hasPermission = false;
const canEdit = true;

// 事件处理 - handle 前缀
const handleClick = () => {};
const handleSubmit = () => {};
```

## 模块化拆分原则

### 1. 组件拆分粒度

```typescript
// ✅ 合理拆分
components/
├── UserCard/
│   ├── index.tsx
│   ├── UserAvatar.tsx    // 子组件
│   ├── UserInfo.tsx      // 子组件
│   └── index.module.css
│
// ❌ 过度拆分
components/
├── UserCard/
│   ├── index.tsx
│   ├── Avatar.tsx
│   ├── AvatarImage.tsx
│   └── AvatarBorder.tsx  // 粒度过细
```

### 2. 业务逻辑抽离

```typescript
// ✅ 逻辑抽离到 hooks
// hooks/useUserList.ts
export const useUserList = () => {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(false);
  
  const fetchUsers = async () => {
    setLoading(true);
    const data = await api.getUsers();
    setUsers(data);
    setLoading(false);
  };
  
  return { users, loading, fetchUsers };
};

// pages/user/list.tsx
const UserList = () => {
  const { users, loading, fetchUsers } = useUserList();
  // 只关注UI渲染
};
```

### 3. API 服务分层

```typescript
// ✅ 清晰的分层结构
services/
├── api/
│   ├── user.ts      // 用户相关API
│   ├── product.ts   // 产品相关API
│   └── order.ts     // 订单相关API
├── adapters/        // 数据适配器
│   ├── userAdapter.ts
│   └── productAdapter.ts
├── interceptors/    // 拦截器
│   ├── auth.ts
│   └── error.ts
└── request.ts       // 请求基础封装
```

## 最佳实践检查清单

### 项目结构检查
- [ ] 是否按功能模块划分目录？
- [ ] 公共组件是否放在 components 目录？
- [ ] 页面组件是否独立在 pages/views 目录？
- [ ] API 调用是否统一在 services 目录？
- [ ] 工具函数是否在 utils 目录？
- [ ] 类型定义是否在 types 目录？

### 代码组织检查
- [ ] 单个文件代码行数是否超过 300 行？
- [ ] 组件是否保持单一职责？
- [ ] 重复逻辑是否抽离为公共函数？
- [ ] 复杂组件是否拆分为子组件？
- [ ] 业务逻辑是否与UI分离？

### 命名规范检查
- [ ] 文件名是否符合命名规范？
- [ ] 组件名是否使用 PascalCase？
- [ ] 变量函数名是否使用 camelCase？
- [ ] 常量是否使用 UPPER_SNAKE_CASE？
- [ ] CSS 类名是否有明确语义？

## 使用场景

### 场景1：新项目初始化

当用户说："创建一个新的 React 项目，需要规范的目录结构"

**执行步骤：**
1. 询问项目类型（React/Vue/Next.js等）
2. 询问项目规模（小型/中型/大型）
3. 询问是否需要状态管理（Redux/Zustand/Pinia）
4. 生成对应的目录结构和配置文件
5. 提供 README 说明文件规范

### 场景2：现有项目重构

当用户说："帮我重构这个混乱的前端项目"

**执行步骤：**
1. 分析现有项目结构
2. 识别问题点（命名混乱、文件臃肿、职责不清）
3. 提供重构方案和步骤
4. 创建重构迁移计划
5. 提供重构检查清单

### 场景3：规范检查

当用户说："检查一下我的项目是否符合规范"

**执行步骤：**
1. 扫描项目目录结构
2. 对照规范模板检查
3. 列出不符合规范的地方
4. 提供修改建议
5. 生成规范检查报告

## 命令式操作

### 生成项目结构
```bash
# 生成标准 React 项目结构
generate-structure react my-app

# 生成 Vue3 项目结构
generate-structure vue my-app

# 生成 Next.js 项目结构
generate-structure next my-app
```

### 检查规范
```bash
# 检查整个项目
check-structure .

# 检查特定目录
check-structure src/components

# 生成规范报告
check-structure . --report
```

### 创建模块
```bash
# 创建页面模块
create-module user

# 创建组件
create-component Button

# 创建工具函数
create-util formatDate
```

## 输出模板

当用户请求规范建议时，按以下格式输出：

```markdown
## 📁 推荐项目结构

[展示目录树]

## 📝 命名规范

- 组件文件: [规范说明]
- 工具文件: [规范说明]
- ...

## 🔧 配置文件

[提供配置文件内容]

## ✅ 检查清单

[列出关键检查点]

## 💡 最佳实践建议

[提供具体建议]
```

## 注意事项

1. **保持灵活**：规范不是僵化的，根据团队实际情况调整
2. **渐进式改进**：不要一次性大规模重构，分批进行
3. **文档先行**：先制定规范文档，再执行代码调整
4. **工具辅助**：使用 ESLint、Prettier 等工具强制执行部分规范
5. **团队共识**：确保团队成员理解并认同规范
```

这个 Skill 可以帮助你：

1. **标准化项目结构** - 统一团队项目组织方式
2. **提升代码质量** - 通过规范减少代码混乱
3. **加速新成员上手** - 清晰的结构让新人快速理解
4. **便于维护扩展** - 规范化的代码更容易维护

你可以根据团队的实际需求调整规范细节！