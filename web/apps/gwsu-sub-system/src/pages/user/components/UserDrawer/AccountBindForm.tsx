import React from 'react';
import { Form, Input, Button } from 'antd';
import type { SysAccountBindDTO } from '../../types';

interface AccountBindFormProps {
  identityType: string;
  onSubmit: (data: SysAccountBindDTO) => Promise<void>;
  onCancel: () => void;
}

const AccountBindForm: React.FC<AccountBindFormProps> = ({ identityType, onSubmit, onCancel }) => {
  const [form] = Form.useForm();
  const [loading, setLoading] = React.useState(false);

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      setLoading(true);
      await onSubmit({
        identityType,
        identifier: values.identifier,
        credential: values.credential,
      });
      onCancel();
    } catch {
      // 验证失败
    } finally {
      setLoading(false);
    }
  };

  return (
    <Form form={form} layout="vertical" size="small" style={{ marginTop: 8 }}>
      {identityType === 'password' && (
        <>
          <Form.Item label="用户名" name="identifier" rules={[{ required: true, message: '请输入用户名' }]}>
            <Input />
          </Form.Item>
          <Form.Item label="密码" name="credential" rules={[{ required: true, message: '请输入密码' }]}>
            <Input.Password />
          </Form.Item>
        </>
      )}
      {identityType === 'phone' && (
        <Form.Item label="手机号" name="identifier" rules={[{ required: true, message: '请输入手机号' }]}>
          <Input placeholder="请输入手机号" />
        </Form.Item>
      )}
      {identityType === 'wechat' && (
        <Form.Item label="微信OpenID" name="identifier" rules={[{ required: true, message: '请输入微信标识' }]}>
          <Input placeholder="请输入微信标识" />
        </Form.Item>
      )}
      <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 8 }}>
        <Button size="small" onClick={onCancel}>取消</Button>
        <Button size="small" type="primary" loading={loading} onClick={handleSubmit}>确认绑定</Button>
      </div>
    </Form>
  );
};

export default AccountBindForm;
