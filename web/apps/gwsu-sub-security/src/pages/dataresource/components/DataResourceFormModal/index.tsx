import React, { useState, useEffect } from 'react';
import {
  Modal,
  Form,
  Input,
  Switch,
  Select,
  InputNumber,
  Button,
  Table,
} from 'antd';
import type { TableProps } from 'antd';
import { PlusOutlined, DeleteOutlined } from '@ant-design/icons';
import styles from './index.module.less';
import type {
  DataResourceInfo,
  DataResourceCondition,
  StringEnumOption,
  ResourceAttribute,
} from '../../types';

const { TextArea } = Input;

interface DataResourceFormModalProps {
  visible: boolean;
  mode: 'create' | 'edit';
  data?: DataResourceInfo | null;
  assertTypeOptions: StringEnumOption[];
  conditionTypeOptions: StringEnumOption[];
  resourceAttributes: ResourceAttribute[];
  onSave: (data: DataResourceInfo) => Promise<boolean>;
  onClose: () => void;
  onSuccess: () => void;
}

/** 创建空条件行 */
const createEmptyCondition = (sort: number): DataResourceCondition => ({
  fieldName: '',
  showNull: false,
  userResourceFields: [],
  assertType: 'EQ',
  relationship: undefined,
  sort,
});

const DataResourceFormModal: React.FC<DataResourceFormModalProps> = ({
  visible,
  mode,
  data,
  assertTypeOptions,
  conditionTypeOptions,
  resourceAttributes,
  onSave,
  onClose,
  onSuccess,
}) => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [conditions, setConditions] = useState<DataResourceCondition[]>([]);
  const isEdit = mode === 'edit';

  useEffect(() => {
    if (visible) {
      if (data) {
        form.setFieldsValue({
          databaseName: data.databaseName,
          tableName: data.tableName,
          description: data.description,
          status: data.status,
        });
        setConditions(
          data.conditions?.length
            ? data.conditions.map((c) => ({ ...c }))
            : [],
        );
      } else {
        form.resetFields();
        form.setFieldsValue({ status: true });
        setConditions([]);
      }
    }
  }, [visible, data, form]);

  /** 添加条件行 */
  const handleAddCondition = () => {
    setConditions((prev) => [
      ...prev,
      createEmptyCondition(prev.length + 1),
    ]);
  };

  /** 删除条件行 */
  const handleDeleteCondition = (index: number) => {
    setConditions((prev) => prev.filter((_, i) => i !== index));
  };

  /** 更新条件行字段 */
  const handleConditionChange = (
    index: number,
    field: keyof DataResourceCondition,
    value: unknown,
  ) => {
    setConditions((prev) =>
      prev.map((item, i) => (i === index ? { ...item, [field]: value } : item)),
    );
  };

  /** 提交 */
  const handleOk = async () => {
    try {
      const values = await form.validateFields();
      setLoading(true);
      const reqData: DataResourceInfo = {
        ...values,
        id: isEdit ? data?.id : undefined,
        conditions: conditions.map((c, i) => ({
          ...c,
          sort: i + 1,
          relationship: i === 0 ? undefined : c.relationship,
        })),
      };
      const success = await onSave(reqData);
      if (success) {
        onSuccess();
      }
    } catch {
      // 表单校验失败或请求错误
    } finally {
      setLoading(false);
    }
  };

  const conditionColumns: TableProps<DataResourceCondition>['columns'] = [
    {
      title: '字段名',
      dataIndex: 'fieldName',
      width: 140,
      render: (_: unknown, __: DataResourceCondition, index: number) => (
        <Input
          value={conditions[index]?.fieldName}
          onChange={(e) =>
            handleConditionChange(index, 'fieldName', e.target.value)
          }
          placeholder="字段名"
          size="small"
        />
      ),
    },
    {
      title: '用户资源字段',
      dataIndex: 'userResourceFields',
      width: 200,
      render: (_: unknown, __: DataResourceCondition, index: number) => (
        <Select
          mode="multiple"
          value={conditions[index]?.userResourceFields ?? []}
          onChange={(val) =>
            handleConditionChange(index, 'userResourceFields', val)
          }
          options={resourceAttributes.map((a) => ({
            label: `${a.desc}(${a.key})`,
            value: a.key,
          }))}
          placeholder="选择字段"
          size="small"
          style={{ width: '100%' }}
          maxTagCount={2}
        />
      ),
    },
    {
      title: '断言类型',
      dataIndex: 'assertType',
      width: 110,
      render: (_: unknown, __: DataResourceCondition, index: number) => (
        <Select
          value={conditions[index]?.assertType ?? 'EQ'}
          onChange={(val) =>
            handleConditionChange(index, 'assertType', val)
          }
          options={assertTypeOptions}
          size="small"
          style={{ width: '100%' }}
        />
      ),
    },
    {
      title: '关联关系',
      dataIndex: 'relationship',
      width: 100,
      render: (_: unknown, __: DataResourceCondition, index: number) => {
        if (index === 0) return <span style={{ color: 'var(--text-quaternary-color)' }}>-</span>;
        return (
          <Select
            value={conditions[index]?.relationship ?? 'AND'}
            onChange={(val) =>
              handleConditionChange(index, 'relationship', val)
            }
            options={conditionTypeOptions}
            size="small"
            style={{ width: '100%' }}
          />
        );
      },
    },
    {
      title: '显示Null',
      dataIndex: 'showNull',
      width: 80,
      render: (_: unknown, __: DataResourceCondition, index: number) => (
        <Switch
          size="small"
          checked={conditions[index]?.showNull ?? false}
          onChange={(val) =>
            handleConditionChange(index, 'showNull', val)
          }
        />
      ),
    },
    {
      title: '排序',
      dataIndex: 'sort',
      width: 70,
      render: (_: unknown, __: DataResourceCondition, index: number) => (
        <InputNumber
          min={1}
          value={index + 1}
          disabled
          size="small"
          style={{ width: '100%' }}
        />
      ),
    },
    {
      title: '',
      width: 40,
      render: (_: unknown, __: DataResourceCondition, index: number) => (
        <Button
          type="text"
          danger
          size="small"
          icon={<DeleteOutlined />}
          onClick={() => handleDeleteCondition(index)}
        />
      ),
    },
  ];

  return (
    <Modal
      title={isEdit ? '编辑数据资源' : '新增数据资源'}
      open={visible}
      onOk={handleOk}
      onCancel={onClose}
      confirmLoading={loading}
      width={860}
      className={styles.formModal}
      destroyOnHidden
    >
      <Form form={form} layout="vertical">
        <Form.Item name="databaseName" label="库名">
          <Input placeholder="留空则匹配所有库" />
        </Form.Item>
        <Form.Item
          name="tableName"
          label="表名"
          rules={[{ required: true, message: '请输入表名' }]}
        >
          <Input placeholder="请输入表名" />
        </Form.Item>
        <Form.Item name="description" label="描述">
          <TextArea
            rows={2}
            placeholder="请输入规则描述"
            showCount
            maxLength={256}
          />
        </Form.Item>
        <Form.Item name="status" label="状态" valuePropName="checked">
          <Switch checkedChildren="启用" unCheckedChildren="禁用" />
        </Form.Item>
      </Form>
      <div className={styles.conditionSection}>
        <div className={styles.sectionHeader}>
          <span className={styles.sectionTitle}>字段条件</span>
          <Button
            type="dashed"
            size="small"
            icon={<PlusOutlined />}
            onClick={handleAddCondition}
          >
            添加条件
          </Button>
        </div>
        <Table<DataResourceCondition>
          rowKey={(_, index) => String(index)}
          columns={conditionColumns}
          dataSource={conditions}
          size="small"
          pagination={false}
          scroll={{ x: 740 }}
          locale={{ emptyText: '暂无条件，点击上方按钮添加' }}
        />
      </div>
    </Modal>
  );
};

export default DataResourceFormModal;
