import { useEffect } from 'react';
import { Modal, Form, Input, message } from 'antd';
import type { DictInfo } from '../services/dict';
import { saveOrUpdateDict } from '../services/dict';

interface DictFormModalProps {
  visible: boolean;
  dict: DictInfo | null;
  onClose: () => void;
  onSuccess: () => void;
}

const DictFormModal: React.FC<DictFormModalProps> = ({ visible, dict, onClose, onSuccess }) => {
  const [form] = Form.useForm();
  const isEdit = !!dict?.id;

  useEffect(() => {
    if (visible) {
      if (dict) {
        form.setFieldsValue({
          dictKey: dict.dictKey,
          dictName: dict.dictName,
          description: dict.description,
        });
      } else {
        form.resetFields();
      }
    }
    if (!visible) {
      form.resetFields();
    }
  }, [visible, dict, form]);

  const handleSave = async () => {
    try {
      const values = await form.validateFields();
      const success = await saveOrUpdateDict({
        id: dict?.id,
        dictKey: values.dictKey,
        dictName: values.dictName,
        description: values.description,
      });

      if (success) {
        message.success(isEdit ? '更新成功' : '新增成功');
        onSuccess();
        onClose();
      }
    } catch {
      // validation failed
    }
  };

  return (
    <Modal
      title={isEdit ? '编辑字典' : '新增字典'}
      open={visible}
      onOk={handleSave}
      onCancel={onClose}
      destroyOnClose
      width={480}
    >
      <Form form={form} layout="vertical">
        <Form.Item name="dictKey" label="字典键" rules={[{ required: true, message: '请输入字典键' }]}>
          <Input placeholder="请输入字典键" disabled={isEdit && dict?.dictType === 1} />
        </Form.Item>
        <Form.Item name="dictName" label="字典名称" rules={[{ required: true, message: '请输入字典名称' }]}>
          <Input placeholder="请输入字典名称" />
        </Form.Item>
        <Form.Item name="description" label="描述">
          <Input.TextArea rows={3} placeholder="请输入描述" />
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default DictFormModal;
