import React, { useState, useCallback, useEffect, useRef } from 'react';
import {
  Table,
  Button,
  Modal,
  Form,
  Input,
  Space,
  Popconfirm,
  App,
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
  getDictPage,
  saveOrUpdateDict,
  deleteDicts,
} from '../services/dict';
import type { DictInfo, DictQuery } from '../services/dict';
import styles from './index.module.less';

const { TextArea } = Input;

interface DictKeyListProps {
  selectedDict: DictInfo | null;
  onSelect: (dict: DictInfo | null) => void;
}

const DictKeyList: React.FC<DictKeyListProps> = ({
  selectedDict,
  onSelect,
}) => {
  const { message } = App.useApp();

  const [loading, setLoading] = useState(false);
  const [dataSource, setDataSource] = useState<DictInfo[]>([]);
  const [total, setTotal] = useState(0);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);

  const queryRef = useRef<DictQuery>({});
  const initializedRef = useRef(false);

  // 搜索表单
  const [searchForm] = Form.useForm<DictQuery>();

  /** 分页查询 */
  const fetchDictPage = useCallback(
    async (query?: DictQuery) => {
      if (query) {
        queryRef.current = query;
      }
      setLoading(true);
      try {
        const params: DictQuery = {
          ...queryRef.current,
          pageNum: query?.pageNum ?? currentPage,
          pageSize: query?.pageSize ?? pageSize,
        };
        const page = await getDictPage(params);
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
      fetchDictPage();
    }
  }, [fetchDictPage]);

  useEffect(() => {
    ensureInitialized();
  }, [ensureInitialized]);

  /** 翻页 */
  const handlePageChange = useCallback(
    (page: number, size: number) => {
      fetchDictPage({ ...queryRef.current, pageNum: page, pageSize: size });
    },
    [fetchDictPage],
  );

  /** 搜索 */
  const handleSearch = useCallback(() => {
    const values = searchForm.getFieldsValue();
    fetchDictPage({ ...values, pageNum: 1 });
  }, [searchForm, fetchDictPage]);

  /** 重置搜索 */
  const handleReset = useCallback(() => {
    searchForm.resetFields();
    fetchDictPage({ pageNum: 1 });
  }, [searchForm, fetchDictPage]);

  // 新增/编辑弹窗
  const [formModalVisible, setFormModalVisible] = useState(false);
  const [formModalMode, setFormModalMode] = useState<'create' | 'edit'>('create');
  const [formModalData, setFormModalData] = useState<DictInfo | null>(null);
  const [form] = Form.useForm();
  const [formLoading, setFormLoading] = useState(false);

  /** 新增 */
  const handleCreate = useCallback(() => {
    setFormModalMode('create');
    setFormModalData(null);
    form.resetFields();
    form.setFieldsValue({ dictType: 2 });
    setFormModalVisible(true);
  }, [form]);

  /** 编辑 */
  const handleEdit = useCallback(
    (record: DictInfo) => {
      setFormModalMode('edit');
      setFormModalData(record);
      form.setFieldsValue({
        dictKey: record.dictKey,
        dictName: record.dictName,
        description: record.description,
        dictType: record.dictType,
      });
      setFormModalVisible(true);
    },
    [form],
  );

  /** 保存字典 */
  const handleFormOk = async () => {
    try {
      const values = await form.validateFields();
      setFormLoading(true);
      const isEdit = formModalMode === 'edit';
      const reqData: Partial<DictInfo> = {
        ...values,
        id: isEdit ? formModalData?.id : undefined,
      };
      await saveOrUpdateDict(reqData);
      message.success(isEdit ? '编辑成功' : '新增成功');
      setFormModalVisible(false);
      await fetchDictPage();
    } catch {
      // 表单校验失败或请求错误
    } finally {
      setFormLoading(false);
    }
  };

  /** 删除字典 */
  const handleDelete = useCallback(
    async (ids: string[]) => {
      try {
        await deleteDicts(ids);
        message.success('删除成功');
        // 如果删除的是当前选中的字典，清除选中
        if (selectedDict && ids.includes(selectedDict.id!)) {
          onSelect(null);
        }
        await fetchDictPage();
      } catch {
        // request 层已自动提示
      }
    },
    [fetchDictPage, selectedDict, onSelect],
  );

  // 表格选中行
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([]);

  /** 批量删除 */
  const handleBatchDelete = useCallback(async () => {
    const ids = selectedRowKeys as string[];
    await handleDelete(ids);
    setSelectedRowKeys([]);
  }, [selectedRowKeys, handleDelete]);

  /** 表格列定义 */
  const columns: TableProps<DictInfo>['columns'] = [
    {
      title: '序号',
      width: 50,
      align: 'center',
      render: (_: unknown, __: DictInfo, index: number) =>
        (currentPage - 1) * pageSize + index + 1,
    },
    {
      title: '字典键',
      dataIndex: 'dictKey',
      width: 140,
      render: (val: string) => <code>{val}</code>,
    },
    {
      title: '字典名称',
      dataIndex: 'dictName',
      width: 120,
    },
    {
      title: '描述',
      dataIndex: 'description',
      ellipsis: true,
    },
    {
      title: '值数量',
      dataIndex: 'valueCount',
      width: 70,
      align: 'center',
    },
    {
      title: '操作',
      width: 120,
      fixed: 'right',
      render: (_: unknown, record: DictInfo) => {
        const isSystemDict = record.dictType === 1;
        return (
          <div className={styles.actionColumn}>
            <Button
              type="link"
              size="small"
              icon={<EditOutlined />}
              onClick={() => handleEdit(record)}
            >
              编辑
            </Button>
            {!isSystemDict && (
              <Popconfirm
                title="删除确认"
                description={`确定删除字典「${record.dictName}」？`}
                onConfirm={() => handleDelete([record.id!])}
                okText="确定"
                cancelText="取消"
              >
                <Button type="link" size="small" danger>
                  删除
                </Button>
              </Popconfirm>
            )}
          </div>
        );
      },
    },
  ];

  return (
    <div className={styles.tableWrapper}>
      {/* 搜索栏 */}
      <div className={styles.searchBar}>
        <Form form={searchForm} layout="inline" component={false}>
          <Form.Item name="dictName" noStyle>
            <Input
              placeholder="字典名称"
              allowClear
              className={styles.searchInput}
              onPressEnter={handleSearch}
            />
          </Form.Item>
        </Form>
        <Space>
          <Button
            type="primary"
            size="small"
            icon={<SearchOutlined />}
            onClick={handleSearch}
          >
            查询
          </Button>
          <Button size="small" icon={<ReloadOutlined />} onClick={handleReset}>
            重置
          </Button>
        </Space>
      </div>

      {/* 表头 */}
      <div className={styles.tableHeader}>
        <span className={styles.tableTitle}>字典列表</span>
        <Space>
          <Button
            type="primary"
            size="small"
            icon={<PlusOutlined />}
            onClick={handleCreate}
          >
            新增
          </Button>
          <Popconfirm
            title="批量删除"
            description={`确定删除选中的 ${selectedRowKeys.length} 个字典？`}
            onConfirm={handleBatchDelete}
            okText="确定"
            cancelText="取消"
            disabled={selectedRowKeys.length === 0}
          >
            <Button
              danger
              size="small"
              icon={<DeleteOutlined />}
              disabled={selectedRowKeys.length === 0}
            >
              删除
            </Button>
          </Popconfirm>
        </Space>
      </div>

      {/* 表格 */}
      <Table<DictInfo>
        rowKey="id"
        rowSelection={{
          selectedRowKeys,
          onChange: setSelectedRowKeys,
        }}
        columns={columns}
        dataSource={dataSource}
        loading={loading}
        size="small"
        scroll={{ y: 400 }}
        pagination={{
          current: currentPage,
          pageSize,
          total,
          size: 'small',
          showSizeChanger: true,
          showQuickJumper: true,
          showTotal: (t) => `共 ${t} 条`,
          onChange: handlePageChange,
        }}
        onRow={(record) => ({
          onClick: () => onSelect(record),
          className:
            selectedDict?.id === record.id
              ? `${styles.dictRow} ${styles.dictRowSelected}`
              : styles.dictRow,
        })}
      />

      {/* 新增/编辑弹窗 */}
      <Modal
        title={formModalMode === 'edit' ? '编辑字典' : '新增字典'}
        open={formModalVisible}
        okText="保存"
        cancelText="取消"
        okButtonProps={{ 'data-ai-approval': 'true' }}
        onOk={handleFormOk}
        onCancel={() => setFormModalVisible(false)}
        confirmLoading={formLoading}
        destroyOnHidden
      >
        <Form form={form} layout="vertical">
          <Form.Item
            name="dictKey"
            label="字典键"
            rules={[{ required: true, message: '请输入字典键' }]}
          >
            <Input
              placeholder="请输入字典键，如 gender"
              disabled={formModalMode === 'edit'}
            />
          </Form.Item>
          <Form.Item
            name="dictName"
            label="字典名称"
            rules={[{ required: true, message: '请输入字典名称' }]}
          >
            <Input placeholder="请输入字典名称" />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <TextArea rows={3} placeholder="请输入描述" />
          </Form.Item>
          <Form.Item name="dictType" label="字典类型" hidden>
            <Input />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default DictKeyList;
