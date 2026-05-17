# 八、列表页开发模式

列表页是后台管理系统最常见的页面类型，包含搜索栏、表格和弹窗三个核心区域。本文档基于项目中角色管理、数据权限管理等实际页面总结开发规范。

## 8.1 页面布局结构

列表页统一采用 `flex` 纵向布局，分为搜索栏和表格区域两部分：

```
┌─────────────────────────────────┐
│           搜索栏 (searchBar)      │  ← 固定高度，flex-wrap
├─────────────────────────────────┤
│  表格标题 + 操作按钮 (tableHeader) │  ← 固定高度
├─────────────────────────────────┤
│                                 │
│           表格内容 (Table)        │  ← flex: 1，自适应剩余高度
│                                 │
└─────────────────────────────────┘
```

对应的样式结构（`index.module.less`）：

```less
.xxxPage {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  background: var(--background-color);
  padding: 16px;
  gap: 12px;
}

.searchBar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  padding: 16px;
  background: var(--surface-color);
  border-radius: 6px;
  border: 1px solid var(--border-color);
}

.searchItem {
  display: flex;
  align-items: center;
  gap: 8px;
}

.searchLabel {
  font-size: 13px;
  color: var(--text-secondary-color);
  white-space: nowrap;
}

.searchActions {
  display: flex;
  gap: 8px;
  margin-left: auto;  // 搜索和重置按钮靠右
}

.tableWrapper {
  flex: 1;
  min-height: 0;
  background: var(--surface-color);
  border-radius: 6px;
  border: 1px solid var(--border-color);
  overflow: hidden;
}

.tableHeader {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid var(--border-color);
}

.tableTitle {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-color);
}

.actionColumn {
  display: flex;
  align-items: center;
  gap: 4px;
}
```

## 8.2 表格操作列规范

操作列是列表页的核心交互区域，统一采用 **"详情" + "更多"下拉菜单** 的模式：

| 位置 | 按钮 | 说明 |
|------|------|------|
| 平铺 | 详情 | 高频操作，始终直接展示 |
| 下拉菜单 | 编辑、关联用户、菜单权限等 | 低频或权限受限的操作，放入"更多"下拉 |

**操作列必须固定在右侧**，设置 `fixed: "right"`，宽度根据操作数量设为 `160~200`。

### 完整代码示例

```tsx
import { Dropdown, type MenuProps } from 'antd';
import { EyeOutlined, EditOutlined, MoreOutlined, UserOutlined } from '@ant-design/icons';
import { AuthGate, useAuth } from '@gwsu/core';

const SomePage: React.FC = () => {
  // 用 useAuth 获取操作列下拉菜单项的权限
  const canEdit = useAuth('xxx_edit');
  const canAssociateUser = useAuth('xxx_association_user');

  /** 获取更多下拉菜单项 */
  const getButtonItem = (record: SomeInfo): MenuProps['items'] => {
    const buttons = [];
    if (canEdit) {
      buttons.push({
        key: 'edit',
        icon: <EditOutlined />,
        label: '编辑',
        onClick: () => handleEdit(record),
      });
    }
    if (canAssociateUser) {
      buttons.push({
        key: 'relatedUser',
        icon: <UserOutlined />,
        label: '关联用户',
        onClick: () => handleRelatedUser(record),
      });
    }
    return buttons;
  };

  const columns: TableProps<SomeInfo>['columns'] = [
    // ... 其他列
    {
      title: '操作',
      width: 200,
      fixed: 'right',
      render: (_: unknown, record: SomeInfo) => (
        <div className={styles.actionColumn}>
          <Button
            type="link"
            size="small"
            icon={<EyeOutlined />}
            onClick={() => handleViewDetail(record)}
          >
            详情
          </Button>
          <Dropdown
            menu={{ items: getButtonItem(record) }}
            disabled={getButtonItem(record).length === 0}
          >
            <Button
              type="link"
              size="small"
              icon={<MoreOutlined />}
              disabled={getButtonItem(record).length === 0}
            >
              更多
            </Button>
          </Dropdown>
        </div>
      ),
    },
  ];
};
```

> **注意**：当所有菜单项都被权限隐藏时，`getButtonItem` 返回空数组，此时"更多"按钮应 `disabled`。

## 8.3 按钮权限控制

项目中按钮权限基于后端菜单树中 `menuType=3` 的按钮节点，通过 `buttonKey`（按钮的 `id`）进行匹配。有两种控制方式：

| 方式 | 适用场景 | 特点 |
|------|---------|------|
| `AuthGate` 组件 | 表格头部的新增/删除/同步等独立按钮 | 无权限时整个按钮不渲染 |
| `useAuth` hook | 操作列"更多"下拉菜单项 | 无权限时不添加菜单项，下拉自动隐藏 |

### 权限标识常量

按钮权限标识（`buttonKey`）在开发阶段可能未确定，后续需要修改。**必须将权限标识定义为常量**，集中放在页面目录的 `permissionConstants.ts` 文件中，禁止在代码中硬编码字符串。

**文件位置**：`src/pages/{页面目录}/permissionConstants.ts`

```typescript
/** 数据权限管理 - 按钮权限标识常量 */

/** 表格头部操作权限 */
export const PERM_ADD = '72974723_add';
export const PERM_REMOVE = '72974723_remove';
export const PERM_SYNC = '72974723_sync';

/** 操作列下拉菜单 / 状态切换权限 */
export const PERM_EDIT = '72974723_edit';
```

**使用方式**：

```tsx
import { PERM_ADD, PERM_REMOVE, PERM_EDIT } from './permissionConstants';

// useAuth
const canEdit = useAuth(PERM_EDIT);

// AuthGate
<AuthGate buttonKey={PERM_ADD}>
  <Button type="primary">新增</Button>
</AuthGate>

<AuthGate buttonKey={PERM_REMOVE}>
  <Popconfirm onConfirm={handleBatchDelete}>
    <Button danger data-ai-approval>删除</Button>
  </Popconfirm>
</AuthGate>
```

> **命名规范**：常量以 `PERM_` 为前缀，使用大写蛇形命名（如 `PERM_ADD`、`PERM_ASSOCIATION_USER`）。按功能分组注释，表格头部操作和操作列权限分开。

### AuthGate — 表格头部按钮

用于表格顶部 `tableHeader` 中的操作按钮，无权限时按钮完全不显示：

```tsx
<Space>
  <AuthGate buttonKey="xxx_add">
    <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>
      新增
    </Button>
  </AuthGate>
  <AuthGate buttonKey="xxx_remove">
    <Popconfirm
      title="批量删除"
      description={`确定删除选中的 ${selectedRowKeys.length} 条记录？`}
      onConfirm={handleBatchDelete}
    >
      <Button danger icon={<DeleteOutlined />} data-ai-approval disabled={selectedRowKeys.length === 0}>
        删除
      </Button>
    </Popconfirm>
  </AuthGate>
</Space>
```

> **注意**：`AuthGate` 包裹的是最外层元素。如果按钮外层有 `Popconfirm`，`AuthGate` 应包裹 `Popconfirm`。

### useAuth — 操作列下拉菜单项

用于操作列"更多"下拉菜单中的动态菜单项，根据权限决定是否添加：

```tsx
const canEdit = useAuth('xxx_edit');
const canDelete = useAuth('xxx_delete');

const getButtonItem = (record: SomeInfo): NonNullable<MenuProps['items']> => {
  const buttons = [];
  if (canEdit) {
    buttons.push({
      key: 'edit',
      icon: <EditOutlined />,
      label: '编辑',
      onClick: () => handleEdit(record),
    });
  }
  if (canDelete) {
    buttons.push({
      key: 'delete',
      icon: <DeleteOutlined />,
      label: '删除',
      onClick: () => handleDelete(record),
    });
  }
  return buttons;
};
```

### AuthGate — 状态切换开关

表格中的状态 Switch 也需要权限控制，用 `AuthGate` 包裹 Switch，无权限时 Switch 不显示（仅显示状态 Tag）：

```tsx
{
  title: '状态',
  dataIndex: 'status',
  width: 120,
  render: (val: boolean, record: SomeInfo) => (
    <Space>
      <AuthGate buttonKey="xxx_edit">
        <Switch
          size="small"
          data-ai-approval
          checked={val}
          onChange={(checked) => handleStatusChange(record, checked)}
        />
      </AuthGate>
      <Tag color={val ? 'green' : 'red'}>{val ? '启用' : '禁用'}</Tag>
    </Space>
  ),
},
```

## 8.4 data-ai-approval 在列表页中的使用

`data-ai-approval` 标记用于标识 AI 自动操作时需要人工审批的危险操作。**核心判断标准**：按钮点击后是否**立即触发后端数据变更**。需要则加，不需要则不加。

### 列表页中的判断表

| 元素 | 是否需要 data-ai-approval | 原因 |
|------|--------------------------|------|
| 表格头部"新增"按钮 | **不需要** | 仅打开空表单弹窗，尚未提交数据 |
| 表格头部"删除"按钮 | **需要** | 批量删除，数据不可逆 |
| 表格头部"同步"按钮 | **需要** | 触发后端数据同步操作 |
| 操作列"详情"按钮 | **不需要** | 纯查看，不修改数据 |
| 操作列"更多>编辑" | **不需要** | 仅打开编辑弹窗，尚未提交 |
| 状态 Switch | **需要** | 直接修改后端数据状态 |
| 弹窗中的"保存/确定"按钮 | **需要** | 表单提交，数据持久化 |
| 弹窗中的"取消"按钮 | **不需要** | 仅关闭弹窗，无数据变更 |

### 代码示例

```tsx
{/* 需要：点击即删除 */}
<Button danger data-ai-approval icon={<DeleteOutlined />}>删除</Button>

{/* 需要：点击即修改状态 */}
<Switch size="small" data-ai-approval checked={val} onChange={handleChange} />

{/* 不需要：仅打开空表单 */}
<Button type="primary" icon={<PlusOutlined />}>新增</Button>

{/* 不需要：仅查看详情 */}
<Button type="link" icon={<EyeOutlined />}>详情</Button>
```

### 通用判断规则

| 分类 | 是否需要 | 示例 |
|------|---------|------|
| 保存/提交 | 需要 | 保存、提交、确认（表单数据写入后端） |
| 删除 | 需要 | 删除、移除、清空（数据不可逆） |
| 状态变更 | 需要 | Switch 切换、审核通过/驳回（直接修改后端数据） |
| 同步/导入导出 | 需要 | 同步到 Redis、导入、导出（触发后端批量操作） |
| 权限变更 | 需要 | 授权、分配权限（安全敏感操作） |
| 纯查看 | 不需要 | 详情、预览、查看（不修改数据） |
| 导航跳转 | 不需要 | 跳转、返回、切换 Tab（仅路由变化） |
| 打开弹窗 | 不需要 | 新增按钮（打开空表单）、编辑按钮（打开编辑弹窗） |
| 搜索筛选 | 不需要 | 搜索、筛选、翻页（只读查询） |
| UI 切换 | 不需要 | 展开、折叠、收起（UI 状态变化） |

## 8.5 列表页完整模板

以下是一个完整的列表页模板，涵盖了搜索栏、操作列、权限控制和审批标记：

```tsx
import React, { useState, useCallback, useEffect } from 'react';
import {
  Button, Table, Input, Select, Tag, Switch, Dropdown,
  Form, Space, Popconfirm, type MenuProps,
} from 'antd';
import type { TableProps } from 'antd';
import {
  PlusOutlined, DeleteOutlined, SearchOutlined, ReloadOutlined,
  EyeOutlined, EditOutlined, MoreOutlined,
} from '@ant-design/icons';
import styles from './index.module.less';
import XxxDetail from './components/XxxDetail';
import XxxFormModal from './components/XxxFormModal';
import { useXxx } from './hooks/useXxx';
import type { XxxInfo, XxxQuery } from './types';
import { AuthGate, useAuth } from '@gwsu/core';

const STATUS_OPTIONS = [
  { label: '启用', value: true },
  { label: '禁用', value: false },
];

const XxxPage: React.FC = () => {
  const {
    loading, dataSource, total, currentPage, pageSize,
    fetchXxxPage, ensureInitialized, handlePageChange,
    handleSaveOrUpdate, handleDelete,
  } = useXxx();

  // 操作列下拉菜单权限
  const canEdit = useAuth('xxx_edit');

  const [searchForm] = Form.useForm<XxxQuery>();

  // 详情抽屉
  const [detailVisible, setDetailVisible] = useState(false);
  const [detailData, setDetailData] = useState<XxxInfo | null>(null);

  // 新增/编辑弹窗
  const [formModalVisible, setFormModalVisible] = useState(false);
  const [formModalMode, setFormModalMode] = useState<'create' | 'edit'>('create');
  const [formModalData, setFormModalData] = useState<XxxInfo | null>(null);

  // 表格选中行
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([]);

  useEffect(() => { ensureInitialized(); }, [ensureInitialized]);

  const handleSearch = useCallback(() => {
    const values = searchForm.getFieldsValue();
    fetchXxxPage({ ...values, pageNum: 1 });
  }, [searchForm, fetchXxxPage]);

  const handleReset = useCallback(() => {
    searchForm.resetFields();
    fetchXxxPage({ pageNum: 1 });
  }, [searchForm, fetchXxxPage]);

  const handleViewDetail = useCallback((record: XxxInfo) => {
    setDetailData(record);
    setDetailVisible(true);
  }, []);

  const handleCreate = useCallback(() => {
    setFormModalMode('create');
    setFormModalData(null);
    setFormModalVisible(true);
  }, []);

  const handleEdit = useCallback((record: XxxInfo) => {
    setFormModalMode('edit');
    setFormModalData(record);
    setFormModalVisible(true);
  }, []);

  const handleSave = useCallback(
    async (data: XxxInfo): Promise<boolean> => handleSaveOrUpdate(data),
    [handleSaveOrUpdate],
  );

  const handleStatusChange = useCallback(
    async (record: XxxInfo, checked: boolean) => {
      await handleSaveOrUpdate({ ...record, status: checked });
    },
    [handleSaveOrUpdate],
  );

  const handleBatchDelete = useCallback(async () => {
    const ids = selectedRowKeys as string[];
    const success = await handleDelete(ids);
    if (success) { setSelectedRowKeys([]); }
  }, [selectedRowKeys, handleDelete]);

  /** 获取更多下拉菜单项 */
  const getButtonItem = (record: XxxInfo): MenuProps['items'] => {
    const buttons = [];
    if (canEdit) {
      buttons.push({
        key: 'edit',
        icon: <EditOutlined />,
        label: '编辑',
        onClick: () => handleEdit(record),
      });
    }
    return buttons;
  };

  const columns: TableProps<XxxInfo>['columns'] = [
    Table.SELECTION_COLUMN,
    {
      title: '序号',
      width: 60,
      align: 'center',
      render: (_: unknown, __: XxxInfo, index: number) =>
        (currentPage - 1) * pageSize + index + 1,
    },
    // ... 业务列
    {
      title: '状态',
      dataIndex: 'status',
      width: 120,
      render: (val: boolean, record: XxxInfo) => (
        <Space>
          <AuthGate buttonKey="xxx_edit">
            <Switch
              size="small"
              data-ai-approval
              checked={val}
              onChange={(checked) => handleStatusChange(record, checked)}
            />
          </AuthGate>
          <Tag color={val ? 'green' : 'red'}>{val ? '启用' : '禁用'}</Tag>
        </Space>
      ),
    },
    {
      title: '操作',
      width: 200,
      fixed: 'right',
      render: (_: unknown, record: XxxInfo) => (
        <div className={styles.actionColumn}>
          <Button
            type="link"
            size="small"
            icon={<EyeOutlined />}
            onClick={() => handleViewDetail(record)}
          >
            详情
          </Button>
          <Dropdown
            menu={{ items: getButtonItem(record) }}
            disabled={getButtonItem(record).length === 0}
          >
            <Button
              type="link"
              size="small"
              icon={<MoreOutlined />}
              disabled={getButtonItem(record).length === 0}
            >
              更多
            </Button>
          </Dropdown>
        </div>
      ),
    },
  ];

  return (
    <div className={styles.xxxPage}>
      {/* 搜索栏 */}
      <div className={styles.searchBar}>
        <Form form={searchForm} layout="inline" component={false}>
          <div className={styles.searchItem}>
            <span className={styles.searchLabel}>名称</span>
            <Form.Item name="name" noStyle>
              <Input placeholder="请输入" allowClear style={{ width: 180 }} onPressEnter={handleSearch} />
            </Form.Item>
          </div>
        </Form>
        <div className={styles.searchActions}>
          <Button type="primary" icon={<SearchOutlined />} onClick={handleSearch}>查询</Button>
          <Button icon={<ReloadOutlined />} onClick={handleReset}>重置</Button>
        </div>
      </div>

      {/* 表格区域 */}
      <div className={styles.tableWrapper}>
        <div className={styles.tableHeader}>
          <span className={styles.tableTitle}>XXX 列表</span>
          <Space>
            <AuthGate buttonKey="xxx_add">
              <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>新增</Button>
            </AuthGate>
            <AuthGate buttonKey="xxx_remove">
              <Popconfirm
                title="批量删除"
                description={`确定删除选中的 ${selectedRowKeys.length} 条记录？`}
                onConfirm={handleBatchDelete}
                disabled={selectedRowKeys.length === 0}
              >
                <Button danger data-ai-approval icon={<DeleteOutlined />} disabled={selectedRowKeys.length === 0}>
                  删除
                </Button>
              </Popconfirm>
            </AuthGate>
          </Space>
        </div>
        <Table<XxxInfo>
          rowKey="id"
          rowSelection={{ selectedRowKeys, onChange: setSelectedRowKeys }}
          columns={columns}
          dataSource={dataSource}
          loading={loading}
          size="middle"
          scroll={{ x: 960 }}
          pagination={{
            current: currentPage, pageSize, total,
            showSizeChanger: true, showQuickJumper: true,
            showTotal: (t) => `共 ${t} 条`,
            onChange: handlePageChange,
          }}
        />
      </div>

      {/* 详情抽屉 */}
      <XxxDetail visible={detailVisible} data={detailData} onClose={() => setDetailVisible(false)} />

      {/* 新增/编辑弹窗 */}
      <XxxFormModal
        visible={formModalVisible} mode={formModalMode} data={formModalData}
        onSave={handleSave} onClose={() => setFormModalVisible(false)}
        onSuccess={() => setFormModalVisible(false)}
      />
    </div>
  );
};

export default XxxPage;
```
