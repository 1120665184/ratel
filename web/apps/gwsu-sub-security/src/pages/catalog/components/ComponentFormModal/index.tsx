import React, { useState, useEffect } from 'react';
import { Modal, Form, Input, InputNumber, Select } from 'antd';
import styles from './index.module.less';
import type { CatalogComponentInfo } from '../../types';
import { CATEGORY_OPTIONS } from '../../types';
import { saveOrUpdateComponent } from '../../services/catalog';

const { TextArea } = Input;

interface ComponentFormModalProps {
  visible: boolean;
  data: CatalogComponentInfo | null;
  onClose: () => void;
  onSuccess: () => void;
}

const ComponentFormModal: React.FC<ComponentFormModalProps> = ({
  visible,
  data,
  onClose,
  onSuccess,
}) => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const isEdit = !!data?.id;

  useEffect(() => {
    if (visible) {
      if (data) {
        form.setFieldsValue({
          componentName: data.componentName,
          description: data.description,
          category: data.category,
          propsSchema: data.propsSchema,
          defaultProps: data.defaultProps,
          sortOrder: data.sortOrder ?? 0,
        });
      } else {
        form.resetFields();
        form.setFieldsValue({
          sortOrder: 0,
        });
      }
    }
  }, [visible, data, form]);

  const handleOk = async () => {
    try {
      const values = await form.validateFields();
      setLoading(true);
      const reqData: CatalogComponentInfo = {
        ...values,
        id: isEdit ? data?.id : undefined,
        status: data?.status ?? true,
      };
      await saveOrUpdateComponent(reqData);
      onSuccess();
    } catch {
      // 表单校验失败或请求错误
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal
      title={isEdit ? '编辑组件' : '新增组件'}
      open={visible}
      okText="保存"
      cancelText="取消"
      okButtonProps={{ 'data-ai-approval': true }}
      onOk={handleOk}
      onCancel={onClose}
      confirmLoading={loading}
      className={styles.modal}
      destroyOnHidden
    >
      <Form form={form} layout="vertical">
        <Form.Item
          name="componentName"
          label="组件名称"
          rules={[{ required: true, message: '请输入组件名称' }]}
        >
          <Input placeholder="请输入组件名称" disabled={isEdit} />
        </Form.Item>
        <Form.Item name="description" label="描述">
          <TextArea rows={3} placeholder="请输入组件描述" showCount maxLength={256} />
        </Form.Item>
        <Form.Item name="category" label="组件分类">
          <Select options={CATEGORY_OPTIONS} placeholder="请选择组件分类" allowClear />
        </Form.Item>
        <Form.Item
          name="propsSchema"
          label="属性定义 (JSON)"
          rules={[{ required: true, message: '请输入属性定义' }]}
        >
          <TextArea
            rows={6}
            placeholder='请输入 JSON 格式的属性定义，如: {"type": "object", "properties": {...}}'
            className={styles.codeArea}
          />
        </Form.Item>
        <Form.Item name="defaultProps" label="默认属性 (JSON)">
          <TextArea
            rows={4}
            placeholder="请输入 JSON 格式的默认属性值（可选）"
            className={styles.codeArea}
          />
        </Form.Item>
        <Form.Item name="sortOrder" label="排序号">
          <InputNumber min={0} style={{ width: '100%' }} placeholder="请输入排序号" />
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default ComponentFormModal;
