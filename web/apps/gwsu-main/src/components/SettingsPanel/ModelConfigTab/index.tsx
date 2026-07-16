import { useCallback, useEffect, useState } from 'react';
import { App, Button, Card, Form, Spin, Switch } from 'antd';
import {
  ApiOutlined,
  BranchesOutlined,
  DeploymentUnitOutlined,
  ReloadOutlined,
  SaveOutlined,
} from '@ant-design/icons';
import { fetchConfigsBatch } from '@gwsu/core';
import { ConfigType, ConfigValueType } from '@gwsu/core';
import { saveLlmModelConfig, saveOrUpdateConfig } from '../services/config';
import type { ConfigInfo } from '../services/config';
import ProviderSelector from './ProviderSelector';
import ProviderConfigForm from './ProviderConfigForm';
import GenerateOptionsForm from './GenerateOptionsForm';
import EmbeddingConfigForm from './EmbeddingConfigForm';
import RerankConfigForm from './RerankConfigForm';
import type {
  GeminiConfig,
  ModelEmbeddingConfig,
  ModelLlmConfig,
  ModelProvider,
  ModelRerankConfig,
  ModelTabKey,
} from './types';
import {
  createDefaultModelEmbeddingConfig,
  createDefaultModelLlmConfig,
  createDefaultModelRerankConfig,
} from './types';
import styles from './index.module.less';

const MODEL_LLM_CONFIG_KEY = 'model_llm_config';
const MODEL_EMBEDDING_CONFIG_KEY = 'model_embedding_config';
const MODEL_RERANK_CONFIG_KEY = 'model_rerank_config';

const ModelConfigTab: React.FC = () => {
  const { message } = App.useApp();
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [activeTab, setActiveTab] = useState<ModelTabKey>('llm');
  const [llmConfig, setLlmConfig] = useState<ModelLlmConfig>(createDefaultModelLlmConfig());
  const [embeddingConfig, setEmbeddingConfig] = useState<ModelEmbeddingConfig>(createDefaultModelEmbeddingConfig());
  const [rerankConfig, setRerankConfig] = useState<ModelRerankConfig>(createDefaultModelRerankConfig());
  const [configIds, setConfigIds] = useState<Record<ModelTabKey, string | undefined>>({
    llm: undefined,
    embedding: undefined,
    rerank: undefined,
  });

  const fetchConfig = useCallback(async () => {
    setLoading(true);
    try {
      const configMap = await fetchConfigsBatch([
        MODEL_LLM_CONFIG_KEY,
        MODEL_EMBEDDING_CONFIG_KEY,
        MODEL_RERANK_CONFIG_KEY,
      ]);

      const llmInfo = configMap[MODEL_LLM_CONFIG_KEY] as ConfigInfo | undefined;
      if (llmInfo?.configValue) {
        try {
          const parsed = JSON.parse(llmInfo.configValue) as Partial<ModelLlmConfig>;
          const defaults = createDefaultModelLlmConfig();
          setLlmConfig({
            provider: parsed.provider || defaults.provider,
            supportMultimodal: parsed.supportMultimodal ?? defaults.supportMultimodal,
            dashscope: { ...defaults.dashscope, ...parsed.dashscope },
            openai: { ...defaults.openai, ...parsed.openai },
            gemini: { ...defaults.gemini, ...parsed.gemini },
            anthropic: { ...defaults.anthropic, ...parsed.anthropic },
            generateOptions: {
              ...defaults.generateOptions,
              ...parsed.generateOptions,
              additionalBodyParams: parsed.generateOptions?.additionalBodyParams ?? {},
            },
          });
          setConfigIds((prev) => ({ ...prev, llm: llmInfo.id }));
        } catch {
          message.warning('LLM 模型配置解析失败，已恢复默认值');
          setLlmConfig(createDefaultModelLlmConfig());
          setConfigIds((prev) => ({ ...prev, llm: llmInfo.id }));
        }
      } else {
        setLlmConfig(createDefaultModelLlmConfig());
        setConfigIds((prev) => ({ ...prev, llm: undefined }));
      }

      const embeddingInfo = configMap[MODEL_EMBEDDING_CONFIG_KEY] as ConfigInfo | undefined;
      if (embeddingInfo?.configValue) {
        try {
          const parsed = JSON.parse(embeddingInfo.configValue) as Partial<ModelEmbeddingConfig>;
          const defaults = createDefaultModelEmbeddingConfig();
          setEmbeddingConfig({
            provider: parsed.provider || defaults.provider,
            dashscope: { ...defaults.dashscope, ...parsed.dashscope },
            openai: { ...defaults.openai, ...parsed.openai },
            ollama: { ...defaults.ollama, ...parsed.ollama },
            zhipuai: { ...defaults.zhipuai, ...parsed.zhipuai },
          });
          setConfigIds((prev) => ({ ...prev, embedding: embeddingInfo.id }));
        } catch {
          message.warning('向量化模型配置解析失败，已恢复默认值');
          setEmbeddingConfig(createDefaultModelEmbeddingConfig());
          setConfigIds((prev) => ({ ...prev, embedding: embeddingInfo.id }));
        }
      } else {
        setEmbeddingConfig(createDefaultModelEmbeddingConfig());
        setConfigIds((prev) => ({ ...prev, embedding: undefined }));
      }

      const rerankInfo = configMap[MODEL_RERANK_CONFIG_KEY] as ConfigInfo | undefined;
      if (rerankInfo?.configValue) {
        try {
          const parsed = JSON.parse(rerankInfo.configValue) as Partial<ModelRerankConfig>;
          const defaults = createDefaultModelRerankConfig();
          setRerankConfig({
            provider: parsed.provider || defaults.provider,
            dashscope: { ...defaults.dashscope, ...parsed.dashscope },
          });
          setConfigIds((prev) => ({ ...prev, rerank: rerankInfo.id }));
        } catch {
          message.warning('重排模型配置解析失败，已恢复默认值');
          setRerankConfig(createDefaultModelRerankConfig());
          setConfigIds((prev) => ({ ...prev, rerank: rerankInfo.id }));
        }
      } else {
        setRerankConfig(createDefaultModelRerankConfig());
        setConfigIds((prev) => ({ ...prev, rerank: undefined }));
      }
    } catch {
      // 由请求工具统一提示
    } finally {
      setLoading(false);
    }
  }, [message]);

  useEffect(() => {
    void fetchConfig();
  }, [fetchConfig]);

  const handleProviderChange = (provider: ModelProvider) => {
    setLlmConfig((prev) => ({ ...prev, provider }));
  };

  const handleSave = async () => {
    if (activeTab === 'llm') {
      const currentConfig = llmConfig[llmConfig.provider];
      if (!currentConfig.apiKey && llmConfig.provider !== 'gemini') {
        message.warning('请填写 API Key');
        return;
      }
      if (llmConfig.provider === 'gemini' && !currentConfig.apiKey && !(currentConfig as GeminiConfig).project) {
        message.warning('Gemini 至少需要填写 API Key 或 GCP Project');
        return;
      }
      const additionalBodyParams = llmConfig.generateOptions?.additionalBodyParams;
      if (additionalBodyParams !== undefined && additionalBodyParams !== null) {
        if (typeof additionalBodyParams !== 'object' || Array.isArray(additionalBodyParams)) {
          message.warning('自定义请求体参数必须为 JSON 对象格式（{}）');
          return;
        }
      }
    }

    if (activeTab === 'embedding') {
      const currentConfig = embeddingConfig[embeddingConfig.provider];
      if (embeddingConfig.provider !== 'ollama' && !('apiKey' in currentConfig && currentConfig.apiKey)) {
        message.warning('请填写 API Key');
        return;
      }
      if (!currentConfig.modelName) {
        message.warning('请填写模型名称');
        return;
      }
    }

    if (activeTab === 'rerank') {
      if (!rerankConfig.dashscope.apiKey) {
        message.warning('请填写 API Key');
        return;
      }
      if (!rerankConfig.dashscope.modelName) {
        message.warning('请填写模型名称');
        return;
      }
    }

    const saveMeta = {
      llm: {
        id: configIds.llm,
        configKey: MODEL_LLM_CONFIG_KEY,
        configName: 'LLM 模型配置',
        configValue: JSON.stringify(llmConfig),
        description: 'LLM 模型提供商、连接参数及生成参数配置',
        successMessage: 'LLM 模型配置保存成功',
      },
      embedding: {
        id: configIds.embedding,
        configKey: MODEL_EMBEDDING_CONFIG_KEY,
        configName: '向量化模型配置',
        configValue: JSON.stringify(embeddingConfig),
        description: '向量化模型提供商及连接参数配置',
        successMessage: '向量化模型配置保存成功',
      },
      rerank: {
        id: configIds.rerank,
        configKey: MODEL_RERANK_CONFIG_KEY,
        configName: '重排模型配置',
        configValue: JSON.stringify(rerankConfig),
        description: '重排模型提供商及连接参数配置',
        successMessage: '重排模型配置保存成功',
      },
    }[activeTab];

    setSaving(true);
    try {
      const { successMessage, ...payload } = saveMeta;
      const saveConfig = activeTab === 'llm' ? saveLlmModelConfig : saveOrUpdateConfig;
      const success = await saveConfig({
        ...payload,
        valueType: ConfigValueType.JSON,
        configType: ConfigType.SYSTEM,
      });
      if (success) {
        message.success(successMessage);
        void fetchConfig();
      }
    } catch {
      // 由请求工具统一提示
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <div className={styles.loading}>
        <Spin size="large" />
      </div>
    );
  }

  const tabs: { key: ModelTabKey; label: string; icon: React.ReactNode }[] = [
    { key: 'llm', label: 'LLM模型', icon: <ApiOutlined /> },
    { key: 'embedding', label: '向量化模型', icon: <DeploymentUnitOutlined /> },
    { key: 'rerank', label: '重排模型', icon: <BranchesOutlined /> },
  ];

  return (
    <div className={styles.assistantConfig}>
      <div className={styles.layout}>
        <div className={styles.sideTabNav}>
          {tabs.map((tab) => (
            <button
              key={tab.key}
              type="button"
              className={`${styles.sideTab} ${activeTab === tab.key ? styles.sideTabActive : ''}`}
              onClick={() => setActiveTab(tab.key)}
              aria-selected={activeTab === tab.key}
            >
              <span className={styles.sideTabIcon}>{tab.icon}</span>
              <span className={styles.sideTabLabel}>{tab.label}</span>
            </button>
          ))}
        </div>

        <div className={styles.sideTabContent}>
          {activeTab === 'llm' && (
            <>
              <Card title="模型提供商" className={`${styles.sectionCard} ${styles.providerSection}`} size="small">
                <ProviderSelector value={llmConfig.provider} onChange={handleProviderChange} />
                <Form layout="vertical" className={styles.inlineOptions}>
                  <Form.Item label="支持多模态">
                    <Switch
                      checked={llmConfig.supportMultimodal}
                      onChange={(supportMultimodal) => setLlmConfig((prev) => ({ ...prev, supportMultimodal }))}
                    />
                  </Form.Item>
                </Form>
              </Card>

              <Card title="连接配置" className={styles.sectionCard} size="small">
                <ProviderConfigForm
                  provider={llmConfig.provider}
                  config={llmConfig}
                  onConfigChange={setLlmConfig}
                />
              </Card>

              <Card title="生成参数" className={`${styles.sectionCard} ${styles.generateOptionsSection}`} size="small">
                <GenerateOptionsForm
                  value={llmConfig.generateOptions}
                  onChange={(generateOptions) => setLlmConfig((prev) => ({ ...prev, generateOptions }))}
                />
              </Card>
            </>
          )}

          {activeTab === 'embedding' && (
            <EmbeddingConfigForm value={embeddingConfig} onChange={setEmbeddingConfig} />
          )}

          {activeTab === 'rerank' && (
            <RerankConfigForm value={rerankConfig} onChange={setRerankConfig} />
          )}

          <div className={styles.actionBar}>
            <Button icon={<ReloadOutlined />} onClick={fetchConfig}>
              重置
            </Button>
            <Button type="primary" icon={<SaveOutlined />} loading={saving} onClick={handleSave} data-ai-approval>
              保存
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default ModelConfigTab;
