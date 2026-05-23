import React, { useState, useEffect } from 'react';
import { Modal, Form, Input } from 'antd';
import styles from './index.module.less';
import type { CatalogInfo } from '../../types';
import { saveOrUpdateCatalog } from '../../services/catalog';

const { TextArea } = Input;

interface CatalogFormModalProps {
  visible: boolean;
  data: CatalogInfo | null;
  onClose: () => void;
  onSuccess: () => void;
}

const CatalogFormModal: React.FC<CatalogFormModalProps> = ({
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
          catalogKey: data.catalogKey,
          catalogName: data.catalogName,
          description: data.description,
          version: data.version,
        });
      } else {
        form.resetFields();
      }
    }
  }, [visible, data, form]);

  const handleOk = async () => {
    try {
      const values = await form.validateFields();
      setLoading(true);
      const reqData: CatalogInfo = {
        ...values,
        id: isEdit ? data?.id : undefined,
        active: data?.active ?? 0,
        status: data?.status ?? true,
      };
      await saveOrUpdateCatalog(reqData);
      onSuccess();
    } catch {
      // 表单校验失败或请求错误
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal
      title={isEdit ? '编辑 Catalog' : '新增 Catalog'}
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
          name="catalogKey"
          label="Catalog Key"
          rules={[{ required: true, message: '请输入 Catalog Key' }]}
        >
          <Input placeholder="请输入唯一标识，如 default-catalog" disabled={isEdit} />
        </Form.Item>
        <Form.Item
          name="catalogName"
          label="Catalog 名称"
          rules={[{ required: true, message: '请输入 Catalog 名称' }]}
        >
          <Input placeholder="请输入 Catalog 名称" />
        </Form.Item>
        <Form.Item name="description" label="描述">
          <TextArea rows={3} placeholder="请输入描述" showCount maxLength={256} />
        </Form.Item>
        <Form.Item name="version" label="版本">
          <Input placeholder="请输入版本号，如 1.0.0" />
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default CatalogFormModal;
