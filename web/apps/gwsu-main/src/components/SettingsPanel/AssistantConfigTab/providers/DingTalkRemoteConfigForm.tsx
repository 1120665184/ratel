import { Form, Input, Tooltip, Typography } from 'antd';
import { QuestionCircleOutlined } from '@ant-design/icons';
import type { DingTalkRemoteConfig } from '../types';

interface DingTalkRemoteConfigFormProps {
  value?: DingTalkRemoteConfig;
  onChange?: (value: DingTalkRemoteConfig) => void;
}

const CARD_PLATFORM_URL = 'https://open-dev.dingtalk.com/fe/card';

const aiCardLabel = (
  <span>
    AI 输出卡片模板 ID{' '}
    <Tooltip
      title={
        <>
          用于在钉钉展示智能体输出内容，需在&quot;钉钉开发者平台 → 卡片平台&quot;中配置。
          <br />
          地址：
          <Typography.Link href={CARD_PLATFORM_URL} target="_blank">
            {CARD_PLATFORM_URL}
          </Typography.Link>
        </>
      }
    >
      <QuestionCircleOutlined style={{ color: 'rgba(0, 0, 0, 0.45)', cursor: 'pointer' }} />
    </Tooltip>
  </span>
);

const DingTalkRemoteConfigForm: React.FC<DingTalkRemoteConfigFormProps> = ({ value, onChange }) => {
  const handleFieldChange = (field: keyof DingTalkRemoteConfig, fieldValue: string) => {
    onChange?.({ ...value!, [field]: fieldValue });
  };

  return (
    <>
      <Form.Item label="协议" required>
        <Input
          value={value?.protocol}
          onChange={(e) => handleFieldChange('protocol', e.target.value)}
          placeholder="https"
        />
      </Form.Item>
      <Form.Item label="区域" required>
        <Input
          value={value?.regionId}
          onChange={(e) => handleFieldChange('regionId', e.target.value)}
          placeholder="central"
        />
      </Form.Item>
      <Form.Item label="端点" required>
        <Input
          value={value?.endpoint}
          onChange={(e) => handleFieldChange('endpoint', e.target.value)}
          placeholder="api.dingtalk.com"
        />
      </Form.Item>
      <Form.Item label="Client ID" required>
        <Input
          value={value?.clientId}
          onChange={(e) => handleFieldChange('clientId', e.target.value)}
          placeholder="请输入钉钉 Client ID"
        />
      </Form.Item>
      <Form.Item label="Client Secret" required>
        <Input.Password
          value={value?.clientSecret}
          onChange={(e) => handleFieldChange('clientSecret', e.target.value)}
          placeholder="请输入钉钉 Client Secret"
        />
      </Form.Item>
      <Form.Item label={aiCardLabel} required>
        <Input
          value={value?.aiCardTemplateId}
          onChange={(e) => handleFieldChange('aiCardTemplateId', e.target.value)}
          placeholder="请输入 AI 输出卡片模板 ID"
        />
      </Form.Item>
    </>
  );
};

export default DingTalkRemoteConfigForm;
