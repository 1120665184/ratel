import { useState, useEffect, useCallback } from 'react';
import { Button, App, Spin } from 'antd';
import {
  SaveOutlined,
  ReloadOutlined,
  GlobalOutlined,
} from '@ant-design/icons';
import { fetchConfigsBatch } from '@gwsu/core';
import { saveOrUpdateConfig } from '../services/config';
import type { ConfigInfo } from '../services/config';
import { ConfigValueType, ConfigType } from '@gwsu/core';
import type { BaseUrlConfig, GeneralTabKey } from './types';
import { createDefaultBaseUrlConfig, DEFAULT_BASE_URL_CONFIG, BASE_URL_CONFIG_KEY } from './types';
import ProjectUrlForm from './ProjectUrlForm';
import styles from './index.module.less';

const GeneralConfigTab: React.FC = () => {
  const { message } = App.useApp();
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [activeTab, setActiveTab] = useState<GeneralTabKey>('projectUrl');

  // 基础地址配置
  const [baseUrlConfig, setBaseUrlConfig] = useState<BaseUrlConfig>(createDefaultBaseUrlConfig());
  const [baseUrlConfigId, setBaseUrlConfigId] = useState<string | undefined>();

  const fetchConfig = useCallback(async () => {
    setLoading(true);
    try {
      const configMap = await fetchConfigsBatch([BASE_URL_CONFIG_KEY]);

      // 解析基础地址配置
      const urlInfo = configMap[BASE_URL_CONFIG_KEY] as ConfigInfo | undefined;
      if (urlInfo?.configValue) {
        try {
          const parsed = JSON.parse(urlInfo.configValue) as BaseUrlConfig;
          setBaseUrlConfig({ ...DEFAULT_BASE_URL_CONFIG, ...parsed });
          setBaseUrlConfigId(urlInfo.id);
        } catch {
          message.warning('项目地址配置解析失败，已恢复默认值');
          setBaseUrlConfig(createDefaultBaseUrlConfig());
          setBaseUrlConfigId(urlInfo.id);
        }
      } else {
        setBaseUrlConfig(createDefaultBaseUrlConfig());
        setBaseUrlConfigId(undefined);
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

  const handleBaseUrlConfigChange = (updated: BaseUrlConfig) => {
    setBaseUrlConfig(updated);
  };

  const handleSave = async () => {
    // 基础地址配置校验
    if (activeTab === 'projectUrl') {
      if (!baseUrlConfig.viewBaseUrl) {
        message.warning('请填写前端地址');
        return;
      }
      if (!baseUrlConfig.apiBaseUrl) {
        message.warning('请填写后端 API 地址');
        return;
      }
    }

    setSaving(true);
    try {
      if (activeTab === 'projectUrl') {
        const success = await saveOrUpdateConfig({
          id: baseUrlConfigId,
          configKey: BASE_URL_CONFIG_KEY,
          configName: '基础地址配置',
          configValue: JSON.stringify(baseUrlConfig),
          valueType: ConfigValueType.JSON,
          configType: ConfigType.SYSTEM,
          description: '项目前后端基础地址配置',
        });
        if (success) {
          message.success('项目地址配置保存成功');
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

  const tabs: { key: GeneralTabKey; label: string; icon: React.ReactNode }[] = [
    { key: 'projectUrl', label: '项目地址', icon: <GlobalOutlined /> },
  ];

  return (
    <div className={styles.generalConfig}>
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
          {activeTab === 'projectUrl' && (
            <ProjectUrlForm value={baseUrlConfig} onChange={handleBaseUrlConfigChange} />
          )}

          {/* 操作栏 */}
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

export default GeneralConfigTab;
