# 四、列表页开发模式

## 4.1 页面布局

```
┌─────────────────────────────────┐
│           搜索栏 (searchBar)      │  ← flex-wrap，搜索按钮靠右
├─────────────────────────────────┤
│  表格标题 + 操作按钮 (tableHeader) │  ← AuthGate 控制头部按钮
├─────────────────────────────────┤
│           表格内容 (Table)        │  ← flex: 1 自适应
└─────────────────────────────────┘
```

样式关键：页面 `display:flex; flex-direction:column; height:100%`，表格容器 `flex:1; min-height:0; overflow:hidden`

## 4.2 操作列规范

统一采用 **"详情" + "更多"下拉菜单** 模式，`fixed: "right"`，宽度 160~200：

```tsx
const canEdit = useAuth('xxx_edit');
const getButtonItem = (record): MenuProps['items'] => {
  const buttons = [];
  if (canEdit) buttons.push({ key: 'edit', icon: <EditOutlined />, label: '编辑', onClick: () => handleEdit(record) });
  return buttons;
};

// 操作列渲染
<div className={styles.actionColumn}>
  <Button type="link" size="small" icon={<EyeOutlined />}>详情</Button>
  <Dropdown menu={{ items: getButtonItem(record) }} disabled={getButtonItem(record).length === 0}>
    <Button type="link" size="small" icon={<MoreOutlined />}>更多</Button>
  </Dropdown>
</div>
```

## 4.3 权限控制方式

| 方式 | 适用场景 | 特点 |
|------|---------|------|
| `AuthGate` | 表格头部按钮 | 无权限不渲染 |
| `useAuth` | 操作列下拉菜单项 | 无权限不添加菜单项 |

**权限标识必须定义为常量**，放在 `permissionConstants.ts`：

```typescript
export const PERM_ADD = '72974723_add';
export const PERM_EDIT = '72974723_edit';
```

## 4.4 状态 Switch

需权限控制 + data-ai-approval：

```tsx
<AuthGate buttonKey="xxx_edit">
  <Switch size="small" data-ai-approval checked={val} onChange={(checked) => handleStatusChange(record, checked)} />
</AuthGate>
<Tag color={val ? 'green' : 'red'}>{val ? '启用' : '禁用'}</Tag>
```

## 4.5 表格头部按钮

```tsx
<Space>
  <AuthGate buttonKey={PERM_ADD}>
    <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>新增</Button>
  </AuthGate>
  <AuthGate buttonKey={PERM_REMOVE}>
    <Popconfirm title="确定删除？" onConfirm={handleBatchDelete}>
      <Button danger data-ai-approval icon={<DeleteOutlined />} disabled={selectedRowKeys.length === 0}>删除</Button>
    </Popconfirm>
  </AuthGate>
</Space>
```

## 4.6 子应用布局

```tsx
const Layout: React.FC = () => (
  <ThemeLayout><Outlet /></ThemeLayout>
);
```
