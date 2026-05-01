import React from 'react';
import { Drawer, Descriptions, Tag } from 'antd';
import styles from './index.module.less';
import type { RoleInfo } from '../../types';
import { ROLE_TYPE_OPTIONS, DATA_SCOPE_OPTIONS } from '../../types';

interface RoleDetailProps {
  visible: boolean;
  role: RoleInfo | null;
  onClose: () => void;
}

/** 根据角色类型值获取标签 */
const getRoleTypeLabel = (value: number): string => {
  return ROLE_TYPE_OPTIONS.find((o) => o.value === value)?.label ?? '未知';
};

/** 根据数据范围值获取标签 */
const getDataScopeLabel = (value: number): string => {
  return DATA_SCOPE_OPTIONS.find((o) => o.value === value)?.label ?? '未知';
};

const RoleDetail: React.FC<RoleDetailProps> = ({ visible, role, onClose }) => {
  return (
    <Drawer
      title="角色详情"
      placement="right"
      width={480}
      open={visible}
      onClose={onClose}
      className={styles.drawerBody}
      destroyOnHidden
    >
      {role && (
        <Descriptions column={1} bordered size="small">
          <Descriptions.Item label="角色编码">
            <code>{role.roleCode}</code>
          </Descriptions.Item>
          <Descriptions.Item label="角色名称">
            {role.roleName}
          </Descriptions.Item>
          <Descriptions.Item label="描述">
            {role.description || '-'}
          </Descriptions.Item>
          <Descriptions.Item label="角色类型">
            <Tag color={role.roleType === 1 ? 'blue' : 'orange'}>
              {getRoleTypeLabel(role.roleType)}
            </Tag>
          </Descriptions.Item>
          <Descriptions.Item label="数据范围">
            {getDataScopeLabel(role.dataScope)}
          </Descriptions.Item>
          <Descriptions.Item label="状态">
            <Tag color={role.status ? 'green' : 'red'}>
              {role.status ? '启用' : '禁用'}
            </Tag>
          </Descriptions.Item>
          <Descriptions.Item label="排序号">
            {role.sort ?? 0}
          </Descriptions.Item>
          <Descriptions.Item label="创建时间">
            {role.createTime || '-'}
          </Descriptions.Item>
        </Descriptions>
      )}
    </Drawer>
  );
};

export default RoleDetail;
