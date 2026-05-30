---
name: gwsu-view-development
description: GWSU前端项目开发规范与指南，包含目录结构、命名规范、组件开发、微前端配置等，只要涉及到前端代码开发必读。
type: skill
---

# GWSU 前端项目开发技能

本技能为 GWSU 前端项目提供开发规范和指导，确保代码风格统一、架构清晰。

## 触发条件

- 涉及前端 TypeScript/TSX/Less 代码的任何改动（**必读**）
- 用户要求创建新的子应用
- 用户要求添加新的页面或组件
- 用户要求创建 API 服务或调用后端接口
- 用户要求修改主题系统
- 用户要求修改状态管理或事件系统
- 用户询问前端开发规范或最佳实践
- 用户使用 @gwsu/core 共享库

## 文档索引

本技能由以下子文档组成，改动前端代码时必须阅读相关文档：

| 文档 | 说明 | 适用场景 |
|------|------|---------|
| [01-project-structure.md](reference/01-project-structure.md) | 目录规范、工作空间、微前端架构 | 新建子应用、了解项目结构 |
| [02-development-conventions.md](reference/02-development-conventions.md) | 开发规范、命名规范、样式规范 | 编写任何前端代码 |
| [03-components-and-pages.md](reference/03-components-and-pages.md) | 组件开发、页面开发模式 | 创建页面或组件 |
| [04-services-and-api.md](reference/04-services-and-api.md) | API 服务层、请求工具、路由前缀规则 | 调用后端接口、创建服务层 |
| [05-core-library.md](reference/05-core-library.md) | @gwsu/core 共享库完整指南 | 使用共享组件、主题、工具函数 |
| [06-state-and-events.md](reference/06-state-and-events.md) | 状态管理（Zustand）、事件系统 | 使用/创建 Store、跨组件通信 |
| [07-checklist.md](reference/07-checklist.md) | 开发检查清单 | 新建子应用/页面/组件后的自查 |
| [08-list-page-pattern.md](reference/08-list-page-pattern.md) | 列表页开发模式、操作列规范、按钮权限、data-ai-approval | 开发列表页、表格操作列 |

## 快速参考

### 开发禁忌
- 禁止使用已标识过期的方法或属性
- 严格遵循TypeScript定义规范

### 样式规范

- **样式必须抽离成单独的 `*.module.less` 文件**，禁止 CSS-in-JS
- 导入方式：`import styles from './index.module.less'`
- 使用方式：`className={styles.xxx}`

### API 路由前缀规则

所有 API 请求路径必须以对应后端业务模块的 `prefix` 作为前缀：

| 后端模块 | prefix | API 路径示例 |
|---------|--------|------------|
| business-security | `security` | `/security/menu/routes/1` |
| business-system | `system` | `/system/manager/page` |

### 从 @gwsu/core 导入

```tsx
import { ThemeLayout, useThemeContext, themes, getThemeByKey } from '@gwsu/core';
import { get, post, put, del } from '@gwsu/core';
import { useUserStore, useMenuStore, useAuthStore } from '@gwsu/core';
import { useAuth, AuthGate } from '@gwsu/core';
import { EventType, emitEvent, onEvent } from '@gwsu/core';
import { fetchDictValuesBatch, fetchConfigsBatch } from '@gwsu/core';
```

### 字典与配置批量获取

**一个页面中需要多个字典或配置时，必须只调用一次接口，将所有键一次性传入**：

```typescript
import { fetchDictValuesBatch, fetchConfigsBatch } from '@gwsu/core';

// 正确：一次获取多个字典
const dictMap = await fetchDictValuesBatch(['user_status', 'gender', 'dept_type']);

// 正确：一次获取多个配置
const configMap = await fetchConfigsBatch(['site_name', 'max_upload_size']);

// 错误：多次调用（浪费请求）
const statusDict = await fetchDictValuesBatch(['user_status']);
const genderDict = await fetchDictValuesBatch(['gender']);
```

### 按钮权限控制

根据 `buttonKey` 控制按钮/内容是否渲染，数据来自后端 `/menu/routes/{owner}` 接口返回的 `menuType=3` 节点。

**权限标识必须定义为常量**，放在 `permissionConstants.ts` 中，禁止硬编码字符串：

```typescript
// permissionConstants.ts
export const PERM_ADD = '72974723_add';
export const PERM_REMOVE = '72974723_remove';
export const PERM_EDIT = '72974723_edit';
```

```tsx
import { PERM_ADD, PERM_REMOVE, PERM_EDIT } from './permissionConstants';

// AuthGate 组件（推荐）
<AuthGate buttonKey={PERM_ADD}>
  <Button type="primary">新增</Button>
</AuthGate>

// useAuth hook
const canEdit = useAuth(PERM_EDIT);
{canEdit && <Button danger>编辑</Button>}

// 无权限时显示替代内容
<AuthGate buttonKey={PERM_EDIT} fallback={<Button disabled>编辑</Button>}>
  <Button type="link">编辑</Button>
</AuthGate>
```

### 列表页操作列模式

操作列统一采用 **"详情" + "更多"下拉菜单** 模式，固定在右侧（`fixed: "right"`）：

```tsx
// 表格头部按钮：用 AuthGate 控制权限
<AuthGate buttonKey="xxx_add">
  <Button type="primary" icon={<PlusOutlined />}>新增</Button>
</AuthGate>

// 操作列下拉菜单：用 useAuth 控制菜单项
const canEdit = useAuth('xxx_edit');
const getButtonItem = (record): MenuProps['items'] => {
  const buttons = [];
  if (canEdit) buttons.push({ key: 'edit', icon: <EditOutlined />, label: '编辑', onClick: () => handleEdit(record) });
  return buttons;
};

// 操作列渲染
<div className={styles.actionColumn}>
  <Button type="link" size="small" icon={<EyeOutlined />}>详情</Button>
  <Dropdown menu={{ items: getButtonItem(record) }} disabled={getButtonItem(record).length === 0}>
    <Button type="link" size="small" icon={<MoreOutlined />}>更多</Button>
  </Dropdown>
</div>
```

### data-ai-approval 判断标准

**核心标准**：按钮点击后是否立即触发后端数据变更。需要则加，不需要则不加。

| 需要 | 不需要 |
|------|--------|
| 删除、批量删除 | 新增（仅打开空表单） |
| 状态 Switch 切换 | 详情、查看 |
| 同步到 Redis | 编辑（仅打开编辑弹窗） |
| 保存/提交 | 搜索、筛选、翻页 |

### 子应用布局模板

```tsx
import React from 'react';
import { Outlet } from 'umi';
import { ThemeLayout } from '@gwsu/core';

const Layout: React.FC = () => {
  return (
    <ThemeLayout>
      <Outlet />
    </ThemeLayout>
  );
};

export default Layout;
```
