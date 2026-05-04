# 数据权限管理界面设计文档

## 概述

开发数据权限管理界面，包括后端枚举接口和前端 CRUD 页面。前端子应用为 `sub-security`，后端微服务为 `business-security`。功能样式参照角色管理。

## 后端设计

### 1. 枚举修改

给 `DataResourceAssertType` 和 `DataResourceFieldConditionType` 添加 `description` 字段：

```java
// DataResourceAssertType
EQ("等于"), LIKE("模糊匹配");

// DataResourceFieldConditionType
AND("与"), OR("或");
```

### 2. 新增枚举接口

在 `SecurityDataResourceController` 中新增：

| 方法 | 路径 | 返回值 | 说明 |
|------|------|--------|------|
| GET | `/security/data-resource/enums/assert-type` | `R<List<EnumOptionVO>>` | 断言类型枚举 |
| GET | `/security/data-resource/enums/condition-type` | `R<List<EnumOptionVO>>` | 条件关联关系枚举 |

返回格式与角色管理枚举接口一致，使用 `EnumOptionVO(label, value)`，其中 value 为枚举 name()（String 类型），label 为 description。

### 3. 已有业务接口

无需新增，已有的 `DataResourceClientApi` 覆盖所有需求：
- `POST /security/data-resource/page` — 分页查询
- `POST /security/data-resource` — 保存/更新
- `POST /security/data-resource/delete` — 批量删除
- `POST /security/data-resource/sync` — 同步到 Redis

### 4. 用户资源属性数据

前端从 `business-system` 的 `GET /basic/dataResourceAttribute` 获取，返回 `List<ResourceRuleKeyProperties>`（key + desc）。

## 前端设计

### 1. 路由配置

在 `web/apps/gwsu-sub-security/config/routes.ts` 中新增：

```ts
{
  path: '/dataresource',
  component: '@/pages/dataresource',
}
```

### 2. 页面结构

```
src/pages/dataresource/
├── index.tsx                        # 列表主页面
├── index.module.less                # 列表页样式
├── types/index.ts                   # 类型定义
├── hooks/useDataResource.ts         # 业务逻辑 Hook
├── services/dataResource.ts         # API 调用封装
└── components/
    ├── DataResourceDetail/          # 详情抽屉
    │   ├── index.tsx
    │   └── index.module.less
    └── DataResourceFormModal/       # 新增/编辑弹窗
        ├── index.tsx
        └── index.module.less
```

### 3. 类型定义

```ts
interface DataResourceInfo {
  id?: string;
  databaseName?: string;
  tableName: string;
  description?: string;
  status: boolean;
  conditions?: DataResourceCondition[];
  createTime?: string;
}

interface DataResourceCondition {
  id?: string;
  fieldName: string;
  showNull: boolean;
  userResourceFields: string[];
  assertType: string;
  relationship?: string;
  sort: number;
}

interface DataResourceQuery {
  tableName?: string;
  databaseName?: string;
  status?: boolean;
  pageNum?: number;
  pageSize?: number;
}

interface ResourceAttribute {
  key: string;
  desc: string;
}

interface EnumOption {
  label: string;
  value: string;
}
```

### 4. 列表页功能

- **搜索栏**：表名（Input 模糊）、库名（Input 模糊）、状态（Select）
- **操作栏**：新增数据资源、批量删除、同步到 Redis
- **表格列**：序号、库名、表名、描述、状态(Switch)、条件数、操作(详情/编辑/删除)
- **分页**：与角色管理一致

### 5. 详情抽屉

- 用 `Descriptions` 展示主表信息（库名、表名、描述、状态、创建时间）
- 下方用 `Table` 展示条件列表（字段名、用户资源字段、断言类型、关联关系、显示Null、排序号）

### 6. 编辑弹窗

- **主表单**：库名、表名（必填）、描述、状态
- **条件子表格**：可增删行，每行包含：
  - 字段名（Input）
  - 用户资源字段（Select 多选，选项从 `/basic/dataResourceAttribute` 获取）
  - 断言类型（Select，从枚举接口获取）
  - 关联关系（Select，从枚举接口获取，第一行隐藏）
  - 显示Null（Switch）
  - 排序号（InputNumber）
  - 删除按钮

### 7. API 封装

```ts
const BASE = '/security/data-resource';

POST ${BASE}/page          // 分页查询
POST ${BASE}               // 保存/更新
POST ${BASE}/delete        // 批量删除
POST ${BASE}/sync          // 同步Redis
GET  ${BASE}/enums/assert-type      // 断言类型枚举
GET  ${BASE}/enums/condition-type   // 条件关联关系枚举
GET  /basic/dataResourceAttribute   // 用户资源属性（跨服务）
```

### 8. 样式规范

- 使用 CSS Modules（`*.module.less`），禁止 CSS-in-JS
- 使用 CSS 变量适配多主题系统
- 参照角色管理的样式结构
