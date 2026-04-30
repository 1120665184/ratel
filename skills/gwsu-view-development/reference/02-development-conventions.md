# 二、开发规范

## 2.1 通用规范

1. **禁止使用已过时的方法**
2. **所有代码必须是企业级别的**，遵循通用规范、逻辑严谨
3. **全程使用中文**注释和 UI 文案
4. **生成界面样式时参考相关技能**：使用 `/frontend-design` 等技能
5. **严格遵循组件化思想**：必要功能抽离成独立组件

## 2.2 样式规范（重要）

### 核心规则

- **样式必须抽离成单独的 `*.module.less` 文件**，禁止 CSS-in-JS、禁止内联样式（除动态计算的 style 外）
- 每个组件/页面对应一个同名的 `.module.less` 文件

### 文件命名与组织

```
components/XxxPanel/
├── index.tsx               # 组件逻辑
└── index.module.less       # 组件样式（与组件同名）
```

### 导入与使用

```tsx
import styles from './index.module.less';

// 使用
<div className={styles.container}>
  <span className={styles.title}>标题</span>
</div>

// 多类名组合
<div className={`${styles.item} ${styles.active}`}>
  内容
</div>

// 条件类名
<div className={`${styles.row} ${isSelected ? styles.selected : ''}`}>
  内容
</div>
```

### Less 编写规范

```less
// 使用 CSS 变量引用主题色（由 ThemeLayout 自动注入）
.container {
  min-height: 100vh;
  background: var(--background-color);
  color: var(--text-color);
}

.primaryButton {
  background: var(--primary-color);
  color: #fff;
  border: none;

  &:hover {
    background: var(--primary-color-light);
  }
}

// 嵌套层级不超过 4 层
.page {
  &Header {
    display: flex;
    align-items: center;

    &Title {
      font-size: 20px;
      color: var(--text-color);
    }
  }
}
```

### 可用 CSS 变量（主题色）

以下 CSS 变量由 `ThemeLayout` 通过 `applyTheme()` 自动注入到 `:root`：

| 变量名 | 说明 | 用途 |
|--------|------|------|
| `--primary-color` | 主色 | 按钮、链接、重点标识 |
| `--primary-color-light` | 主色浅色 | hover 状态 |
| `--primary-color-dark` | 主色深色 | active 状态 |
| `--background-color` | 背景色 | 页面/区域背景 |
| `--surface-color` | 表面色 | 卡片、面板背景 |
| `--text-color` | 主文字色 | 正文、标题 |
| `--text-secondary-color` | 辅助文字色 | 描述、说明 |
| `--border-color` | 边框色 | 分割线、边框 |
| `--success-color` | 成功色 | 成功状态 |
| `--warning-color` | 警告色 | 警告状态 |
| `--error-color` | 错误色 | 错误状态 |
| `--info-color` | 信息色 | 信息提示 |
| `--header-bg` | 头部背景 | 顶部导航栏 |
| `--sider-bg` | 侧边栏背景 | 左侧菜单 |
| `--menu-text` | 菜单文字色 | 菜单项文字 |
| `--menu-text-active` | 菜单激活文字色 | 选中菜单项 |
| `--menu-bg-active` | 菜单激活背景色 | 选中菜单项背景 |

## 2.3 TypeScript 规范

### 类型定义

- 接口名使用 PascalCase，以 `I` 前缀可选（项目不强制）
- 类型别名使用 PascalCase
- 枚举使用 PascalCase

```typescript
// 接口定义
export interface DeptTreeNode {
  id: number;
  name: string;
  children?: DeptTreeNode[];
}

// 枚举定义
export enum TerminalType {
  PC = 'PC',
  WEB = 'WEB',
  MOBILE = 'MOBILE',
}

// 常量映射
export const USER_STATUS_MAP: Record<number, { text: string; color: string }> = {
  0: { text: '禁用', color: '#ff4d4f' },
  1: { text: '启用', color: '#52c41a' },
};
```

### 组件类型

- 函数组件使用 `React.FC` 或直接函数声明
- Props 使用独立的 interface 定义

```tsx
interface StatCardProps {
  title: string;
  value: string;
  prefix?: React.ReactNode;
  trend?: 'up' | 'down';
}

const StatCard: React.FC<StatCardProps> = ({ title, value, prefix, trend }) => (
  <Card variant="borderless">
    <Statistic title={title} value={value} prefix={prefix} />
  </Card>
);
```

## 2.4 命名规范总览

| 层级 | 命名规则 | 示例 |
|------|---------|------|
| 页面目录 | 小写连字符（kebab-case） | `dept/`, `org-chart/` |
| 组件目录 | PascalCase | `DeptTreePanel/`, `UserFormModal/` |
| 页面文件 | 小写（index.tsx） | `index.tsx` |
| 样式文件 | 页面名.module.less | `index.module.less`, `dashboard.module.less` |
| 服务文件 | 小写（业务名.ts） | `dept.ts`, `user.ts` |
| 类型文件 | index.ts（放在 types 目录） | `types/index.ts` |
| Hooks 文件 | use + 名称.tsx | `useDeptTree.tsx` |
| CSS 类名 | camelCase | `.deptPage`, `.treePanel` |
| CSS 变量 | --kebab-case | `--primary-color` |

## 2.5 UmiJS 约定

### 路由配置

- 路由在 `config/routes.ts` 中集中配置
- 页面组件放在 `src/pages/` 目录
- 布局组件放在 `src/layouts/` 目录

### 导入 Umi 模块

```tsx
// 路由导航
import { history, useLocation, MicroApp } from 'umi';

// 路由跳转
history.push('/sub-system/dashboard');
history.replace('/sub-system/login');
```

### 环境变量

在 `.env` 文件中定义，通过 `process.env` 访问：

```typescript
const homePath = process.env.UMI_APP_HOME_PATH as string;
const loginPath = process.env.UMI_APP_LOGIN_PATH as string;
```

## 2.6 Ant Design 使用规范

- 使用 Ant Design 6.x 组件
- 使用 ProComponents 2.x 中后台组件
- 图标使用 `@ant-design/icons`
- 主题通过 `ConfigProvider` + CSS 变量统一管理，**不直接修改 Ant Design 组件的 token**
- 使用 `App.useApp()` 获取上下文化的 `message`、`notification`、`modal`

```tsx
import { App } from 'antd';

const MyComponent = () => {
  const { message, notification, modal } = App.useApp();

  const handleClick = () => {
    message.success('操作成功');
  };

  return <Button onClick={handleClick}>点击</Button>;
};
```

## 2.7 组件化原则

### 何时抽离组件

- **可复用**：在多处使用的 UI 片段
- **独立关注点**：功能逻辑独立、状态独立的部分
- **复杂度拆分**：单文件超过 200 行时考虑拆分
- **表单弹窗/抽屉**：单独抽离为独立组件

### 组件目录结构

```
components/XxxPanel/
├── index.tsx               # 组件实现
└── index.module.less       # 组件样式
```

### 简单组件可以内联

当组件仅在一个页面内使用且逻辑简单（不超过 30 行），可以在页面文件内联定义：

```tsx
// 页面文件内联的简单子组件
interface StatCardProps {
  title: string;
  value: string;
}

const StatCard: React.FC<StatCardProps> = ({ title, value }) => (
  <Card variant="borderless">
    <Statistic title={title} value={value} />
  </Card>
);

// 页面主组件
export default function Dashboard() {
  return (
    <div>
      <StatCard title="总用户数" value="12,847" />
    </div>
  );
}
```

## 2.8 其他规范

### 导入顺序

```tsx
// 1. React
import React, { useState, useCallback, useEffect } from 'react';

// 2. 第三方库
import { message, Modal } from 'antd';
import { ApartmentOutlined } from '@ant-design/icons';

// 3. @gwsu/core 共享库
import { get, post, EventType, emitEvent, useUserStore } from '@gwsu/core';

// 4. Umi 框架
import { history } from 'umi';

// 5. 应用内模块
import DeptTreePanel from './components/DeptTreePanel';
import { getDeptTree } from '@/services/dept';
import type { DeptTreeNode } from './types';

// 6. 样式
import styles from './index.module.less';
```

### 错误处理

- API 错误由 `@gwsu/core` 的 request 模块统一处理（自动 toast 提示）
- 业务逻辑错误使用 `message.error()` 或 `notification.error()` 提示
- 不要使用 `try-catch` 吞掉错误而不处理

```tsx
// 正确：需要额外处理时
try {
  await someAction();
  message.success('操作成功');
} catch (error) {
  // request 层已自动 toast，此处无需再提示
  // 如需额外处理（如回滚状态），在此处处理
}

// 正确：简单操作，不需要 try-catch
const handleDelete = async (id: string) => {
  await deleteDept(id);
  message.success('删除成功');
  await loadTreeData();
};
```
