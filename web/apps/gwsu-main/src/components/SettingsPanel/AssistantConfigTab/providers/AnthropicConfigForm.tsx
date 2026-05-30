import { Form, Input, Select, Switch } from 'antd';
import type { AnthropicConfig } from '../types';
import { ANTHROPIC_MODELS } from '../types';

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
        <Select
          value={value?.modelName}
          onChange={(val) => handleFieldChange('modelName', val)}
          options={ANTHROPIC_MODELS}
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
