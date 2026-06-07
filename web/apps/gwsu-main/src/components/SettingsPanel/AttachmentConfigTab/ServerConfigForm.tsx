import React, { useCallback } from 'react';
import { Card, Radio, Input, Form } from 'antd';
import type { UploadServerConfig, FileServiceType } from './types';
import { FILE_SERVICE_TYPE_OPTIONS, createDefaultServerConfig } from './types';
import styles from './index.module.less';

interface ServerConfigFormProps {
  value: UploadServerConfig;
  onChange: (config: UploadServerConfig) => void;
}

const ServerConfigForm: React.FC<ServerConfigFormProps> = ({ value, onChange }) => {
  const handleTypeChange = useCallback(
    (type: FileServiceType) => {
      const defaults = createDefaultServerConfig();
      onChange({
        ...value,
        type,
        local: type === 'LOCAL' ? defaults.local : value.local,
        minio: type === 'MINIO' ? defaults.minio : value.minio,
        oss: type === 'OSS' ? defaults.oss : value.oss,
        cos: type === 'COS' ? defaults.cos : value.cos,
      });
    },
    [value, onChange],
  );

  const updateField = useCallback(
    (path: string, val: string) => {
      const keys = path.split('.');
      const updated = { ...value };
      let obj: Record<string, unknown> = updated as Record<string, unknown>;
      for (let i = 0; i < keys.length - 1; i++) {
        obj[keys[i] as string] = { ...(obj[keys[i] as string] as Record<string, unknown>) };
        obj = obj[keys[i] as string] as Record<string, unknown>;
      }
      obj[keys[keys.length - 1] as string] = val;
      onChange(updated);
    },
    [value, onChange],
  );

  return (
    <>
      <Card title="上传服务类型" className={styles.sectionCard} size="small">
        <Radio.Group
          value={value.type}
          onChange={(e) => handleTypeChange(e.target.value as FileServiceType)}
          optionType="button"
          buttonStyle="solid"
        >
          {FILE_SERVICE_TYPE_OPTIONS.map((opt) => (
            <Radio.Button key={opt.value} value={opt.value}>
              {opt.label}
            </Radio.Button>
          ))}
        </Radio.Group>
      </Card>

      <Card title="连接配置" className={styles.sectionCard} size="small">
        {value.type === 'LOCAL' && (
          <Form layout="vertical" size="small">
            <Form.Item label="上传路径">
              <Input
                value={value.local.path}
                onChange={(e) => updateField('local.path', e.target.value)}
                placeholder="/data/upload"
              />
            </Form.Item>
          </Form>
        )}

        {value.type === 'MINIO' && (
          <Form layout="vertical" size="small">
            <Form.Item label="服务地址">
              <Input
                value={value.minio.url}
                onChange={(e) => updateField('minio.url', e.target.value)}
                placeholder="http://127.0.0.1:9000"
              />
            </Form.Item>
            <Form.Item label="Access Key">
              <Input
                value={value.minio.accessKey}
                onChange={(e) => updateField('minio.accessKey', e.target.value)}
                placeholder="minioadmin"
              />
            </Form.Item>
            <Form.Item label="Secret Key">
              <Input.Password
                value={value.minio.secretKey}
                onChange={(e) => updateField('minio.secretKey', e.target.value)}
                placeholder="minioadmin"
              />
            </Form.Item>
          </Form>
        )}

        {value.type === 'OSS' && (
          <Form layout="vertical" size="small">
            <Form.Item label="Endpoint">
              <Input
                value={value.oss.endpoint}
                onChange={(e) => updateField('oss.endpoint', e.target.value)}
                placeholder="https://oss-cn-hangzhou.aliyuncs.com"
              />
            </Form.Item>
            <Form.Item label="Access Key ID">
              <Input
                value={value.oss.accessKey}
                onChange={(e) => updateField('oss.accessKey', e.target.value)}
                placeholder="LTAI5t..."
              />
            </Form.Item>
            <Form.Item label="Access Key Secret">
              <Input.Password
                value={value.oss.secretKey}
                onChange={(e) => updateField('oss.secretKey', e.target.value)}
                placeholder="请输入 Secret"
              />
            </Form.Item>
          </Form>
        )}

        {value.type === 'COS' && (
          <Form layout="vertical" size="small">
            <Form.Item label="Endpoint">
              <Input
                value={value.cos.endpoint}
                onChange={(e) => updateField('cos.endpoint', e.target.value)}
                placeholder="https://cos.ap-guangzhou.myqcloud.com"
              />
            </Form.Item>
            <Form.Item label="Region">
              <Input
                value={value.cos.region}
                onChange={(e) => updateField('cos.region', e.target.value)}
                placeholder="ap-guangzhou"
              />
            </Form.Item>
            <Form.Item label="Secret ID">
              <Input
                value={value.cos.accessKey}
                onChange={(e) => updateField('cos.accessKey', e.target.value)}
                placeholder="AKID..."
              />
            </Form.Item>
            <Form.Item label="Secret Key">
              <Input.Password
                value={value.cos.secretKey}
                onChange={(e) => updateField('cos.secretKey', e.target.value)}
                placeholder="请输入 Secret Key"
              />
            </Form.Item>
          </Form>
        )}
      </Card>

      <Card title="文件组" className={styles.sectionCard} size="small">
        <Form layout="vertical" size="small">
          <Form.Item label="文件组名称">
            <Input
              value={value.group}
              onChange={(e) => updateField('group', e.target.value)}
              placeholder="common"
            />
          </Form.Item>
        </Form>
      </Card>
    </>
  );
};

export default ServerConfigForm;
