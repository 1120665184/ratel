# 五、@gwsu/core 共享库完整指南

`@gwsu/core` 是前端项目的共享核心库，所有子应用和主应用共同依赖。**编写前端代码时优先使用该库提供的组件、工具函数和类型**。

---

## 5.1 组件

### 5.1.1 ThemeLayout — 主题布局组件

> 路径：`gwsu-core/src/components/ThemeLayout.tsx`

提供全局主题上下文和 Ant Design ConfigProvider，所有应用必须使用此组件作为根布局。

**Props**：

| 属性 | 类型 | 说明 |
|------|------|------|
| children | `React.ReactNode` | 子组件 |

**提供的 Context**：

```tsx
interface ThemeContextValue {
  currentTheme: ThemeConfig;          // 当前主题配置
  changeTheme: (theme: ThemeConfig) => void;  // 切换主题
}
```

**使用方式**：

```tsx
import { ThemeLayout, useThemeContext } from '@gwsu/core';

// 包裹应用根
<ThemeLayout>
  <App />
</ThemeLayout>

// 在子组件中使用主题
function MyComponent() {
  const { currentTheme, changeTheme } = useThemeContext();
  return (
    <div style={{ background: currentTheme.colors.surface }}>
      <button onClick={() => changeTheme(anotherTheme)}>切换主题</button>
    </div>
  );
}
```

**内部行为**：
- 初始化时从 localStorage 读取保存的主题（key: `gwsu-theme`）
- 通过 `window.postMessage` 监听跨应用主题变更事件
- 自动应用 CSS 变量到 `:root`
- 自动配置 Ant Design `ConfigProvider` 的主题 token 和 algorithm（暗色/亮色）

---

## 5.2 常量

### 5.2.1 主题配置

> 路径：`gwsu-core/src/constants/theme.ts`

提供 6 种内置主题：

| 主题 | key | 类型 |
|------|-----|------|
| 深海蓝 | `ocean` | 亮色（默认） |
| 森林绿 | `forest` | 亮色 |
| 紫罗兰 | `violet` | 亮色 |
| 琥珀橙 | `amber` | 亮色 |
| 石墨灰 | `graphite` | 亮色 |
| 午夜暗色 | `midnight` | 暗色 |

**导出**：

```typescript
import { themes, defaultTheme, getThemeByKey, themeMap } from '@gwsu/core';

// 所有主题列表
themes: ThemeConfig[];

// 默认主题（ocean）
defaultTheme: ThemeConfig;

// 按 key 获取主题
const theme = getThemeByKey('forest');  // 不存在时返回 defaultTheme

// 主题映射表
themeMap: Record<string, ThemeConfig>;  // { ocean: ..., forest: ..., ... }
```

### 5.2.2 事件类型

> 路径：`gwsu-core/src/constants/events.ts`

```typescript
import { EventType, emitEvent, onEvent } from '@gwsu/core';

enum EventType {
  TOKEN_EXPIRED = 'TOKEN_EXPIRED',   // Token 过期，需要重新登录
  LOGIN_SUCCESS = 'LOGIN_SUCCESS',   // 登录成功
  LOGOUT = 'LOGOUT',                 // 退出登录
  THEME_CHANGE = 'THEME_CHANGE',     // 主题变更
}
```

**使用方式**：

```typescript
// 发送事件
emitEvent(EventType.LOGIN_SUCCESS);
emitEvent(EventType.THEME_CHANGE, themeConfig);

// 监听事件（返回取消监听函数）
const unsubscribe = onEvent(EventType.TOKEN_EXPIRED, (payload) => {
  console.log('Token 已过期');
  history.push('/login');
});

// 取消监听
unsubscribe();
```

---

## 5.3 工具函数

### 5.3.1 request — HTTP 请求工具

> 路径：`gwsu-core/src/utils/request.ts`

基于 fetch 封装的统一请求工具，详见 [04-services-and-api.md](04-services-and-api.md)。

**导出**：

```typescript
import {
  request,        // 通用请求方法
  get,            // GET
  post,           // POST
  put,            // PUT
  del,            // DELETE
  patch,          // PATCH
  setRequestConfig,       // 设置全局配置
  getRequestConfig,       // 获取当前配置
  setErrorToastHandler,   // 设置错误提示回调
  addRequestInterceptor,  // 添加请求拦截器
  addResponseInterceptor, // 添加响应拦截器
  addErrorInterceptor,    // 添加错误拦截器
  clearInterceptors,      // 清除所有拦截器
} from '@gwsu/core';
```

### 5.3.2 theme — 主题工具

> 路径：`gwsu-core/src/utils/theme.ts`

| 函数 | 说明 | 示例 |
|------|------|------|
| `applyTheme(theme)` | 应用主题到 CSS 变量 | `applyTheme(oceanTheme)` |
| `saveTheme(themeKey)` | 保存主题 key 到 localStorage | `saveTheme('forest')` |
| `notifyThemeChange(theme)` | 通知子应用主题变更 | `notifyThemeChange(theme)` |
| `getAntdThemeConfig(theme)` | 获取 Ant Design 主题配置 | `getAntdThemeConfig(theme)` |

### 5.3.3 menu — 菜单转换工具

> 路径：`gwsu-core/src/utils/menu.ts`

| 函数 | 说明 | 示例 |
|------|------|------|
| `getIconComponent(iconName)` | 获取图标组件 | `getIconComponent('dashboard')` |
| `transformToMenuItems(menus)` | 将后端菜单数据转换为 Ant Design Menu items | `transformToMenuItems(menuList)` |

**支持图标映射**：

`dashboard`, `setting`, `security`, `user`, `menu`, `appstore`, `home`, `file`, `team`, `lock`

---

## 5.4 类型定义

### 5.4.1 ThemeConfig — 主题配置类型

> 路径：`gwsu-core/src/types/theme.ts`

```typescript
interface ThemeColors {
  primary: string;
  primaryLight: string;
  primaryDark: string;
  background: string;
  surface: string;
  text: string;
  textSecondary: string;
  border: string;
  success: string;
  warning: string;
  error: string;
  info: string;
}

interface ThemeConfig {
  name: string;       // 主题名称（如 "深海蓝"）
  key: string;        // 主题标识（如 "ocean"）
  colors: ThemeColors;
}
```

### 5.4.2 request — 请求相关类型

> 路径：`gwsu-core/src/types/request.ts`

```typescript
interface ApiResponse<T = unknown> {
  code: number;
  msg: string;
  data: T;
  timestamp?: number;
  errCode?: string;
}

interface PaginationParams {
  current?: number;
  pageSize?: number;
  sortField?: string;
  sortOrder?: 'asc' | 'desc';
}

interface PaginationData<T> {
  list: T[];
  total: number;
  current: number;
  pageSize: number;
}
```

### 5.4.3 menu — 菜单相关类型

> 路径：`gwsu-core/src/types/menu.ts`

```typescript
enum MenuPosition {
  SIDEBAR = 1,
  HEADER = 2,
}

enum MenuOwner {
  ADMIN = 1,
  APP = 2,
}

type MenuRoute = (MenuItemType | SubMenuType) & {
  'data-micro-app'?: string;
};
```

---

## 5.5 服务

### 5.5.1 route — 路由服务

> 路径：`gwsu-core/src/services/route.ts`

```typescript
import { fetchUserRoutes } from '@gwsu/core';
import type { MenuItem } from '@gwsu/core';

interface MenuItem {
  id: number;
  parentId: number | null;
  menuName: string;
  menuType: number;       // 1-目录 2-菜单 3-按钮
  sort: number;
  icon: string;
  path: string;
  microApp?: string;
  visible: boolean;
  status: boolean;
  permission?: string;
  position?: MenuPosition;
  owner?: MenuOwner;
  children?: MenuItem[];
}

// 获取当前用户路由菜单（ADMIN 类型）
const menus = await fetchUserRoutes();
```

### 5.5.2 user — 用户服务

> 路径：`gwsu-core/src/services/user.ts`

```typescript
import { fetchCurrentUserInfo } from '@gwsu/core';
import type { UserInfo } from '@gwsu/core';

// 获取当前登录用户详细信息
const userInfo = await fetchCurrentUserInfo();
```

---

## 5.6 统一导出

`@gwsu/core` 通过 `src/index.ts` 统一导出所有模块：

```tsx
// 常用导入汇总
import {
  // 组件
  ThemeLayout,
  useThemeContext,

  // 常量
  themes,
  defaultTheme,
  getThemeByKey,
  themeMap,
  EventType,
  emitEvent,
  onEvent,

  // 请求工具
  get, post, put, del, patch,
  setRequestConfig,
  setErrorToastHandler,

  // 主题工具
  applyTheme,
  saveTheme,

  // 菜单工具
  getIconComponent,
  transformToMenuItems,

  // 状态管理
  useUserStore,
  useMenuStore,

  // 服务
  fetchUserRoutes,
  fetchCurrentUserInfo,

  // 类型
  type ThemeConfig,
  type ThemeColors,
  type ApiResponse,
  type MenuItem,
  type UserInfo,
  type TokenInfo,
} from '@gwsu/core';
```
