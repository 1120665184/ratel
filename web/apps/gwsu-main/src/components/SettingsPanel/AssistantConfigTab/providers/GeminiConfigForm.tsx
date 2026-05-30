import { Form, Input, Select, Switch } from 'antd';
import type { GeminiConfig } from '../types';
import { GEMINI_MODELS } from '../types';

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
        <Select
          value={value?.modelName}
          onChange={(val) => handleFieldChange('modelName', val)}
          options={GEMINI_MODELS}
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
