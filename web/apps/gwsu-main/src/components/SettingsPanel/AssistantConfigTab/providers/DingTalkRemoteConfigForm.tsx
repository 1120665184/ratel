import { Form, Input, Tooltip, Typography, message } from 'antd';
import { QuestionCircleOutlined, DownloadOutlined } from '@ant-design/icons';
import type { DingTalkRemoteConfig } from '../types';
import { AI_CARD_TEMPLATE_DOWNLOAD_URL } from '../../services/config';
import { downloadRequest } from '@gwsu/core';
import styles from './DingTalkRemoteConfigForm.module.less';

interface DingTalkRemoteConfigFormProps {
  value?: DingTalkRemoteConfig;
  onChange?: (value: DingTalkRemoteConfig) => void;
}

const CARD_PLATFORM_URL = 'https://open-dev.dingtalk.com/fe/card';

const DingTalkRemoteConfigForm: React.FC<DingTalkRemoteConfigFormProps> = ({ value, onChange }) => {
  const handleFieldChange = (field: keyof DingTalkRemoteConfig, fieldValue: string) => {
    onChange?.({ ...value!, [field]: fieldValue });
  };

  const handleDownloadTemplate = async () => {
    try {
      const response = await downloadRequest({ url: AI_CARD_TEMPLATE_DOWNLOAD_URL });
      const blob = await response.blob();
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = 'aiCard.json';
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      URL.revokeObjectURL(url);
      message.success('模板文件下载成功');
    } catch {
      message.error('模板文件下载失败');
    }
  };

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
      <Tooltip title="下载模板JSON">
        <DownloadOutlined
          className={styles.downloadIcon}
          onClick={handleDownloadTemplate}
          aria-label="下载模板JSON"
        />
      </Tooltip>
    </span>
  );

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
