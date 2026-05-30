# 四、API 服务层与请求规范

## 4.1 请求工具（@gwsu/core/request）

前端统一使用 `@gwsu/core` 提供的 fetch 封装，**禁止直接使用 axios 或原生 fetch**。

### 导入方式

```tsx
import { get, post, put, del, patch } from '@gwsu/core';
```

### 请求方法

| 方法 | 签名 | 说明 |
|------|------|------|
| `get` | `get<T>(url, params?, options?)` | GET 请求 |
| `post` | `post<T>(url, data?, options?)` | POST 请求 |
| `put` | `put<T>(url, data?, options?)` | PUT 请求 |
| `del` | `del<T>(url, data?, options?)` | DELETE 请求 |
| `patch` | `patch<T>(url, data?, options?)` | PATCH 请求 |

### 响应结构

```typescript
interface ApiResponse<T = unknown> {
  code: number;      // 状态码：0 或 200 表示成功
  msg: string;       // 响应消息
  data: T;           // 响应数据
  timestamp?: number;
  errCode?: string;  // 错误码（业务错误时存在）
}
```

### 请求配置

```typescript
interface RequestOptions {
  url: string;
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH';
  params?: Record<string, unknown>;   // URL 查询参数
  data?: Record<string, unknown> | unknown;  // 请求体
  headers?: Record<string, string>;
  timeout?: number;                    // 超时时间(ms)，默认 30000
  showError?: boolean;                 // 是否显示错误提示，默认 true
  showLoading?: boolean;
  errorMessage?: string;
}
```

### 默认行为

- **baseURL**：`/api`（所有请求自动添加 `/api` 前缀）
- **超时**：30 秒
- **Token 注入**：自动从 `userStore` 获取 token，添加到 `Authorization: Bearer {token}`
- **错误提示**：默认自动 toast 提示（notification.error）
- **401 处理**：自动清除用户数据，触发 `TOKEN_EXPIRED` 事件

## 4.2 API 路由前缀规则（重要）

**规则**：所有 API 请求路径必须以对应后端业务模块的 `BusinessModuleInfoProvider` 中定义的 `prefix` 作为统一前缀。

**路径格式**：`/{模块prefix}/{业务路径}`

### 当前模块前缀映射

| 后端模块 | prefix | API 路径示例 |
|---------|--------|------------|
| business-security | `security` | `/security/menu/routes/1` |
| business-system | `system` | `/system/manager/page` |

### 说明

- `prefix` 来源于后端 `BusinessModuleInfoProvider` 的 `module().prefix()` 返回值
- 前端所有接口请求路径必须添加对应模块的 `prefix` 前缀
- 新增后端模块时，前端使用该模块的 `prefix` 作为 API 路由前缀

### 错误示例

```typescript
// 错误：缺少模块前缀
export async function getDeptTree() {
  const res = await get<DeptTreeNode[]>('/dept/tree');
  return res.data;
}
```

### 正确示例

```typescript
// 正确：system 模块的接口以 /system 为前缀
export async function getDeptTree() {
  const res = await get<DeptTreeNode[]>('/system/dept/tree');
  return res.data;
}

// 正确：security 模块的接口以 /security 为前缀
export async function fetchUserRoutes() {
  const res = await get<MenuItem[]>('/security/menu/routes/1');
  return res.data;
}
```

## 4.3 服务层组织

### 服务文件位置

- **应用级公共服务**：`src/services/` 目录（多个页面共享的 API）
- **页面级服务**：`src/pages/{业务名}/services/` 目录（仅单个页面使用的 API）

### 服务文件模板

```typescript
// services/xxx.ts
import { get, post, put, del } from '@gwsu/core';
import type { XxxItem, XxxQuery, XxxSaveRequest } from '@/pages/xxx/types';

/** 分页查询 */
export async function getXxxPage(query: XxxQuery) {
  const res = await post<{ records: XxxItem[]; total: number }>(
    '/system/xxx/page',
    query,
  );
  return res.data;
}

/** 根据ID查询 */
export async function getXxxById(id: string): Promise<XxxItem> {
  const res = await get<XxxItem>(`/system/xxx/${id}`);
  return res.data;
}

/** 查询列表 */
export async function getXxxList(): Promise<XxxItem[]> {
  const res = await get<XxxItem[]>('/system/xxx/list');
  return res.data;
}

/** 新增或更新 */
export async function saveOrUpdateXxx(data: XxxSaveRequest): Promise<string> {
  const res = await post<string>('/system/xxx', data);
  return res.data;
}

/** 删除 */
export async function deleteXxx(id: string): Promise<void> {
  await del<void>(`/system/xxx/${id}`);
}

/** 批量删除 */
export async function batchDeleteXxx(ids: string[]): Promise<void> {
  await del<void>('/system/xxx', ids);
}
```

### 服务函数命名规范

| 操作 | 命名规则 | 示例 |
|------|---------|------|
| 分页查询 | get + 实体 + Page | `getUserPage` |
| 列表查询 | get + 实体 + List | `getDeptList` |
| 树形查询 | get + 实体 + Tree | `getDeptTree` |
| 单条查询 | get + 实体 + ById | `getUserById` |
| 新增或更新 | saveOrUpdate + 实体 | `saveOrUpdateUser` |
| 删除 | delete + 实体 | `deleteDept` |
| 批量删除 | batchDelete + 实体 | `batchDeleteUsers` |
| 状态更新 | update + 实体 + Status | `updateUserStatus` |
| 自定义操作 | 动词 + 实体 + 名词 | `bindAccount`, `resetPassword` |

## 4.4 代理配置

开发环境中，前端通过 UmiJS 代理将 `/api` 请求转发到后端：

```typescript
// config/config.ts
proxy: {
  '/api': {
    target: 'http://localhost:8888',  // 后端网关地址
    changeOrigin: true,
    pathRewrite: { '^/api': '' },     // 去掉 /api 前缀
  },
}
```

**请求流程**：

```
前端 get('/system/manager/page')
  → fetch('/api/system/manager/page')     // 添加 baseURL /api
  → 代理转发到 http://localhost:8888/system/manager/page  // 去掉 /api
  → 后端网关 → 具体微服务
```

## 4.5 拦截器

### 默认拦截器

请求工具已内置以下拦截器，**无需手动配置**：

1. **请求拦截器**：自动注入 Authorization token
2. **响应拦截器**：处理业务错误码（code 非 0/200 时抛出错误）
3. **401 拦截**：token 过期自动清除数据并触发事件
4. **错误拦截器**：console 记录错误日志

### 自定义拦截器

如需添加自定义拦截器：

```typescript
import { addRequestInterceptor, addResponseInterceptor } from '@gwsu/core';

// 添加请求拦截器（如添加自定义 header）
addRequestInterceptor((options) => {
  options.headers = {
    ...options.headers,
    'X-Custom-Header': 'value',
  };
  return options;
});

// 添加响应拦截器
addResponseInterceptor((response) => {
  // 自定义响应处理
  return response;
});
```

## 4.6 异常捕获规范（重要）

在组件中调用异步接口时，**必须捕获异常**，防止未处理的 Promise rejection 向上传递导致白屏。

### 规则

- 使用 `await` 调用接口时，必须使用 `try {} catch {}` 包裹
- 不使用 `await` 时，必须使用 `.catch()` 捕获异常
- `catch` 中可以不做额外处理（空 `catch {}`），因为 request 层已自动 toast 错误提示

### 正确示例

```tsx
// 方式一：try/catch
const handleDelete = async () => {
  try {
    await batchDeleteUsers(selectedRowKeys as string[]);
    message.success('删除成功');
    setSelectedRowKeys([]);
    onRefresh();
  } catch {}
};

// 方式二：.catch()
useEffect(() => {
  void loadMenus().catch(console.error);
}, []);

// 方式三：fire-and-forget（无需等待结果时）
const loadData = () => {
  getXxxList()
    .then(setData)
    .catch(() => {});
};
```

### 错误示例

```tsx
// 错误：未捕获异常，rejected Promise 向上传递
const handleDelete = async () => {
  await batchDeleteUsers(selectedRowKeys as string[]);
  message.success('删除成功');
};

// 错误：useEffect 中的异步操作未捕获
useEffect(() => {
  void loadMenus();  // 如果 loadMenus 内部抛出未捕获的异常，会导致 unhandledrejection
}, []);
```

## 4.7 请求与后端 R\<T\> 对应

后端统一使用 `R<T>` 返回，前端 `ApiResponse<T>` 与之对应：

| 后端 | 前端 | 说明 |
|------|------|------|
| `R.ok(data)` | `{ code: 200, data, msg: '...' }` | 成功 |
| `R.fail("msg")` | `{ code: 500, msg: '...', errCode: '...' }` | 失败 |
| `R.ok(data, "msg")` | `{ code: 200, data, msg: '...' }` | 成功带消息 |

前端通过 `response.code === 0 || response.code === 200` 判断成功，否则视为业务错误。

## 4.8 字典与配置批量获取规范（重要）

`@gwsu/core` 提供了字典和配置的批量获取工具，**一个页面中需要多个字典或配置时，必须只调用一次接口，将所有键一次性传入**，避免多次请求浪费性能。

### 导入方式

```typescript
import { fetchDictValuesBatch, fetchConfigsBatch } from '@gwsu/core';
import type { DictValueMap, ConfigMap } from '@gwsu/core';
```

### 字典批量获取

```typescript
// 正确：一次获取页面所需的所有字典
const dictMap = await fetchDictValuesBatch(['user_status', 'gender', 'dept_type']);
// dictMap = { user_status: [...], gender: [...], dept_type: [...] }

// 在组件中使用
const userStatusOptions = dictMap['user_status']?.map(item => ({
  label: item.dictLabel,
  value: item.dictValue,
}));
```

### 配置批量获取

```typescript
// 正确：一次获取页面所需的所有配置
const configMap = await fetchConfigsBatch(['site_name', 'max_upload_size', 'enable_register']);
// configMap = { site_name: {...}, max_upload_size: {...}, enable_register: {...} }

// 获取具体配置值
const siteName = configMap['site_name']?.configValue;
```

### 禁止的做法

```typescript
// 错误：多次调用获取不同字典（浪费请求，性能差）
const statusDict = await fetchDictValuesBatch(['user_status']);
const genderDict = await fetchValuesBatch(['gender']);

// 错误：多次调用获取不同配置（浪费请求，性能差）
const siteName = await fetchConfigsBatch(['site_name']);
const maxSize = await fetchConfigsBatch(['max_upload_size']);
```

### 页面级使用示例

```tsx
import React, { useEffect, useState } from 'react';
import { fetchDictValuesBatch, fetchConfigsBatch } from '@gwsu/core';
import type { DictValueMap, ConfigMap } from '@gwsu/core';

const MyPage: React.FC = () => {
  const [dictMap, setDictMap] = useState<DictValueMap>({});
  const [configMap, setConfigMap] = useState<ConfigMap>({});

  useEffect(() => {
    // 一次获取所有字典 + 一次获取所有配置，总共只有 2 个请求
    Promise.all([
      fetchDictValuesBatch(['user_status', 'gender']).catch(() => {}),
      fetchConfigsBatch(['site_name', 'max_upload_size']).catch(() => {}),
    ]).then(([dicts, configs]) => {
      if (dicts) setDictMap(dicts);
      if (configs) setConfigMap(configs);
    });
  }, []);

  const statusOptions = dictMap['user_status']?.map(item => ({
    label: item.dictLabel,
    value: item.dictValue,
  }));

  const maxUploadSize = configMap['max_upload_size']?.configValue;

  return (
    // ... 使用 statusOptions 和 maxUploadSize
  );
};
```
