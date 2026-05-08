# 角色关联用户功能设计

## 概述

在角色管理列表的"更多"操作下新增"关联用户"功能，支持查看角色已关联的用户，并通过穿梭框增减关联用户。

## 后端设计

### 新增接口：根据角色ID查询关联用户ID列表

- **端点**：`GET /security/role/{roleId}/subjects`
- **返回**：`R<List<String>>` — 已关联的用户ID列表
- **实现**：
  - `ISecurityRoleService` 新增 `listSubjectIdsByRoleId(String roleId)` 方法
  - `SecurityRoleServiceImpl` 通过 `SecurityRoleSubjectMapper` 查询 `security_role_subject` 表中 `roleId` 对应的所有 `subjectId`

### 复用已有接口

- **分配用户给角色**：`PUT /security/role/allocationSubject/{roleId}`（覆盖式，传入用户ID列表）
- **用户分页查询**：`GET /page/userInfo`（BasicController.userPage，获取全量用户数据源）

## 前端设计

### 新建组件：RelatedUserModal

**位置**：`web/apps/gwsu-sub-security/src/pages/role/components/RelatedUserModal/`

**功能**：
1. 模态框打开时加载数据：
   - 调用 `userPage` 获取用户列表作为穿梭框左侧数据源
   - 调用 `GET /security/role/{roleId}/subjects` 获取已关联用户ID，设置为穿梭框右侧（targetKeys）
2. 穿梭框展示：每项仅显示用户名，支持搜索过滤
3. 用户操作穿梭框增减关联后，点确认调用 `PUT /security/role/allocationSubject/{roleId}` 保存

### 服务层新增

在 `role/services/role.ts` 中新增：
- `getSubjectIdsByRoleId(roleId)` — 查询角色关联用户ID列表
- `allocateSubjectsToRole(roleId, subjectIds)` — 给角色分配用户

在 `role/types/index.ts` 中新增相关类型（如需要）。

### 列表页集成

在角色列表页 `role/index.tsx` 的"更多"下拉菜单中，在"菜单权限配置"之后新增"关联用户"选项，点击后打开 `RelatedUserModal`。

## 数据流

```
打开模态框 → 加载用户列表(userPage) + 加载已关联用户ID(subjects)
         → 穿梭框展示(左侧可选 | 右侧已关联)
         → 用户操作穿梭框
         → 点确认 → 调用 allocationSubject 保存
```

## 约束

- userPage 接口在 business-system 模块中，角色管理在 business-security 模块中，需通过 ClientApi 跨模块调用
- 分配接口为覆盖式，保存时传入右侧所有用户ID即可
