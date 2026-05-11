# 按钮级权限控制设计方案

## 背景

后端 `/menu/routes/{owner}` 接口已返回 menuType=3 的按钮权限数据作为菜单的 children，其中 `buttonKey` 字段标识按钮权限。前端当前在 `transformToMenuItems()` 中过滤掉了按钮数据，且没有任何按钮级权限控制逻辑。需要实现前端按钮级权限控制，根据当前路由的按钮权限决定按钮是否显示。

## 核心思路

保留后端返回的完整菜单树数据（含 menuType=3 按钮），根据当前路由提取对应按钮权限，通过 `buttonKey` 判定按钮是否显示。

## 数据流

```
后端返回菜单树（含 menuType=3 按钮作为 children）
  → menuStore.menus 存储完整菜单树
  → 路由变化时，updateCurrentMenuRouteByPath() 更新 currentMenuRoute
  → authStore 监听 currentMenuRoute 变化
  → 提取 currentMenuRoute.children 中 menuType=3 的项
  → 建立 buttonKey → true 的权限映射 Map
  → <AuthGate buttonKey="xxx"> 或 useAuth('xxx') 判断是否渲染
```

## 新增文件

| 文件 | 说明 |
|------|------|
| `gwsu-core/src/stores/authStore.ts` | AuthStore，管理按钮权限状态 |
| `gwsu-core/src/hooks/useAuth.ts` | `useAuth(buttonKey)` hook |
| `gwsu-core/src/components/AuthGate.tsx` | `<AuthGate>` 权限门卫组件 |
| `gwsu-core/src/components/AuthGate.module.less` | AuthGate 样式（空，预留） |

## 修改文件

| 文件 | 修改内容 |
|------|----------|
| `gwsu-core/src/stores/menuStore.ts` | `updateCurrentMenuRouteByPath` 中同步调用 `authStore.updateAuthByMenuRoute()` |
| `gwsu-core/src/stores/index.ts` | 导出 authStore |
| `gwsu-core/src/hooks/index.ts` | 导出 useAuth |
| `gwsu-core/src/components/index.ts` | 导出 AuthGate |

## authStore 设计

```typescript
interface AuthState {
  buttonAuthMap: Record<string, boolean>;
  updateAuthByMenuRoute: (menuRoute: MenuItem | null) => void;
  hasAuth: (buttonKey: string) => boolean;
  clearAuth: () => void;
}
```

### 关键逻辑

- `updateAuthByMenuRoute`：接收 currentMenuRoute，遍历其 children，筛选 `menuType === 3` 的项，将其 `buttonKey` 加入 map
- `hasAuth`：从 buttonAuthMap 中查找 buttonKey 是否存在
- `clearAuth`：清空权限映射（退出登录时调用）
- 通过 `rawWindow` 跨微应用共享实例（与 menuStore/userStore 一致模式），全局键 `__GWSU_AUTH_STORE__`

### 路由切换时权限更新

在 `menuStore.updateCurrentMenuRouteByPath()` 中，更新 currentMenuRoute 后同步调用 `useAuthStore.getState().updateAuthByMenuRoute(found)`。

## useAuth Hook

```typescript
function useAuth(buttonKey: string): boolean {
  const buttonAuthMap = useAuthStore(state => state.buttonAuthMap);
  return !!buttonAuthMap[buttonKey];
}
```

## AuthGate 组件

权限门卫组件，控制内容是否通过"门卫"显示。不限于按钮，任何需要权限控制的内容均可使用。

```tsx
interface AuthGateProps {
  buttonKey: string;
  children: React.ReactNode;
  fallback?: React.ReactNode;  // 无权限时的替代内容，默认 null
}

const AuthGate: React.FC<AuthGateProps> = ({ buttonKey, children, fallback = null }) => {
  const hasAuth = useAuth(buttonKey);
  return hasAuth ? <>{children}</> : <>{fallback}</>;
};
```

## 业务页面使用示例

```tsx
import { AuthGate, useAuth } from '@gwsu/core';

// 方式1：AuthGate 组件包裹（按钮）
<AuthGate buttonKey="101_add">
  <Button type="primary">新增</Button>
</AuthGate>

// 方式2：AuthGate 组件包裹（非按钮内容，如整个区块）
<AuthGate buttonKey="101_export">
  <Card title="数据导出">...</Card>
</AuthGate>

// 方式3：useAuth hook
const canDelete = useAuth('101_delete');
{canDelete && <Button danger>删除</Button>}

// 方式4：无权限时显示替代内容
<AuthGate buttonKey="101_edit" fallback={<Tooltip title="无权限"><Button disabled>编辑</Button></Tooltip>}>
  <Button type="link">编辑</Button>
</AuthGate>
```

## 边界情况

1. **菜单未加载完**：buttonAuthMap 为空，所有按钮不显示
2. **当前路由不在菜单树中**：buttonAuthMap 为空，所有按钮不显示
3. **退出登录**：`clearAuth()` 清空权限映射
4. **跨微应用**：通过 rawWindow 共享 authStore 实例

## 决策记录

- 使用独立 AuthStore（方案A），与 menuStore/userStore 架构一致
- 使用完整 buttonKey 匹配，不拆分
- 不兼容 permission 字段，仅使用 buttonKey
- 核心逻辑全部在 gwsu-core 中实现
