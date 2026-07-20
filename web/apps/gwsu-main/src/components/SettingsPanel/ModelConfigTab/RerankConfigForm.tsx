import { Card, Form, Input, InputNumber, Select, Switch } from 'antd';
import type { ModelRerankConfig, RerankProvider } from './types';
import { RERANK_PROVIDER_LIST } from './types';
import styles from './index.module.less';

interface RerankConfigFormProps {
  value: ModelRerankConfig;
  onChange: (value: ModelRerankConfig) => void;
}

const RerankConfigForm: React.FC<RerankConfigFormProps> = ({ value, onChange }) => {
  const handleProviderChange = (provider: RerankProvider) => {
    onChange({ ...value, provider });
  };

  const handleDashscopeChange = (field: keyof ModelRerankConfig['dashscope'], fieldValue: unknown) => {
    onChange({ ...value, dashscope: { ...value.dashscope, [field]: fieldValue } });
  };

  return (
    <>
      <Card title="模型提供商" className={`${styles.sectionCard} ${styles.providerSection}`} size="small">
        <Form layout="vertical">
          <Form.Item label="启用">
            <Switch checked={value.enabled} onChange={(enabled) => onChange({ ...value, enabled })} />
          </Form.Item>
          <Form.Item label="提供商">
            <Select
              value={value.provider}
              onChange={handleProviderChange}
              options={RERANK_PROVIDER_LIST.map((item) => ({
                label: `${item.label} - ${item.description}`,
                value: item.key,
              }))}
              className={styles.fullWidthControl}
              aria-label="重排模型提供商"
            />
          </Form.Item>
        </Form>
      </Card>
      <Card title="连接配置" className={styles.sectionCard} size="small">
        <Form layout="vertical">
          <Form.Item label="API Key" required>
            <Input.Password
              value={value.dashscope.apiKey}
              onChange={(event) => handleDashscopeChange('apiKey', event.target.value)}
              placeholder="请输入 DashScope API Key"
            />
          </Form.Item>
          <Form.Item label="模型名称" required>
            <Input
              value={value.dashscope.modelName}
              onChange={(event) => handleDashscopeChange('modelName', event.target.value)}
              placeholder="例如 gte-rerank-v2"
            />
          </Form.Item>
          <Form.Item label="Base URL">
            <Input
              value={value.dashscope.baseUrl}
              onChange={(event) => handleDashscopeChange('baseUrl', event.target.value)}
              placeholder="可选，留空使用默认地址"
            />
          </Form.Item>
          <Form.Item label="返回数量 Top N">
            <InputNumber
              min={1}
              max={100}
              value={value.dashscope.topN}
              onChange={(topN) => handleDashscopeChange('topN', topN ?? undefined)}
              className={styles.fullWidthControl}
            />
          </Form.Item>
          <Form.Item label="返回文档内容">
            <Switch
              checked={value.dashscope.returnDocuments}
              onChange={(checked) => handleDashscopeChange('returnDocuments', checked)}
            />
          </Form.Item>
        </Form>
      </Card>
    </>
  );
};

export default RerankConfigForm;
