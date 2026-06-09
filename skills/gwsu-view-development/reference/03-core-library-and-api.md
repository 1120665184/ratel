# 三、@gwsu/core 核心库与 API 规范

## 3.1 请求工具

**禁止直接使用 axios 或原生 fetch**，统一使用 `@gwsu/core` 的 fetch 封装：

```tsx
import { get, post, put, del } from '@gwsu/core';
```

- baseURL 自动添加 `/api`，Token 自动注入，401 自动处理
- 错误默认自动 toast 提示
- 异步调用**必须捕获异常**：`try {} catch {}` 或 `.catch(() => {})`

## 3.2 API 路由前缀

所有 API 路径必须以模块 `prefix` 为前缀：`/{模块prefix}/{业务路径}`

| 模块 | prefix | 示例 |
|------|--------|------|
| business-security | `security` | `/security/dept/tree` |
| business-system | `system` | `/system/manager/page` |

## 3.3 服务层组织

- 应用级公共服务：`src/services/`
- 页面级服务：`src/pages/{业务名}/services/`

**服务函数命名**：getXxxPage / getXxxList / getXxxTree / getXxxById / saveOrUpdateXxx / deleteXxx / batchDeleteXxx

```typescript
export async function getXxxPage(query: XxxQuery) {
  const res = await post<{ records: XxxItem[]; total: number }>('/system/xxx/page', query);
  return res.data;
}
export async function saveOrUpdateXxx(data: XxxSaveRequest): Promise<string> {
  const res = await post<string>('/system/xxx', data);
  return res.data;
}
```

## 3.4 字典与配置批量获取

**一个页面多个字典/配置时，必须只调用一次接口**：

```typescript
const dictMap = await fetchDictValuesBatch(['user_status', 'gender', 'dept_type']);
const configMap = await fetchConfigsBatch(['site_name', 'max_upload_size']);
```

## 3.5 组件

### ThemeLayout — 主题布局

所有应用必须使用此组件作为根布局，提供全局主题上下文和 Ant Design ConfigProvider。

```tsx
<ThemeLayout><App /></ThemeLayout>
const { currentTheme, changeTheme } = useThemeContext();
```

### AuthGate — 权限门卫

```tsx
<AuthGate buttonKey="101_add"><Button>新增</Button></AuthGate>
<AuthGate buttonKey="101_edit" fallback={<Button disabled>编辑</Button>}><Button>编辑</Button></AuthGate>
const canEdit = useAuth('101_edit');
```

### FileUpload — 文件上传

内置分片上传、断点续传、MD5 去重。**禁止自行封装文件上传逻辑**。

```tsx
<FileUpload
  property={{ scope: FileScope.PROTECTED, categorize: 'attachment' }}
  multiple maxCount={5} maxSize={50 * 1024 * 1024}
  onChange={(ids) => console.log('文件ID:', ids)}
/>
// 拖拽模式：加 draggable
// 图片卡片：listType="picture-card"
// 表单集成：<Form.Item name="fileIds"><FileUpload ... /></Form.Item>
```

### FileDownloadButton — 文件下载

内置分片下载。**禁止自行封装文件下载逻辑**。

```tsx
<FileDownloadButton fileId={record.fileId} fileName={record.fileName} />
```

## 3.6 Hooks

### useFileUpload / useFileDownload

自定义 UI 或编程式调用时使用：

```tsx
const { upload, progress, abort } = useFileUpload({ scope: FileScope.PROTECTED });
const result = await upload(file, { property: { categorize: 'avatar' } });

const { download, progress, abort } = useFileDownload();
await download('file-id-123');
```

## 3.7 状态管理（Zustand）

| Store | 用途 | 组件内使用 | 非组件使用 |
|-------|------|-----------|-----------|
| useUserStore | 用户认证/登录状态 | `useUserStore()` | `useUserStore.getState()` |
| useMenuStore | 导航菜单 | `useMenuStore()` | `useMenuStore.getState()` |
| useAuthStore | 按钮权限 | `useAuthStore()` | `useAuthStore.getState()` |

**规则**：React 组件内用 Hook 方式，非组件环境用 `getState()` 方式。

## 3.8 事件系统

```tsx
import { EventType, emitEvent, onEvent } from '@gwsu/core';

emitEvent(EventType.LOGIN_SUCCESS);
emitEvent(EventType.THEME_CHANGE, themeConfig);

useEffect(() => {
  const unsub = onEvent(EventType.TOKEN_EXPIRED, () => history.push('/login'));
  return () => { unsub(); };
}, []);
```

| 事件 | 说明 |
|------|------|
| LOGIN_SUCCESS | 登录成功 |
| TOKEN_EXPIRED | Token 过期 |
| LOGOUT | 退出登录 |
| THEME_CHANGE | 主题变更 |

## 3.9 主题

6 种内置主题：ocean（默认）、forest、violet、amber、graphite、midnight（暗色）

```tsx
import { themes, getThemeByKey } from '@gwsu/core';
const theme = getThemeByKey('forest');
```

## 3.10 文件相关类型

```typescript
enum FileScope { PUBLIC = 'PUBLIC', PROTECTED = 'PROTECTED', PRIVATE = 'PRIVATE' }

interface FileProperty { disposable?: boolean; scope?: FileScope; categorize?: string; expiredTime?: string; }
interface KitFileInfoVO { fileId: string; fileName: string; fileUrl: string; /* ... */ }
interface FileUploadOptions { property?: FileProperty; chunkSize?: number; maxConcurrentChunks?: number; onProgress?: (p: ChunkUploadProgress) => void; }
interface FileDownloadOptions { chunkSize?: number; maxConcurrentChunks?: number; onProgress?: (p: FileDownloadProgress) => void; }
```

## 3.11 统一导出

```tsx
import {
  ThemeLayout, useThemeContext, AuthGate, FileUpload, FileDownloadButton,
  themes, getThemeByKey, EventType, emitEvent, onEvent, FileScope,
  get, post, put, del, uploadFile, downloadFile,
  useAuth, useFileUpload, useFileDownload,
  useUserStore, useMenuStore, useAuthStore,
  fetchUserRoutes, fetchCurrentUserInfo, fetchDictValuesBatch, fetchConfigsBatch,
  getFileInfo, removeFile,
  type KitFileInfoVO, type FileProperty, type FileUploadOptions, type FileDownloadOptions,
} from '@gwsu/core';
```
