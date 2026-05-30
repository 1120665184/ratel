import { useEffect } from 'react';
import { Modal, Form, Input, Slider, message } from 'antd';
import type { ConfigInfo } from '../services/config';
import { saveOrUpdateConfig } from '../services/config';
import type { AssistantConfigFormValues } from './types';

interface ConfigFormModalProps {
  visible: boolean;
  config: ConfigInfo | null;
  onClose: () => void;
  onSuccess: () => void;
}

const ConfigFormModal: React.FC<ConfigFormModalProps> = ({ visible, config, onClose, onSuccess }) => {
  const [form] = Form.useForm();
  const isAssistant = config?.configKey === 'assistant_config';

  useEffect(() => {
    if (visible && config) {
      if (isAssistant) {
        try {
          const values: AssistantConfigFormValues = JSON.parse(config.configValue || '{}');
          form.setFieldsValue({ model: values.model || 'default', temperature: values.temperature ?? 0.7 });
        } catch {
          form.setFieldsValue({ model: 'default', temperature: 0.7 });
        }
      } else {
        form.setFieldsValue({ configValue: config.configValue || '' });
      }
    }
    if (!visible) {
      form.resetFields();
    }
  }, [visible, config, form, isAssistant]);

  const handleSave = async () => {
    try {
      const values = await form.validateFields();
      let configValue: string;

      if (isAssistant) {
        configValue = JSON.stringify({ model: values.model, temperature: values.temperature });
      } else {
        configValue = values.configValue;
      }

      const success = await saveOrUpdateConfig({
        id: config?.id,
        configKey: config?.configKey,
        configName: config?.configName,
        configValue,
        valueType: 2,
        description: config?.description,
      });

      if (success) {
        message.success('保存成功');
        onSuccess();
        onClose();
      }
    } catch {
      // validation failed
    }
  };

  return (
    <Modal
      title={`编辑${config?.configName || '配置'}`}
      open={visible}
      onOk={handleSave}
      onCancel={onClose}
      destroyOnClose
      width={520}
    >
      <Form form={form} layout="vertical">
        {isAssistant ? (
          <>
            <Form.Item name="model" label="模型" rules={[{ required: true, message: '请输入模型名称' }]}>
              <Input placeholder="请输入模型名称" />
            </Form.Item>
            <Form.Item name="temperature" label="温度" rules={[{ required: true, message: '请设置温度' }]}>
              <Slider min={0} max={2} step={0.1} marks={{ 0: '0', 0.7: '0.7', 1: '1', 2: '2' }} />
            </Form.Item>
          </>
        ) : (
          <Form.Item name="configValue" label="配置值（JSON）" rules={[{ required: true, message: '请输入配置值' }]}>
            <Input.TextArea rows={12} placeholder="请输入JSON格式的配置值" />
          </Form.Item>
        )}
      </Form>
    </Modal>
  );
};

export default ConfigFormModal;
