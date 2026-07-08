# 二、组件与页面开发

## 2.1 页面主组件模式

页面主组件负责：声明状态 → 加载数据 → 组合子组件 → 处理事件回调

```tsx
const XxxPage: React.FC = () => {
  const [data, setData] = useState<XxxItem[]>([]);
  const [loading, setLoading] = useState(false);

  const loadData = useCallback(async () => {
    setLoading(true);
    try { setData(await getXxxList()); } catch {} finally { setLoading(false); }
  }, []);

  useEffect(() => { void loadData(); }, [loadData]);

  return (
    <div className={styles.xxxPage}>
      <XxxPanel data={data} loading={loading} onEdit={handleEdit} />
      <XxxFormModal visible={modalVisible} data={selectedItem} onClose={() => setModalVisible(false)} onSuccess={handleFormSuccess} />
    </div>
  );
};
```

## 2.2 子组件模式

每个独立组件放在自己的目录：`components/XxxPanel/index.tsx` + `index.module.less`

```tsx
interface XxxPanelProps {
  data: XxxItem[];
  loading: boolean;
  onCreate: () => void;
  onEdit: (item: XxxItem) => void;
}

const XxxPanel: React.FC<XxxPanelProps> = ({ data, loading, onCreate, onEdit }) => (
  <Card title="XXX 列表" extra={<AuthGate buttonKey="101_add"><Button type="primary" onClick={onCreate}>新增</Button></AuthGate>} loading={loading}>
    {data.map((item) => (
      <div key={item.id}>
        <span>{item.name}</span>
        <AuthGate buttonKey="101_edit"><Button type="link" onClick={() => onEdit(item)}>编辑</Button></AuthGate>
      </div>
    ))}
  </Card>
);
```

## 2.3 表单弹窗模式

```tsx
const XxxFormModal: React.FC<XxxFormModalProps> = ({ visible, data, onClose, onSuccess }) => {
  const [form] = Form.useForm();
  const isEdit = !!data?.id;

  useEffect(() => { visible ? (data ? form.setFieldsValue(data) : form.resetFields()) : null; }, [visible, data, form]);

  const handleOk = async () => {
    const values = await form.validateFields();
    await saveXxx({ ...data, ...values });
    message.success(isEdit ? '编辑成功' : '新增成功');
    onSuccess();
  };

  return (
    <Modal title={isEdit ? '编辑' : '新增'} open={visible} onOk={handleOk} onCancel={onClose}
      okButtonProps={{ 'data-ai-approval': true }}>
      <Form form={form} layout="vertical">
        <Form.Item name="name" label="名称" rules={[{ required: true, message: '请输入名称' }]}>
          <Input placeholder="请输入名称" />
        </Form.Item>
      </Form>
    </Modal>
  );
};
```

## 2.4 自定义 Hooks

页面级复杂逻辑抽离为 Hooks，放在 `hooks/` 目录：

```tsx
export function useXxxTree() {
  const [treeData, setTreeData] = useState<XxxTreeNode[]>([]);
  const [loading, setLoading] = useState(false);

  const loadTree = useCallback(async () => {
    setLoading(true);
    try { setTreeData(await getXxxTree()); } catch { message.error('加载失败'); } finally { setLoading(false); }
  }, []);

  useEffect(() => { void loadTree(); }, [loadTree]);
  return { treeData, loading, loadTree };
}
```

## 2.5 无障碍规范（WCAG 2.1）

开发自定义组件时**必须遵循 WCAG 2.1**：

1. **优先使用原生 HTML 标签**（`<button>`、`<a>`、`<nav>`），原生标签自带无障碍语义
2. **无法使用原生标签时，必须用 ARIA 属性补充语义**
3. 所有交互元素必须可通过键盘操作
4. 图标按钮必须添加 `aria-label`，装饰性图标添加 `aria-hidden="true"`

**常用 ARIA 属性**：

| 属性 | 场景 |
|------|------|
| `role` | 声明元素角色（`role="tablist"`/`role="tab"`/`role="menu"`） |
| `aria-label` | 图标按钮、无文本交互元素的可访问名称 |
| `aria-expanded` | 展开/折叠状态（下拉菜单、手风琴） |
| `aria-selected` | 选中状态（标签页、列表选项） |
| `aria-disabled` | 禁用状态（保留焦点能力） |
| `aria-hidden` | 装饰性图标 |
| `aria-live` | 动态更新区域（Toast 通知） |
| `aria-haspopup` | 弹出内容类型 |

```tsx
// 图标按钮
<button onClick={handleClose} aria-label="关闭"><CloseOutlined aria-hidden="true" /></button>

// 自定义下拉菜单
<button aria-haspopup="menu" aria-expanded={open} aria-label="更多操作"><MoreOutlined aria-hidden="true" /></button>

// Ant Design 组件补充
<Input aria-label="搜索用户" placeholder="请输入" />
<Table aria-label="用户列表" columns={columns} dataSource={data} />
```

## 2.6 data-ai-approval 标记

**核心标准**：按钮点击后是否立即触发后端数据变更。需要则加，不需要则不加。

| 需要 | 不需要 |
|------|--------|
| 删除、批量删除 | 新增（仅打开空表单） |
| 状态 Switch 切换 | 详情、查看 |
| 保存/提交 | 搜索、筛选、翻页 |
| 同步到 Redis | 编辑（仅打开编辑弹窗） |

```tsx
<Button danger data-ai-approval>删除</Button>
<Switch data-ai-approval checked={val} onChange={handleChange} />
<Modal okButtonProps={{ 'data-ai-approval': true }}>...</Modal>
```
