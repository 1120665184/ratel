import React, { useState, useEffect } from 'react';
import { Modal, Form, Input, Slider, App } from 'antd';
import type { ConfigInfo } from '../services/config';
import styles from './index.module.less';

const { TextArea } = Input;

interface ConfigFormModalProps {
  visible: boolean;
  data: ConfigInfo | null;
  onSave: (data: Partial<ConfigInfo>) => Promise<boolean>;
  onClose: () => void;
  onSuccess: () => void;
}

/** assistant_config 的结构定义 */
interface AssistantConfigForm {
  modelName: string;
  temperature: number;
}

const ConfigFormModal: React.FC<ConfigFormModalProps> = ({
  visible,
  data,
  onSave,
  onClose,
  onSuccess,
}) => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const { message } = App.useApp();

  const isAssistantConfig = data?.configKey === 'assistant_config';

  useEffect(() => {
    if (visible && data) {
      if (isAssistantConfig) {
        // 解析 JSON 字符串到表单字段
        try {
          const parsed: AssistantConfigForm = JSON.parse(data.configValue || '{}');
          form.setFieldsValue({
            configKey: data.configKey,
            configName: data.configName,
            modelName: parsed.modelName ?? '',
            temperature: parsed.temperature ?? 0.7,
          });
        } catch {
          form.setFieldsValue({
            configKey: data.configKey,
            configName: data.configName,
            modelName: '',
            temperature: 0.7,
          });
        }
      } else {
        // 通用配置：显示 JSON 文本编辑器
        form.setFieldsValue({
          configKey: data.configKey,
          configName: data.configName,
          configValue: data.configValue,
        });
      }
    }
  }, [visible, data, form, isAssistantConfig]);

  const handleOk = async () => {
    try {
      const values = await form.validateFields();
      setLoading(true);

      let configValue: string;
      if (isAssistantConfig) {
        // 序列化表单值为 JSON
        const assistantData: AssistantConfigForm = {
          modelName: values.modelName,
          temperature: values.temperature,
        };
        configValue = JSON.stringify(assistantData);
      } else {
        configValue = values.configValue;
      }

      const reqData: Partial<ConfigInfo> = {
        id: data?.id,
        configKey: values.configKey,
        configName: values.configName,
        configValue,
        valueType: 2, // JSON 类型
        configType: 1, // 系统配置
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
      title="编辑助手配置"
      open={visible}
      okText="保存"
      cancelText="取消"
      okButtonProps={{ 'data-ai-approval': 'true' }}
      onOk={handleOk}
      onCancel={onClose}
      confirmLoading={loading}
      className={styles.formModal}
      destroyOnHidden
    >
      <Form form={form} layout="vertical">
        <Form.Item
          name="configKey"
          label="配置键"
          rules={[{ required: true, message: '请输入配置键' }]}
        >
          <Input placeholder="配置键" disabled />
        </Form.Item>
        <Form.Item
          name="configName"
          label="配置名称"
          rules={[{ required: true, message: '请输入配置名称' }]}
        >
          <Input placeholder="请输入配置名称" disabled />
        </Form.Item>

        {isAssistantConfig ? (
          <>
            <Form.Item
              name="modelName"
              label="模型名称"
              rules={[{ required: true, message: '请输入模型名称' }]}
            >
              <Input placeholder="请输入模型名称" />
            </Form.Item>
            <Form.Item
              name="temperature"
              label="温度参数"
              tooltip="控制输出的随机性，值越大输出越随机"
            >
              <Slider
                min={0}
                max={2}
                step={0.1}
                marks={{
                  0: '0',
                  0.7: '0.7',
                  1: '1',
                  2: '2',
                }}
              />
            </Form.Item>
          </>
        ) : (
          <Form.Item
            name="configValue"
            label="配置值（JSON）"
            rules={[{ required: true, message: '请输入配置值' }]}
          >
            <TextArea
              rows={8}
              placeholder="请输入 JSON 格式的配置值"
              style={{ fontFamily: 'monospace' }}
            />
          </Form.Item>
        )}
      </Form>
    </Modal>
  );
};

export default ConfigFormModal;
