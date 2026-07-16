import { Form, Input, Switch } from 'antd';
import type { DashscopeConfig } from '../types';

interface DashScopeConfigFormProps {
  value?: DashscopeConfig;
  onChange?: (value: DashscopeConfig) => void;
}

const DashScopeConfigForm: React.FC<DashScopeConfigFormProps> = ({ value, onChange }) => {
  const handleFieldChange = (field: keyof DashscopeConfig, fieldValue: unknown) => {
    onChange?.({ ...value!, [field]: fieldValue });
  };

  return (
    <>
      <Form.Item label="API Key" required>
        <Input.Password
          value={value?.apiKey}
          onChange={(e) => handleFieldChange('apiKey', e.target.value)}
          placeholder="请输入 DashScope API Key"
        />
      </Form.Item>
      <Form.Item label="模型名称" required>
        <Input
          value={value?.modelName}
          onChange={(e) => handleFieldChange('modelName', e.target.value)}
          placeholder="例如 qwen-plus、qwen-max"
        />
      </Form.Item>
      <Form.Item label="流式响应">
        <Switch
          checked={value?.stream}
          onChange={(val) => handleFieldChange('stream', val)}
        />
      </Form.Item>
      <Form.Item label="启用思考模式">
        <Switch
          checked={value?.enableThinking}
          onChange={(val) => handleFieldChange('enableThinking', val)}
        />
      </Form.Item>
      <Form.Item label="启用搜索增强">
        <Switch
          checked={value?.enableSearch}
          onChange={(val) => handleFieldChange('enableSearch', val)}
        />
      </Form.Item>
      <Form.Item label="自定义 API 地址">
        <Input
          value={value?.baseUrl}
          onChange={(e) => handleFieldChange('baseUrl', e.target.value)}
          placeholder="可选，留空使用默认地址"
        />
      </Form.Item>
    </>
  );
};

export default DashScopeConfigForm;
