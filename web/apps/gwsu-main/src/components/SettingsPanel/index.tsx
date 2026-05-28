import { Tabs } from 'antd';
import AssistantConfigTab from './AssistantConfigTab';
import DictConfigTab from './DictConfigTab';
import CustomConfigTab from './CustomConfigTab';
import styles from './index.module.less';

const SettingsPanel: React.FC = () => {
  const items = [
    { key: 'assistant', label: '助手配置', children: <AssistantConfigTab /> },
    { key: 'dict', label: '字典配置', children: <DictConfigTab /> },
    { key: 'custom', label: '其他配置', children: <CustomConfigTab /> },
  ];

  return (
    <div className={styles.settingsPanel}>
      <Tabs
        defaultActiveKey="assistant"
        items={items}
        className={styles.settingsTabs}
      />
    </div>
  );
};

export default SettingsPanel;
