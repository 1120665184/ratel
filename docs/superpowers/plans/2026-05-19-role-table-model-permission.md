# 角色表模型权限配置 - 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在角色管理页面实现表模型权限配置弹窗，支持左右分栏布局，左侧表列表、右侧字段权限配置

**Architecture:** 新建 TableModelPermissionModal 组件目录，遵循项目已有的组件拆分模式（参考 MenuPermissionModal）。数据逻辑抽取到 hook，API 调用添加到 services/role.ts，类型定义添加到 types/index.ts。样式使用 CSS Modules (.module.less)。

**Tech Stack:** React 18 + Ant Design 6 + TypeScript + CSS Modules

---

## 文件结构

| 操作 | 文件路径 | 职责 |
|------|---------|------|
| 修改 | `web/apps/gwsu-sub-security/src/pages/role/types/index.ts` | 新增表模型权限相关类型 |
| 修改 | `web/apps/gwsu-sub-security/src/pages/role/services/role.ts` | 新增 3 个 API 函数 |
| 创建 | `web/apps/gwsu-sub-security/src/pages/role/components/TableModelPermissionModal/types.ts` | 弹窗内部类型定义 |
| 创建 | `web/apps/gwsu-sub-security/src/pages/role/components/TableModelPermissionModal/hooks/useTableModelPermission.ts` | 数据逻辑 hook |
| 创建 | `web/apps/gwsu-sub-security/src/pages/role/components/TableModelPermissionModal/AddTableModelModal.tsx` | 新增模型权限弹框 |
| 创建 | `web/apps/gwsu-sub-security/src/pages/role/components/TableModelPermissionModal/TableListPanel.tsx` | 左侧表列表面板 |
| 创建 | `web/apps/gwsu-sub-security/src/pages/role/components/TableModelPermissionModal/FieldConfigTable.tsx` | 右侧字段配置表格 |
| 创建 | `web/apps/gwsu-sub-security/src/pages/role/components/TableModelPermissionModal/index.module.less` | 样式文件 |
| 创建 | `web/apps/gwsu-sub-security/src/pages/role/components/TableModelPermissionModal/index.tsx` | 主弹窗组件 |
| 修改 | `web/apps/gwsu-sub-security/src/pages/role/index.tsx` | 接入弹窗，替换占位逻辑 |

---

### Task 1: 新增类型定义

**Files:**
- 修改: `web/apps/gwsu-sub-security/src/pages/role/types/index.ts`

- [ ] **Step 1: 在 types/index.ts 末尾添加表模型权限相关类型**

在文件末尾追加：

```typescript
/** 字段配置项 */
export interface FieldConfigItem {
  fieldName: string;
  show: boolean;
  desensitize: boolean;
  strategy: string;
  prefixNoMaskLen?: number;
  suffixNoMaskLen?: number;
  symbol?: string;
}

/** 列信息 */
export interface ColumnInfo {
  columnName: string;
  columnComment: string;
  fixedFieldConfig?: FieldConfigItem;
  customFieldConfig?: FieldConfigItem;
}

/** 角色表模型权限 VO */
export interface RolePermissionTableModelVO {
  type: number;
  tableModelId: string;
  id?: string;
  modulePrefix: string;
  datasource: string;
  tableName: string;
  tableComment: string;
  columns: ColumnInfo[];
}

/** 角色表模型权限保存 DTO */
export interface RoleTableModelSaveDTO {
  id?: string;
  roleId: string;
  modulePrefix: string;
  tableName: string;
  datasource: string;
  fields: FieldConfigItem[];
}
```

- [ ] **Step 2: 提交**

```bash
git add web/apps/gwsu-sub-security/src/pages/role/types/index.ts
git commit -m "feat: 添加角色表模型权限相关类型定义"
```

---

### Task 2: 新增 API 函数

**Files:**
- 修改: `web/apps/gwsu-sub-security/src/pages/role/services/role.ts`

- [ ] **Step 1: 在 role.ts 末尾添加 3 个 API 函数**

在文件末尾追加（BASE 变量已定义为 `/security/role`）：

```typescript
import type { RolePermissionTableModelVO, RoleTableModelSaveDTO } from '../types';

const TABLE_MODEL_BASE = '/security/roleTableModel';

/** 获取角色表模型权限列表 */
export async function getTableModelPermission(
  roleId: string,
): Promise<RolePermissionTableModelVO[]> {
  const res = await get<RolePermissionTableModelVO[]>(
    `${TABLE_MODEL_BASE}/getTableModelPermission/${roleId}`,
  );
  return res.data ?? [];
}

/** 保存或更新角色表模型权限 */
export async function saveOrUpdateRoleTableModel(
  data: RoleTableModelSaveDTO,
): Promise<boolean> {
  const res = await post<boolean>(TABLE_MODEL_BASE, data);
  return res.data;
}

/** 批量删除角色表模型权限 */
export async function deleteRoleTableModels(ids: string[]): Promise<boolean> {
  const res = await del<boolean>(TABLE_MODEL_BASE, ids);
  return res.data;
}
```

注意：需在文件顶部 import 中补充 `RolePermissionTableModelVO` 和 `RoleTableModelSaveDTO` 类型导入。

- [ ] **Step 2: 提交**

```bash
git add web/apps/gwsu-sub-security/src/pages/role/services/role.ts
git commit -m "feat: 添加角色表模型权限 API 函数"
```

---

### Task 3: 新增弹窗内部类型和 hook

**Files:**
- 创建: `web/apps/gwsu-sub-security/src/pages/role/components/TableModelPermissionModal/types.ts`
- 创建: `web/apps/gwsu-sub-security/src/pages/role/components/TableModelPermissionModal/hooks/useTableModelPermission.ts`

- [ ] **Step 1: 创建 types.ts**

```typescript
import type { FieldConfigItem, ColumnInfo } from '../../../types';

/** 字段编辑行数据（合并 fixed + custom 后的展示数据） */
export interface FieldEditRow {
  columnName: string;
  columnComment: string;
  /** 是否被 fixedFieldConfig 锁定 */
  locked: boolean;
  /** 当前有效配置值（fixed 优先，否则取 custom，否则默认） */
  show: boolean;
  desensitize: boolean;
  strategy: string;
  prefixNoMaskLen?: number;
  suffixNoMaskLen?: number;
  symbol?: string;
}

/** 将 ColumnInfo 转换为 FieldEditRow */
export function columnToEditRow(column: ColumnInfo): FieldEditRow {
  const fixed = column.fixedFieldConfig;
  const custom = column.customFieldConfig;

  if (fixed) {
    return {
      columnName: column.columnName,
      columnComment: column.columnComment,
      locked: true,
      show: fixed.show,
      desensitize: fixed.desensitize,
      strategy: fixed.strategy,
      prefixNoMaskLen: fixed.prefixNoMaskLen,
      suffixNoMaskLen: fixed.suffixNoMaskLen,
      symbol: fixed.symbol,
    };
  }

  if (custom) {
    return {
      columnName: column.columnName,
      columnComment: column.columnComment,
      locked: false,
      show: custom.show,
      desensitize: custom.desensitize,
      strategy: custom.strategy,
      prefixNoMaskLen: custom.prefixNoMaskLen,
      suffixNoMaskLen: custom.suffixNoMaskLen,
      symbol: custom.symbol,
    };
  }

  // 无配置，使用默认值
  return {
    columnName: column.columnName,
    columnComment: column.columnComment,
    locked: false,
    show: true,
    desensitize: false,
    strategy: 'NONE',
  };
}

/** 将编辑行转换回保存 DTO 的 FieldConfigItem（排除锁定字段） */
export function editRowToFieldConfig(row: FieldEditRow): FieldConfigItem | null {
  if (row.locked) return null;
  return {
    fieldName: row.columnName,
    show: row.show,
    desensitize: row.desensitize,
    strategy: row.strategy,
    prefixNoMaskLen: row.prefixNoMaskLen,
    suffixNoMaskLen: row.suffixNoMaskLen,
    symbol: row.symbol,
  };
}
```

- [ ] **Step 2: 创建 hooks/useTableModelPermission.ts**

```typescript
import { useState, useCallback, useRef } from 'react';
import { App } from 'antd';
import {
  getTableModelPermission,
  saveOrUpdateRoleTableModel,
  deleteRoleTableModels,
} from '../../../services/role';
import type { RolePermissionTableModelVO, FieldConfigItem } from '../../../types';
import { columnToEditRow, editRowToFieldConfig } from '../types';
import type { FieldEditRow } from '../types';

/**
 * 角色表模型权限数据逻辑 hook
 */
export function useTableModelPermission() {
  const { message } = App.useApp();

  /** 角色拥有的表模型权限列表 */
  const [tables, setTables] = useState<RolePermissionTableModelVO[]>([]);
  /** 当前选中的表模型 ID */
  const [selectedTableId, setSelectedTableId] = useState<string | null>(null);
  /** 当前选中表的字段编辑行数据 */
  const [fieldRows, setFieldRows] = useState<FieldEditRow[]>([]);
  /** 加载状态 */
  const [loading, setLoading] = useState(false);
  /** 保存中 */
  const [saving, setSaving] = useState(false);

  /** 保存初始数据快照，用于重置 */
  const initialRowsRef = useRef<FieldEditRow[]>([]);

  /** 当前选中的表模型 VO */
  const selectedTable = tables.find((t) => t.tableModelId === selectedTableId) ?? null;

  /** 加载表模型权限数据 */
  const loadData = useCallback(async (roleId: string) => {
    setLoading(true);
    try {
      const data = await getTableModelPermission(roleId);
      setTables(data);
      // 默认选中第一个
      if (data.length > 0) {
        setSelectedTableId(data[0].tableModelId);
        const rows = data[0].columns.map(columnToEditRow);
        setFieldRows(rows);
        initialRowsRef.current = rows;
      } else {
        setSelectedTableId(null);
        setFieldRows([]);
        initialRowsRef.current = [];
      }
    } catch {
      // request 层已自动提示
    } finally {
      setLoading(false);
    }
  }, []);

  /** 选中某张表 */
  const selectTable = useCallback(
    (tableModelId: string) => {
      const table = tables.find((t) => t.tableModelId === tableModelId);
      if (!table) return;
      setSelectedTableId(tableModelId);
      const rows = table.columns.map(columnToEditRow);
      setFieldRows(rows);
      initialRowsRef.current = rows;
    },
    [tables],
  );

  /** 更新某个字段的配置 */
  const updateFieldRow = useCallback(
    (columnName: string, updates: Partial<FieldEditRow>) => {
      setFieldRows((prev) =>
        prev.map((row) =>
          row.columnName === columnName && !row.locked
            ? { ...row, ...updates }
            : row,
        ),
      );
    },
    [],
  );

  /** 保存当前表的字段配置 */
  const handleSave = useCallback(async () => {
    if (!selectedTable) return false;
    const fields: FieldConfigItem[] = [];
    for (const row of fieldRows) {
      const item = editRowToFieldConfig(row);
      if (item) fields.push(item);
    }

    setSaving(true);
    try {
      await saveOrUpdateRoleTableModel({
        id: selectedTable.id,
        roleId: selectedTable.roleId ?? '',
        modulePrefix: selectedTable.modulePrefix,
        tableName: selectedTable.tableName,
        datasource: selectedTable.datasource,
        fields,
      });
      message.success('保存成功');
      // 更新初始快照
      initialRowsRef.current = [...fieldRows];
      return true;
    } catch {
      return false;
    } finally {
      setSaving(false);
    }
  }, [selectedTable, fieldRows, message]);

  /** 重置到初始数据 */
  const handleReset = useCallback(() => {
    setFieldRows([...initialRowsRef.current]);
  }, []);

  /** 删除某个表的权限（仅 type=1 可删除） */
  const handleDeleteTable = useCallback(
    async (id: string) => {
      try {
        await deleteRoleTableModels([id]);
        message.success('删除成功');
        return true;
      } catch {
        return false;
      }
    },
    [message],
  );

  /** 添加新表到列表（新增模型权限后调用） */
  const addTablesToList = useCallback(
    (newTables: RolePermissionTableModelVO[]) => {
      setTables((prev) => [...prev, ...newTables]);
      // 如果之前没有选中，选第一个新增的
      if (!selectedTableId && newTables.length > 0) {
        setSelectedTableId(newTables[0].tableModelId);
        const rows = newTables[0].columns.map(columnToEditRow);
        setFieldRows(rows);
        initialRowsRef.current = rows;
      }
    },
    [selectedTableId],
  );

  /** 从列表中移除表（删除后调用） */
  const removeTableFromList = useCallback(
    (tableModelId: string) => {
      setTables((prev) => prev.filter((t) => t.tableModelId !== tableModelId));
      if (selectedTableId === tableModelId) {
        const remaining = tables.filter((t) => t.tableModelId !== tableModelId);
        if (remaining.length > 0) {
          selectTable(remaining[0].tableModelId);
        } else {
          setSelectedTableId(null);
          setFieldRows([]);
          initialRowsRef.current = [];
        }
      }
    },
    [selectedTableId, tables, selectTable],
  );

  return {
    tables,
    selectedTableId,
    selectedTable,
    fieldRows,
    loading,
    saving,
    loadData,
    selectTable,
    updateFieldRow,
    handleSave,
    handleReset,
    handleDeleteTable,
    addTablesToList,
    removeTableFromList,
  };
}
```

- [ ] **Step 3: 提交**

```bash
git add web/apps/gwsu-sub-security/src/pages/role/components/TableModelPermissionModal/
git commit -m "feat: 添加表模型权限弹窗类型定义和数据逻辑 hook"
```

---

### Task 4: 新增模型权限弹框组件

**Files:**
- 创建: `web/apps/gwsu-sub-security/src/pages/role/components/TableModelPermissionModal/AddTableModelModal.tsx`

- [ ] **Step 1: 创建 AddTableModelModal.tsx**

该组件负责搜索 `security_tablemodel_tables`，排除已有表，让用户勾选后批量新增。

```tsx
import React, { useState, useEffect, useCallback } from 'react';
import { Modal, Input, Table, App } from 'antd';
import type { TableProps } from 'antd';
import { getTableModelPage } from '../../../tablemodel/services/tableModel';
import type { TableModelInfo } from '../../../tablemodel/types';
import type { RolePermissionTableModelVO } from '../../../types';
import { saveOrUpdateRoleTableModel } from '../../../services/role';

interface AddTableModelModalProps {
  visible: boolean;
  roleId: string | null;
  /** 当前角色已拥有的表模型 ID 集合，用于排除 */
  existingTableIds: Set<string>;
  onClose: () => void;
  onSuccess: (newTables: RolePermissionTableModelVO[]) => void;
}

const AddTableModelModal: React.FC<AddTableModelModalProps> = ({
  visible,
  roleId,
  existingTableIds,
  onClose,
  onSuccess,
}) => {
  const { message } = App.useApp();
  const [searchText, setSearchText] = useState('');
  const [loading, setLoading] = useState(false);
  const [dataSource, setDataSource] = useState<TableModelInfo[]>([]);
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([]);
  const [confirmLoading, setConfirmLoading] = useState(false);

  /** 搜索表模型 */
  const doSearch = useCallback(async () => {
    setLoading(true);
    try {
      const result = await getTableModelPage({
        tableName: searchText || undefined,
        pageNum: 1,
        pageSize: 100,
      });
      // 排除已有表
      setDataSource(
        (result?.records ?? []).filter((t) => !existingTableIds.has(t.id)),
      );
    } catch {
      // request 层已自动提示
    } finally {
      setLoading(false);
    }
  }, [searchText, existingTableIds]);

  useEffect(() => {
    if (visible) {
      setSearchText('');
      setSelectedRowKeys([]);
      doSearch();
    }
  }, [visible, doSearch]);

  /** 确认添加 */
  const handleConfirm = useCallback(async () => {
    if (!roleId || selectedRowKeys.length === 0) return;

    setConfirmLoading(true);
    try {
      const newTables: RolePermissionTableModelVO[] = [];
      for (const key of selectedRowKeys) {
        const record = dataSource.find((d) => d.id === key);
        if (!record) continue;

        await saveOrUpdateRoleTableModel({
          roleId,
          modulePrefix: record.modulePrefix,
          tableName: record.tableName,
          datasource: record.dataSource,
          fields: [],
        });

        newTables.push({
          type: 1,
          tableModelId: record.id,
          modulePrefix: record.modulePrefix,
          datasource: record.dataSource,
          tableName: record.tableName,
          tableComment: record.tableComment,
          columns: [],
        });
      }
      message.success(`成功添加 ${newTables.length} 个表模型权限`);
      onSuccess(newTables);
      onClose();
    } catch {
      // request 层已自动提示
    } finally {
      setConfirmLoading(false);
    }
  }, [roleId, selectedRowKeys, dataSource, onSuccess, onClose, message]);

  const columns: TableProps<TableModelInfo>['columns'] = [
    {
      title: '表名',
      dataIndex: 'tableName',
      width: 160,
    },
    {
      title: '表注释',
      dataIndex: 'tableComment',
      ellipsis: true,
    },
    {
      title: '模块',
      dataIndex: 'modulePrefix',
      width: 100,
    },
    {
      title: '数据源',
      dataIndex: 'dataSource',
      width: 100,
    },
  ];

  return (
    <Modal
      title="新增模型权限"
      open={visible}
      onCancel={onClose}
      onOk={handleConfirm}
      okText="确认添加"
      cancelText="取消"
      okButtonProps={{ disabled: selectedRowKeys.length === 0, 'data-ai-approval': 'true' }}
      confirmLoading={confirmLoading}
      width={640}
      destroyOnHidden
    >
      <Input.Search
        placeholder="输入表名搜索"
        allowClear
        value={searchText}
        onChange={(e) => setSearchText(e.target.value)}
        onSearch={doSearch}
        style={{ marginBottom: 12 }}
      />
      <Table<TableModelInfo>
        rowKey="id"
        columns={columns}
        dataSource={dataSource}
        loading={loading}
        size="small"
        scroll={{ y: 360 }}
        rowSelection={{
          selectedRowKeys,
          onChange: setSelectedRowKeys,
        }}
        pagination={false}
      />
    </Modal>
  );
};

export default AddTableModelModal;
```

- [ ] **Step 2: 提交**

```bash
git add web/apps/gwsu-sub-security/src/pages/role/components/TableModelPermissionModal/AddTableModelModal.tsx
git commit -m "feat: 添加新增模型权限弹框组件"
```

---

### Task 5: 左侧表列表面板

**Files:**
- 创建: `web/apps/gwsu-sub-security/src/pages/role/components/TableModelPermissionModal/TableListPanel.tsx`

- [ ] **Step 1: 创建 TableListPanel.tsx**

```tsx
import React, { useState, useMemo, useCallback } from 'react';
import { Button, Input, Tag, Popconfirm } from 'antd';
import { PlusOutlined, DeleteOutlined, SearchOutlined } from '@ant-design/icons';
import type { RolePermissionTableModelVO } from '../../../types';
import styles from './index.module.less';

interface TableListPanelProps {
  tables: RolePermissionTableModelVO[];
  selectedId: string | null;
  onSelect: (tableModelId: string) => void;
  onAdd: () => void;
  onDelete: (tableModelId: string, id: string) => void;
}

const TableListPanel: React.FC<TableListPanelProps> = ({
  tables,
  selectedId,
  onSelect,
  onAdd,
  onDelete,
}) => {
  const [searchText, setSearchText] = useState('');

  /** 按搜索文本过滤 */
  const filteredTables = useMemo(() => {
    if (!searchText.trim()) return tables;
    const keyword = searchText.toLowerCase();
    return tables.filter(
      (t) =>
        t.tableName.toLowerCase().includes(keyword) ||
        t.tableComment.toLowerCase().includes(keyword),
    );
  }, [tables, searchText]);

  const handleSelect = useCallback(
    (tableModelId: string) => {
      onSelect(tableModelId);
    },
    [onSelect],
  );

  return (
    <div className={styles.leftPanel}>
      <div className={styles.leftHeader}>
        <span className={styles.leftTitle}>表模型列表</span>
        <Button type="primary" size="small" icon={<PlusOutlined />} onClick={onAdd}>
          新增
        </Button>
      </div>
      <div className={styles.leftSearch}>
        <Input
          placeholder="搜索表名"
          allowClear
          prefix={<SearchOutlined />}
          value={searchText}
          onChange={(e) => setSearchText(e.target.value)}
          size="small"
        />
      </div>
      <div className={styles.groupList}>
        {filteredTables.length === 0 ? (
          <div className={styles.leftEmpty}>
            <span className={styles.leftEmptyText}>暂无表模型权限</span>
          </div>
        ) : (
          filteredTables.map((table) => (
            <div
              key={table.tableModelId}
              className={`${styles.groupItem} ${
                selectedId === table.tableModelId ? styles.groupItemActive : ''
              }`}
              onClick={() => handleSelect(table.tableModelId)}
            >
              <div className={styles.groupInfo}>
                <div className={styles.groupLabel}>
                  {table.tableComment || table.tableName}
                </div>
                <div className={styles.groupCount}>
                  <span>{table.tableName}</span>
                  <Tag
                    color={table.type === 1 ? 'green' : 'blue'}
                    style={{ marginLeft: 4, fontSize: 11 }}
                  >
                    {table.type === 1 ? '自定义' : '接口关联'}
                  </Tag>
                </div>
              </div>
              {table.type === 1 && table.id && (
                <Popconfirm
                  title="确认删除"
                  description="删除后该角色的表模型自定义权限将被清除"
                  onConfirm={(e) => {
                    e?.stopPropagation();
                    onDelete(table.tableModelId, table.id!);
                  }}
                  okText="确定"
                  cancelText="取消"
                >
                  <Button
                    type="text"
                    size="small"
                    danger
                    icon={<DeleteOutlined />}
                    className={styles.groupDeleteBtn}
                    onClick={(e) => e.stopPropagation()}
                  />
                </Popconfirm>
              )}
            </div>
          ))
        )}
      </div>
    </div>
  );
};

export default TableListPanel;
```

- [ ] **Step 2: 提交**

```bash
git add web/apps/gwsu-sub-security/src/pages/role/components/TableModelPermissionModal/TableListPanel.tsx
git commit -m "feat: 添加左侧表列表面板组件"
```

---

### Task 6: 右侧字段配置表格

**Files:**
- 创建: `web/apps/gwsu-sub-security/src/pages/role/components/TableModelPermissionModal/FieldConfigTable.tsx`

- [ ] **Step 1: 创建 FieldConfigTable.tsx**

```tsx
import React, { useCallback } from 'react';
import { Table, Switch, Select, Input, InputNumber, Tooltip, Tag } from 'antd';
import type { TableProps } from 'antd';
import { LockOutlined } from '@ant-design/icons';
import type { RolePermissionTableModelVO } from '../../../types';
import type { FieldEditRow } from './types';
import styles from './index.module.less';

/** 脱敏策略选项 */
const STRATEGY_OPTIONS = [
  { label: '无', value: 'NONE' },
  { label: '用户名（张**）', value: 'USERNAME' },
  { label: '身份证（3301**********1234）', value: 'ID_CARD' },
  { label: '手机号（138****1234）', value: 'PHONE' },
  { label: '邮箱（a****b@example.com）', value: 'EMAIL' },
  { label: '地址（浙江省****杭州市****）', value: 'ADDRESS' },
  { label: '自定义', value: 'CUSTOM' },
];

interface FieldConfigTableProps {
  table: RolePermissionTableModelVO | null;
  fieldRows: FieldEditRow[];
  onUpdateRow: (columnName: string, updates: Partial<FieldEditRow>) => void;
}

const FieldConfigTable: React.FC<FieldConfigTableProps> = ({
  table,
  fieldRows,
  onUpdateRow,
}) => {
  const handleShowChange = useCallback(
    (columnName: string, checked: boolean) => {
      onUpdateRow(columnName, { show: checked });
    },
    [onUpdateRow],
  );

  const handleDesensitizeChange = useCallback(
    (columnName: string, checked: boolean) => {
      onUpdateRow(columnName, {
        desensitize: checked,
        strategy: checked ? 'PHONE' : 'NONE',
      });
    },
    [onUpdateRow],
  );

  const handleStrategyChange = useCallback(
    (columnName: string, strategy: string) => {
      onUpdateRow(columnName, { strategy });
    },
    [onUpdateRow],
  );

  const columns: TableProps<FieldEditRow>['columns'] = [
    {
      title: '字段名',
      dataIndex: 'columnName',
      width: 140,
      render: (val: string, row: FieldEditRow) => (
        <span className={row.locked ? styles.lockedText : undefined}>
          {row.locked && (
            <Tooltip title="默认配置，禁止修改">
              <LockOutlined style={{ marginRight: 4, color: '#faad14' }} />
            </Tooltip>
          )}
          {val}
        </span>
      ),
    },
    {
      title: '字段注释',
      dataIndex: 'columnComment',
      width: 160,
      ellipsis: true,
    },
    {
      title: '允许查询',
      dataIndex: 'show',
      width: 90,
      align: 'center',
      render: (val: boolean, row: FieldEditRow) => (
        <Switch
          size="small"
          checked={val}
          disabled={row.locked}
          onChange={(checked) => handleShowChange(row.columnName, checked)}
        />
      ),
    },
    {
      title: '是否脱敏',
      dataIndex: 'desensitize',
      width: 90,
      align: 'center',
      render: (val: boolean, row: FieldEditRow) => (
        <Switch
          size="small"
          checked={val}
          disabled={row.locked}
          onChange={(checked) => handleDesensitizeChange(row.columnName, checked)}
        />
      ),
    },
    {
      title: '脱敏策略',
      dataIndex: 'strategy',
      width: 160,
      render: (val: string, row: FieldEditRow) => (
        <Select
          size="small"
          value={val}
          options={STRATEGY_OPTIONS}
          disabled={row.locked || !row.desensitize}
          onChange={(strategy) => handleStrategyChange(row.columnName, strategy)}
          style={{ width: '100%' }}
        />
      ),
    },
    {
      title: '自定义参数',
      width: 240,
      render: (_: unknown, row: FieldEditRow) => {
        if (!row.desensitize || row.strategy !== 'CUSTOM') return '-';
        if (row.locked) {
          return (
            <span className={styles.customParams}>
              前{row.prefixNoMaskLen ?? 0} 后{row.suffixNoMaskLen ?? 0} 符号{row.symbol ?? '*'}
            </span>
          );
        }
        return (
          <div className={styles.customParams}>
            <InputNumber
              size="small"
              min={0}
              value={row.prefixNoMaskLen ?? 0}
              onChange={(v) => onUpdateRow(row.columnName, { prefixNoMaskLen: v ?? 0 })}
              addonBefore="前"
              style={{ width: 80 }}
            />
            <InputNumber
              size="small"
              min={0}
              value={row.suffixNoMaskLen ?? 0}
              onChange={(v) => onUpdateRow(row.columnName, { suffixNoMaskLen: v ?? 0 })}
              addonBefore="后"
              style={{ width: 80 }}
            />
            <Input
              size="small"
              value={row.symbol ?? '*'}
              onChange={(e) => onUpdateRow(row.columnName, { symbol: e.target.value })}
              addonBefore="符号"
              style={{ width: 80 }}
            />
          </div>
        );
      },
    },
  ];

  if (!table) {
    return (
      <div className={styles.rightEmpty}>
        <span className={styles.rightEmptyText}>请选择左侧表模型</span>
      </div>
    );
  }

  return (
    <div className={styles.fieldConfigWrapper}>
      {/* 表信息区 */}
      <div className={styles.tableInfoSection}>
        <span className={styles.tableInfoName}>
          {table.tableComment || table.tableName}
          {table.tableComment && table.tableName !== table.tableComment && (
            <span className={styles.tableInfoTableName}>（{table.tableName}）</span>
          )}
        </span>
        <span className={styles.tableInfoMeta}>
          模块：{table.modulePrefix} &nbsp; 数据源：{table.datasource}
        </span>
        {table.id && (
          <Tag color="green" style={{ marginLeft: 8 }}>
            已自定义配置
          </Tag>
        )}
      </div>

      {/* 字段配置表格 */}
      <div className={styles.fieldTableWrapper}>
        <Table<FieldEditRow>
          rowKey="columnName"
          columns={columns}
          dataSource={fieldRows}
          size="small"
          pagination={false}
          scroll={{ y: 420 }}
          rowClassName={(row) => (row.locked ? styles.lockedRow : '')}
        />
      </div>
    </div>
  );
};

export default FieldConfigTable;
```

- [ ] **Step 2: 提交**

```bash
git add web/apps/gwsu-sub-security/src/pages/role/components/TableModelPermissionModal/FieldConfigTable.tsx
git commit -m "feat: 添加右侧字段配置表格组件"
```

---

### Task 7: 样式文件

**Files:**
- 创建: `web/apps/gwsu-sub-security/src/pages/role/components/TableModelPermissionModal/index.module.less`

- [ ] **Step 1: 创建 index.module.less**

```less
/* ========================================
 * TableModelPermissionModal 样式
 * 左右分栏布局：左侧表列表，右侧字段配置
 * ======================================== */

.modal {
  :global {
    .ant-modal-body {
      padding: 0;
    }
    .ant-modal-content {
      overflow: hidden;
    }
  }
}

/* 主体容器 - 左右分栏 */
.container {
  display: flex;
  height: 680px;
  min-height: 680px;
}

/* ==================== 左侧面板 ==================== */
.leftPanel {
  width: 280px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  border-right: 1px solid var(--border-color);
  background: var(--surface-color);
}

.leftHeader {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid var(--border-color);
}

.leftTitle {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-color);
}

.leftSearch {
  padding: 8px 12px;
  border-bottom: 1px solid var(--border-color);
}

/* 列表 */
.groupList {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.groupItem {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
  margin-bottom: 4px;
  border-left: 3px solid transparent;

  &:hover {
    background: var(--hover-color);
  }
}

.groupItemActive {
  background: color-mix(in srgb, var(--ant-primary-color, #1677ff) 8%, transparent);
  border-left-color: var(--ant-primary-color, #1677ff);

  .groupLabel {
    color: var(--ant-primary-color, #1677ff);
    font-weight: 600;
  }

  &:hover {
    background: color-mix(in srgb, var(--ant-primary-color, #1677ff) 12%, transparent);
  }
}

.groupInfo {
  flex: 1;
  min-width: 0;
}

.groupLabel {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-color);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.groupCount {
  font-size: 12px;
  color: var(--text-secondary-color);
  margin-top: 2px;
  display: flex;
  align-items: center;
}

.groupDeleteBtn {
  flex-shrink: 0;
  opacity: 0;
  transition: opacity 0.2s;

  .groupItem:hover & {
    opacity: 1;
  }
}

/* 左侧空状态 */
.leftEmpty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  gap: 8px;
}

.leftEmptyText {
  font-size: 13px;
  color: var(--text-secondary-color);
}

/* ==================== 右侧面板 ==================== */
.rightPanel {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  background: var(--background-color);
}

/* 表信息区 */
.tableInfoSection {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: var(--surface-color);
  border-bottom: 1px solid var(--border-color);
  flex-shrink: 0;
}

.tableInfoName {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-color);
}

.tableInfoTableName {
  font-weight: 400;
  color: var(--text-secondary-color);
  font-size: 13px;
}

.tableInfoMeta {
  font-size: 12px;
  color: var(--text-secondary-color);
}

/* 字段配置区域 */
.fieldConfigWrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.fieldTableWrapper {
  flex: 1;
  min-height: 0;
  padding: 0 16px 12px;
  overflow-y: auto;
}

/* 锁定行样式 */
.lockedRow {
  background: color-mix(in srgb, #000 3%, transparent) !important;

  td {
    color: var(--text-secondary-color);
  }
}

.lockedText {
  color: var(--text-secondary-color);
}

/* 自定义参数区 */
.customParams {
  display: flex;
  gap: 4px;
  align-items: center;
  flex-wrap: wrap;
}

/* 右侧空状态 */
.rightEmpty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  gap: 12px;
}

.rightEmptyText {
  font-size: 14px;
  color: var(--text-secondary-color);
}

/* ==================== 底部按钮区 ==================== */
.footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  padding: 12px 16px;
  border-top: 1px solid var(--border-color);
  background: var(--surface-color);
}
```

- [ ] **Step 2: 提交**

```bash
git add web/apps/gwsu-sub-security/src/pages/role/components/TableModelPermissionModal/index.module.less
git commit -m "feat: 添加表模型权限弹窗样式"
```

---

### Task 8: 主弹窗组件

**Files:**
- 创建: `web/apps/gwsu-sub-security/src/pages/role/components/TableModelPermissionModal/index.tsx`

- [ ] **Step 1: 创建 index.tsx**

```tsx
import React, { useState, useEffect, useCallback } from 'react';
import { Modal, Button, Spin } from 'antd';
import styles from './index.module.less';
import TableListPanel from './TableListPanel';
import FieldConfigTable from './FieldConfigTable';
import AddTableModelModal from './AddTableModelModal';
import { useTableModelPermission } from './hooks/useTableModelPermission';

interface TableModelPermissionModalProps {
  visible: boolean;
  roleId: string | null;
  roleName: string;
  onClose: () => void;
}

const TableModelPermissionModal: React.FC<TableModelPermissionModalProps> = ({
  visible,
  roleId,
  roleName,
  onClose,
}) => {
  const {
    tables,
    selectedTableId,
    selectedTable,
    fieldRows,
    loading,
    saving,
    loadData,
    selectTable,
    updateFieldRow,
    handleSave,
    handleReset,
    handleDeleteTable,
    addTablesToList,
    removeTableFromList,
  } = useTableModelPermission();

  const [addModalVisible, setAddModalVisible] = useState(false);

  /** 弹窗打开时加载数据 */
  useEffect(() => {
    if (visible && roleId) {
      loadData(roleId);
    }
  }, [visible, roleId, loadData]);

  /** 保存 */
  const onSave = useCallback(async () => {
    const success = await handleSave();
    if (success) {
      // 重新加载数据以获取后端最新状态
      if (roleId) loadData(roleId);
    }
  }, [handleSave, roleId, loadData]);

  /** 删除表权限 */
  const onDeleteTable = useCallback(
    async (tableModelId: string, id: string) => {
      const success = await handleDeleteTable(id);
      if (success) {
        removeTableFromList(tableModelId);
      }
    },
    [handleDeleteTable, removeTableFromList],
  );

  /** 新增模型权限成功回调 */
  const onAddSuccess = useCallback(
    (newTables) => {
      addTablesToList(newTables);
      // 重新加载数据以获取完整列信息
      if (roleId) loadData(roleId);
    },
    [addTablesToList, roleId, loadData],
  );

  /** 已有表的 ID 集合 */
  const existingTableIds = new Set(tables.map((t) => t.tableModelId));

  return (
    <Modal
      title={`表模型权限配置 - ${roleName}`}
      open={visible}
      onCancel={onClose}
      width={1080}
      className={styles.modal}
      footer={null}
      destroyOnHidden
    >
      <Spin spinning={loading}>
        <div className={styles.container}>
          <TableListPanel
            tables={tables}
            selectedId={selectedTableId}
            onSelect={selectTable}
            onAdd={() => setAddModalVisible(true)}
            onDelete={onDeleteTable}
          />
          <div className={styles.rightPanel}>
            <FieldConfigTable
              table={selectedTable}
              fieldRows={fieldRows}
              onUpdateRow={updateFieldRow}
            />
            <div className={styles.footer}>
              <Button onClick={handleReset}>重置</Button>
              <Button type="primary" loading={saving} onClick={onSave} data-ai-approval>
                保存
              </Button>
            </div>
          </div>
        </div>
      </Spin>

      <AddTableModelModal
        visible={addModalVisible}
        roleId={roleId}
        existingTableIds={existingTableIds}
        onClose={() => setAddModalVisible(false)}
        onSuccess={onAddSuccess}
      />
    </Modal>
  );
};

export default TableModelPermissionModal;
```

- [ ] **Step 2: 提交**

```bash
git add web/apps/gwsu-sub-security/src/pages/role/components/TableModelPermissionModal/index.tsx
git commit -m "feat: 添加表模型权限主弹窗组件"
```

---

### Task 9: 接入角色页面

**Files:**
- 修改: `web/apps/gwsu-sub-security/src/pages/role/index.tsx`

- [ ] **Step 1: 导入组件**

在文件顶部 import 区域添加：

```typescript
import TableModelPermissionModal from './components/TableModelPermissionModal';
```

- [ ] **Step 2: 添加状态变量**

在 `relatedUserRoleName` 状态后添加：

```typescript
// 表模型权限弹窗
const [tableModelPermVisible, setTableModelPermVisible] = useState(false);
const [tableModelPermRoleId, setTableModelPermRoleId] = useState<string | null>(null);
const [tableModelPermRoleName, setTableModelPermRoleName] = useState<string>('');
```

- [ ] **Step 3: 添加处理函数**

在 `handleRelatedUser` 后添加：

```typescript
/** 表模型权限配置 */
const handleTableModelPermission = useCallback((role: RoleInfo) => {
  setTableModelPermRoleId(role.id ?? null);
  setTableModelPermRoleName(role.roleName);
  setTableModelPermVisible(true);
}, []);
```

- [ ] **Step 4: 替换占位逻辑**

在 `getButtonItem` 函数中，将表模型权限的 `handlePlaceholder` 替换为真实处理：

将：
```typescript
if(canTableModelPermission){
  buttons.push({
    key: "tablePermission",
    icon: <TableOutlined />,
    label: "表模型权限",
    onClick: () => handlePlaceholder("表模型权限"),
  });
}
```

替换为：
```typescript
if(canTableModelPermission){
  buttons.push({
    key: "tablePermission",
    icon: <TableOutlined />,
    label: "表模型权限",
    onClick: () => handleTableModelPermission(record),
  });
}
```

- [ ] **Step 5: 添加弹窗渲染**

在 `RelatedUserModal` 组件后添加：

```tsx
{/* 表模型权限配置弹窗 */}
<TableModelPermissionModal
  visible={tableModelPermVisible}
  roleId={tableModelPermRoleId}
  roleName={tableModelPermRoleName}
  onClose={() => setTableModelPermVisible(false)}
/>
```

- [ ] **Step 6: 删除 handlePlaceholder 函数**

如果 `handlePlaceholder` 不再被其他地方引用，可以删除。先检查 `handlePlaceholder("字段权限")` 是否还有其他调用——如果有则保留，没有则删除。

- [ ] **Step 7: 提交**

```bash
git add web/apps/gwsu-sub-security/src/pages/role/index.tsx
git commit -m "feat: 角色页面接入表模型权限弹窗"
```

---

### Task 10: 验证和修复

- [ ] **Step 1: 启动前端开发服务器**

```bash
cd web && pnpm dev:sub-security
```

- [ ] **Step 2: 浏览器验证**

1. 打开角色管理页面
2. 点击某个角色的"更多" → "表模型权限"
3. 验证弹窗正常弹出，左侧表列表正常加载
4. 点击左侧表，右侧字段配置表格正常显示
5. 验证锁定行（fixedFieldConfig）显示锁图标且不可编辑
6. 修改可编辑字段的开关/选择器，验证联动逻辑
7. 验证脱敏策略选择 CUSTOM 后自定义参数显示
8. 点击"保存"，验证数据保存成功
9. 点击"重置"，验证数据恢复
10. 点击"新增"，验证搜索和添加功能
11. 删除自定义配置的表，验证删除成功

- [ ] **Step 3: 修复发现的问题**

根据测试中发现的问题逐一修复。

- [ ] **Step 4: 最终提交**

```bash
git add -A
git commit -m "fix: 修复表模型权限功能验证中发现的问题"
```
