import React, { useState, useEffect, useCallback } from 'react';
import { Modal, Input, Table, App } from 'antd';
import type { TableProps } from 'antd';
import {
  getTableModelDetail,
  getTableModelPage,
} from '../../../tablemodel/services/tableModel';
import type { TableModelInfo } from '../../../tablemodel/types';
import type { RolePermissionTableModelVO } from '../../types';
import { saveOrUpdateRoleTableModel } from '../../services/role';

interface AddTableModelModalProps {
  visible: boolean;
  roleId: string | null;
  existingTableIds: Set<string>;
  onClose: () => void;
  onSuccess: (newTables: RolePermissionTableModelVO[]) => void;
}

const AddTableModelModal: React.FC<AddTableModelModalProps> = ({
  visible,
  roleId,
  existingTableIds,
  onClose,
  onSuccess,
}) => {
  const { message } = App.useApp();
  const [searchText, setSearchText] = useState('');
  const [loading, setLoading] = useState(false);
  const [dataSource, setDataSource] = useState<TableModelInfo[]>([]);
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([]);
  const [confirmLoading, setConfirmLoading] = useState(false);

  const doSearch = useCallback(async (keyword?: string) => {
    setLoading(true);
    try {
      const result = await getTableModelPage({
        tableName: keyword ?? (searchText || undefined),
        pageNum: 1,
        pageSize: 100,
      });
      setDataSource(
        (result?.records ?? []).filter((t) => !existingTableIds.has(t.id)),
      );
    } catch {
      // request 层已自动提示
    } finally {
      setLoading(false);
    }
  }, [searchText, existingTableIds]);

  /** 弹窗打开时加载初始数据（不依赖 doSearch，避免搜索时触发重置） */
  useEffect(() => {
    if (visible) {
      setSearchText('');
      setSelectedRowKeys([]);
      // 直接调用接口加载初始数据，不走 doSearch
      (async () => {
        setLoading(true);
        try {
          const result = await getTableModelPage({ pageNum: 1, pageSize: 100 });
          setDataSource(
            (result?.records ?? []).filter((t) => !existingTableIds.has(t.id)),
          );
        } catch {
          // request 层已自动提示
        } finally {
          setLoading(false);
        }
      })();
    }
  }, [visible, existingTableIds]);

  const handleConfirm = useCallback(async () => {
    if (!roleId || selectedRowKeys.length === 0) return;

    setConfirmLoading(true);
    try {
      const newTables: RolePermissionTableModelVO[] = [];
      for (const key of selectedRowKeys) {
        const record = dataSource.find((d) => d.id === key);
        if (!record) continue;

        await saveOrUpdateRoleTableModel({
          roleId,
          modulePrefix: record.modulePrefix,
          tableName: record.tableName,
          datasource: record.dataSource,
          fields: [],
        });

        const detail = await getTableModelDetail(
          record.modulePrefix,
          record.dataSource,
          record.tableName,
        );

        newTables.push({
          type: 1,
          tableModelId: record.id,
          modulePrefix: record.modulePrefix,
          datasource: record.dataSource,
          tableName: record.tableName,
          tableComment: record.tableComment,
          columns: detail.columns.map((column) => ({
            columnName: column.columnName,
            columnComment: column.columnComment ?? '',
          })),
        });
      }
      message.success(`成功添加 ${newTables.length} 个表模型权限`);
      onSuccess(newTables);
      onClose();
    } catch {
      // request 层已自动提示
    } finally {
      setConfirmLoading(false);
    }
  }, [roleId, selectedRowKeys, dataSource, onSuccess, onClose, message]);

  const columns: TableProps<TableModelInfo>['columns'] = [
    { title: '表名', dataIndex: 'tableName', width: 160 },
    { title: '表注释', dataIndex: 'tableComment', ellipsis: true },
    { title: '模块', dataIndex: 'modulePrefix', width: 100 },
    { title: '数据源', dataIndex: 'dataSource', width: 100 },
  ];

  return (
    <Modal
      title="新增模型权限"
      open={visible}
      onCancel={onClose}
      onOk={handleConfirm}
      okText="确认添加"
      cancelText="取消"
      okButtonProps={{ disabled: selectedRowKeys.length === 0, 'data-ai-approval': 'true' }}
      confirmLoading={confirmLoading}
      width={640}
      destroyOnHidden
    >
      <Input.Search
        placeholder="输入表名搜索"
        allowClear
        value={searchText}
        onChange={(e) => setSearchText(e.target.value)}
        onSearch={doSearch}
        style={{ marginBottom: 12 }}
      />
      <Table<TableModelInfo>
        rowKey="id"
        columns={columns}
        dataSource={dataSource}
        loading={loading}
        size="small"
        scroll={{ y: 360 }}
        rowSelection={{ selectedRowKeys, onChange: setSelectedRowKeys }}
        pagination={false}
      />
    </Modal>
  );
};

export default AddTableModelModal;
