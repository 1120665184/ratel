import React, { useState, useCallback, useEffect } from 'react';
import {
  Button,
  Table,
  Tag,
  Dropdown,
  Space,
  Popconfirm,
  type MenuProps,
} from 'antd';
import type { TableProps } from 'antd';
import {
  PlusOutlined,
  DeleteOutlined,
  MoreOutlined,
  EditOutlined,
  CheckCircleOutlined,
  LinkOutlined,
  AppstoreOutlined,
} from '@ant-design/icons';
import { AuthGate, useAuth } from '@gwsu/core';
import styles from './index.module.less';
import ComponentPool from './components/ComponentPool';
import CatalogFormModal from './components/CatalogFormModal';
import CatalogComponentBind from './components/CatalogComponentBind';
import type { CatalogInfo } from './types';
import {
  getCatalogList,
  deleteCatalogs,
  activateCatalog,
} from './services/catalog';
import {
  PERM_CATALOG_ADD,
  PERM_CATALOG_EDIT,
  PERM_CATALOG_REMOVE,
  PERM_CATALOG_ACTIVATE,
  PERM_BIND_COMPONENT,
} from './permissionConstants';

const CatalogPage: React.FC = () => {
  const canEdit = useAuth(PERM_CATALOG_EDIT);
  const canActivate = useAuth(PERM_CATALOG_ACTIVATE);
  const canBind = useAuth(PERM_BIND_COMPONENT);

  // Catalog 列表
  const [catalogData, setCatalogData] = useState<CatalogInfo[]>([]);
  const [loading, setLoading] = useState(false);
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([]);

  // Catalog 表单弹窗
  const [formModalVisible, setFormModalVisible] = useState(false);
  const [editData, setEditData] = useState<CatalogInfo | null>(null);

  // 右侧面板 Tab
  const [activeTab, setActiveTab] = useState<'pool' | 'bind'>('pool');
  const [selectedCatalog, setSelectedCatalog] = useState<CatalogInfo | null>(null);

  /** 加载 Catalog 列表 */
  const fetchData = useCallback(async () => {
    try {
      setLoading(true);
      const list = await getCatalogList();
      setCatalogData(list);
    } catch {
      // 请求层自动提示
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  /** 新增 Catalog */
  const handleCreate = useCallback(() => {
    setEditData(null);
    setFormModalVisible(true);
  }, []);

  /** 编辑 Catalog */
  const handleEdit = useCallback((record: CatalogInfo) => {
    setEditData(record);
    setFormModalVisible(true);
  }, []);

  /** 激活 Catalog */
  const handleActivate = useCallback(
    async (record: CatalogInfo) => {
      try {
        await activateCatalog(record.id!);
        fetchData();
      } catch {
        // 请求层自动提示
      }
    },
    [fetchData],
  );

  /** 关联组件 */
  const handleBindComponent = useCallback((record: CatalogInfo) => {
    setSelectedCatalog(record);
    setActiveTab('bind');
  }, []);

  /** 批量删除 */
  const handleBatchDelete = useCallback(async () => {
    try {
      const ids = selectedRowKeys as string[];
      await deleteCatalogs(ids);
      setSelectedRowKeys([]);
      fetchData();
    } catch {
      // 请求层自动提示
    }
  }, [selectedRowKeys, fetchData]);

  /** 获取操作菜单项 */
  const getActionItems = (record: CatalogInfo): MenuProps['items'] => {
    const items = [];
    if (canEdit) {
      items.push({
        key: 'edit',
        icon: <EditOutlined />,
        label: '编辑',
        onClick: () => handleEdit(record),
      });
    }
    if (canActivate && !record.active) {
      items.push({
        key: 'activate',
        icon: <CheckCircleOutlined />,
        label: '激活',
        onClick: () => handleActivate(record),
      });
    }
    if (canBind) {
      items.push({
        key: 'bind',
        icon: <LinkOutlined />,
        label: '关联组件',
        onClick: () => handleBindComponent(record),
      });
    }
    return items;
  };

  /** 表格列定义 */
  const columns: TableProps<CatalogInfo>['columns'] = [
    Table.SELECTION_COLUMN,
    {
      title: 'Catalog Key',
      dataIndex: 'catalogKey',
      width: 140,
    },
    {
      title: 'Catalog 名称',
      dataIndex: 'catalogName',
      width: 160,
    },
    {
      title: '描述',
      dataIndex: 'description',
      ellipsis: true,
    },
    {
      title: '状态',
      dataIndex: 'active',
      width: 80,
      render: (val: number) =>
        val ? (
          <Tag color="green" icon={<CheckCircleOutlined />}>激活</Tag>
        ) : (
          <Tag color="default">未激活</Tag>
        ),
    },
    {
      title: '操作',
      width: 180,
      fixed: 'right',
      render: (_: unknown, record: CatalogInfo) => (
        <div className={styles.actionColumn}>
          <Dropdown menu={{ items: getActionItems(record) }}>
            <Button type="link" size="small" icon={<MoreOutlined />}>
              更多
            </Button>
          </Dropdown>
        </div>
      ),
    },
  ];

  return (
    <div className={styles.catalogPage}>
      {/* 左侧：Catalog 列表 */}
      <div className={styles.leftPanel}>
        <div className={styles.panelHeader}>
          <span className={styles.panelTitle}>Catalog 列表</span>
          <Space>
            <AuthGate buttonKey={PERM_CATALOG_ADD}>
              <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>
                新增
              </Button>
            </AuthGate>
            <AuthGate buttonKey={PERM_CATALOG_REMOVE}>
              <Popconfirm
                title="批量删除"
                description={`确定删除选中的 ${selectedRowKeys.length} 个 Catalog？`}
                onConfirm={handleBatchDelete}
                okText="确定"
                cancelText="取消"
                disabled={selectedRowKeys.length === 0}
              >
                <Button
                  danger
                  icon={<DeleteOutlined />}
                  disabled={selectedRowKeys.length === 0}
                  data-ai-approval
                >
                  删除
                </Button>
              </Popconfirm>
            </AuthGate>
          </Space>
        </div>
        <Table<CatalogInfo>
          rowKey="id"
          rowSelection={{
            selectedRowKeys,
            onChange: setSelectedRowKeys,
          }}
          columns={columns}
          dataSource={catalogData}
          loading={loading}
          size="middle"
          scroll={{ y: 'calc(100vh - 280px)' }}
          pagination={false}
        />
      </div>

      {/* 右侧：组件池 / 关联组件 */}
      <div className={styles.rightPanel}>
        <div className={styles.tabBar}>
          <Button
            type={activeTab === 'pool' ? 'primary' : 'default'}
            icon={<AppstoreOutlined />}
            onClick={() => setActiveTab('pool')}
          >
            组件池
          </Button>
          {selectedCatalog && (
            <Button
              type={activeTab === 'bind' ? 'primary' : 'default'}
              icon={<LinkOutlined />}
              onClick={() => setActiveTab('bind')}
            >
              关联组件
            </Button>
          )}
        </div>
        <div className={styles.tabContent}>
          {activeTab === 'pool' && <ComponentPool />}
          {activeTab === 'bind' && selectedCatalog && (
            <CatalogComponentBind
              catalogId={selectedCatalog.id!}
              catalogName={selectedCatalog.catalogName}
              onClose={() => setActiveTab('pool')}
            />
          )}
        </div>
      </div>

      {/* 新增/编辑 Catalog 弹窗 */}
      <CatalogFormModal
        visible={formModalVisible}
        data={editData}
        onClose={() => setFormModalVisible(false)}
        onSuccess={() => {
          setFormModalVisible(false);
          fetchData();
        }}
      />
    </div>
  );
};

export default CatalogPage;
