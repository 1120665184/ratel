# 六、状态管理与事件系统

## 6.1 状态管理（Zustand）

项目使用 Zustand 进行状态管理，所有 Store 定义在 `@gwsu/core` 中。

### 6.1.1 useUserStore — 用户状态管理

> 路径：`gwsu-core/src/stores/userStore.ts`

管理用户认证信息和登录状态，自动同步 localStorage。

**接口定义**：

```typescript
interface UserState {
  tokenInfo: TokenInfo | null;
  userInfo: UserInfo | null;
  isLoggedIn: boolean;

  setTokenInfo: (tokenInfo: TokenInfo | null) => void;
  getTokenInfo: () => TokenInfo | null;
  setUserInfo: (userInfo: UserInfo | null) => void;
  getUserInfo: () => UserInfo | null;
  checkLogin: () => boolean;
  isTokenExpired: () => boolean;
  clearUserData: () => void;
  logout: () => void;
}
```

**TokenInfo 类型**：

```typescript
interface TokenInfo {
  token: string;
  userId: number;
  expires: number;         // 有效期（秒）
  expireTime?: number;     // 过期时间戳（毫秒）
}
```

**UserInfo 类型**：

```typescript
interface UserInfo {
  userId: number;
  username: string;
  nickname?: string;
  avatar?: string;
  email?: string;
  phone?: string;
  status?: number;
  deptId?: number;
  deptName?: string;
  roles?: string[];
  permissions?: string[];
  extraData?: Record<string, unknown>;
}
```

**使用方式**：

```tsx
import { useUserStore } from '@gwsu/core';

// 在 React 组件中使用 Hook
function MyComponent() {
  const { isLoggedIn, userInfo, tokenInfo } = useUserStore();
  // ...
}

// 在非组件中使用（如 services、事件处理）
const tokenInfo = useUserStore.getState().getTokenInfo();
useUserStore.getState().setTokenInfo(newToken);
useUserStore.getState().clearUserData();
```

**localStorage 同步**：

| 操作 | localStorage Key | 说明 |
|------|-----------------|------|
| 存储 token | `gwsu_token` | JSON 格式 |
| 存储用户信息 | `gwsu_user` | JSON 格式 |
| 登录标识 | `gwsu_isLoggedIn` | `'true'` / 移除 |

### 6.1.2 useMenuStore — 菜单状态管理

> 路径：`gwsu-core/src/stores/menuStore.ts`

管理导航菜单数据和加载状态。

**接口定义**：

```typescript
interface MenuState {
  menus: MenuItem[];
  loading: boolean;
  currentMenuRoute: MenuItem | null;

  setMenus: (menus: MenuItem[]) => void;
  setLoading: (loading: boolean) => void;
  loadMenus: () => Promise<void>;
  clearMenus: () => void;
  setCurrentMenuRoute: (route: MenuItem | null) => void;
  updateCurrentMenuRouteByPath: (path: string) => void;
}
```

**使用方式**：

```tsx
import { useMenuStore } from '@gwsu/core';

// 在 React 组件中
function Navigation() {
  const { menus, loading, currentMenuRoute } = useMenuStore();
  // ...
}

// 加载菜单（通常在登录成功后）
await useMenuStore.getState().loadMenus();

// 根据路径匹配当前菜单
useMenuStore.getState().updateCurrentMenuRouteByPath('/sub-system/dept');

// 清空菜单（退出登录时）
useMenuStore.getState().clearMenus();
```

### 6.1.3 Store 使用规范

| 场景 | 使用方式 | 示例 |
|------|---------|------|
| React 组件内 | `useXxxStore()` Hook | `const { tokenInfo } = useUserStore()` |
| 非组件环境 | `useXxxStore.getState()` | `useUserStore.getState().getTokenInfo()` |
| 事件回调 | `useXxxStore.getState()` | `useUserStore.getState().clearUserData()` |
| app.tsx | `useXxxStore.getState()` | `useMenuStore.getState().loadMenus()` |

**重要**：在 React 组件中使用 Hook 方式访问 Store，确保组件能响应状态变化；在非组件环境中使用 `getState()` 方式。

---

## 6.2 事件系统

### 6.2.1 概述

项目使用 `window.postMessage` 实现跨应用（主应用与子应用之间）的事件通信。事件类型集中定义在 `EventType` 枚举中，确保类型安全。

### 6.2.2 事件类型

| 事件类型 | 枚举值 | payload 类型 | 说明 |
|---------|--------|-------------|------|
| `LOGIN_SUCCESS` | `'LOGIN_SUCCESS'` | 无 | 登录成功 |
| `TOKEN_EXPIRED` | `'TOKEN_EXPIRED'` | 无 | Token 过期 |
| `LOGOUT` | `'LOGOUT'` | 无 | 退出登录 |
| `THEME_CHANGE` | `'THEME_CHANGE'` | `ThemeConfig` | 主题变更 |

### 6.2.3 发送事件

```typescript
import { emitEvent, EventType } from '@gwsu/core';

// 无 payload 事件
emitEvent(EventType.LOGIN_SUCCESS);
emitEvent(EventType.TOKEN_EXPIRED);

// 有 payload 事件
emitEvent(EventType.THEME_CHANGE, themeConfig);
```

### 6.2.4 监听事件

```typescript
import { onEvent, EventType } from '@gwsu/core';

// 监听事件（返回取消监听函数）
const unsubscribe = onEvent(EventType.THEME_CHANGE, (payload) => {
  const theme = payload as ThemeConfig;
  // 处理主题变更
});

// 组件中监听事件的标准模式
useEffect(() => {
  const unsubLogin = onEvent(EventType.LOGIN_SUCCESS, () => {
    history.push('/sub-system/dashboard');
  });
  const unsubExpire = onEvent(EventType.TOKEN_EXPIRED, () => {
    history.push('/sub-system/login');
  });

  // 清理：取消监听
  return () => {
    unsubLogin();
    unsubExpire();
  };
}, []);
```

### 6.2.5 事件使用场景

| 场景 | 发送方 | 监听方 | 事件 |
|------|--------|--------|------|
| 登录成功 | 子应用（login 页面） | 主应用（Layout） | `LOGIN_SUCCESS` |
| Token 过期 | @gwsu/core request 拦截器 | 主应用（Layout） | `TOKEN_EXPIRED` |
| 退出登录 | 主应用（Layout） | 各子应用 | `LOGOUT` |
| 主题变更 | 主应用（ThemeSwitcher） | 各子应用（ThemeLayout） | `THEME_CHANGE` |

### 6.2.6 新增事件类型

如需新增跨应用事件，按以下步骤操作：

1. 在 `gwsu-core/src/constants/events.ts` 的 `EventType` 枚举中添加新类型
2. 在发送方调用 `emitEvent()`
3. 在监听方使用 `onEvent()` 监听
4. 确保 `useEffect` 中正确清理监听器

```typescript
// 1. 在 events.ts 中添加
export enum EventType {
  // ...existing events
  CUSTOM_EVENT = 'CUSTOM_EVENT',
}

// 2. 发送
emitEvent(EventType.CUSTOM_EVENT, { key: 'value' });

// 3. 监听
const unsubscribe = onEvent(EventType.CUSTOM_EVENT, (payload) => {
  console.log(payload);
});
```

---

## 6.3 典型流程

### 登录流程

```
用户输入账号密码
  → login service 调用 POST /system/auth/login/manager
  → 获取 LoginToken
  → useUserStore.setTokenInfo(token) — 存储 token
  → fetchCurrentUserInfo() — 获取用户详情
  → useUserStore.setUserInfo(userInfo) — 存储用户信息
  → useMenuStore.loadMenus() — 加载菜单
  → emitEvent(EventType.LOGIN_SUCCESS) — 通知主应用
  → 主应用监听到 LOGIN_SUCCESS → history.push(homePath)
```

### 退出流程

```
用户点击退出
  → Modal.confirm 确认
  → logout() 调用后端退出接口
  → localStorage.removeItem('token')
  → message.success('退出成功')
  → history.push(loginPath)
```

### Token 过期流程

```
任意 API 请求返回 401
  → request 响应拦截器自动处理
  → useUserStore.clearUserData() — 清除用户数据
  → emitEvent(EventType.TOKEN_EXPIRED) — 通知主应用
  → 主应用监听到 TOKEN_EXPIRED → history.push(loginPath)
```
