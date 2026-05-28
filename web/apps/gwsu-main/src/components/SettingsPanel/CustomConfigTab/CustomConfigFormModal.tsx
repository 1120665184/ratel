import React, { useState, useEffect } from 'react';
import { Modal, Form, Input, Select, App } from 'antd';
import type { ConfigInfo } from '../services/config';

const { TextArea } = Input;

interface CustomConfigFormModalProps {
  visible: boolean;
  mode: 'create' | 'edit';
  data: ConfigInfo | null;
  onSave: (data: Partial<ConfigInfo>) => Promise<boolean>;
  onClose: () => void;
  onSuccess: () => void;
}

const VALUE_TYPE_OPTIONS = [
  { label: '基本类型', value: 1 },
  { label: 'JSON', value: 2 },
];

const CustomConfigFormModal: React.FC<CustomConfigFormModalProps> = ({
  visible,
  mode,
  data,
  onSave,
  onClose,
  onSuccess,
}) => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const { message } = App.useApp();
  const isEdit = mode === 'edit';

  const valueType = Form.useWatch('valueType', form);

  useEffect(() => {
    if (visible) {
      if (data) {
        form.setFieldsValue({
          configKey: data.configKey,
          configName: data.configName,
          valueType: data.valueType,
          configValue: data.configValue,
          description: data.description,
        });
      } else {
        form.resetFields();
        form.setFieldsValue({ valueType: 1, configType: 2 });
      }
    }
  }, [visible, data, form]);

  const handleOk = async () => {
    try {
      const values = await form.validateFields();
      setLoading(true);
      const reqData: Partial<ConfigInfo> = {
        ...values,
        id: isEdit ? data?.id : undefined,
        configType: 2, // 自定义配置
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

  return (
    <Modal
      title={isEdit ? '编辑自定义配置' : '新增自定义配置'}
      open={visible}
      okText="保存"
      cancelText="取消"
      okButtonProps={{ 'data-ai-approval': 'true' }}
      onOk={handleOk}
      onCancel={onClose}
      confirmLoading={loading}
      destroyOnHidden
    >
      <Form form={form} layout="vertical">
        <Form.Item
          name="configKey"
          label="配置键"
          rules={[{ required: true, message: '请输入配置键' }]}
        >
          <Input
            placeholder="请输入配置键，如 custom.setting"
            disabled={isEdit}
          />
        </Form.Item>
        <Form.Item
          name="configName"
          label="配置名称"
          rules={[{ required: true, message: '请输入配置名称' }]}
        >
          <Input placeholder="请输入配置名称" />
        </Form.Item>
        <Form.Item
          name="valueType"
          label="值类型"
          rules={[{ required: true, message: '请选择值类型' }]}
        >
          <Select options={VALUE_TYPE_OPTIONS} placeholder="请选择值类型" />
        </Form.Item>
        <Form.Item
          name="configValue"
          label="配置值"
          rules={[{ required: true, message: '请输入配置值' }]}
        >
          {valueType === 2 ? (
            <TextArea
              rows={6}
              placeholder="请输入 JSON 格式的配置值"
              style={{ fontFamily: 'monospace' }}
            />
          ) : (
            <Input placeholder="请输入配置值" />
          )}
        </Form.Item>
        <Form.Item name="description" label="描述">
          <TextArea rows={3} placeholder="请输入描述" showCount maxLength={256} />
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default CustomConfigFormModal;
