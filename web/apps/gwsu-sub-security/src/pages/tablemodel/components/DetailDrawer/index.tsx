import React, { useState, useEffect, useCallback } from 'react';
import { Drawer, Table, Tag, Input, Button, Space, message, Popconfirm, Form } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons';
import type { TableModelInfo, TableModelDetail, TableModelColumnInfo, TableModelForeignKeyInfo } from '../../types';
import { getTableModelDetail, updateColumnComment, updateForeignKeyRemark, saveForeignKey } from '../../services/tableModel';
import { SOURCE_TYPE_MAP } from '../../types';
import styles from './index.module.less';

interface DetailDrawerProps {
  visible: boolean;
  record: TableModelInfo | null;
  onClose: () => void;
}

const DetailDrawer: React.FC<DetailDrawerProps> = ({ visible, record, onClose }) => {
  const [detail, setDetail] = useState<TableModelDetail | null>(null);
  const [loading, setLoading] = useState(false);
  const [editingColumnId, setEditingColumnId] = useState<string | null>(null);
  const [editingFkId, setEditingFkId] = useState<string | null>(null);
  const [fkFormVisible, setFkFormVisible] = useState(false);
  const [fkForm] = Form.useForm();
  const [editingFkData, setEditingFkData] = useState<TableModelForeignKeyInfo | null>(null);

  /** 加载详情 */
  const loadDetail = useCallback(async () => {
    if (!record) return;
    setLoading(true);
    try {
      const data = await getTableModelDetail(
        record.modulePrefix,
        record.dataSource,
        record.tableName,
      );
      setDetail(data);
    } catch {
      // request 层已自动提示
    } finally {
      setLoading(false);
    }
  }, [record]);

  useEffect(() => {
    if (visible && record) {
      loadDetail();
    }
  }, [visible, record, loadDetail]);

  /** 修改字段注释 */
  const handleColumnCommentSave = useCallback(async (columnId: string, comment: string) => {
    try {
      await updateColumnComment(columnId, comment);
      message.success('注释修改成功');
      setEditingColumnId(null);
      loadDetail();
    } catch {
      // request 层已自动提示
    }
  }, [loadDetail]);

  /** 修改外键备注 */
  const handleFkRemarkSave = useCallback(async (fkId: string, remark: string) => {
    try {
      await updateForeignKeyRemark(fkId, remark);
      message.success('备注修改成功');
      setEditingFkId(null);
      loadDetail();
    } catch {
      // request 层已自动提示
    }
  }, [loadDetail]);

  /** 新增/编辑外键 */
  const handleFkSave = useCallback(async () => {
    try {
      const values = await fkForm.validateFields();
      const data = editingFkData
        ? { ...values, id: editingFkData.id, tableId: record?.id }
        : { ...values, tableId: record?.id, dataType: 1 };
      await saveForeignKey(data);
      message.success(editingFkData ? '修改成功' : '添加成功');
      setFkFormVisible(false);
      setEditingFkData(null);
      fkForm.resetFields();
      loadDetail();
    } catch {
      // request 层已自动提示
    }
  }, [fkForm, editingFkData, record, loadDetail]);

  /** 字段表格列定义 */
  const columnDefs = [
    { title: '序号', dataIndex: 'ordinalPosition', key: 'ordinalPosition', width: 60 },
    { title: '字段名', dataIndex: 'columnName', key: 'columnName', width: 150 },
    { title: '类型', dataIndex: 'columnType', key: 'columnType', width: 120 },
    {
      title: '长度',
      key: 'length',
      width: 80,
      render: (_: unknown, r: TableModelColumnInfo) =>
        r.columnLength != null ? `${r.columnLength}${r.columnScale != null ? `,${r.columnScale}` : ''}` : '-',
    },
    {
      title: '可空',
      dataIndex: 'isNullable',
      key: 'isNullable',
      width: 60,
      render: (v: boolean) => v ? <Tag color="default">YES</Tag> : <Tag color="red">NO</Tag>,
    },
    {
      title: '主键',
      dataIndex: 'isPrimaryKey',
      key: 'isPrimaryKey',
      width: 60,
      render: (v: boolean) => v ? <Tag color="blue">PK</Tag> : '-',
    },
    { title: '默认值', dataIndex: 'defaultValue', key: 'defaultValue', width: 100, ellipsis: true },
    {
      title: '注释',
      dataIndex: 'columnComment',
      key: 'columnComment',
      width: 180,
      render: (text: string | null, r: TableModelColumnInfo) => {
        if (editingColumnId === r.id) {
          return (
            <Input
              size="small"
              defaultValue={text || ''}
              autoFocus
              onPressEnter={(e) => handleColumnCommentSave(r.id, (e.target as HTMLInputElement).value)}
              onBlur={(e) => handleColumnCommentSave(r.id, e.target.value)}
            />
          );
        }
        return (
          <span
            className={styles.commentEditable}
            onClick={() => setEditingColumnId(r.id)}
          >
            {text || '-'}
            <EditOutlined style={{ marginLeft: 4, fontSize: 12, color: 'var(--text-color-secondary)' }} />
          </span>
        );
      },
    },
  ];

  /** 外键表格列定义 */
  const fkColumnDefs = [
    { title: '约束名', dataIndex: 'constraintName', key: 'constraintName', width: 160 },
    { title: '字段名', dataIndex: 'columnName', key: 'columnName', width: 120 },
    { title: '引用表', dataIndex: 'referencedTableName', key: 'referencedTableName', width: 150 },
    { title: '引用字段', dataIndex: 'referencedColumnName', key: 'referencedColumnName', width: 120 },
    {
      title: '类型',
      dataIndex: 'dataType',
      key: 'dataType',
      width: 80,
      render: (v: number) =>
        v === 0 ? <Tag color="blue">采集</Tag> : <Tag color="green">自定义</Tag>,
    },
    {
      title: '备注',
      dataIndex: 'remark',
      key: 'remark',
      width: 180,
      render: (text: string | null, r: TableModelForeignKeyInfo) => {
        if (editingFkId === r.id) {
          return (
            <Input
              size="small"
              defaultValue={text || ''}
              autoFocus
              onPressEnter={(e) => handleFkRemarkSave(r.id, (e.target as HTMLInputElement).value)}
              onBlur={(e) => handleFkRemarkSave(r.id, e.target.value)}
            />
          );
        }
        return (
          <span
            className={styles.fkRemarkEditable}
            onClick={() => setEditingFkId(r.id)}
          >
            {text || '-'}
            <EditOutlined style={{ marginLeft: 4, fontSize: 12, color: 'var(--text-color-secondary)' }} />
          </span>
        );
      },
    },
    {
      title: '操作',
      key: 'action',
      width: 80,
      render: (_: unknown, r: TableModelForeignKeyInfo) => {
        // 采集的外键禁止修改关键内容，只能修改备注；自定义的外键可以任意修改
        if (r.dataType === 1) {
          return (
            <Space size="small">
              <Button
                type="link"
                size="small"
                onClick={() => {
                  setEditingFkData(r);
                  fkForm.setFieldsValue(r);
                  setFkFormVisible(true);
                }}
              >
                编辑
              </Button>
            </Space>
          );
        }
        return null;
      },
    },
  ];

  return (
    <Drawer
      title="表模型详情"
      open={visible}
      width={900}
      onClose={onClose}
      loading={loading}
      destroyOnClose
    >
      <div className={styles.drawerContent}>
        {/* 基本信息 */}
        {record && (
          <div className={styles.basicInfo}>
            <div className={styles.infoRow}>
              <div className={styles.infoItem}>
                <span className={styles.infoLabel}>表名：</span>
                <span className={styles.infoValue}>{record.tableName}</span>
              </div>
              <div className={styles.infoItem}>
                <span className={styles.infoLabel}>所属模块：</span>
                <span className={styles.infoValue}>{record.modulePrefix}</span>
              </div>
            </div>
            <div className={styles.infoRow}>
              <div className={styles.infoItem}>
                <span className={styles.infoLabel}>数据源：</span>
                <span className={styles.infoValue}>{record.dataSource}</span>
              </div>
              <div className={styles.infoItem}>
                <span className={styles.infoLabel}>来源类型：</span>
                <Tag color={SOURCE_TYPE_MAP[record.sourceType]?.color}>
                  {SOURCE_TYPE_MAP[record.sourceType]?.text}
                </Tag>
              </div>
            </div>
            {record.tableComment && (
              <div className={styles.infoRow}>
                <div className={styles.infoItem}>
                  <span className={styles.infoLabel}>表注释：</span>
                  <span className={styles.infoValue}>{record.tableComment}</span>
                </div>
              </div>
            )}
          </div>
        )}

        {/* 字段信息 */}
        <div className={styles.columnTable}>
          <div className={styles.sectionTitle}>
            <span>字段信息（{detail?.columns.length || 0}）</span>
          </div>
          <Table
            rowKey="id"
            size="small"
            dataSource={detail?.columns || []}
            columns={columnDefs}
            pagination={false}
            scroll={{ y: 300 }}
          />
        </div>

        {/* 外键信息 */}
        <div>
          <div className={styles.sectionTitle}>
            <span>外键信息（{detail?.foreignKeys.length || 0}）</span>
            <Button
              type="link"
              size="small"
              icon={<PlusOutlined />}
              onClick={() => {
                setEditingFkData(null);
                fkForm.resetFields();
                setFkFormVisible(true);
              }}
            >
              添加外键
            </Button>
          </div>
          <Table
            rowKey="id"
            size="small"
            dataSource={detail?.foreignKeys || []}
            columns={fkColumnDefs}
            pagination={false}
            scroll={{ y: 200 }}
          />
        </div>

        {/* 外键编辑弹窗 */}
        <Drawer
          title={editingFkData ? '编辑外键' : '添加外键'}
          open={fkFormVisible}
          width={420}
          onClose={() => {
            setFkFormVisible(false);
            setEditingFkData(null);
            fkForm.resetFields();
          }}
          extra={
            <Space>
              <Button onClick={() => setFkFormVisible(false)}>取消</Button>
              <Button type="primary" onClick={handleFkSave}>保存</Button>
            </Space>
          }
        >
          <Form form={fkForm} layout="vertical">
            <Form.Item name="constraintName" label="约束名" rules={[{ required: true, message: '请输入约束名' }]}>
              <Input placeholder="请输入约束名" disabled={editingFkData?.dataType === 0} />
            </Form.Item>
            <Form.Item name="columnName" label="字段名" rules={[{ required: true, message: '请输入字段名' }]}>
              <Input placeholder="请输入字段名" disabled={editingFkData?.dataType === 0} />
            </Form.Item>
            <Form.Item name="referencedTableName" label="引用表" rules={[{ required: true, message: '请输入引用表' }]}>
              <Input placeholder="请输入引用表" disabled={editingFkData?.dataType === 0} />
            </Form.Item>
            <Form.Item name="referencedColumnName" label="引用字段" rules={[{ required: true, message: '请输入引用字段' }]}>
              <Input placeholder="请输入引用字段" disabled={editingFkData?.dataType === 0} />
            </Form.Item>
            <Form.Item name="remark" label="备注">
              <Input.TextArea placeholder="请输入备注" rows={3} />
            </Form.Item>
          </Form>
        </Drawer>
      </div>
    </Drawer>
  );
};

export default DetailDrawer;
