import { useState, useEffect, useCallback } from 'react';
import { Card, Button, App, Spin } from 'antd';
import {
  SaveOutlined,
  ReloadOutlined,
  EyeOutlined,
  ApiOutlined,
  ControlOutlined,
} from '@ant-design/icons';
import { fetchConfigsBatch } from '@gwsu/core';
import { saveOrUpdateConfig, saveRemoteControlConfig } from '../services/config';
import type { ConfigInfo } from '../services/config';
import { ConfigValueType, ConfigType } from '@gwsu/core';
import type { AssistantConfig, ModelProvider, GeminiConfig, ViewConfig, RemoteControlConfig, AssistantTabKey } from './types';
import { createDefaultAssistantConfig, createDefaultViewConfig, createDefaultRemoteControlConfig, DEFAULT_DINGTALK_REMOTE_CONFIG } from './types';
import ProviderSelector from './ProviderSelector';
import ProviderConfigForm from './ProviderConfigForm';
import GenerateOptionsForm from './GenerateOptionsForm';
import ViewConfigForm from './ViewConfigForm';
import RemoteControlForm from './RemoteControlForm';
import styles from './index.module.less';

const LLM_CONFIG_KEY = 'assistant_llm_config';
const VIEW_CONFIG_KEY = 'assistant_view_config';
const REMOTE_CONTROL_CONFIG_KEY = 'assistant_remote_control_config';

const AssistantConfigTab: React.FC = () => {
  const { message } = App.useApp();
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [activeTab, setActiveTab] = useState<AssistantTabKey>('view');

  // LLM 配置
  const [llmConfig, setLlmConfig] = useState<AssistantConfig>(createDefaultAssistantConfig());
  const [llmConfigId, setLlmConfigId] = useState<string | undefined>();

  // 展示配置
  const [viewConfig, setViewConfig] = useState<ViewConfig>(createDefaultViewConfig());
  const [viewConfigId, setViewConfigId] = useState<string | undefined>();

  // 远程操作配置
  const [remoteControlConfig, setRemoteControlConfig] = useState<RemoteControlConfig>(createDefaultRemoteControlConfig());
  const [remoteControlConfigId, setRemoteControlConfigId] = useState<string | undefined>();

  const fetchConfig = useCallback(async () => {
    setLoading(true);
    try {
      const configMap = await fetchConfigsBatch([LLM_CONFIG_KEY, VIEW_CONFIG_KEY, REMOTE_CONTROL_CONFIG_KEY]);

      // 解析 LLM 配置
      const llmInfo = configMap[LLM_CONFIG_KEY] as ConfigInfo | undefined;
      if (llmInfo?.configValue) {
        try {
          const parsed = JSON.parse(llmInfo.configValue) as AssistantConfig;
          setLlmConfig({
            provider: parsed.provider || 'openai',
            dashscope: { ...createDefaultAssistantConfig().dashscope, ...parsed.dashscope },
            openai: { ...createDefaultAssistantConfig().openai, ...parsed.openai },
            gemini: { ...createDefaultAssistantConfig().gemini, ...parsed.gemini },
            anthropic: { ...createDefaultAssistantConfig().anthropic, ...parsed.anthropic },
            generateOptions: {
              ...createDefaultAssistantConfig().generateOptions,
              ...parsed.generateOptions,
              additionalBodyParams: parsed.generateOptions?.additionalBodyParams ?? {},
            },
          });
          setLlmConfigId(llmInfo.id);
        } catch {
          message.warning('LLM 配置解析失败，已恢复默认值');
          setLlmConfig(createDefaultAssistantConfig());
          setLlmConfigId(llmInfo.id);
        }
      } else {
        setLlmConfig(createDefaultAssistantConfig());
        setLlmConfigId(undefined);
      }

      // 解析展示配置
      const viewInfo = configMap[VIEW_CONFIG_KEY] as ConfigInfo | undefined;
      if (viewInfo?.configValue) {
        try {
          const parsed = JSON.parse(viewInfo.configValue) as ViewConfig;
          setViewConfig({ ...createDefaultViewConfig(), ...parsed });
          setViewConfigId(viewInfo.id);
        } catch {
          message.warning('展示配置解析失败，已恢复默认值');
          setViewConfig(createDefaultViewConfig());
          setViewConfigId(viewInfo.id);
        }
      } else {
        setViewConfig(createDefaultViewConfig());
        setViewConfigId(undefined);
      }

      // 解析远程操作配置
      const remoteInfo = configMap[REMOTE_CONTROL_CONFIG_KEY] as ConfigInfo | undefined;
      if (remoteInfo?.configValue) {
        try {
          const parsed = JSON.parse(remoteInfo.configValue) as RemoteControlConfig;
          setRemoteControlConfig({
            type: parsed.type || 'NONE',
            dingTalk: { ...DEFAULT_DINGTALK_REMOTE_CONFIG, ...parsed.dingTalk },
          });
          setRemoteControlConfigId(remoteInfo.id);
        } catch {
          message.warning('远程操作配置解析失败，已恢复默认值');
          setRemoteControlConfig(createDefaultRemoteControlConfig());
          setRemoteControlConfigId(remoteInfo.id);
        }
      } else {
        setRemoteControlConfig(createDefaultRemoteControlConfig());
        setRemoteControlConfigId(undefined);
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
    setLlmConfig((prev) => ({ ...prev, provider }));
  };

  const handleProviderConfigChange = (updated: AssistantConfig) => {
    setLlmConfig(updated);
  };

  const handleGenerateOptionsChange = (generateOptions: AssistantConfig['generateOptions']) => {
    setLlmConfig((prev) => ({ ...prev, generateOptions }));
  };

  const handleViewConfigChange = (updated: ViewConfig) => {
    setViewConfig(updated);
  };

  const handleRemoteControlConfigChange = (updated: RemoteControlConfig) => {
    setRemoteControlConfig(updated);
  };

  const handleSave = async () => {
    // LLM 配置校验
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

    // 远程操作配置校验
    if (activeTab === 'remote' && remoteControlConfig.type === 'DING_TALK') {
      const dt = remoteControlConfig.dingTalk;
      if (!dt.protocol || !dt.regionId || !dt.endpoint) {
        message.warning('请填写完整的钉钉连接配置');
        return;
      }
      if (!dt.clientId) {
        message.warning('请填写 Client ID');
        return;
      }
      if (!dt.clientSecret) {
        message.warning('请填写 Client Secret');
        return;
      }
      if (!dt.aiCardTemplateId) {
        message.warning('请填写 AI 输出卡片模板 ID');
        return;
      }
    }

    setSaving(true);
    try {
      // 保存当前激活的 Tab 配置
      if (activeTab === 'llm') {
        const success = await saveOrUpdateConfig({
          id: llmConfigId,
          configKey: LLM_CONFIG_KEY,
          configName: '助手 LLM 配置',
          configValue: JSON.stringify(llmConfig),
          valueType: ConfigValueType.JSON,
          configType: ConfigType.SYSTEM,
          description: 'AI 助手模型提供商及生成参数配置',
        });
        if (success) {
          message.success('LLM 配置保存成功');
          fetchConfig();
        }
      } else if (activeTab === 'view') {
        const success = await saveOrUpdateConfig({
          id: viewConfigId,
          configKey: VIEW_CONFIG_KEY,
          configName: '助手展示配置',
          configValue: JSON.stringify(viewConfig),
          valueType: ConfigValueType.JSON,
          configType: ConfigType.SYSTEM,
          description: 'AI 助手界面展示配置',
        });
        if (success) {
          message.success('展示配置保存成功');
          fetchConfig();
        }
      } else if (activeTab === 'remote') {
        const success = await saveRemoteControlConfig(remoteControlConfig);
        if (success) {
          message.success('远程操作配置保存成功');
          fetchConfig();
        }
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

  const tabs: { key: AssistantTabKey; label: string; icon: React.ReactNode }[] = [
    { key: 'view', label: '展示配置', icon: <EyeOutlined /> },
    { key: 'llm', label: 'LLM 配置', icon: <ApiOutlined /> },
    { key: 'remote', label: '远程操作', icon: <ControlOutlined /> },
  ];

  return (
    <div className={styles.assistantConfig}>
      <div className={styles.layout}>
        {/* 左侧 Tab 导航 */}
        <div className={styles.sideTabNav}>
          {tabs.map((tab) => (
            <button
              key={tab.key}
              className={`${styles.sideTab} ${activeTab === tab.key ? styles.sideTabActive : ''}`}
              onClick={() => setActiveTab(tab.key)}
            >
              <span className={styles.sideTabIcon}>{tab.icon}</span>
              <span className={styles.sideTabLabel}>{tab.label}</span>
            </button>
          ))}
        </div>

        {/* 右侧内容区 */}
        <div className={styles.sideTabContent}>
          {activeTab === 'view' && (
            <ViewConfigForm value={viewConfig} onChange={handleViewConfigChange} />
          )}

          {activeTab === 'llm' && (
            <>
              <Card title="模型提供商" className={`${styles.sectionCard} ${styles.providerSection}`} size="small">
                <ProviderSelector value={llmConfig.provider} onChange={handleProviderChange} />
              </Card>

              <Card title="连接配置" className={styles.sectionCard} size="small">
                <ProviderConfigForm
                  provider={llmConfig.provider}
                  config={llmConfig}
                  onConfigChange={handleProviderConfigChange}
                />
              </Card>

              <Card title="生成参数" className={`${styles.sectionCard} ${styles.generateOptionsSection}`} size="small">
                <GenerateOptionsForm
                  value={llmConfig.generateOptions}
                  onChange={handleGenerateOptionsChange}
                />
              </Card>
            </>
          )}

          {activeTab === 'remote' && (
            <RemoteControlForm
              value={remoteControlConfig}
              onChange={handleRemoteControlConfigChange}
            />
          )}

          {/* 操作栏 */}
          <div className={styles.actionBar}>
            <Button icon={<ReloadOutlined />} onClick={fetchConfig}>
              重置
            </Button>
            <Button type="primary" icon={<SaveOutlined />} loading={saving} onClick={handleSave}>
              保存
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default AssistantConfigTab;
