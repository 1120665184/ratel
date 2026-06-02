import { Switch, Typography } from 'antd';
import {
  BulbOutlined,
  ToolOutlined,
  HistoryOutlined,
  DragOutlined,
} from '@ant-design/icons';
import type { ViewConfig } from './types';
import styles from './ViewConfigForm.module.less';

interface ViewConfigFormProps {
  value?: ViewConfig;
  onChange?: (value: ViewConfig) => void;
}

interface SwitchItemProps {
  icon: React.ReactNode;
  title: string;
  description: string;
  checked: boolean;
  onChange: (checked: boolean) => void;
}

const SwitchItem: React.FC<SwitchItemProps> = ({ icon, title, description, checked, onChange }) => (
  <div className={styles.switchItem}>
    <div className={styles.switchIcon}>{icon}</div>
    <div className={styles.switchContent}>
      <Typography.Text strong className={styles.switchTitle}>{title}</Typography.Text>
      <Typography.Text type="secondary" className={styles.switchDesc}>{description}</Typography.Text>
    </div>
    <Switch checked={checked} onChange={onChange} />
  </div>
);

const ViewConfigForm: React.FC<ViewConfigFormProps> = ({ value, onChange }) => {
  const handleChange = (field: keyof ViewConfig, checked: boolean) => {
    onChange?.({ ...value!, [field]: checked });
  };

  return (
    <div className={styles.viewConfigForm}>
      <SwitchItem
        icon={<BulbOutlined />}
        title="思考内容展示"
        description="在对话中展示 AI 的推理思考过程"
        checked={value?.showThinking ?? true}
        onChange={(checked) => handleChange('showThinking', checked)}
      />
      <SwitchItem
        icon={<ToolOutlined />}
        title="工具调用展示"
        description="在对话中展示 AI 调用工具的详细过程"
        checked={value?.showToolCalls ?? true}
        onChange={(checked) => handleChange('showToolCalls', checked)}
      />
      <SwitchItem
        icon={<HistoryOutlined />}
        title="历史记录展示"
        description="在侧边栏展示历史对话记录列表"
        checked={value?.showHistory ?? true}
        onChange={(checked) => handleChange('showHistory', checked)}
      />
      <SwitchItem
        icon={<DragOutlined />}
        title="拖拽模式"
        description="启用后可拖拽调整助手面板的位置和大小"
        checked={value?.enableDragMode ?? false}
        onChange={(checked) => handleChange('enableDragMode', checked)}
      />
    </div>
  );
};

export default ViewConfigForm;
