import React, { useState, useEffect, useCallback } from 'react';
import { Drawer, Form, Input, Button, App, Segmented, Checkbox, Empty, Space } from 'antd';
import { getBusinessFunctionDetail, saveOrUpdateBusinessFunction } from '../../services/businessFunction';
import { getTableModelPage } from '../../services/tableModel';
import type { BusinessFunctionInfo, TableModelInfo, ModuleInfo } from '../../types';
import { getModuleList } from '../../services/tableModel';
import styles from './index.module.less';

interface BusinessFunctionDrawerProps {
  visible: boolean;
  editData: BusinessFunctionInfo | null;
  onClose: () => void;
  onSuccess: () => void;
}

const BusinessFunctionDrawer: React.FC<BusinessFunctionDrawerProps> = ({
  visible,
  editData,
  onClose,
  onSuccess,
}) => {
  const { message } = App.useApp();
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [mdMode, setMdMode] = useState<'edit' | 'preview' | 'split'>('edit');
  const [detail, setDetail] = useState('');

  const [modules, setModules] = useState<ModuleInfo[]>([]);
  const [activeModule, setActiveModule] = useState<string>('');
  const [allTables, setAllTables] = useState<TableModelInfo[]>([]);
  const [selectedTableIds, setSelectedTableIds] = useState<string[]>([]);

  const isEdit = !!editData?.id;

  useEffect(() => {
    if (visible) {
      if (editData) {
        form.setFieldsValue({
          name: editData.name,
          summary: editData.summary,
          detail: editData.detail,
          sortOrder: editData.sortOrder,
        });
        setDetail(editData.detail || '');
        if (editData.id) {
          getBusinessFunctionDetail(editData.id)
            .then((res) => {
              setSelectedTableIds(res.tables?.map((t) => t.id) || []);
            })
            .catch(() => {});
        }
      } else {
        form.resetFields();
        setDetail('');
        setSelectedTableIds([]);
      }
      getModuleList()
        .then((mods) => {
          setModules(mods);
          if (mods.length > 0) {
            setActiveModule(mods[0].prefix);
          }
        })
        .catch(() => {});
    }
  }, [visible, editData, form]);

  useEffect(() => {
    if (activeModule) {
      getTableModelPage({ modulePrefix: activeModule, pageNum: 1, pageSize: 500 })
        .then((res) => {
          setAllTables(res.records);
        })
        .catch(() => {
          setAllTables([]);
        });
    }
  }, [activeModule]);

  const handleDetailChange = useCallback(
    (e: React.ChangeEvent<HTMLTextAreaElement>) => {
      setDetail(e.target.value);
      form.setFieldValue('detail', e.target.value);
    },
    [form],
  );

  const handleTableCheck = useCallback(
    (tableId: string, checked: boolean) => {
      setSelectedTableIds((prev) =>
        checked ? [...prev, tableId] : prev.filter((id) => id !== tableId),
      );
    },
    [],
  );

  const handleOk = useCallback(async () => {
    try {
      const values = await form.validateFields();
      if (!detail.trim()) {
        message.warning('请输入详细介绍');
        return;
      }
      setLoading(true);
      const data: BusinessFunctionInfo = {
        ...editData,
        ...values,
        detail,
        tableIds: selectedTableIds,
      };
      await saveOrUpdateBusinessFunction(data);
      message.success(isEdit ? '编辑成功' : '新增成功');
      onSuccess();
    } catch {} finally {
      setLoading(false);
    }
  }, [form, editData, detail, selectedTableIds, isEdit, onSuccess, message]);

  const renderMarkdownPreview = useCallback(
    (content: string) => {
      const simpleHtml = content
        .replace(/^### (.+)$/gm, '<h3>$1</h3>')
        .replace(/^## (.+)$/gm, '<h2>$1</h2>')
        .replace(/^# (.+)$/gm, '<h1>$1</h1>')
        .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
        .replace(/\*(.+?)\*/g, '<em>$1</em>')
        .replace(/`(.+?)`/g, '<code>$1</code>')
        .replace(/^- (.+)$/gm, '<li>$1</li>')
        .replace(/^> (.+)$/gm, '<blockquote>$1</blockquote>')
        .replace(/\n/g, '<br/>');
      return simpleHtml;
    },
    [],
  );

  return (
    <Drawer
      title={isEdit ? '编辑业务功能' : '新增业务功能'}
      width={720}
      open={visible}
      onClose={onClose}
      extra={
        <Space>
          <Button onClick={onClose}>取消</Button>
          <Button type="primary" data-ai-approval loading={loading} onClick={handleOk}>
            保存
          </Button>
        </Space>
      }
    >
      <Form form={form} layout="vertical" className={styles.drawerBody}>
        <Form.Item
          name="name"
          label="业务名称"
          rules={[{ required: true, message: '请输入业务名称' }]}
        >
          <Input placeholder="请输入业务名称" maxLength={128} />
        </Form.Item>

        <Form.Item
          name="summary"
          label="业务简介"
          rules={[{ required: true, message: '请输入业务简介' }]}
        >
          <Input.TextArea
            placeholder="请输入业务简介"
            maxLength={512}
            rows={2}
            showCount
          />
        </Form.Item>

        <div className={styles.mdEditorLabel}><span className={styles.requiredMark}>*</span>详细介绍（Markdown）</div>
        <div className={styles.mdEditor}>
          <div className={styles.mdToolbar}>
            <Segmented
              size="small"
              options={[
                { label: '编辑', value: 'edit' },
                { label: '预览', value: 'preview' },
                { label: '分屏', value: 'split' },
              ]}
              value={mdMode}
              onChange={(val) => setMdMode(val as 'edit' | 'preview' | 'split')}
            />
          </div>
          <div className={styles.mdContent}>
            {(mdMode === 'edit' || mdMode === 'split') && (
              <textarea
                className={styles.mdEditArea}
                value={detail}
                onChange={handleDetailChange}
                placeholder="请输入Markdown格式的详细介绍，支持业务描述、规则说明、示例等内容"
                style={{ width: mdMode === 'split' ? '50%' : '100%' }}
              />
            )}
            {(mdMode === 'preview' || mdMode === 'split') && (
              <div
                className={styles.mdPreview}
                style={{ width: mdMode === 'split' ? '50%' : '100%' }}
                dangerouslySetInnerHTML={{ __html: renderMarkdownPreview(detail) }}
              />
            )}
          </div>
        </div>

        <Form.Item name="sortOrder" label="排序号">
          <Input type="number" placeholder="请输入排序号" />
        </Form.Item>

        <Form.Item label="关联表模型">
          <div className={styles.tableSelector}>
            <div style={{ display: 'flex', height: 320 }}>
              <div className={styles.moduleTree} style={{ width: 180 }}>
                {modules.map((mod) => (
                  <div
                    key={mod.prefix}
                    className={`${styles.moduleItem} ${
                      activeModule === mod.prefix ? styles.moduleItemActive : ''
                    }`}
                    onClick={() => setActiveModule(mod.prefix)}
                  >
                    {mod.note || mod.prefix}
                  </div>
                ))}
              </div>
              <div className={styles.tableCheckList} style={{ flex: 1 }}>
                {allTables.length === 0 ? (
                  <Empty description="暂无表模型" image={Empty.PRESENTED_IMAGE_SIMPLE} />
                ) : (
                  allTables.map((table) => (
                    <div key={table.id} className={styles.tableCheckItem}>
                      <Checkbox
                        checked={selectedTableIds.includes(table.id)}
                        onChange={(e) =>
                          handleTableCheck(table.id, e.target.checked)
                        }
                      />
                      <span className={styles.tableName}>{table.tableName}</span>
                      <span className={styles.tableComment}>
                        {table.tableComment}
                      </span>
                    </div>
                  ))
                )}
              </div>
            </div>
            <div className={styles.selectedInfo}>
              已选择 {selectedTableIds.length} 张表
            </div>
          </div>
        </Form.Item>
      </Form>
    </Drawer>
  );
};

export default BusinessFunctionDrawer;
