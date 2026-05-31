import { Form, Input, Switch } from 'antd';
import type { GeminiConfig } from '../types';

interface GeminiConfigFormProps {
  value?: GeminiConfig;
  onChange?: (value: GeminiConfig) => void;
}

const GeminiConfigForm: React.FC<GeminiConfigFormProps> = ({ value, onChange }) => {
  const handleFieldChange = (field: keyof GeminiConfig, fieldValue: unknown) => {
    onChange?.({ ...value!, [field]: fieldValue });
  };

  return (
    <>
      <Form.Item label="API Key" required={!value?.project}>
        <Input.Password
          value={value?.apiKey}
          onChange={(e) => handleFieldChange('apiKey', e.target.value)}
          placeholder="直接 API 模式必填"
        />
      </Form.Item>
      <Form.Item label="模型名称" required>
        <Input
          value={value?.modelName}
          onChange={(e) => handleFieldChange('modelName', e.target.value)}
          placeholder="例如 gemini-2.0-flash、gemini-2.5-pro"
        />
      </Form.Item>
      <Form.Item label="流式响应">
        <Switch
          checked={value?.stream}
          onChange={(val) => handleFieldChange('stream', val)}
        />
      </Form.Item>
      <Form.Item label="GCP Project" required={!value?.apiKey}>
        <Input
          value={value?.project}
          onChange={(e) => handleFieldChange('project', e.target.value)}
          placeholder="Vertex AI 模式必填，配置后自动启用 Vertex AI"
        />
      </Form.Item>
      <Form.Item label="GCP Location">
        <Input
          value={value?.location}
          onChange={(e) => handleFieldChange('location', e.target.value)}
          placeholder="us-central1"
        />
      </Form.Item>
    </>
  );
};

export default GeminiConfigForm;
