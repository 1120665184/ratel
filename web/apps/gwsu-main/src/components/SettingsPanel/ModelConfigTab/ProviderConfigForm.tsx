import { Form } from 'antd';
import type { ModelProvider, ModelLlmConfig } from './types';
import DashScopeConfigForm from './providers/DashScopeConfigForm';
import OpenAIConfigForm from './providers/OpenAIConfigForm';
import GeminiConfigForm from './providers/GeminiConfigForm';
import AnthropicConfigForm from './providers/AnthropicConfigForm';

interface ProviderConfigFormProps {
  provider: ModelProvider;
  config: ModelLlmConfig;
  onConfigChange: (config: ModelLlmConfig) => void;
}

const ProviderConfigForm: React.FC<ProviderConfigFormProps> = ({ provider, config, onConfigChange }) => {
  const handleProviderConfigChange = (providerConfig: ModelLlmConfig[ModelProvider]) => {
    onConfigChange({ ...config, [provider]: providerConfig });
  };

  const renderForm = () => {
    switch (provider) {
      case 'dashscope':
        return <DashScopeConfigForm value={config.dashscope} onChange={handleProviderConfigChange} />;
      case 'openai':
        return <OpenAIConfigForm value={config.openai} onChange={handleProviderConfigChange} />;
      case 'gemini':
        return <GeminiConfigForm value={config.gemini} onChange={handleProviderConfigChange} />;
      case 'anthropic':
        return <AnthropicConfigForm value={config.anthropic} onChange={handleProviderConfigChange} />;
      default:
        return null;
    }
  };

  return <Form layout="vertical">{renderForm()}</Form>;
};

export default ProviderConfigForm;
