import React, { useState, useCallback, useEffect } from 'react';
import { Button, Table, Tag, Dropdown, Space, Popconfirm, type MenuProps } from 'antd';
import type { TableProps } from 'antd';
import {
  PlusOutlined,
  DeleteOutlined,
  EditOutlined,
  MoreOutlined,
} from '@ant-design/icons';
import { AuthGate } from '@gwsu/core';
import styles from './index.module.less';
import ComponentFormModal from '../ComponentFormModal';
import type { CatalogComponentInfo } from '../../types';
import { getComponentList, deleteComponents } from '../../services/catalog';
import { PERM_COMPONENT_ADD, PERM_COMPONENT_EDIT, PERM_COMPONENT_REMOVE } from '../../permissionConstants';

const CATEGORY_COLOR_MAP: Record<string, string> = {
  display: 'blue',
  chart: 'green',
  form: 'orange',
};

const ComponentPool: React.FC = () => {
  const [dataSource, setDataSource] = useState<CatalogComponentInfo[]>([]);
  const [loading, setLoading] = useState(false);
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([]);

  // 组件表单弹窗
  const [formModalVisible, setFormModalVisible] = useState(false);
  const [editData, setEditData] = useState<CatalogComponentInfo | null>(null);

  /** 加载组件列表 */
  const fetchData = useCallback(async () => {
    try {
      setLoading(true);
      const list = await getComponentList();
      setDataSource(list);
    } catch {
      // 请求层自动提示
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  /** 新增组件 */
  const handleCreate = useCallback(() => {
    setEditData(null);
    setFormModalVisible(true);
  }, []);

  /** 编辑组件 */
  const handleEdit = useCallback((record: CatalogComponentInfo) => {
    setEditData(record);
    setFormModalVisible(true);
  }, []);

  /** 批量删除 */
  const handleBatchDelete = useCallback(async () => {
    try {
      const ids = selectedRowKeys as string[];
      await deleteComponents(ids);
      setSelectedRowKeys([]);
      fetchData();
    } catch {
      // 请求层自动提示
    }
  }, [selectedRowKeys, fetchData]);

  /** 获取操作菜单项 */
  const getActionItems = (record: CatalogComponentInfo): MenuProps['items'] => {
    const items = [];
    items.push({
      key: 'edit',
      icon: <EditOutlined />,
      label: '编辑',
      onClick: () => handleEdit(record),
    });
    return items;
  };

  /** 表格列定义 */
  const columns: TableProps<CatalogComponentInfo>['columns'] = [
    Table.SELECTION_COLUMN,
    {
      title: '组件名称',
      dataIndex: 'componentName',
      width: 160,
    },
    {
      title: '描述',
      dataIndex: 'description',
      ellipsis: true,
    },
    {
      title: '分类',
      dataIndex: 'category',
      width: 100,
      render: (val: string) => (
        <Tag color={CATEGORY_COLOR_MAP[val] ?? 'default'}>{val ?? '-'}</Tag>
      ),
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 80,
      render: (val: boolean) => (
        <Tag color={val ? 'green' : 'red'}>{val ? '启用' : '禁用'}</Tag>
      ),
    },
    {
      title: '排序',
      dataIndex: 'sortOrder',
      width: 80,
      align: 'center',
    },
    {
      title: '操作',
      width: 160,
      fixed: 'right',
      render: (_: unknown, record: CatalogComponentInfo) => (
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
    <div className={styles.poolWrapper}>
      <div className={styles.poolHeader}>
        <span className={styles.poolTitle}>组件池</span>
        <Space>
          <AuthGate buttonKey={PERM_COMPONENT_ADD}>
            <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>
              新增组件
            </Button>
          </AuthGate>
          <AuthGate buttonKey={PERM_COMPONENT_REMOVE}>
            <Popconfirm
              title="批量删除"
              description={`确定删除选中的 ${selectedRowKeys.length} 个组件？`}
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
      <Table<CatalogComponentInfo>
        rowKey="id"
        rowSelection={{
          selectedRowKeys,
          onChange: setSelectedRowKeys,
        }}
        columns={columns}
        dataSource={dataSource}
        loading={loading}
        size="middle"
        scroll={{ y: 400 }}
        pagination={false}
      />

      {/* 新增/编辑组件弹窗 */}
      <ComponentFormModal
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

export default ComponentPool;
