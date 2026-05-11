# 三、组件与页面开发模式

## 3.1 页面开发模式

### 页面文件命名

- 页面主文件统一为 `index.tsx`
- 页面样式统一为 `index.module.less`
- 页面放在 `src/pages/{业务名}/` 目录下

### 页面主组件结构

页面主组件负责：
1. 声明页面级状态
2. 加载数据
3. 组合子组件
4. 处理事件回调

```tsx
import React, { useState, useCallback, useEffect } from 'react';
import { message } from 'antd';
import { ApartmentOutlined } from '@ant-design/icons';
import styles from './index.module.less';
import XxxPanel from './components/XxxPanel';
import XxxFormModal from './components/XxxFormModal';
import { getXxxList } from '@/services/xxx';
import type { XxxItem } from './types';

const XxxPage: React.FC = () => {
  // 1. 状态声明
  const [data, setData] = useState<XxxItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [selectedItem, setSelectedItem] = useState<XxxItem | null>(null);
  const [modalVisible, setModalVisible] = useState(false);

  // 2. 数据加载
  const loadData = useCallback(async () => {
    setLoading(true);
    try {
      const result = await getXxxList();
      setData(result);
    } catch {
      message.error('加载数据失败');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadData();
  }, [loadData]);

  // 3. 事件处理
  const handleCreate = useCallback(() => {
    setSelectedItem(null);
    setModalVisible(true);
  }, []);

  const handleEdit = useCallback((item: XxxItem) => {
    setSelectedItem(item);
    setModalVisible(true);
  }, []);

  const handleFormSuccess = useCallback(() => {
    setModalVisible(false);
    void loadData();
  }, [loadData]);

  // 4. 渲染
  return (
    <div className={styles.xxxPage}>
      <XxxPanel
        data={data}
        loading={loading}
        onCreate={handleCreate}
        onEdit={handleEdit}
      />
      <XxxFormModal
        visible={modalVisible}
        data={selectedItem}
        onClose={() => setModalVisible(false)}
        onSuccess={handleFormSuccess}
      />
    </div>
  );
};

export default XxxPage;
```

### 页面类型定义

类型放在页面目录的 `types/index.ts` 中：

```typescript
/** XXX 项数据结构 */
export interface XxxItem {
  id: string;
  name: string;
  status: number;
  createTime: string;
}

/** XXX 查询条件 */
export interface XxxQuery {
  keyword?: string;
  status?: number;
}

/** XXX 保存请求 */
export interface XxxSaveRequest {
  id?: string;
  name: string;
  status?: number;
}
```

## 3.2 子组件开发模式

### 组件目录结构

每个独立组件放在自己的目录中：

```
components/XxxPanel/
├── index.tsx               # 组件实现
└── index.module.less       # 组件样式
```

### 组件模板

```tsx
import React from 'react';
import { Card, Button } from 'antd';
import styles from './index.module.less';

interface XxxPanelProps {
  /** 数据列表 */
  data: XxxItem[];
  /** 加载状态 */
  loading: boolean;
  /** 创建回调 */
  onCreate: () => void;
  /** 编辑回调 */
  onEdit: (item: XxxItem) => void;
}

const XxxPanel: React.FC<XxxPanelProps> = ({ data, loading, onCreate, onEdit }) => {
  return (
    <Card
      title="XXX 列表"
      extra={<Button type="primary" onClick={onCreate}>新增</Button>}
      loading={loading}
      className={styles.panel}
    >
      {data.map((item) => (
        <div key={item.id} className={styles.item} onClick={() => onEdit(item)}>
          {item.name}
        </div>
      ))}
    </Card>
  );
};

export default XxxPanel;
```

### 表单弹窗/抽屉组件

表单弹窗和抽屉是常见的独立组件模式：

```tsx
import React, { useState, useEffect } from 'react';
import { Modal, Form, Input, message } from 'antd';
import styles from './index.module.less';
import { saveXxx } from '@/services/xxx';
import type { XxxItem } from '../../types';

interface XxxFormModalProps {
  /** 是否显示 */
  visible: boolean;
  /** 编辑模式下的数据 */
  data?: XxxItem | null;
  /** 关闭回调 */
  onClose: () => void;
  /** 保存成功回调 */
  onSuccess: () => void;
}

const XxxFormModal: React.FC<XxxFormModalProps> = ({ visible, data, onClose, onSuccess }) => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const isEdit = !!data?.id;

  // 编辑模式下回填数据
  useEffect(() => {
    if (visible) {
      if (data) {
        form.setFieldsValue(data);
      } else {
        form.resetFields();
      }
    }
  }, [visible, data, form]);

  const handleOk = async () => {
    try {
      const values = await form.validateFields();
      setLoading(true);
      await saveXxx({ ...data, ...values });
      message.success(isEdit ? '编辑成功' : '新增成功');
      onSuccess();
    } catch (error) {
      // 表单校验失败或请求错误，由各自机制处理
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal
      title={isEdit ? '编辑' : '新增'}
      open={visible}
      onOk={handleOk}
      onCancel={onClose}
      confirmLoading={loading}
      className={styles.modal}
    >
      <Form form={form} layout="vertical">
        <Form.Item name="name" label="名称" rules={[{ required: true, message: '请输入名称' }]}>
          <Input placeholder="请输入名称" />
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default XxxFormModal;
```

## 3.3 自定义 Hooks

页面级的复杂逻辑可以抽离为自定义 Hooks，放在 `hooks/` 目录：

```tsx
// hooks/useXxxTree.tsx
import { useState, useCallback, useEffect } from 'react';
import { message } from 'antd';
import { getXxxTree, getXxxDetail } from '@/services/xxx';
import type { XxxTreeNode, XxxDetail } from '../types';

export function useXxxTree() {
  const [treeData, setTreeData] = useState<XxxTreeNode[]>([]);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [selectedDetail, setSelectedDetail] = useState<XxxDetail | null>(null);
  const [loading, setLoading] = useState(false);

  const loadTree = useCallback(async () => {
    setLoading(true);
    try {
      const tree = await getXxxTree();
      setTreeData(tree);
      // 刷新时同步刷新选中项的详情
      if (selectedId) {
        const detail = await getXxxDetail(String(selectedId));
        setSelectedDetail(detail);
      }
    } catch {
      message.error('加载树失败');
    } finally {
      setLoading(false);
    }
  }, [selectedId]);

  useEffect(() => {
    void loadTree();
  }, [loadTree]);

  const handleSelect = useCallback(async (id: number) => {
    setSelectedId(id);
    const detail = await getXxxDetail(String(id));
    setSelectedDetail(detail);
  }, []);

  return {
    treeData,
    selectedId,
    selectedDetail,
    loading,
    loadTree,
    handleSelect,
  };
}
```

使用：

```tsx
const { treeData, selectedDetail, loading, loadTree, handleSelect } = useXxxTree();
```

## 3.4 主应用布局模式

主应用布局是特殊的，它包含 `ThemeLayout`、顶部导航栏和 `<MicroApp>` 渲染区域：

```tsx
import { ThemeLayout, useThemeContext, EventType, onEvent } from '@gwsu/core';
import { MicroApp, history, useLocation } from 'umi';
import styles from './index.module.less';

export default function LayoutComponent() {
  return (
    <ThemeLayout>
      <LayoutInner />
    </ThemeLayout>
  );
}

function LayoutInner() {
  const location = useLocation();
  const { currentTheme, changeTheme } = useThemeContext();
  const isLoginPage = location.pathname.includes('/login');

  // 登录事件监听
  useEffect(() => {
    const unsubLogin = onEvent(EventType.LOGIN_SUCCESS, () => {
      history.push('/sub-system/dashboard');
    });
    const unsubExpire = onEvent(EventType.TOKEN_EXPIRED, () => {
      history.push('/sub-system/login');
    });
    return () => {
      unsubLogin();
      unsubExpire();
    };
  }, []);

  // 判断当前子应用
  const currentApp = location.pathname.startsWith('/sub-security')
    ? 'gwsu-sub-security'
    : 'gwsu-sub-system';

  if (isLoginPage) {
    return (
      <div className={`${styles.mainLayout} ${styles.loginMode}`}>
        <div className={styles.loginContent}>
          <MicroApp name={currentApp} />
        </div>
      </div>
    );
  }

  return (
    <div className={styles.mainLayout}>
      <header className={styles.mainHeader} style={{ background: currentTheme.colors.surface }}>
        {/* 顶部导航栏 */}
      </header>
      <div className={styles.contentLayout}>
        <MicroApp name={currentApp} />
      </div>
    </div>
  );
}
```

## 3.5 子应用布局模式

子应用布局统一使用 `ThemeLayout` 包裹 `<Outlet />`：

```tsx
import React from 'react';
import { Outlet } from 'umi';
import { ThemeLayout } from '@gwsu/core';

const Layout: React.FC = () => {
  return (
    <ThemeLayout>
      <Outlet />
    </ThemeLayout>
  );
};

export default Layout;
```

**关键点**：
- `ThemeLayout` 必须包裹子应用的全部内容，确保主题同步
- `<Outlet />` 是 UmiJS 路由的出口，渲染匹配的页面组件
- 子应用的 `ThemeLayout` 会通过 `window.postMessage` 监听主应用的主题变更事件

## 3.6 AI 操作审批标记（data-ai-approval）

AI 助手可以通过 `GetPageState` 工具查看界面元素，并通过 `ClickElement` 工具点击元素。对于涉及数据变更的危险操作，需要添加 `data-ai-approval` 属性，使 AI 在点击时触发人工审批流程。

### 何时添加 data-ai-approval

**必须添加**的场景（涉及数据持久化或不可逆操作）：

| 操作类型 | 示例 | 说明 |
|---------|------|------|
| 保存/提交 | 保存、提交、确认 | 表单数据写入后端 |
| 编辑/修改 | 编辑、修改、更新 | 修改已有数据 |
| 删除 | 删除、移除、清空 | 数据不可逆删除 |
| 审核/审批 | 通过、驳回、审批 | 状态流转操作 |
| 导入/导出 | 导入、导出、下载 | 批量数据操作 |
| 权限变更 | 授权、分配权限 | 安全敏感操作 |

**不需要添加**的场景：

| 操作类型 | 示例 | 说明 |
|---------|------|------|
| 纯查看 | 查看、详情、预览 | 不修改数据 |
| 导航/跳转 | 跳转、返回、切换Tab | 仅路由变化 |
| 筛选/搜索 | 搜索、筛选、翻页 | 只读查询 |
| 展开/折叠 | 展开、折叠、收起 | UI状态切换 |
| 弹窗打开 | 新增按钮（打开空表单） | 仅打开表单，尚未提交 |

### 用法

在按钮元素上添加 `data-ai-approval` 属性（无需赋值，仅作为标识）：

```tsx
// 表单保存按钮
<Button type="primary" data-ai-approval onClick={handleSave}>
  保存
</Button>

// 删除按钮
<Button danger data-ai-approval onClick={handleDelete}>
  删除
</Button>

// 编辑按钮
<Button data-ai-approval onClick={handleEdit}>
  编辑
</Button>
```

### 组件内置按钮的标注

当使用 Ant Design 组件的内置按钮（如 Modal 的确认按钮）时，无法直接在 `<Button>` 上添加属性。这些组件提供了 `okButtonProps` API 来透传属性给内置确认按钮：

```tsx
// Modal 的确认按钮需要审批
<Modal
  title="编辑"
  open={visible}
  onOk={handleOk}
  onCancel={onClose}
  okButtonProps={{ 'data-ai-approval': true }}
>
  {/* 表单内容 */}
</Modal>
```

同理，`cancelButtonProps` 可用于取消按钮（一般不需要审批）。

### AI 视角与审批流程

1. **GetPageState** 返回的简化 HTML 中，带审批标记的元素会显示 `{approval}` 标签：
   ```
   [0]{approval}<button>保存</button>
   [1]<button>取消</button>
   ```

2. **ClickElement** 调用时，AI 会将 `tags` 参数传给后端（如 `tags: "approval"`）

3. 后端 `NeedClickApprovalCondition` 判断 `tags` 包含 `approval`，触发 `@HumanInTheLoop` 人工审批

4. 前端收到审批事件，展示审批确认 UI，用户确认后才执行点击操作

### 完整示例

```tsx
// 典型的表单弹窗：保存按钮需要审批，取消按钮不需要
const XxxFormModal: React.FC<XxxFormModalProps> = ({ visible, data, onClose, onSuccess }) => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const isEdit = !!data?.id;

  const handleOk = async () => {
    const values = await form.validateFields();
    setLoading(true);
    await saveXxx({ ...data, ...values });
    message.success(isEdit ? '编辑成功' : '新增成功');
    onSuccess();
  };

  return (
    <Modal
      title={isEdit ? '编辑' : '新增'}
      open={visible}
      onCancel={onClose}
      footer={[
        <Button key="cancel" onClick={onClose}>取消</Button>,
        <Button key="submit" type="primary" data-ai-approval loading={loading} onClick={handleOk}>
          保存
        </Button>,
      ]}
    >
      <Form form={form} layout="vertical">
        {/* 表单内容 */}
      </Form>
    </Modal>
  );
};
```

```tsx
// 使用 okButtonProps 给 Modal 内置确认按钮添加审批标记
<Modal
  title="确定删除该条记录吗？"
  open={visible}
  onOk={() => handleDelete(record.id)}
  okButtonProps={{ 'data-ai-approval': 'true' }}
  onCancel={onClose}
>
  <p>删除后数据将无法恢复，请确认操作。</p>
</Modal>
```
