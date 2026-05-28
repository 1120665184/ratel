import { useEffect } from 'react';
import { Modal, Form, Input, Select, message } from 'antd';
import type { ConfigInfo } from '../services/config';
import { saveOrUpdateConfig } from '../services/config';

interface CustomConfigFormModalProps {
  visible: boolean;
  config: ConfigInfo | null;
  onClose: () => void;
  onSuccess: () => void;
}

const CustomConfigFormModal: React.FC<CustomConfigFormModalProps> = ({ visible, config, onClose, onSuccess }) => {
  const [form] = Form.useForm();
  const isEdit = !!config?.id;

  useEffect(() => {
    if (visible) {
      if (config) {
        form.setFieldsValue({
          configKey: config.configKey,
          configName: config.configName,
          configValue: config.configValue,
          valueType: config.valueType,
          description: config.description,
        });
      } else {
        form.resetFields();
      }
    }
    if (!visible) {
      form.resetFields();
    }
  }, [visible, config, form]);

  const handleSave = async () => {
    try {
      const values = await form.validateFields();
      const success = await saveOrUpdateConfig({
        id: config?.id,
        configKey: values.configKey,
        configName: values.configName,
        configValue: values.configValue,
        valueType: values.valueType,
        configType: 2,
        description: values.description,
      });

      if (success) {
        message.success(isEdit ? '更新成功' : '新增成功');
        onSuccess();
        onClose();
      }
    } catch {
      // validation failed
    }
  };

  return (
    <Modal
      title={isEdit ? '编辑自定义配置' : '新增自定义配置'}
      open={visible}
      onOk={handleSave}
      onCancel={onClose}
      destroyOnClose
      width={560}
    >
      <Form form={form} layout="vertical">
        <Form.Item name="configKey" label="配置键" rules={[{ required: true, message: '请输入配置键' }]}>
          <Input placeholder="请输入配置键" disabled={isEdit} />
        </Form.Item>
        <Form.Item name="configName" label="配置名称" rules={[{ required: true, message: '请输入配置名称' }]}>
          <Input placeholder="请输入配置名称" />
        </Form.Item>
        <Form.Item name="valueType" label="值类型" rules={[{ required: true, message: '请选择值类型' }]} initialValue={1}>
          <Select
            options={[
              { label: '基本类型', value: 1 },
              { label: 'JSON对象', value: 2 },
            ]}
          />
        </Form.Item>
        <Form.Item name="configValue" label="配置值" rules={[{ required: true, message: '请输入配置值' }]}>
          <Input.TextArea rows={6} placeholder="请输入配置值" />
        </Form.Item>
        <Form.Item name="description" label="描述">
          <Input.TextArea rows={2} placeholder="请输入描述" />
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default CustomConfigFormModal;
