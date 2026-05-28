import React, { useState, useCallback, useEffect } from 'react';
import {
  Button,
  Modal,
  Form,
  Input,
  InputNumber,
  Space,
  Popconfirm,
  List,
  App,
  Empty,
} from 'antd';
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  ArrowUpOutlined,
  ArrowDownOutlined,
} from '@ant-design/icons';
import {
  getDictValues,
  saveOrUpdateDictValue,
  deleteDictValues,
  updateDictValueSort,
} from '../services/dict';
import type { DictInfo, DictValueInfo } from '../services/dict';
import styles from './index.module.less';

interface DictValueListProps {
  selectedDict: DictInfo | null;
}

const DictValueList: React.FC<DictValueListProps> = ({ selectedDict }) => {
  const { message } = App.useApp();

  const [loading, setLoading] = useState(false);
  const [dataSource, setDataSource] = useState<DictValueInfo[]>([]);

  /** 加载字典值列表 */
  const fetchDictValues = useCallback(async () => {
    if (!selectedDict?.id) {
      setDataSource([]);
      return;
    }
    setLoading(true);
    try {
      const list = await getDictValues(selectedDict.id);
      setDataSource(list);
    } catch {
      // request 层已自动提示
    } finally {
      setLoading(false);
    }
  }, [selectedDict?.id]);

  useEffect(() => {
    fetchDictValues();
  }, [fetchDictValues]);

  // 新增/编辑弹窗
  const [formModalVisible, setFormModalVisible] = useState(false);
  const [formModalMode, setFormModalMode] = useState<'create' | 'edit'>('create');
  const [formModalData, setFormModalData] = useState<DictValueInfo | null>(null);
  const [form] = Form.useForm();
  const [formLoading, setFormLoading] = useState(false);

  /** 新增字典值 */
  const handleCreate = useCallback(() => {
    setFormModalMode('create');
    setFormModalData(null);
    form.resetFields();
    form.setFieldsValue({
      sort: dataSource.length + 1,
    });
    setFormModalVisible(true);
  }, [form, dataSource.length]);

  /** 编辑字典值 */
  const handleEdit = useCallback(
    (record: DictValueInfo) => {
      setFormModalMode('edit');
      setFormModalData(record);
      form.setFieldsValue({
        dictValue: record.dictValue,
        sort: record.sort,
      });
      setFormModalVisible(true);
    },
    [form],
  );

  /** 保存字典值 */
  const handleFormOk = async () => {
    if (!selectedDict?.id) return;
    try {
      const values = await form.validateFields();
      setFormLoading(true);
      const isEdit = formModalMode === 'edit';
      const reqData: Partial<DictValueInfo> = {
        dictId: selectedDict.id,
        dictValue: values.dictValue,
        sort: values.sort,
        id: isEdit ? formModalData?.id : undefined,
      };
      await saveOrUpdateDictValue(reqData);
      message.success(isEdit ? '编辑成功' : '新增成功');
      setFormModalVisible(false);
      await fetchDictValues();
    } catch {
      // 表单校验失败或请求错误
    } finally {
      setFormLoading(false);
    }
  };

  /** 删除字典值 */
  const handleDelete = useCallback(
    async (id: string) => {
      try {
        await deleteDictValues([id]);
        message.success('删除成功');
        await fetchDictValues();
      } catch {
        // request 层已自动提示
      }
    },
    [fetchDictValues],
  );

  /** 移动排序 */
  const handleMove = useCallback(
    async (index: number, direction: 'up' | 'down') => {
      if (!selectedDict?.id) return;
      const newList = [...dataSource];
      const targetIndex = direction === 'up' ? index - 1 : index + 1;
      if (targetIndex < 0 || targetIndex >= newList.length) return;

      // 交换位置
      [newList[index], newList[targetIndex]] = [newList[targetIndex], newList[index]];
      const ids = newList.map((item) => item.id!);

      try {
        await updateDictValueSort(ids);
        await fetchDictValues();
      } catch {
        // request 层已自动提示
      }
    },
    [dataSource, selectedDict?.id, fetchDictValues],
  );

  if (!selectedDict) {
    return (
      <div className={styles.tableWrapper}>
        <div className={styles.tableHeader}>
          <span className={styles.tableTitle}>字典值</span>
        </div>
        <div className={styles.emptyState}>
          <Empty description="请选择左侧字典" />
        </div>
      </div>
    );
  }

  return (
    <div className={styles.tableWrapper}>
      {/* 表头 */}
      <div className={styles.tableHeader}>
        <span className={styles.tableTitle}>
          字典值 - {selectedDict.dictName}
        </span>
        <Button
          type="primary"
          size="small"
          icon={<PlusOutlined />}
          onClick={handleCreate}
        >
          新增值
        </Button>
      </div>

      {/* 字典值列表 */}
      <div className={styles.valueListContainer}>
        <List<DictValueInfo>
          loading={loading}
          dataSource={dataSource}
          size="small"
          renderItem={(item, index) => (
            <List.Item
              actions={[
                <Button
                  type="link"
                  size="small"
                  icon={<EditOutlined />}
                  onClick={() => handleEdit(item)}
                  key="edit"
                >
                  编辑
                </Button>,
                <Popconfirm
                  title="删除确认"
                  description="确定删除该字典值？"
                  onConfirm={() => handleDelete(item.id!)}
                  okText="确定"
                  cancelText="取消"
                  key="delete"
                >
                  <Button type="link" size="small" danger icon={<DeleteOutlined />}>
                    删除
                  </Button>
                </Popconfirm>,
                <Button
                  type="link"
                  size="small"
                  icon={<ArrowUpOutlined />}
                  disabled={index === 0}
                  onClick={() => handleMove(index, 'up')}
                  key="up"
                >
                  上移
                </Button>,
                <Button
                  type="link"
                  size="small"
                  icon={<ArrowDownOutlined />}
                  disabled={index === dataSource.length - 1}
                  onClick={() => handleMove(index, 'down')}
                  key="down"
                >
                  下移
                </Button>,
              ]}
            >
              <List.Item.Meta
                title={
                  <Space>
                    <span className={styles.sortBadge}>
                      #{item.sort}
                    </span>
                    <span>{item.dictValue}</span>
                  </Space>
                }
              />
            </List.Item>
          )}
          locale={{ emptyText: '暂无字典值' }}
        />
      </div>

      {/* 新增/编辑弹窗 */}
      <Modal
        title={formModalMode === 'edit' ? '编辑字典值' : '新增字典值'}
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
            name="dictValue"
            label="字典值"
            rules={[{ required: true, message: '请输入字典值' }]}
          >
            <Input placeholder="请输入字典值" />
          </Form.Item>
          <Form.Item
            name="sort"
            label="排序号"
            rules={[{ required: true, message: '请输入排序号' }]}
          >
            <InputNumber min={1} style={{ width: '100%' }} placeholder="请输入排序号" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default DictValueList;
