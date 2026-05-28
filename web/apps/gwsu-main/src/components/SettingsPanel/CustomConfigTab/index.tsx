import React, { useState, useCallback, useEffect, useRef } from 'react';
import {
  Table,
  Button,
  Input,
  Space,
  Popconfirm,
  Tag,
  App,
  Form,
} from 'antd';
import type { TableProps } from 'antd';
import {
  PlusOutlined,
  DeleteOutlined,
  EditOutlined,
  SearchOutlined,
  ReloadOutlined,
} from '@ant-design/icons';
import {
  getConfigPage,
  saveOrUpdateConfig,
  deleteConfigs,
} from '../services/config';
import type { ConfigInfo, ConfigQuery } from '../services/config';
import CustomConfigFormModal from './CustomConfigFormModal';
import styles from './index.module.less';

const VALUE_TYPE_MAP: Record<number, { label: string; color: string }> = {
  1: { label: '基本类型', color: 'blue' },
  2: { label: 'JSON', color: 'purple' },
};

const CustomConfigTab: React.FC = () => {
  const { message } = App.useApp();

  const [loading, setLoading] = useState(false);
  const [dataSource, setDataSource] = useState<ConfigInfo[]>([]);
  const [total, setTotal] = useState(0);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);

  const queryRef = useRef<ConfigQuery>({ configType: 2 });
  const initializedRef = useRef(false);

  // 搜索表单
  const [searchForm] = Form.useForm<ConfigQuery>();

  /** 分页查询 */
  const fetchConfigPage = useCallback(
    async (query?: ConfigQuery) => {
      if (query) {
        queryRef.current = query;
      }
      setLoading(true);
      try {
        const params: ConfigQuery = {
          ...queryRef.current,
          pageNum: query?.pageNum ?? currentPage,
          pageSize: query?.pageSize ?? pageSize,
        };
        const page = await getConfigPage(params);
        setDataSource(page?.records ?? []);
        setTotal(page?.total ?? 0);
        setCurrentPage(page?.current ?? 1);
        setPageSize(page?.size ?? 10);
      } catch {
        // request 层已自动提示
      } finally {
        setLoading(false);
      }
    },
    [currentPage, pageSize],
  );

  /** 初始化加载 */
  const ensureInitialized = useCallback(() => {
    if (!initializedRef.current) {
      initializedRef.current = true;
      fetchConfigPage({ configType: 2 });
    }
  }, [fetchConfigPage]);

  useEffect(() => {
    ensureInitialized();
  }, [ensureInitialized]);

  /** 翻页 */
  const handlePageChange = useCallback(
    (page: number, size: number) => {
      fetchConfigPage({ ...queryRef.current, pageNum: page, pageSize: size });
    },
    [fetchConfigPage],
  );

  /** 搜索 */
  const handleSearch = useCallback(() => {
    const values = searchForm.getFieldsValue();
    fetchConfigPage({ ...values, configType: 2, pageNum: 1 });
  }, [searchForm, fetchConfigPage]);

  /** 重置搜索 */
  const handleReset = useCallback(() => {
    searchForm.resetFields();
    fetchConfigPage({ configType: 2, pageNum: 1 });
  }, [searchForm, fetchConfigPage]);

  /** 删除 */
  const handleDelete = useCallback(
    async (ids: string[]) => {
      try {
        await deleteConfigs(ids);
        message.success('删除成功');
        await fetchConfigPage();
        return true;
      } catch {
        return false;
      }
    },
    [fetchConfigPage, message],
  );

  // 表格选中行
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([]);

  /** 批量删除 */
  const handleBatchDelete = useCallback(async () => {
    const ids = selectedRowKeys as string[];
    const success = await handleDelete(ids);
    if (success) {
      setSelectedRowKeys([]);
    }
  }, [selectedRowKeys, handleDelete]);

  // 新增/编辑弹窗
  const [formModalVisible, setFormModalVisible] = useState(false);
  const [formModalMode, setFormModalMode] = useState<'create' | 'edit'>('create');
  const [formModalData, setFormModalData] = useState<ConfigInfo | null>(null);

  /** 新增 */
  const handleCreate = useCallback(() => {
    setFormModalMode('create');
    setFormModalData(null);
    setFormModalVisible(true);
  }, []);

  /** 编辑 */
  const handleEdit = useCallback((record: ConfigInfo) => {
    setFormModalMode('edit');
    setFormModalData(record);
    setFormModalVisible(true);
  }, []);

  /** 保存 */
  const handleSave = useCallback(
    async (data: Partial<ConfigInfo>): Promise<boolean> => {
      try {
        await saveOrUpdateConfig(data);
        message.success(data.id ? '编辑成功' : '新增成功');
        await fetchConfigPage();
        return true;
      } catch {
        return false;
      }
    },
    [fetchConfigPage, message],
  );

  /** 截断显示配置值 */
  const truncateValue = (val: string, maxLen = 50): string => {
    if (!val) return '';
    return val.length > maxLen ? val.substring(0, maxLen) + '...' : val;
  };

  /** 表格列定义 */
  const columns: TableProps<ConfigInfo>['columns'] = [
    Table.SELECTION_COLUMN,
    {
      title: '序号',
      width: 60,
      align: 'center',
      render: (_: unknown, __: ConfigInfo, index: number) =>
        (currentPage - 1) * pageSize + index + 1,
    },
    {
      title: '配置键',
      dataIndex: 'configKey',
      width: 180,
      render: (val: string) => <code>{val}</code>,
    },
    {
      title: '配置名称',
      dataIndex: 'configName',
      width: 160,
    },
    {
      title: '配置值',
      dataIndex: 'configValue',
      ellipsis: true,
      render: (val: string) => <span title={val}>{truncateValue(val)}</span>,
    },
    {
      title: '值类型',
      dataIndex: 'valueType',
      width: 100,
      render: (val: number) => {
        const typeInfo = VALUE_TYPE_MAP[val];
        return typeInfo ? (
          <Tag color={typeInfo.color}>{typeInfo.label}</Tag>
        ) : (
          <Tag>未知</Tag>
        );
      },
    },
    {
      title: '描述',
      dataIndex: 'description',
      ellipsis: true,
    },
    {
      title: '更新时间',
      dataIndex: 'modifyTime',
      width: 180,
    },
    {
      title: '操作',
      width: 140,
      fixed: 'right',
      render: (_: unknown, record: ConfigInfo) => (
        <div className={styles.actionColumn}>
          <Button
            type="link"
            size="small"
            icon={<EditOutlined />}
            onClick={() => handleEdit(record)}
          >
            编辑
          </Button>
          <Popconfirm
            title="删除确认"
            description={`确定删除配置「${record.configName}」？`}
            onConfirm={() => handleDelete([record.id!])}
            okText="确定"
            cancelText="取消"
          >
            <Button type="link" size="small" danger>
              删除
            </Button>
          </Popconfirm>
        </div>
      ),
    },
  ];

  return (
    <div className={styles.customConfigTab}>
      {/* 搜索栏 */}
      <div className={styles.searchBar}>
        <Form form={searchForm} layout="inline" component={false}>
          <div className={styles.searchItem}>
            <span className={styles.searchLabel}>配置键</span>
            <Form.Item name="configKey" noStyle>
              <Input
                placeholder="请输入配置键"
                allowClear
                style={{ width: 180 }}
                onPressEnter={handleSearch}
              />
            </Form.Item>
          </div>
          <div className={styles.searchItem}>
            <span className={styles.searchLabel}>配置名称</span>
            <Form.Item name="configName" noStyle>
              <Input
                placeholder="请输入配置名称"
                allowClear
                style={{ width: 180 }}
                onPressEnter={handleSearch}
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
          <span className={styles.tableTitle}>自定义配置列表</span>
          <Space>
            <Button
              type="primary"
              icon={<PlusOutlined />}
              onClick={handleCreate}
            >
              新增配置
            </Button>
            <Popconfirm
              title="批量删除"
              description={`确定删除选中的 ${selectedRowKeys.length} 个配置？`}
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
          </Space>
        </div>
        <Table<ConfigInfo>
          rowKey="id"
          rowSelection={{
            selectedRowKeys,
            onChange: setSelectedRowKeys,
          }}
          columns={columns}
          dataSource={dataSource}
          loading={loading}
          size="middle"
          scroll={{ x: 1000 }}
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

      {/* 新增/编辑弹窗 */}
      <CustomConfigFormModal
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

export default CustomConfigTab;
