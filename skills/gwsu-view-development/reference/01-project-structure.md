# 一、目录规范

## 1.1 项目整体结构

```
web/
├── apps/                              # 应用目录
│   ├── gwsu-main/                     # 主应用（端口 8000）— qiankun master
│   │   ├── config/
│   │   │   ├── config.ts              # Umi + qiankun 主应用配置
│   │   │   └── routes.ts             # 主应用路由（含子应用挂载）
│   │   ├── src/
│   │   │   ├── layouts/              # 全局布局（含 ThemeLayout）
│   │   │   │   ├── index.tsx
│   │   │   │   └── index.module.less
│   │   │   ├── components/           # 主应用专属组件
│   │   │   ├── providers/            # Provider 组件（如 CopilotKit）
│   │   │   ├── services/             # 主应用专属 API 服务
│   │   │   └── app.tsx              # 应用初始化（onRouteChange 等）
│   │   └── package.json
│   │
│   ├── gwsu-sub-system/              # 子应用-系统管理（端口 8001）
│   │   ├── config/
│   │   │   ├── config.ts             # Umi + qiankun slave 配置
│   │   │   └── routes.ts            # 子应用路由
│   │   ├── src/
│   │   │   ├── layouts/             # 子应用布局（ThemeLayout 包裹）
│   │   │   ├── components/          # 子应用专属组件
│   │   │   ├── pages/              # 页面目录
│   │   │   │   ├── dashboard.tsx
│   │   │   │   ├── dashboard.module.less
│   │   │   │   ├── login.tsx
│   │   │   │   ├── login.module.less
│   │   │   │   ├── dept/            # 业务模块页面（按功能分目录）
│   │   │   │   │   ├── index.tsx
│   │   │   │   │   ├── index.module.less
│   │   │   │   │   ├── types/       # 页面级类型定义
│   │   │   │   │   ├── hooks/       # 页面级自定义 Hooks
│   │   │   │   │   ├── components/  # 页面级组件
│   │   │   │   │   │   ├── DeptTreePanel/
│   │   │   │   │   │   │   ├── index.tsx
│   │   │   │   │   │   │   └── index.module.less
│   │   │   │   │   │   └── DeptFormModal/
│   │   │   │   │   │       ├── index.tsx
│   │   │   │   │   │       └── index.module.less
│   │   │   │   │   └── services/    # 页面级 API 服务
│   │   │   │   └── user/
│   │   │   │       └── (同上结构)
│   │   │   └── services/            # 子应用级公共 API 服务
│   │   └── package.json
│   │
│   └── gwsu-sub-security/           # 子应用-安全中心（端口 8002）
│       ├── config/
│       │   ├── config.ts
│       │   └── routes.ts
│       ├── src/
│       │   ├── layouts/
│       │   └── pages/
│       └── package.json
│
├── gwsu-core/                        # 共享核心库（@gwsu/core）
│   ├── src/
│   │   ├── components/              # 共享组件
│   │   │   └── ThemeLayout.tsx      # 主题布局组件
│   │   ├── constants/               # 常量定义
│   │   │   ├── theme.ts            # 主题配置（6 种主题）
│   │   │   ├── events.ts           # 事件类型枚举
│   │   │   └── index.ts            # 常量统一导出
│   │   ├── types/                   # 类型定义
│   │   │   ├── theme.ts
│   │   │   ├── request.ts
│   │   │   ├── menu.ts
│   │   │   └── index.ts
│   │   ├── utils/                   # 工具函数
│   │   │   ├── request.ts          # HTTP 请求封装
│   │   │   ├── theme.ts            # 主题应用工具
│   │   │   ├── menu.ts             # 菜单转换工具
│   │   │   └── index.ts
│   │   ├── hooks/                   # 共享 Hooks
│   │   │   └── index.ts
│   │   ├── stores/                  # Zustand 状态管理
│   │   │   ├── userStore.ts
│   │   │   ├── menuStore.ts
│   │   │   └── index.ts
│   │   ├── services/                # 共享 API 服务
│   │   │   ├── route.ts
│   │   │   ├── user.ts
│   │   │   └── index.ts
│   │   └── index.ts                # 统一导出
│   ├── package.json
│   └── tsconfig.json
│
├── package.json                      # 工作空间根配置
└── pnpm-workspace.yaml              # pnpm 工作空间配置
```

## 1.2 工作空间配置

**pnpm-workspace.yaml**：

```yaml
packages:
  - 'apps/*'
  - 'gwsu-core'
```

**根 package.json 关键脚本**：

```json
{
  "scripts": {
    "dev:main": "pnpm --filter gwsu-main dev",
    "dev:sub-system": "pnpm --filter gwsu-sub-system dev",
    "dev:sub-security": "pnpm --filter gwsu-sub-security dev",
    "dev:all": "pnpm -r --parallel dev",
    "build:core": "pnpm --filter @gwsu/core build",
    "build:main": "pnpm --filter gwsu-main build",
    "build:sub-system": "pnpm --filter gwsu-sub-system build",
    "build:sub-security": "pnpm --filter gwsu-sub-security build",
    "build:all": "pnpm build:core && pnpm -r build",
    "clean": "pnpm -r exec rm -rf node_modules dist .umi"
  }
}
```

## 1.3 微前端应用配置

### 主应用（gwsu-main）

**config/config.ts**：

```typescript
import { defineConfig } from 'umi';
import routes from './routes';

export default defineConfig({
  npmClient: 'pnpm',
  mfsu: false,
  plugins: ['@umijs/plugins/dist/qiankun'],
  qiankun: {
    master: {
      apps: [
        { name: 'gwsu-sub-system', entry: '//localhost:8001' },
        { name: 'gwsu-sub-security', entry: '//localhost:8002' },
      ],
    },
  },
  routes,
  proxy: {
    '/api': {
      target: 'http://localhost:8888',
      changeOrigin: true,
      pathRewrite: { '^/api': '' },
    },
  },
});
```

**config/routes.ts**：

```typescript
export default [
  {
    path: '/',
    component: '@/layouts/index',
    routes: [
      { path: '/', redirect: '/sub-system/dashboard' },
      {
        path: '/sub-system/*',
        microApp: 'gwsu-sub-system',
      },
      {
        path: '/sub-security/*',
        microApp: 'gwsu-sub-security',
      },
    ],
  },
];
```

### 子应用（gwsu-sub-system）

**config/config.ts**：

```typescript
import { defineConfig } from 'umi';
import routes from './routes';

export default defineConfig({
  npmClient: 'pnpm',
  base: '/sub-system',          // 必须与主应用路由前缀一致
  mountElementId: 'sub-system-root',  // 挂载元素 ID，各子应用唯一
  mfsu: false,
  esbuildMinifyIIFE: true,       // 子应用必需，避免 IIFE 冲突
  plugins: ['@umijs/plugins/dist/qiankun'],
  qiankun: {
    slave: {},
  },
  routes,
  proxy: {
    '/api': {
      target: 'http://localhost:8888',
      changeOrigin: true,
      pathRewrite: { '^/api': '' },
    },
  },
});
```

## 1.4 应用与端口映射

| 应用 | 角色 | 端口 | base 路径 | mountElementId |
|------|------|------|----------|----------------|
| gwsu-main | 主应用（master） | 8000 | `/` | 默认 |
| gwsu-sub-system | 子应用（slave） | 8001 | `/sub-system` | `sub-system-root` |
| gwsu-sub-security | 子应用（slave） | 8002 | `/sub-security` | `sub-security-root` |

## 1.5 新建子应用步骤

1. 在 `apps/` 下创建应用目录，如 `gwsu-sub-xxx`
2. 初始化 UmiJS 项目，安装依赖
3. 配置 `config/config.ts`：
   - 设置 `base: '/sub-xxx'`
   - 设置 `mountElementId: 'sub-xxx-root'`
   - 添加 `esbuildMinifyIIFE: true`
   - 配置 `qiankun: { slave: {} }`
4. 添加到 `pnpm-workspace.yaml` 的 `packages` 列表
5. 在主应用 `config/config.ts` 的 `qiankun.master.apps` 中注册：
   ```typescript
   { name: 'gwsu-sub-xxx', entry: '//localhost:800N' }
   ```
6. 在主应用 `config/routes.ts` 添加路由：
   ```typescript
   {
     path: '/sub-xxx/*',
     microApp: 'gwsu-sub-xxx',
   }
   ```
7. 在根 `package.json` 添加 dev/build 脚本
8. 子应用布局必须使用 `ThemeLayout` 包裹

## 1.6 业务页面目录结构

每个业务功能页面遵循统一的目录结构：

```
pages/{业务名}/
├── index.tsx                   # 页面主组件
├── index.module.less           # 页面样式
├── types/                      # 页面级类型定义
│   └── index.ts
├── hooks/                      # 页面级自定义 Hooks（可选）
│   └── useXxx.tsx
├── components/                 # 页面级组件
│   ├── XxxPanel/
│   │   ├── index.tsx
│   │   └── index.module.less
│   └── XxxFormModal/
│       ├── index.tsx
│       └── index.module.less
└── services/                   # 页面级 API 服务（可选，简单页面可放在应用级 services）
    └── xxx.ts
```
