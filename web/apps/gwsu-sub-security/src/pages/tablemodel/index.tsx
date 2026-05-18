import React, { useEffect, useState, useCallback } from 'react';
import { Button, Table, Tag, Form, Select, Input, Space, Popconfirm } from 'antd';
import { PlusOutlined, CloudDownloadOutlined } from '@ant-design/icons';
import { useTableModel } from './hooks/useTableModel';
import CollectModal from './components/CollectModal';
import CustomAddModal from './components/CustomAddModal';
import DetailDrawer from './components/DetailDrawer';
import ChangeDatasourceModal from './components/ChangeDatasourceModal';
import { SOURCE_TYPE_MAP } from './types';
import type { TableModelInfo } from './types';
import styles from './index.module.less';

const TableModelPage: React.FC = () => {
  const {
    loading,
    pageData,
    modules,
    loadModules,
    loadPageData,
    handleSearch,
    handlePageChange,
    handleSync,
  } = useTableModel();

  const [collectVisible, setCollectVisible] = useState(false);
  const [customAddVisible, setCustomAddVisible] = useState(false);
  const [detailVisible, setDetailVisible] = useState(false);
  const [changeDatasourceVisible, setChangeDatasourceVisible] = useState(false);
  const [currentRecord, setCurrentRecord] = useState<TableModelInfo | null>(null);

  const [searchForm] = Form.useForm();

  useEffect(() => {
    loadModules();
    loadPageData();
  }, []);

  /** 搜索 */
  const onSearch = useCallback(() => {
    const values = searchForm.getFieldsValue();
    handleSearch(values);
  }, [searchForm, handleSearch]);

  /** 重置 */
  const onReset = useCallback(() => {
    searchForm.resetFields();
    handleSearch({});
  }, [searchForm, handleSearch]);

  /** 查看详情 */
  const handleViewDetail = useCallback((record: TableModelInfo) => {
    setCurrentRecord(record);
    setDetailVisible(true);
  }, []);

  /** 修改数据源 */
  const handleChangeDatasource = useCallback((record: TableModelInfo) => {
    setCurrentRecord(record);
    setChangeDatasourceVisible(true);
  }, []);

  /** 刷新列表 */
  const refreshList = useCallback(() => {
    loadPageData();
  }, [loadPageData]);

  const columns = [
    {
      title: '表名',
      dataIndex: 'tableName',
      key: 'tableName',
      width: 200,
    },
    {
      title: '所属模块',
      dataIndex: 'modulePrefix',
      key: 'modulePrefix',
      width: 120,
    },
    {
      title: '数据源',
      dataIndex: 'dataSource',
      key: 'dataSource',
      width: 120,
    },
    {
      title: '表注释',
      dataIndex: 'tableComment',
      key: 'tableComment',
      width: 200,
      ellipsis: true,
    },
    {
      title: '来源类型',
      dataIndex: 'sourceType',
      key: 'sourceType',
      width: 100,
      render: (val: number) => {
        const info = SOURCE_TYPE_MAP[val];
        return info ? <Tag color={info.color}>{info.text}</Tag> : '-';
      },
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      key: 'createTime',
      width: 180,
    },
    {
      title: '操作',
      key: 'action',
      width: 220,
      render: (_: unknown, record: TableModelInfo) => (
        <Space size="small">
          <Button type="link" size="small" onClick={() => handleViewDetail(record)}>
            详情
          </Button>
          {record.sourceType === 0 && (
            <Popconfirm
              title="确认同步？同步将从库中获取最新字段信息"
              onConfirm={() => handleSync(record)}
            >
              <Button type="link" size="small">同步</Button>
            </Popconfirm>
          )}
          <Button type="link" size="small" onClick={() => handleChangeDatasource(record)}>
            修改数据源
          </Button>
        </Space>
      ),
    },
  ];

  return (
    <div className={styles.tableModelPage}>
      {/* 搜索栏 */}
      <div className={styles.searchBar}>
        <Form form={searchForm} layout="inline">
          <Form.Item name="modulePrefix" label="所属模块">
            <Select
              placeholder="请选择模块"
              allowClear
              style={{ width: 160 }}
              options={modules.map((m) => ({ label: m.note || m.prefix, value: m.prefix }))}
            />
          </Form.Item>
          <Form.Item name="tableName" label="表名">
            <Input placeholder="请输入表名" allowClear style={{ width: 160 }} />
          </Form.Item>
          <Form.Item name="dataSource" label="数据源">
            <Input placeholder="请输入数据源" allowClear style={{ width: 160 }} />
          </Form.Item>
          <Form.Item name="sourceType" label="来源类型">
            <Select
              placeholder="请选择"
              allowClear
              style={{ width: 120 }}
              options={[
                { label: '采集', value: 0 },
                { label: '自定义', value: 1 },
              ]}
            />
          </Form.Item>
          <Form.Item>
            <Space>
              <Button type="primary" onClick={onSearch}>查询</Button>
              <Button onClick={onReset}>重置</Button>
            </Space>
          </Form.Item>
        </Form>
      </div>

      {/* 操作按钮 + 表格 */}
      <div className={styles.tableCard}>
        <div className={styles.actionBar}>
          <Button
            type="primary"
            icon={<CloudDownloadOutlined />}
            onClick={() => setCollectVisible(true)}
          >
            采集
          </Button>
          <Button
            icon={<PlusOutlined />}
            onClick={() => setCustomAddVisible(true)}
          >
            自定义添加
          </Button>
        </div>

        <Table
          rowKey="id"
          loading={loading}
          dataSource={pageData.records}
          columns={columns}
          pagination={{
            current: pageData.current,
            pageSize: pageData.size,
            total: pageData.total,
            showSizeChanger: true,
            showTotal: (total) => `共 ${total} 条`,
            onChange: handlePageChange,
          }}
          scroll={{ y: 'calc(100vh - 380px)' }}
        />
      </div>

      {/* 采集弹窗 */}
      <CollectModal
        visible={collectVisible}
        modules={modules}
        onClose={() => setCollectVisible(false)}
        onSuccess={refreshList}
      />

      {/* 自定义添加弹窗 */}
      <CustomAddModal
        visible={customAddVisible}
        modules={modules}
        onClose={() => setCustomAddVisible(false)}
        onSuccess={refreshList}
      />

      {/* 详情抽屉 */}
      <DetailDrawer
        visible={detailVisible}
        record={currentRecord}
        onClose={() => {
          setDetailVisible(false);
          setCurrentRecord(null);
        }}
      />

      {/* 修改数据源弹窗 */}
      <ChangeDatasourceModal
        visible={changeDatasourceVisible}
        record={currentRecord}
        onClose={() => {
          setChangeDatasourceVisible(false);
          setCurrentRecord(null);
        }}
        onSuccess={refreshList}
      />
    </div>
  );
};

export default TableModelPage;
