import { Form, Select, Card, Typography } from 'antd';
import type { RemoteControlConfig, RemoteControlType } from './types';
import { REMOTE_CONTROL_TYPE_OPTIONS } from './types';
import DingTalkRemoteConfigForm from './providers/DingTalkRemoteConfigForm';
import styles from './RemoteControlForm.module.less';

interface RemoteControlFormProps {
  value?: RemoteControlConfig;
  onChange?: (value: RemoteControlConfig) => void;
}

const RemoteControlForm: React.FC<RemoteControlFormProps> = ({ value, onChange }) => {
  const handleTypeChange = (type: RemoteControlType) => {
    onChange?.({ ...value!, type });
  };

  const handleDingTalkConfigChange = (dingTalk: RemoteControlConfig['dingTalk']) => {
    onChange?.({ ...value!, dingTalk });
  };

  return (
    <div className={styles.remoteControlForm}>
      <Form layout="vertical" className={styles.typeSelector}>
        <Form.Item label="操作应用">
          <Select
            value={value?.type}
            onChange={handleTypeChange}
            options={REMOTE_CONTROL_TYPE_OPTIONS}
            aria-label="远程操作类型"
          />
        </Form.Item>
      </Form>

      {value?.type === 'NONE' && (
        <div className={styles.typeHint}>
          <Typography.Text type="secondary">未启用任何远程操作</Typography.Text>
        </div>
      )}

      {value?.type === 'DING_TALK' && (
        <Card title="钉钉配置" size="small">
          <Form layout="vertical">
            <DingTalkRemoteConfigForm
              value={value?.dingTalk}
              onChange={handleDingTalkConfigChange}
            />
          </Form>
        </Card>
      )}
    </div>
  );
};

export default RemoteControlForm;
