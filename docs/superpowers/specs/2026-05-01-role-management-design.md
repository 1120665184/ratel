# 角色管理功能设计文档

## 1. 概述

角色管理是安全中心模块的核心功能，提供角色的CRUD、菜单权限配置（支持带时效）、角色禁用/启用等能力。本设计涉及后端（business-security模块）和前端（gwsu-sub-security子应用）两部分。

## 2. 功能清单

| 功能 | 说明 | 本期 |
|------|------|------|
| 角色CRUD | 创建、查询、更新、删除角色 | 是 |
| 角色禁用/启用 | 禁用后用户失去该角色权限，但不删除关联 | 是 |
| 菜单权限配置 | 给角色关联菜单/按钮，支持配置带时效的权限 | 是 |
| Casbin时间函数 | timeInRange、cycleWeekly、cycleMonthly | 是 |
| 字段权限 | 行内按钮占位，点击提示"功能开发中" | 占位 |
| 表模型权限 | 行内按钮占位，点击提示"功能开发中" | 占位 |
| 角色成员管理 | 查看角色下用户，从角色维度添加/移除用户 | 否 |
| 角色复制 | 基于已有角色快速创建新角色 | 否 |

## 3. 数据库设计

### 3.1 修改 security_role 表

新增字段：

| 字段 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| role_type | SMALLINT | 2 | 角色类型：1=系统角色，2=业务角色 |
| data_scope | SMALLINT | 1 | 数据范围：0=自定义，1=全部数据，2=本部门及以下，3=本部门，4=仅本人 |
| status | SMALLINT | 1 | 状态：0=禁用，1=启用 |

**数据范围说明**：数值越小权限越大。0=自定义表示完全依赖数据权限配置来定义每张表的过滤规则，优先级最高。当某张表有数据权限配置时，使用配置的精细规则（覆盖预设）；没有配置时，使用所有角色中最低的预设scope值。

### 3.2 修改 security_role_menu 表

将 `abac_permission_id` 字段移到 `security_role_menu_permission` 表，新增时效字段，使该表真正成为角色-菜单的一对一关联。

| 字段 | 类型 | 说明 |
|------|------|------|
| id | VARCHAR(24) | 主键 |
| role_id | VARCHAR(24) | 角色ID |
| menu_id | VARCHAR(24) | 菜单ID |
| ~~abac_permission_id~~ | - | **移除**，挪到 permission 表 |
| valid_type | SMALLINT | 时效类型：1=永久，2=绝对时间范围，3=周期性 |
| valid_start | DATETIME/TIMESTAMP | 绝对时间-开始时间（valid_type=2时有效） |
| valid_end | DATETIME/TIMESTAMP | 绝对时间-结束时间（valid_type=2时有效） |
| cycle_type | SMALLINT | 周期类型：1=按周，2=按月（valid_type=3时有效） |
| cycle_value | VARCHAR(100) | 周期值：按周存"1,2,3,4,5"，按月存"1,15" |
| cycle_start_time | TIME | 周期-每日开始时间（为空表示00:00:00） |
| cycle_end_time | TIME | 周期-每日结束时间（为空表示23:59:59） |
| 审计字段 | - | tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time |

**唯一索引**：`uk_role_menu (role_id, menu_id)` — 真正的一对一关联

### 3.3 新建 security_role_menu_permission 表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | VARCHAR(24) | 主键 |
| role_menu_id | VARCHAR(24) | 关联 security_role_menu.id |
| abac_permission_id | VARCHAR(24) | ABAC接口权限ID，关联security_abac_permission表 |
| 审计字段 | - | 同上 |

**索引**：`idx_role_menu_id (role_menu_id)`, `idx_abac_permission_id (abac_permission_id)`

### 3.4 数据关系变化

**调整前**：
```
角色 1--N security_role_menu N--1 菜单
                    |
                abac_permission_id (直接字段)
```

**调整后**：
```
角色 1--N security_role_menu 1--1 菜单    (含时效配置)
                    |
              1--N security_role_menu_permission N--1 security_abac_permission
```

一个角色绑定一个菜单只有一条记录，该菜单下挂的多个接口通过 `security_role_menu_permission` 关联。

## 4. Casbin自定义函数

在 `common-security/casbin/function/` 目录下新建三个函数：

### 4.1 TimeInRangeFunction

- **函数名**：`timeInRange`
- **参数**：`(startStr, endStr)` — 绝对时间的开始和结束时间字符串
- **逻辑**：判断当前时间是否在 `[start, end]` 范围内
- **表达式示例**：`timeInRange("2026-05-01 08:00", "2026-12-31 18:00")`

### 4.2 CycleWeeklyFunction

- **函数名**：`cycleWeekly`
- **参数**：`(weekDays, startTime, endTime)` — 周几(逗号分隔)、每日开始时间、每日结束时间
- **逻辑**：判断当前时间对应的星期几是否在 weekDays 中，且时间在范围内
- **startTime/endTime为空的处理**：
  - 均为空：全天（00:00:00 ~ 23:59:59）
  - startTime有值，endTime为空：从startTime到23:59:59
  - startTime为空，endTime有值：从00:00:00到endTime
  - 均有值：从startTime到endTime
- **表达式示例**：`cycleWeekly("1,2,3,4,5", "09:00", "18:00")`

### 4.3 CycleMonthlyFunction

- **函数名**：`cycleMonthly`
- **参数**：`(monthDays, startTime, endTime)` — 每月几号(逗号分隔)、每日开始时间、每日结束时间
- **逻辑**：判断当前日期的日是否在 monthDays 中，且时间在范围内
- **startTime/endTime为空的处理**：同CycleWeeklyFunction
- **表达式示例**：`cycleMonthly("1,15", "09:00", "18:00")`

## 5. ABAC表达式构建

### 5.1 表达式格式

表达式不含接口URL，只关注角色+时效：

- **永久（valid_type=1）**：`contains(r.sub.roles, 'role_admin')`
- **绝对时间范围（valid_type=2）**：`contains(r.sub.roles, 'role_admin') && timeInRange("2026-05-01 08:00", "2026-12-31 18:00")`
- **周期性-按周（valid_type=3, cycle_type=1）**：`contains(r.sub.roles, 'role_admin') && cycleWeekly("1,2,3,4,5", "09:00", "18:00")`
- **周期性-按月（valid_type=3, cycle_type=2）**：`contains(r.sub.roles, 'role_admin') && cycleMonthly("1,15", "09:00", "18:00")`

接口URL的关联通过 `security_role_menu_permission` → `security_abac_permission` 的数据关系体现，不在表达式中重复。

### 5.2 RoleBindingMenuAbacLoading 改造

**buildExpression(ExpressionContext context)** 方法改造：
1. 从 context 获取角色信息构建基础表达式：`"contains(r.sub.roles, '%s')".formatted(roleId)`
2. 从 context 获取该角色-菜单的时效配置 (valid_type)
3. 根据 valid_type 追加时效条件：
   - valid_type=1：不追加
   - valid_type=2：追加 `&& timeInRange(validStart, validEnd)`
   - valid_type=3, cycle_type=1：追加 `&& cycleWeekly(cycleValue, cycleStartTime, cycleEndTime)`
   - valid_type=3, cycle_type=2：追加 `&& cycleMonthly(cycleValue, cycleStartTime, cycleEndTime)`
4. 返回完整表达式

**alterationUrlPermission(ExpressionContext context, AbacPermissionUrlWrapper wrapper)** 方法改造：
1. 删除角色原有 `security_role_menu` 和 `security_role_menu_permission` 数据
2. 遍历选中的菜单列表：
   a. 创建 `security_role_menu` 记录（含时效字段）
   b. 解析菜单的 permission 字段，拆分出多个接口权限
   c. 为每个接口权限创建 `security_abac_permission` 记录
   d. 创建 `security_role_menu_permission` 关联记录
   e. 调用 buildExpression 构建ABAC表达式
3. 保存所有ABAC权限策略

## 6. 后端API接口

所有接口在 `SecurityRoleController` 中，路由前缀 `/security/role`。

### 6.1 角色CRUD

| HTTP方法 | 路径 | 说明 | 入参 | 返回 |
|---------|------|------|------|------|
| POST | /security/role/page | 分页查询 | RoleQueryDTO | R<IPage<RoleVO>> |
| GET | /security/role/{id} | 角色详情 | id | R<RoleVO> |
| GET | /security/role/list | 角色全量列表 | status(可选) | R<List<RoleVO>> |
| POST | /security/role | 新增或更新 | RoleVO | R<String> |
| DELETE | /security/role | 批量删除 | List<String> | R<Boolean> |
| PUT | /security/role/status | 启用/禁用 | id, status | R<Boolean> |

### 6.2 菜单权限配置

| HTTP方法 | 路径 | 说明 | 入参 | 返回 |
|---------|------|------|------|------|
| GET | /security/role/valid-groups/{roleId} | 获取角色时效分组列表（左侧面板） | roleId | R<List<RoleValidGroupVO>> |
| GET | /security/role/menu-tree | 获取完整菜单树（含角色关联状态） | roleId(必填), owner(可选) | R<List<MenuTreeNodeVO>> |
| POST | /security/role/valid-group | 新增或更新时效组（时效+菜单，单一操作） | RoleValidGroupDTO | R<Boolean> |
| DELETE | /security/role/valid-group/{roleMenuId} | 删除时效组 | roleMenuId | R<Boolean> |

### 6.3 VO/DTO

**RoleVO**（新增/更新，api模块）：
- id, roleCode, roleName, description, sort, roleType, dataScope, status

**RoleQueryDTO**（分页查询，api模块，继承BaseDTO）：
- roleName（模糊）, roleType, dataScope, status

**RoleValidGroupVO**（时效分组，左侧面板）：
- roleMenuId: String（security_role_menu的id，用于编辑/删除）
- validType: Integer（1=永久，2=绝对时间范围，3=周期性）
- validStart, validEnd: LocalDateTime（绝对时间）
- cycleType: Integer（1=按周，2=按月）
- cycleValue: String（周期值）
- cycleStartTime, cycleEndTime: LocalTime（周期时间段）
- menuCount: Integer（该组关联的菜单/按钮数量）
- menuIds: List<String>（该组关联的菜单ID列表，用于右侧菜单树回显勾选状态）

**MenuTreeNodeVO**（菜单树节点）：
- id, parentId, name, type（目录/菜单/按钮）, owner, position, icon
- disabled: Boolean（是否已被其他时效组关联，用于菜单互斥）
- boundRoleMenuId: String（已关联的时效组ID，用于互斥时标记"已配置"）

**RoleValidGroupDTO**（新增/更新时效组）：
- roleMenuId: String（更新时传入，新增时为空）
- roleId: String
- validType: Integer
- validStart, validEnd: LocalDateTime
- cycleType: Integer
- cycleValue: String
- cycleStartTime, cycleEndTime: LocalTime
- menuIds: List<String>（关联的菜单/按钮ID列表）

### 6.4 错误码（SecurityErrorCode）

| 编码 | 说明 |
|------|------|
| E02001 | 角色不存在 |
| E02002 | 角色编码已存在 |
| E02003 | 系统角色不可删除 |
| E02004 | 角色已禁用 |
| E02005 | 时效配置无效（如开始时间大于结束时间） |

### 6.5 规范对齐

- 保存和更新合并为 `POST /security/role` 的 `saveOrUpdate` 方法
- 新增时必填字段使用 `AssertUtils` 验证
- 分页查询使用 DTO 继承 BaseDTO
- 所有返回统一使用 `R<T>`
- 路由前缀使用模块prefix `security`

## 7. 前端设计

### 7.1 角色列表页

**布局**：纯表格 + 抽屉详情

- 搜索栏：角色名称、角色类型、数据范围、状态
- 操作栏：新增按钮
- 表格列：序号、角色编码、角色名称、角色类型（Tag）、数据范围、状态（Tag）、操作
- 操作列：详情、更多（下拉：编辑、菜单权限、字段权限、表模型权限）
- 点击"详情"：右侧滑出抽屉，只展示基本信息（角色编码、名称、描述、类型、数据范围、状态、创建时间等）

**角色类型Tag**：系统角色=蓝色，业务角色=橙色
**状态Tag**：启用=绿色，禁用=红色

### 7.2 菜单权限配置

**入口**：点击"更多 > 菜单权限"

**布局**：左右格局

**左侧 - 时效分组列表**：
- 按相同时效配置值分组（如"永久"、"2026-05-01 08:00 ~ 2026-12-31 18:00"、"每周一至周五 09:00~18:00"）
- 每组显示：时效信息 + 菜单/按钮数量
- 顶部："+ 新增时效组"按钮
- 选中某组时高亮，右侧展示该组关联的菜单

**右侧 - 菜单树 + 时效编辑**：
- 顶部Tab：按MenuOwner分「后端管理 | 移动端APP」
- 菜单树：所有菜单和按钮，MenuPosition通过标签区分（蓝色=侧边栏，橙色=顶部栏）
- 时效信息区：展示当前组的时效配置，可点击"编辑时效"进入编辑模式

**交互流程**：

1. **查看模式**：左侧点击时效组 → 右侧显示该组关联的菜单树（已勾选）+ 时效信息（只读）
2. **编辑时效**：点击"编辑时效" → 时效信息区变为可编辑 + 菜单树可勾选/取消 → 保存
3. **新增时效组**：点击"+ 新增时效组" → 左侧新增一项"未配置" → 右侧进入编辑模式 → 配置时效 + 勾选菜单
4. **删除时效组**：点击"删除此组" → 确认后删除，关联的菜单自动释放
5. **菜单互斥**：一个菜单同一时间只能属于一个时效组，在其他组中自动禁用（灰色+已配置标记）
6. **Tab切换**：时效组是跨MenuOwner的，切换Tab只切换右侧菜单树展示，勾选状态跨Tab保持

### 7.3 占位功能

- **字段权限**：行内按钮可见，点击后提示"功能开发中"
- **表模型权限**：行内按钮可见，点击后提示"功能开发中"

### 7.4 前端文件结构

```
apps/gwsu-sub-security/src/pages/role/
├── components/
│   ├── RoleDetail/              # 角色详情抽屉
│   │   └── index.tsx
│   ├── RoleFormModal/           # 新增/编辑角色弹窗
│   │   └── index.tsx
│   └── MenuPermissionModal/     # 菜单权限配置（左右布局）
│       ├── index.tsx            # 主组件
│       ├── ValidGroupList.tsx   # 左侧时效分组列表
│       ├── MenuTreePanel.tsx    # 右侧菜单树面板
│       └── ValidConfigForm.tsx  # 时效配置表单
├── hooks/
│   └── useRole.ts               # 角色相关hooks
├── services/
│   └── role.ts                  # API服务
├── types/
│   └── index.ts                 # 类型定义
└── index.tsx                    # 角色管理页面
```

### 7.5 路由配置

在 `apps/gwsu-sub-security/config/routes.ts` 中新增：
```typescript
{ path: '/role', component: '@/pages/role' },
```

## 8. 数据范围与数据权限配置的关系

### 8.1 层次关系

```
角色数据范围 (粗粒度，角色级)
    ↓ 作为默认策略
数据权限配置 (细粒度，表/字段级)
    ↓ 可覆盖
SQL执行时自动拼接WHERE条件
```

### 8.2 数据范围级别

| 数值 | 含义 | 默认过滤策略 |
|------|------|-------------|
| 0 | 自定义 | 完全依赖数据权限配置 |
| 1 | 全部数据 | 不添加WHERE条件 |
| 2 | 本部门及以下 | dept_id及子部门过滤 |
| 3 | 本部门 | dept_id过滤 |
| 4 | 仅本人 | creator_id过滤 |

### 8.3 多角色生效规则

**自定义覆盖优先原则**：预设范围取宽，但自定义配置是"明确声明"，优先级高于预设。

```
生效过滤 =
  如果该表有数据权限配置 → 使用配置（多角色间取宽）
  否则 → 使用所有角色中最低的预设scope值（min(scope)）
```

示例：用户有角色A（scope=3）和角色B（scope=0，自定义）：
- 没有数据权限配置的表 → 按 scope=3 本部门过滤
- 有数据权限配置"仅本人"的工资表 → 仅本人，scope=3 不覆盖
- 有数据权限配置"全部数据"的日志表 → 全部数据
