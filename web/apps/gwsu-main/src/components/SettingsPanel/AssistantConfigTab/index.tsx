import { useState, useEffect, useCallback } from 'react';
import { Card, Button, App, Spin } from 'antd';
import { SaveOutlined, ReloadOutlined } from '@ant-design/icons';
import { fetchConfigsBatch } from '@gwsu/core';
import { saveOrUpdateConfig } from '../services/config';
import type { ConfigInfo } from '../services/config';
import { ConfigValueType, ConfigType } from '@gwsu/core';
import type { AssistantConfig, ModelProvider, GeminiConfig } from './types';
import { createDefaultAssistantConfig } from './types';
import ProviderSelector from './ProviderSelector';
import ProviderConfigForm from './ProviderConfigForm';
import GenerateOptionsForm from './GenerateOptionsForm';
import styles from './index.module.less';

const CONFIG_KEY = 'assistant_llm_config';

const AssistantConfigTab: React.FC = () => {
  const { message } = App.useApp();
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [config, setConfig] = useState<AssistantConfig>(createDefaultAssistantConfig());
  const [configId, setConfigId] = useState<string | undefined>();

  const fetchConfig = useCallback(async () => {
    setLoading(true);
    try {
      const configMap = await fetchConfigsBatch([CONFIG_KEY]);
      const configInfo = configMap[CONFIG_KEY] as ConfigInfo | undefined;

      if (configInfo?.configValue) {
        try {
          const parsed = JSON.parse(configInfo.configValue) as AssistantConfig;
          setConfig({
            provider: parsed.provider || 'openai',
            dashscope: { ...createDefaultAssistantConfig().dashscope, ...parsed.dashscope },
            openai: { ...createDefaultAssistantConfig().openai, ...parsed.openai },
            gemini: { ...createDefaultAssistantConfig().gemini, ...parsed.gemini },
            anthropic: { ...createDefaultAssistantConfig().anthropic, ...parsed.anthropic },
            generateOptions: { ...createDefaultAssistantConfig().generateOptions, ...parsed.generateOptions },
          });
          setConfigId(configInfo.id);
        } catch {
          message.warning('助手配置解析失败，已恢复默认值');
          setConfig(createDefaultAssistantConfig());
          setConfigId(configInfo.id);
        }
      } else {
        setConfig(createDefaultAssistantConfig());
        setConfigId(undefined);
      }
    } catch {
      // error handled by request util
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchConfig();
  }, [fetchConfig]);

  const handleProviderChange = (provider: ModelProvider) => {
    setConfig((prev) => ({ ...prev, provider }));
  };

  const handleProviderConfigChange = (updated: AssistantConfig) => {
    setConfig(updated);
  };

  const handleGenerateOptionsChange = (generateOptions: AssistantConfig['generateOptions']) => {
    setConfig((prev) => ({ ...prev, generateOptions }));
  };

  const handleSave = async () => {
    // 校验当前提供商必填项
    const currentConfig = config[config.provider];
    if (!currentConfig.apiKey && config.provider !== 'gemini') {
      message.warning('请填写 API Key');
      return;
    }
    if (config.provider === 'gemini' && !currentConfig.apiKey && !(currentConfig as GeminiConfig).project) {
      message.warning('Gemini 至少需要填写 API Key 或 GCP Project');
      return;
    }

    setSaving(true);
    try {
      const success = await saveOrUpdateConfig({
        id: configId,
        configKey: CONFIG_KEY,
        configName: '助手配置',
        configValue: JSON.stringify(config),
        valueType: ConfigValueType.JSON,
        configType: ConfigType.SYSTEM,
        description: 'AI 助手模型提供商及生成参数配置',
      });

      if (success) {
        message.success('保存成功');
        fetchConfig();
      }
    } catch {
      // error handled by request util
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <div style={{ textAlign: 'center', padding: 48 }}>
        <Spin size="large" />
      </div>
    );
  }

  return (
    <div className={styles.assistantConfig}>
      <Card title="模型提供商" className={`${styles.sectionCard} ${styles.providerSection}`} size="small">
        <ProviderSelector value={config.provider} onChange={handleProviderChange} />
      </Card>

      <Card title="连接配置" className={styles.sectionCard} size="small">
        <ProviderConfigForm
          provider={config.provider}
          config={config}
          onConfigChange={handleProviderConfigChange}
        />
      </Card>

      <Card title="生成参数" className={`${styles.sectionCard} ${styles.generateOptionsSection}`} size="small">
        <GenerateOptionsForm
          value={config.generateOptions}
          onChange={handleGenerateOptionsChange}
        />
      </Card>

      <div className={styles.actionBar}>
        <Button icon={<ReloadOutlined />} onClick={fetchConfig}>
          重置
        </Button>
        <Button type="primary" icon={<SaveOutlined />} loading={saving} onClick={handleSave}>
          保存
        </Button>
      </div>
    </div>
  );
};

export default AssistantConfigTab;
