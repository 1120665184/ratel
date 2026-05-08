# 角色关联用户功能 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在角色管理列表"更多"菜单下新增"关联用户"功能，支持查看已关联用户和通过穿梭框增减关联用户。

**Architecture:** 后端新增查询角色关联用户ID列表的API接口；前端新建 RelatedUserModal 组件，使用 Ant Design Transfer 穿梭框展示用户列表并支持搜索，保存时复用已有分配接口。

**Tech Stack:** Spring Boot 4 / MyBatis Plus（后端），React 18 / Ant Design 6 / TypeScript（前端）

---

## 文件结构

| 操作 | 文件路径 | 职责 |
|------|----------|------|
| 修改 | `business/business-security/business-security-server/src/main/java/org/quyq/gwsu/security/role/service/ISecurityRoleService.java` | 新增 `listSubjectIdsByRoleId` 方法声明 |
| 修改 | `business/business-security/business-security-server/src/main/java/org/quyq/gwsu/security/role/service/impl/SecurityRoleServiceImpl.java` | 实现 `listSubjectIdsByRoleId` 方法 |
| 修改 | `business/business-security/business-security-server/src/main/java/org/quyq/gwsu/security/role/controller/SecurityRoleController.java` | 新增 `GET /role/{roleId}/subjects` 端点 |
| 修改 | `web/apps/gwsu-sub-security/src/pages/role/services/role.ts` | 新增前端API调用函数 |
| 修改 | `web/apps/gwsu-sub-security/src/pages/role/types/index.ts` | 新增用户相关类型定义 |
| 创建 | `web/apps/gwsu-sub-security/src/pages/role/components/RelatedUserModal/index.tsx` | 关联用户穿梭框模态框组件 |
| 创建 | `web/apps/gwsu-sub-security/src/pages/role/components/RelatedUserModal/index.module.less` | 模态框样式 |
| 修改 | `web/apps/gwsu-sub-security/src/pages/role/index.tsx` | 集成"关联用户"菜单项和模态框 |

---

### Task 1: 后端 — Service 层新增查询方法

**Files:**
- Modify: `business/business-security/business-security-server/src/main/java/org/quyq/gwsu/security/role/service/ISecurityRoleService.java`
- Modify: `business/business-security/business-security-server/src/main/java/org/quyq/gwsu/security/role/service/impl/SecurityRoleServiceImpl.java`

- [ ] **Step 1: 在 ISecurityRoleService 接口中新增方法声明**

在 `ISecurityRoleService.java` 的 `allocationSubjectToRole` 方法声明之后追加：

```java
    /**
     * 根据角色ID查询关联的主体ID列表
     *
     * @param roleId 角色ID
     * @return 主体ID列表
     */
    List<String> listSubjectIdsByRoleId(String roleId);
```

- [ ] **Step 2: 在 SecurityRoleServiceImpl 中实现方法**

在 `SecurityRoleServiceImpl.java` 的 `allocationSubjectToRole` 方法之后追加：

```java
    @Override
    public List<String> listSubjectIdsByRoleId(String roleId) {
        List<SecurityRoleSubject> list = roleSubjectMapper.selectList(
                new LambdaQueryWrapper<SecurityRoleSubject>()
                        .eq(SecurityRoleSubject::getRoleId, roleId));
        return list.stream()
                .map(SecurityRoleSubject::getSubjectId)
                .toList();
    }
```

- [ ] **Step 3: 编译验证**

Run: `cd /Users/quyq/Documents/work/personal/gwsu-basic && mvn compile -pl business/business-security/business-security-server -am -DskipTests -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: 提交**

```bash
git add business/business-security/business-security-server/src/main/java/org/quyq/gwsu/security/role/service/ISecurityRoleService.java business/business-security/business-security-server/src/main/java/org/quyq/gwsu/security/role/service/impl/SecurityRoleServiceImpl.java
git commit -m "feat(security): 角色Service新增listSubjectIdsByRoleId方法"
```

---

### Task 2: 后端 — Controller 层新增查询端点

**Files:**
- Modify: `business/business-security/business-security-server/src/main/java/org/quyq/gwsu/security/role/controller/SecurityRoleController.java`

- [ ] **Step 1: 新增 Controller 端点**

在 `SecurityRoleController.java` 的 `allocationSubjectToRole` 方法之后追加：

```java
    @Operation(summary = "根据角色ID查询关联的主体ID列表")
    @GetMapping("/{roleId}/subjects")
    public R<List<String>> listSubjectIdsByRoleId(@PathVariable String roleId) {
        return R.ok(roleService.listSubjectIdsByRoleId(roleId));
    }
```

- [ ] **Step 2: 编译验证**

Run: `cd /Users/quyq/Documents/work/personal/gwsu-basic && mvn compile -pl business/business-security/business-security-server -am -DskipTests -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add business/business-security/business-security-server/src/main/java/org/quyq/gwsu/security/role/controller/SecurityRoleController.java
git commit -m "feat(security): 角色Controller新增查询关联主体ID列表端点"
```

---

### Task 3: 前端 — 类型定义和 API 服务函数

**Files:**
- Modify: `web/apps/gwsu-sub-security/src/pages/role/types/index.ts`
- Modify: `web/apps/gwsu-sub-security/src/pages/role/services/role.ts`

**注意：** `page/userInfo` 接口已改为 POST，前端使用 `post` 调用。

- [ ] **Step 1: 在 types/index.ts 末尾新增用户信息类型**

追加以下类型定义：

```ts
/** 用户信息（穿梭框数据源） */
export interface UserInfoItem {
  id: string;
  userName: string;
  nickname: string;
}

/** 用户分页查询参数 */
export interface UserPageQuery {
  search?: string;
  pageNum?: number;
  pageSize?: number;
  orderByColumn?: string;
  asc?: string;
}

/** 用户分页响应 */
export interface UserPageResult {
  records: UserInfoItem[];
  total: number;
  current: number;
  size: number;
}
```

- [ ] **Step 2: 在 services/role.ts 末尾新增 API 函数**

追加以下函数：

```ts
/** 根据角色ID查询关联的主体ID列表 */
export async function getSubjectIdsByRoleId(
  roleId: string,
): Promise<string[]> {
  const res = await get<string[]>(`${BASE}/${roleId}/subjects`);
  return res.data ?? [];
}

/** 给角色分配主体 */
export async function allocateSubjectsToRole(
  roleId: string,
  subjectIds: string[],
): Promise<void> {
  await put<void>(`${BASE}/allocationSubject/${roleId}`, subjectIds);
}

/** 分页查询用户信息（用于穿梭框数据源，接口来自 business-system 模块，POST） */
export async function getUserPage(query: UserPageQuery) {
  const res = await post<UserPageResult>('/system/basic/page/userInfo', query);
  return res.data;
}
```

同时在文件顶部 import 中补充 `UserPageQuery` 和 `UserPageResult`：

将：
```ts
import type {
  RoleInfo,
  RoleQuery,
  ValidGroup,
  MenuTreeNode,
  ValidGroupSaveRequest,
} from '../types';
```

改为：
```ts
import type {
  RoleInfo,
  RoleQuery,
  ValidGroup,
  MenuTreeNode,
  ValidGroupSaveRequest,
  UserPageQuery,
  UserPageResult,
} from '../types';
```

- [ ] **Step 3: 提交**

```bash
git add web/apps/gwsu-sub-security/src/pages/role/types/index.ts web/apps/gwsu-sub-security/src/pages/role/services/role.ts
git commit -m "feat(security): 角色管理前端新增关联用户API和类型定义"
```

---

### Task 4: 前端 — RelatedUserModal 组件

**Files:**
- Create: `web/apps/gwsu-sub-security/src/pages/role/components/RelatedUserModal/index.tsx`
- Create: `web/apps/gwsu-sub-security/src/pages/role/components/RelatedUserModal/index.module.less`

- [ ] **Step 1: 创建样式文件**

创建 `web/apps/gwsu-sub-security/src/pages/role/components/RelatedUserModal/index.module.less`：

```less
.relatedUserModal {
  :global {
    .ant-transfer {
      display: flex;
      align-items: stretch;
    }
  }
}

.transferWrapper {
  display: flex;
  justify-content: center;
  padding: 8px 0;
}
```

- [ ] **Step 2: 创建组件文件**

创建 `web/apps/gwsu-sub-security/src/pages/role/components/RelatedUserModal/index.tsx`：

```tsx
import React, { useState, useEffect, useCallback, useMemo } from 'react';
import { Modal, Transfer, Spin, message } from 'antd';
import type { TransferProps } from 'antd';
import styles from './index.module.less';
import {
  getSubjectIdsByRoleId,
  allocateSubjectsToRole,
  getUserPage,
} from '../../services/role';
import type { UserInfoItem } from '../../types';

interface RelatedUserModalProps {
  visible: boolean;
  roleId: string | null;
  roleName: string;
  onClose: () => void;
}

const RelatedUserModal: React.FC<RelatedUserModalProps> = ({
  visible,
  roleId,
  roleName,
  onClose,
}) => {
  const [allUsers, setAllUsers] = useState<UserInfoItem[]>([]);
  const [targetKeys, setTargetKeys] = useState<string[]>([]);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);

  /** 加载数据 */
  const loadData = useCallback(async () => {
    if (!roleId) return;
    setLoading(true);
    try {
      const [users, subjectIds] = await Promise.all([
        getUserPage({ pageNum: 1, pageSize: 9999 }),
        getSubjectIdsByRoleId(roleId),
      ]);
      setAllUsers(users?.records ?? []);
      setTargetKeys(subjectIds);
    } catch {
      message.error('加载数据失败');
    } finally {
      setLoading(false);
    }
  }, [roleId]);

  useEffect(() => {
    if (visible && roleId) {
      loadData();
    }
  }, [visible, roleId, loadData]);

  /** 穿梭框数据源 */
  const transferDataSource = useMemo(() => {
    return allUsers.map((user) => ({
      key: user.id,
      title: user.nickname || user.userName,
    }));
  }, [allUsers]);

  /** 穿梭框变更 */
  const handleChange: TransferProps['onChange'] = (nextTargetKeys) => {
    setTargetKeys(nextTargetKeys as string[]);
  };

  /** 穿梭框搜索过滤 */
  const filterOption = (
    inputValue: string,
    option: { key?: string; title?: string },
  ) => (option.title ?? '').toLowerCase().includes(inputValue.toLowerCase());

  /** 保存 */
  const handleSave = async () => {
    if (!roleId) return;
    setSaving(true);
    try {
      await allocateSubjectsToRole(roleId, targetKeys);
      message.success('关联用户保存成功');
      onClose();
    } catch {
      // request 层已自动提示
    } finally {
      setSaving(false);
    }
  };

  return (
    <Modal
      title={`关联用户 - ${roleName}`}
      open={visible}
      onCancel={onClose}
      onOk={handleSave}
      confirmLoading={saving}
      okText="保存"
      cancelText="取消"
      width={720}
      destroyOnHidden
      className={styles.relatedUserModal}
    >
      <Spin spinning={loading}>
        <div className={styles.transferWrapper}>
          <Transfer
            dataSource={transferDataSource}
            targetKeys={targetKeys}
            onChange={handleChange}
            filterOption={filterOption}
            showSearch
            titles={['未关联', '已关联']}
            listStyle={{ width: 300, height: 400 }}
            oneWay={false}
            render={(item) => item.title ?? ''}
          />
        </div>
      </Spin>
    </Modal>
  );
};

export default RelatedUserModal;
```

- [ ] **Step 3: 提交**

```bash
git add web/apps/gwsu-sub-security/src/pages/role/components/RelatedUserModal/
git commit -m "feat(security): 新增RelatedUserModal穿梭框组件"
```

---

### Task 5: 前端 — 集成到角色列表页

**Files:**
- Modify: `web/apps/gwsu-sub-security/src/pages/role/index.tsx`

- [ ] **Step 1: 导入 RelatedUserModal 组件和图标**

在文件顶部 import 区域，在 `import MenuPermissionModal from "./components/MenuPermissionModal";` 之后追加：

```tsx
import RelatedUserModal from "./components/RelatedUserModal";
```

在 `@ant-design/icons` 的导入中追加 `UserOutlined`：

将：
```tsx
import {
  PlusOutlined,
  DeleteOutlined,
  SearchOutlined,
  ReloadOutlined,
  MoreOutlined,
  EyeOutlined,
  EditOutlined,
  MenuOutlined,
  LockOutlined,
  TableOutlined,
} from "@ant-design/icons";
```

改为：
```tsx
import {
  PlusOutlined,
  DeleteOutlined,
  SearchOutlined,
  ReloadOutlined,
  MoreOutlined,
  EyeOutlined,
  EditOutlined,
  MenuOutlined,
  LockOutlined,
  TableOutlined,
  UserOutlined,
} from "@ant-design/icons";
```

- [ ] **Step 2: 新增关联用户模态框状态**

在 `menuPermRoleName` 状态声明之后追加：

```tsx
  // 关联用户弹窗
  const [relatedUserVisible, setRelatedUserVisible] = useState(false);
  const [relatedUserRoleId, setRelatedUserRoleId] = useState<string | null>(null);
  const [relatedUserRoleName, setRelatedUserRoleName] = useState<string>("");
```

- [ ] **Step 3: 新增关联用户处理函数**

在 `handleMenuPermission` 函数之后追加：

```tsx
  /** 关联用户 */
  const handleRelatedUser = useCallback((role: RoleInfo) => {
    setRelatedUserRoleId(role.id ?? null);
    setRelatedUserRoleName(role.roleName);
    setRelatedUserVisible(true);
  }, []);
```

- [ ] **Step 4: 在"更多"下拉菜单中新增"关联用户"选项**

在表格操作列的 `Dropdown` 菜单 `items` 数组中，在 `menuPermission` 项之后插入：

```tsx
                {
                  key: "relatedUser",
                  icon: <UserOutlined />,
                  label: "关联用户",
                  onClick: () => handleRelatedUser(record),
                },
```

完整的 items 数组应为：
```tsx
              items: [
                {
                  key: "edit",
                  icon: <EditOutlined />,
                  label: "编辑",
                  onClick: () => handleEdit(record),
                },
                {
                  key: "menuPermission",
                  icon: <MenuOutlined />,
                  label: "菜单权限",
                  onClick: () => handleMenuPermission(record),
                },
                {
                  key: "relatedUser",
                  icon: <UserOutlined />,
                  label: "关联用户",
                  onClick: () => handleRelatedUser(record),
                },
                {
                  key: "fieldPermission",
                  icon: <LockOutlined />,
                  label: "字段权限",
                  onClick: () => handlePlaceholder("字段权限"),
                },
                {
                  key: "tablePermission",
                  icon: <TableOutlined />,
                  label: "表模型权限",
                  onClick: () => handlePlaceholder("表模型权限"),
                },
              ],
```

- [ ] **Step 5: 在 JSX 中渲染 RelatedUserModal**

在 `MenuPermissionModal` 组件之后追加：

```tsx
      {/* 关联用户弹窗 */}
      <RelatedUserModal
        visible={relatedUserVisible}
        roleId={relatedUserRoleId}
        roleName={relatedUserRoleName}
        onClose={() => setRelatedUserVisible(false)}
      />
```

- [ ] **Step 6: 提交**

```bash
git add web/apps/gwsu-sub-security/src/pages/role/index.tsx
git commit -m "feat(security): 角色列表更多菜单新增关联用户功能"
```

---

### Task 6: 前端构建验证

- [ ] **Step 1: 构建前端项目验证无编译错误**

Run: `cd /Users/quyq/Documents/work/personal/gwsu-basic/web && pnpm build:core && pnpm build:sub-security`
Expected: 构建成功，无 TypeScript 错误

- [ ] **Step 2: 最终提交（如有构建修复）**

如有修复则提交，否则跳过。
