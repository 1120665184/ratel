import { useState, useEffect, useCallback } from 'react';
import { Table, Button, Space, Tag, message, Popconfirm } from 'antd';
import type { TableProps } from 'antd';
import { EditOutlined, ReloadOutlined, SearchOutlined } from '@ant-design/icons';
import { getConfigPage, deleteConfigs } from '../services/config';
import type { ConfigInfo } from '../services/config';
import ConfigFormModal from './ConfigFormModal';
import styles from './index.module.less';

const AssistantConfigTab: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [dataSource, setDataSource] = useState<ConfigInfo[]>([]);
  const [total, setTotal] = useState(0);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);

  const [formVisible, setFormVisible] = useState(false);
  const [editingConfig, setEditingConfig] = useState<ConfigInfo | null>(null);

  const fetchData = useCallback(async (pageNum = currentPage, pSize = pageSize) => {
    setLoading(true);
    try {
      const result = await getConfigPage({ configType: 1, pageNum, pageSize: pSize });
      if (result) {
        setDataSource(result.records);
        setTotal(result.total);
        setCurrentPage(pageNum);
        setPageSize(pSize);
      }
    } catch {
      // error handled by request util
    } finally {
      setLoading(false);
    }
  }, [currentPage, pageSize]);

  useEffect(() => {
    fetchData(1);
  }, []);

  const handleEdit = useCallback((record: ConfigInfo) => {
    setEditingConfig(record);
    setFormVisible(true);
  }, []);

  const columns: TableProps<ConfigInfo>['columns'] = [
    {
      title: '序号',
      width: 60,
      align: 'center',
      render: (_: unknown, __: ConfigInfo, index: number) => (currentPage - 1) * pageSize + index + 1,
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
      title: '值类型',
      dataIndex: 'valueType',
      width: 100,
      render: (val: number) => <Tag color={val === 2 ? 'blue' : 'default'}>{val === 2 ? 'JSON' : '基本类型'}</Tag>,
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
        <Space>
          <Button type="link" size="small" icon={<EditOutlined />} onClick={() => handleEdit(record)}>
            编辑
          </Button>
        </Space>
      ),
    },
  ];

  return (
    <div className={styles.configTab}>
      <div className={styles.tableHeader}>
        <span className={styles.tableTitle}>系统配置列表</span>
        <Button icon={<ReloadOutlined />} onClick={() => fetchData(1)}>
          刷新
        </Button>
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
          onChange: (page, size) => fetchData(page, size),
        }}
      />
      <ConfigFormModal
        visible={formVisible}
        config={editingConfig}
        onClose={() => { setFormVisible(false); setEditingConfig(null); }}
        onSuccess={() => fetchData(currentPage)}
      />
    </div>
  );
};

export default AssistantConfigTab;
