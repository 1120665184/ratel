import { Form, Input, Switch } from 'antd';
import type { AnthropicConfig } from '../types';

interface AnthropicConfigFormProps {
  value?: AnthropicConfig;
  onChange?: (value: AnthropicConfig) => void;
}

const AnthropicConfigForm: React.FC<AnthropicConfigFormProps> = ({ value, onChange }) => {
  const handleFieldChange = (field: keyof AnthropicConfig, fieldValue: unknown) => {
    onChange?.({ ...value!, [field]: fieldValue });
  };

  return (
    <>
      <Form.Item label="API Key" required>
        <Input.Password
          value={value?.apiKey}
          onChange={(e) => handleFieldChange('apiKey', e.target.value)}
          placeholder="请输入 Anthropic API Key"
        />
      </Form.Item>
      <Form.Item label="模型名称" required>
        <Input
          value={value?.modelName}
          onChange={(e) => handleFieldChange('modelName', e.target.value)}
          placeholder="例如 claude-sonnet-4-5、claude-opus-4"
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
          placeholder="可选，自定义 API 地址"
        />
      </Form.Item>
    </>
  );
};

export default AnthropicConfigForm;
