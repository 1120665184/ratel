import { Form, Input, Switch } from 'antd';
import type { OpenaiConfig } from '../types';

interface OpenAIConfigFormProps {
  value?: OpenaiConfig;
  onChange?: (value: OpenaiConfig) => void;
}

const OpenAIConfigForm: React.FC<OpenAIConfigFormProps> = ({ value, onChange }) => {
  const handleFieldChange = (field: keyof OpenaiConfig, fieldValue: unknown) => {
    onChange?.({ ...value!, [field]: fieldValue });
  };

  return (
    <>
      <Form.Item label="API Key" required>
        <Input.Password
          value={value?.apiKey}
          onChange={(e) => handleFieldChange('apiKey', e.target.value)}
          placeholder="请输入 OpenAI API Key"
        />
      </Form.Item>
      <Form.Item label="模型名称" required>
        <Input
          value={value?.modelName}
          onChange={(e) => handleFieldChange('modelName', e.target.value)}
          placeholder="例如 gpt-4.1-mini、gpt-4o"
        />
      </Form.Item>
      <Form.Item label="流式响应">
        <Switch
          checked={value?.stream}
          onChange={(val) => handleFieldChange('stream', val)}
        />
      </Form.Item>
      <Form.Item label="Base URL">
        <Input
          value={value?.baseUrl}
          onChange={(e) => handleFieldChange('baseUrl', e.target.value)}
          placeholder="可选，兼容 OpenAI 的端点 URL"
        />
      </Form.Item>
      <Form.Item label="Endpoint Path">
        <Input
          value={value?.endpointPath}
          onChange={(e) => handleFieldChange('endpointPath', e.target.value)}
          placeholder="可选，自定义 API 路径，如 /v4/chat/completions"
        />
      </Form.Item>
    </>
  );
};

export default OpenAIConfigForm;
