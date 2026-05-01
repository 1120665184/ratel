import React, { useState, useCallback, useEffect } from 'react';
import {
  Button,
  Table,
  Input,
  Select,
  Tag,
  Switch,
  Dropdown,
  Modal,
  Form,
  Space,
} from 'antd';
import type { TableProps } from 'antd';
import {
  PlusOutlined,
  SearchOutlined,
  ReloadOutlined,
  MoreOutlined,
  EyeOutlined,
  EditOutlined,
  MenuOutlined,
  LockOutlined,
  TableOutlined,
} from '@ant-design/icons';
import styles from './index.module.less';
import RoleDetail from './components/RoleDetail';
import RoleFormModal from './components/RoleFormModal';
import { useRole } from './hooks/useRole';
import type { RoleInfo, RoleQuery } from './types';
import { ROLE_TYPE_OPTIONS, DATA_SCOPE_OPTIONS } from './types';

const STATUS_OPTIONS = [
  { label: '启用', value: true },
  { label: '禁用', value: false },
];

const RolePage: React.FC = () => {
  const {
    loading,
    dataSource,
    total,
    currentPage,
    pageSize,
    fetchRolePage,
    ensureInitialized,
    handlePageChange,
    handleSaveOrUpdate,
    handleDelete,
    handleToggleStatus,
  } = useRole();

  const [searchForm] = Form.useForm<RoleQuery>();

  // 详情抽屉
  const [detailVisible, setDetailVisible] = useState(false);
  const [detailRole, setDetailRole] = useState<RoleInfo | null>(null);

  // 新增/编辑弹窗
  const [formModalVisible, setFormModalVisible] = useState(false);
  const [formModalMode, setFormModalMode] = useState<'create' | 'edit'>('create');
  const [formModalData, setFormModalData] = useState<RoleInfo | null>(null);

  /** 初始化加载 */
  useEffect(() => {
    ensureInitialized();
  }, [ensureInitialized]);

  /** 搜索 */
  const handleSearch = useCallback(() => {
    const values = searchForm.getFieldsValue();
    fetchRolePage({ ...values, pageNum: 1 });
  }, [searchForm, fetchRolePage]);

  /** 重置搜索 */
  const handleReset = useCallback(() => {
    searchForm.resetFields();
    fetchRolePage({ pageNum: 1 });
  }, [searchForm, fetchRolePage]);

  /** 查看详情 */
  const handleViewDetail = useCallback((role: RoleInfo) => {
    setDetailRole(role);
    setDetailVisible(true);
  }, []);

  /** 新增角色 */
  const handleCreate = useCallback(() => {
    setFormModalMode('create');
    setFormModalData(null);
    setFormModalVisible(true);
  }, []);

  /** 编辑角色 */
  const handleEdit = useCallback((role: RoleInfo) => {
    setFormModalMode('edit');
    setFormModalData(role);
    setFormModalVisible(true);
  }, []);

  /** 保存角色（由弹窗调用） */
  const handleSave = useCallback(
    async (data: RoleInfo): Promise<boolean> => {
      return handleSaveOrUpdate(data);
    },
    [handleSaveOrUpdate],
  );

  /** 状态切换 */
  const handleStatusChange = useCallback(
    async (role: RoleInfo, checked: boolean) => {
      await handleToggleStatus(role.id!, checked);
    },
    [handleToggleStatus],
  );

  /** 占位功能提示 */
  const handlePlaceholder = useCallback((title: string) => {
    Modal.warning({
      title,
      content: '功能开发中，敬请期待',
    });
  }, []);

  /** 获取数据范围的文字描述 */
  const getDataScopeLabel = (value: number): string => {
    return DATA_SCOPE_OPTIONS.find((o) => o.value === value)?.label ?? '未知';
  };

  /** 表格列定义 */
  const columns: TableProps<RoleInfo>['columns'] = [
    {
      title: '序号',
      width: 60,
      align: 'center',
      render: (_: unknown, __: RoleInfo, index: number) =>
        (currentPage - 1) * pageSize + index + 1,
    },
    {
      title: '角色编码',
      dataIndex: 'roleCode',
      width: 160,
      render: (val: string) => <code>{val}</code>,
    },
    {
      title: '角色名称',
      dataIndex: 'roleName',
      width: 160,
    },
    {
      title: '角色类型',
      dataIndex: 'roleType',
      width: 120,
      render: (val: number) => (
        <Tag color={val === 1 ? 'blue' : 'orange'}>
          {ROLE_TYPE_OPTIONS.find((o) => o.value === val)?.label ?? '未知'}
        </Tag>
      ),
    },
    {
      title: '数据范围',
      dataIndex: 'dataScope',
      width: 140,
      render: (val: number) => getDataScopeLabel(val),
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 120,
      render: (val: boolean, record: RoleInfo) => (
        <Space>
          <Switch
            size="small"
            checked={val}
            onChange={(checked) => handleStatusChange(record, checked)}
          />
          <Tag color={val ? 'green' : 'red'}>{val ? '启用' : '禁用'}</Tag>
        </Space>
      ),
    },
    {
      title: '操作',
      width: 200,
      fixed: 'right',
      render: (_: unknown, record: RoleInfo) => (
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
            menu={{
              items: [
                {
                  key: 'edit',
                  icon: <EditOutlined />,
                  label: '编辑',
                  onClick: () => handleEdit(record),
                },
                {
                  key: 'menuPermission',
                  icon: <MenuOutlined />,
                  label: '菜单权限',
                  onClick: () => handlePlaceholder('菜单权限'),
                },
                {
                  key: 'fieldPermission',
                  icon: <LockOutlined />,
                  label: '字段权限',
                  onClick: () => handlePlaceholder('字段权限'),
                },
                {
                  key: 'tablePermission',
                  icon: <TableOutlined />,
                  label: '表模型权限',
                  onClick: () => handlePlaceholder('表模型权限'),
                },
              ],
            }}
          >
            <Button type="link" size="small" icon={<MoreOutlined />}>
              更多
            </Button>
          </Dropdown>
        </div>
      ),
    },
  ];

  return (
    <div className={styles.rolePage}>
      {/* 搜索栏 */}
      <div className={styles.searchBar}>
        <Form form={searchForm} layout="inline" component={false}>
          <div className={styles.searchItem}>
            <span className={styles.searchLabel}>角色名称</span>
            <Form.Item name="roleName" noStyle>
              <Input
                placeholder="请输入角色名称"
                allowClear
                style={{ width: 180 }}
                onPressEnter={handleSearch}
              />
            </Form.Item>
          </div>
          <div className={styles.searchItem}>
            <span className={styles.searchLabel}>角色类型</span>
            <Form.Item name="roleType" noStyle>
              <Select
                placeholder="全部"
                allowClear
                style={{ width: 140 }}
                options={ROLE_TYPE_OPTIONS}
              />
            </Form.Item>
          </div>
          <div className={styles.searchItem}>
            <span className={styles.searchLabel}>数据范围</span>
            <Form.Item name="dataScope" noStyle>
              <Select
                placeholder="全部"
                allowClear
                style={{ width: 140 }}
                options={DATA_SCOPE_OPTIONS}
              />
            </Form.Item>
          </div>
          <div className={styles.searchItem}>
            <span className={styles.searchLabel}>状态</span>
            <Form.Item name="status" noStyle>
              <Select
                placeholder="全部"
                allowClear
                style={{ width: 120 }}
                options={STATUS_OPTIONS}
              />
            </Form.Item>
          </div>
        </Form>
        <div className={styles.searchActions}>
          <Button
            type="primary"
            icon={<SearchOutlined />}
            onClick={handleSearch}
          >
            查询
          </Button>
          <Button icon={<ReloadOutlined />} onClick={handleReset}>
            重置
          </Button>
        </div>
      </div>

      {/* 表格区域 */}
      <div className={styles.tableWrapper}>
        <div className={styles.tableHeader}>
          <span className={styles.tableTitle}>角色列表</span>
          <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>
            新增角色
          </Button>
        </div>
        <Table<RoleInfo>
          rowKey="id"
          columns={columns}
          dataSource={dataSource}
          loading={loading}
          size="middle"
          scroll={{ x: 960 }}
          pagination={{
            current: currentPage,
            pageSize,
            total,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (t) => `共 ${t} 条`,
            onChange: handlePageChange,
          }}
        />
      </div>

      {/* 角色详情抽屉 */}
      <RoleDetail
        visible={detailVisible}
        role={detailRole}
        onClose={() => setDetailVisible(false)}
      />

      {/* 新增/编辑角色弹窗 */}
      <RoleFormModal
        visible={formModalVisible}
        mode={formModalMode}
        data={formModalData}
        onSave={handleSave}
        onClose={() => setFormModalVisible(false)}
        onSuccess={() => setFormModalVisible(false)}
      />
    </div>
  );
};

export default RolePage;
