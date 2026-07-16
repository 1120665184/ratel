import { Tabs } from 'antd';
import GeneralConfigTab from './GeneralConfigTab';
import ModelConfigTab from './ModelConfigTab';
import AssistantConfigTab from './AssistantConfigTab';
import AttachmentConfigTab from './AttachmentConfigTab';
import DictConfigTab from './DictConfigTab';
import CustomConfigTab from './CustomConfigTab';
import styles from './index.module.less';

const SettingsPanel: React.FC = () => {
  const items = [
    { key: 'general', label: '通用配置', children: <GeneralConfigTab /> },
    { key: 'model', label: '模型配置', children: <ModelConfigTab /> },
    { key: 'assistant', label: '助手配置', children: <AssistantConfigTab /> },
    { key: 'attachment', label: '附件配置', children: <AttachmentConfigTab /> },
    { key: 'dict', label: '字典配置', children: <DictConfigTab /> },
    { key: 'custom', label: '其他配置', children: <CustomConfigTab /> },
  ];

  return (
    <div className={styles.settingsPanel}>
      <Tabs
        defaultActiveKey="general"
        items={items}
        className={styles.settingsTabs}
      />
    </div>
  );
};

export default SettingsPanel;
