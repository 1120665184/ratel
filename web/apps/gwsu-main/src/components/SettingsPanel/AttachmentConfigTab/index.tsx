import { useState, useEffect, useCallback } from 'react';
import { Button, App, Spin, Modal } from 'antd';
import { SaveOutlined, ReloadOutlined, CloudServerOutlined, FilterOutlined } from '@ant-design/icons';
import { fetchConfigsBatch, ConfigValueType, ConfigType } from '@gwsu/core';
import { saveOrUpdateConfig } from '../services/config';
import type { ConfigInfo } from '../services/config';
import ServerConfigForm from './ServerConfigForm';
import ExtensionFilterForm from './ExtensionFilterForm';
import type { UploadServerConfig, ExtensionFilterConfig, AttachmentTabKey } from './types';
import {
  UPLOAD_SERVER_CONFIG_KEY,
  UPLOAD_EXTENSION_CONFIG_KEY,
  createDefaultServerConfig,
  createDefaultExtensionConfig,
} from './types';
import styles from './index.module.less';

const AttachmentConfigTab: React.FC = () => {
  const { message } = App.useApp();
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [activeTab, setActiveTab] = useState<AttachmentTabKey>('server');

  const [serverConfig, setServerConfig] = useState<UploadServerConfig>(createDefaultServerConfig());
  const [serverConfigId, setServerConfigId] = useState<string | undefined>();
  const [extensionConfig, setExtensionConfig] = useState<ExtensionFilterConfig>(createDefaultExtensionConfig());
  const [extensionConfigId, setExtensionConfigId] = useState<string | undefined>();

  const fetchConfig = useCallback(async () => {
    setLoading(true);
    try {
      const configMap = await fetchConfigsBatch([UPLOAD_SERVER_CONFIG_KEY, UPLOAD_EXTENSION_CONFIG_KEY]);

      const serverInfo = configMap[UPLOAD_SERVER_CONFIG_KEY] as ConfigInfo | undefined;
      if (serverInfo?.configValue) {
        try {
          const parsed = JSON.parse(serverInfo.configValue) as UploadServerConfig;
          setServerConfig({ ...createDefaultServerConfig(), ...parsed });
          setServerConfigId(serverInfo.id);
        } catch {
          message.warning('服务配置解析失败，已恢复默认值');
          setServerConfig(createDefaultServerConfig());
          setServerConfigId(serverInfo.id);
        }
      } else {
        setServerConfig(createDefaultServerConfig());
        setServerConfigId(undefined);
      }

      const extInfo = configMap[UPLOAD_EXTENSION_CONFIG_KEY] as ConfigInfo | undefined;
      if (extInfo?.configValue) {
        try {
          const parsed = JSON.parse(extInfo.configValue) as ExtensionFilterConfig;
          setExtensionConfig({ ...createDefaultExtensionConfig(), ...parsed });
          setExtensionConfigId(extInfo.id);
        } catch {
          message.warning('文件过滤配置解析失败，已恢复默认值');
          setExtensionConfig(createDefaultExtensionConfig());
          setExtensionConfigId(extInfo.id);
        }
      } else {
        setExtensionConfig(createDefaultExtensionConfig());
        setExtensionConfigId(undefined);
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

  const handleSaveServer = useCallback(async () => {
    Modal.confirm({
      title: '确认更改',
      content: '更改上传服务配置可能导致原上传文件不可用，确认更改吗？',
      okText: '确认更改',
      okType: 'warning',
      cancelText: '取消',
      onOk: async () => {
        setSaving(true);
        try {
          const success = await saveOrUpdateConfig({
            id: serverConfigId,
            configKey: UPLOAD_SERVER_CONFIG_KEY,
            configName: '上传服务配置',
            configValue: JSON.stringify(serverConfig),
            valueType: ConfigValueType.JSON,
            configType: ConfigType.SYSTEM,
            description: '文件上传服务类型及连接配置',
          });
          if (success) {
            message.success('服务配置保存成功');
            fetchConfig();
          }
        } catch {
          // error handled by request util
        } finally {
          setSaving(false);
        }
      },
    });
  }, [serverConfig, serverConfigId, fetchConfig, message]);

  const handleSaveFilter = useCallback(async () => {
    setSaving(true);
    try {
      const success = await saveOrUpdateConfig({
        id: extensionConfigId,
        configKey: UPLOAD_EXTENSION_CONFIG_KEY,
        configName: '文件后缀过滤配置',
        configValue: JSON.stringify(extensionConfig),
        valueType: ConfigValueType.JSON,
        configType: ConfigType.SYSTEM,
        description: '禁止上传的文件后缀配置',
      });
      if (success) {
        message.success('文件过滤配置保存成功');
        fetchConfig();
      }
    } catch {
      // error handled by request util
    } finally {
      setSaving(false);
    }
  }, [extensionConfig, extensionConfigId, fetchConfig, message]);

  if (loading) {
    return (
      <div style={{ textAlign: 'center', padding: 48 }}>
        <Spin size="large" />
      </div>
    );
  }

  const tabs: { key: AttachmentTabKey; label: string; icon: React.ReactNode }[] = [
    { key: 'server', label: '服务配置', icon: <CloudServerOutlined /> },
    { key: 'filter', label: '文件过滤', icon: <FilterOutlined /> },
  ];

  return (
    <div className={styles.attachmentConfig}>
      <div className={styles.layout}>
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

        <div className={styles.sideTabContent}>
          {activeTab === 'server' && (
            <ServerConfigForm value={serverConfig} onChange={setServerConfig} />
          )}
          {activeTab === 'filter' && (
            <ExtensionFilterForm value={extensionConfig} onChange={setExtensionConfig} />
          )}

          <div className={styles.actionBar}>
            <Button icon={<ReloadOutlined />} onClick={fetchConfig}>
              重置
            </Button>
            <Button
              type="primary"
              icon={<SaveOutlined />}
              loading={saving}
              onClick={activeTab === 'server' ? handleSaveServer : handleSaveFilter}
            >
              保存
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default AttachmentConfigTab;
