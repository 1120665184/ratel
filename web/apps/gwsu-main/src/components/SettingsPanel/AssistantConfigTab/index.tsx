import React, { useState, useCallback, useEffect, useRef } from 'react';
import { Table, Button, Space, App } from 'antd';
import type { TableProps } from 'antd';
import { EditOutlined, ReloadOutlined } from '@ant-design/icons';
import { getConfigPage, saveOrUpdateConfig } from '../services/config';
import type { ConfigInfo, ConfigQuery } from '../services/config';
import ConfigFormModal from './ConfigFormModal';
import styles from './index.module.less';

const AssistantConfigTab: React.FC = () => {
  const { message } = App.useApp();

  const [loading, setLoading] = useState(false);
  const [dataSource, setDataSource] = useState<ConfigInfo[]>([]);
  const [total, setTotal] = useState(0);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);

  const queryRef = useRef<ConfigQuery>({ configType: 1 });
  const initializedRef = useRef(false);

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
      fetchConfigPage({ configType: 1 });
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

  /** 刷新 */
  const handleRefresh = useCallback(() => {
    fetchConfigPage();
  }, [fetchConfigPage]);

  // 编辑弹窗
  const [formModalVisible, setFormModalVisible] = useState(false);
  const [formModalData, setFormModalData] = useState<ConfigInfo | null>(null);

  /** 编辑 */
  const handleEdit = useCallback((record: ConfigInfo) => {
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

  /** 表格列定义 */
  const columns: TableProps<ConfigInfo>['columns'] = [
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
      width: 200,
      render: (val: string) => <code>{val}</code>,
    },
    {
      title: '配置名称',
      dataIndex: 'configName',
      width: 180,
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
      width: 100,
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
        </div>
      ),
    },
  ];

  return (
    <div className={styles.assistantConfigTab}>
      <div className={styles.tableWrapper}>
        <div className={styles.tableHeader}>
          <span className={styles.tableTitle}>助手配置列表</span>
          <Space>
            <Button
              icon={<ReloadOutlined />}
              onClick={handleRefresh}
            >
              刷新
            </Button>
          </Space>
        </div>
        <Table<ConfigInfo>
          rowKey="id"
          columns={columns}
          dataSource={dataSource}
          loading={loading}
          size="middle"
          scroll={{ x: 800 }}
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

      {/* 编辑弹窗 */}
      <ConfigFormModal
        visible={formModalVisible}
        data={formModalData}
        onSave={handleSave}
        onClose={() => setFormModalVisible(false)}
        onSuccess={() => setFormModalVisible(false)}
      />
    </div>
  );
};

export default AssistantConfigTab;
