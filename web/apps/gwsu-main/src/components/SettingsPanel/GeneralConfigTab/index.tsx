import { useState, useEffect, useCallback } from 'react';
import { Button, App, Spin } from 'antd';
import {
  SaveOutlined,
  ReloadOutlined,
  GlobalOutlined,
  SafetyCertificateOutlined,
} from '@ant-design/icons';
import { fetchConfigsBatch, useProjectConfigStore } from '@gwsu/core';
import { getCaptchaTypeOptions, saveOrUpdateConfig } from '../services/config';
import type { ConfigInfo } from '../services/config';
import { ConfigValueType, ConfigType } from '@gwsu/core';
import type { BaseUrlConfig, CaptchaConfig, CaptchaTypeOption, GeneralTabKey } from './types';
import {
  BASE_URL_CONFIG_KEY,
  CAPTCHA_CONFIG_KEY,
  createDefaultBaseUrlConfig,
  createDefaultCaptchaConfig,
  DEFAULT_BASE_URL_CONFIG,
  normalizeCaptchaConfig,
} from './types';
import ProjectUrlForm from './ProjectUrlForm';
import CaptchaConfigForm from './CaptchaConfigForm';
import styles from './index.module.less';

const GeneralConfigTab: React.FC = () => {
  const { message } = App.useApp();
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [activeTab, setActiveTab] = useState<GeneralTabKey>('projectUrl');

  // 基础地址配置
  const [baseUrlConfig, setBaseUrlConfig] = useState<BaseUrlConfig>(createDefaultBaseUrlConfig());
  const [baseUrlConfigId, setBaseUrlConfigId] = useState<string | undefined>();
  const [captchaConfig, setCaptchaConfig] = useState<CaptchaConfig>(createDefaultCaptchaConfig());
  const [captchaConfigId, setCaptchaConfigId] = useState<string | undefined>();
  const [captchaTypeOptions, setCaptchaTypeOptions] = useState<CaptchaTypeOption[]>([]);
  const [captchaTypeLoading, setCaptchaTypeLoading] = useState(false);

  const fetchConfig = useCallback(async () => {
    setLoading(true);
    try {
      const configMap = await fetchConfigsBatch([BASE_URL_CONFIG_KEY, CAPTCHA_CONFIG_KEY]);

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

      // 解析图形验证码配置
      const captchaInfo = configMap[CAPTCHA_CONFIG_KEY] as ConfigInfo | undefined;
      if (captchaInfo?.configValue) {
        try {
          const parsed = JSON.parse(captchaInfo.configValue) as Partial<CaptchaConfig>;
          setCaptchaConfig(normalizeCaptchaConfig(parsed));
          setCaptchaConfigId(captchaInfo.id);
        } catch {
          message.warning('图形验证码配置解析失败，已恢复默认值');
          setCaptchaConfig(createDefaultCaptchaConfig());
          setCaptchaConfigId(captchaInfo.id);
        }
      } else {
        setCaptchaConfig(createDefaultCaptchaConfig());
        setCaptchaConfigId(undefined);
      }
    } catch {
      // error handled by request util
    } finally {
      setLoading(false);
    }
  }, []);

  const fetchCaptchaTypes = useCallback(async () => {
    setCaptchaTypeLoading(true);
    try {
      const options = await getCaptchaTypeOptions();
      setCaptchaTypeOptions(options);
    } catch {
      // error handled by request util
    } finally {
      setCaptchaTypeLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchConfig();
    fetchCaptchaTypes();
  }, [fetchConfig, fetchCaptchaTypes]);

  const handleBaseUrlConfigChange = (updated: BaseUrlConfig) => {
    setBaseUrlConfig(updated);
  };

  const handleCaptchaConfigChange = (updated: CaptchaConfig) => {
    setCaptchaConfig(updated);
  };

  const handleReload = () => {
    fetchConfig();
    fetchCaptchaTypes();
  };

  const handleSave = async () => {
    // 基础地址配置校验
    if (activeTab === 'projectUrl') {
      if (!baseUrlConfig.projectName) {
        message.warning('请填写项目名称');
        return;
      }
      if (!baseUrlConfig.viewBaseUrl) {
        message.warning('请填写前端地址');
        return;
      }
      if (!baseUrlConfig.apiBaseUrl) {
        message.warning('请填写后端 API 地址');
        return;
      }
    }
    if (activeTab === 'captcha') {
      if (!captchaConfig.type) {
        message.warning('请选择验证码类型');
        return;
      }
      if (!captchaConfig.waterMark) {
        message.warning('请填写水印文字');
        return;
      }
      if (!captchaConfig.expireSeconds || captchaConfig.expireSeconds <= 0) {
        message.warning('请填写有效的验证码有效时间');
        return;
      }
      if (!captchaConfig.verificationExpireSeconds || captchaConfig.verificationExpireSeconds <= 0) {
        message.warning('请填写有效的二次校验凭证有效时间');
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
          // 同步更新全局项目配置 store
          useProjectConfigStore.getState().setBaseUrlConfig(baseUrlConfig);
          fetchConfig();
        }
      }
      if (activeTab === 'captcha') {
        const success = await saveOrUpdateConfig({
          id: captchaConfigId,
          configKey: CAPTCHA_CONFIG_KEY,
          configName: '验证码配置',
          configValue: JSON.stringify(normalizeCaptchaConfig(captchaConfig)),
          valueType: ConfigValueType.JSON,
          configType: ConfigType.SYSTEM,
          description: '登录验证码默认配置',
        });
        if (success) {
          message.success('图形验证码配置保存成功');
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
    { key: 'projectUrl', label: '项目信息', icon: <GlobalOutlined /> },
    { key: 'captcha', label: '图形验证码', icon: <SafetyCertificateOutlined /> },
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
          {activeTab === 'captcha' && (
            <CaptchaConfigForm
              value={captchaConfig}
              typeOptions={captchaTypeOptions}
              typeLoading={captchaTypeLoading}
              onChange={handleCaptchaConfigChange}
            />
          )}

          {/* 操作栏 */}
          <div className={styles.actionBar}>
            <Button icon={<ReloadOutlined />} onClick={handleReload}>
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
