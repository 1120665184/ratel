import React, { useMemo, useCallback } from 'react';
import {
  Tabs,
  Tree,
  Tag,
  Button,
  Spin,
  Form,
  Popconfirm,
} from 'antd';
import type { TreeProps } from 'antd';
import {
  MenuOutlined,
  EditOutlined,
  SaveOutlined,
  CloseOutlined,
  DeleteOutlined,
  FolderOutlined,
  FileOutlined,
} from '@ant-design/icons';
import styles from './index.module.less';
import type { MenuTreeNode, ValidGroup } from '../../types';
import { getValidGroupLabel } from './ValidConfigForm';
import ValidConfigForm from './ValidConfigForm';

/** MenuOwner 选项 */
const MENU_OWNER_OPTIONS = [
  { key: '1', label: '后端管理' },
  { key: '2', label: '移动端APP' },
];

/** MenuPosition 映射 */
const POSITION_TAG_MAP: Record<number, { label: string; color: string }> = {
  1: { label: '侧边栏', color: 'blue' },
  2: { label: '顶部栏', color: 'orange' },
};

interface MenuTreePanelProps {
  /** 当前选中的时效组 */
  selectedGroup: ValidGroup | null;
  /** 菜单树数据（按 owner 分类） */
  menuTreeData: MenuTreeNode[];
  /** 当前 Tab (MenuOwner) */
  currentOwner: number;
  /** 切换 Tab */
  onOwnerChange: (owner: number) => void;
  /** 已勾选的菜单 ID 列表（当前编辑中） */
  checkedMenuIds: string[];
  /** 勾选变更 */
  onCheckedChange: (checkedIds: string[]) => void;
  /** 是否编辑模式 */
  editing: boolean;
  /** 进入编辑模式 */
  onEdit: () => void;
  /** 保存编辑 */
  onSave: () => void;
  /** 取消编辑 */
  onCancelEdit: () => void;
  /** 删除当前时效组 */
  onDeleteGroup: () => void;
  /** 加载中 */
  loading: boolean;
  /** 时效配置表单实例 */
  validForm: ReturnType<typeof Form.useForm>[0];
}

/** 右侧 - 菜单树面板组件 */
const MenuTreePanel: React.FC<MenuTreePanelProps> = ({
  selectedGroup,
  menuTreeData,
  currentOwner,
  onOwnerChange,
  checkedMenuIds,
  onCheckedChange,
  editing,
  onEdit,
  onSave,
  onCancelEdit,
  onDeleteGroup,
  loading,
  validForm,
}) => {
  /** 按当前 owner 过滤菜单树 */
  const filteredTreeData = useMemo(() => {
    const filterByOwner = (nodes: MenuTreeNode[]): MenuTreeNode[] => {
      return nodes
        .filter((node) => node.owner === currentOwner || node.owner === undefined)
        .map((node) => ({
          ...node,
          children: node.children ? filterByOwner(node.children) : undefined,
        }))
        .filter((node) => {
          // 如果节点没有子节点，保留它自身；如果有子节点，保留有内容的
          if (node.children && node.children.length === 0) {
            return node.owner === currentOwner || node.owner === undefined;
          }
          return true;
        });
    };
    return filterByOwner(menuTreeData);
  }, [menuTreeData, currentOwner]);

  /** 收集所有被其他时效组占用的菜单 ID */
  const boundMenuIds = useMemo(() => {
    if (!selectedGroup) return new Set<string>();
    const ids = new Set<string>();
    const collect = (nodes: MenuTreeNode[]) => {
      for (const node of nodes) {
        if (
          node.disabled &&
          node.boundRoleMenuId &&
          node.boundRoleMenuId !== selectedGroup.roleMenuId
        ) {
          ids.add(node.id);
        }
        if (node.children) {
          collect(node.children);
        }
      }
    };
    collect(menuTreeData);
    return ids;
  }, [menuTreeData, selectedGroup]);

  /** 将 MenuTreeNode 转换为 Ant Design Tree 所需的 treeData 格式 */
  const treeData = useMemo<TreeProps['treeData']>(() => {
    const convert = (nodes: MenuTreeNode[]): TreeProps['treeData'] => {
      return nodes.map((node) => {
        const isBound = boundMenuIds.has(node.id);
        const positionTag = POSITION_TAG_MAP[node.position ?? 0];

        return {
          key: node.id,
          title: (
            <div className={styles.treeNode}>
              <span>
                {node.menuType === 1 ? <FolderOutlined /> : <FileOutlined />}
              </span>
              <span
                className={`${styles.nodeName} ${isBound ? styles.nodeNameDisabled : ''}`}
              >
                {node.menuName}
              </span>
              {positionTag && (
                <Tag color={positionTag.color} className={styles.positionTag}>
                  {positionTag.label}
                </Tag>
              )}
              {isBound && (
                <span className={styles.boundTag}>已配置</span>
              )}
            </div>
          ),
          disabled: isBound,
          children: node.children ? convert(node.children) : undefined,
        };
      });
    };
    return convert(filteredTreeData);
  }, [filteredTreeData, boundMenuIds, checkedMenuIds]);

  /** 处理勾选变更 */
  const handleCheck: TreeProps['onCheck'] = useCallback(
    (checkedKeys: React.Key[] | { checked: React.Key[]; halfChecked: React.Key[] }) => {
      const keys = Array.isArray(checkedKeys) ? checkedKeys : checkedKeys.checked;
      onCheckedChange(keys as string[]);
    },
    [onCheckedChange],
  );

  /** 没有选中时效组时的空状态 */
  if (!selectedGroup) {
    return (
      <div className={styles.rightPanel}>
        <div className={styles.rightEmpty}>
          <MenuOutlined className={styles.rightEmptyIcon} />
          <span className={styles.rightEmptyText}>请选择左侧时效分组查看菜单权限</span>
        </div>
      </div>
    );
  }

  return (
    <div className={styles.rightPanel}>
      {/* 顶部 Tab + 操作按钮 */}
      <div className={styles.rightHeader}>
        <Tabs
          className={styles.ownerTabs}
          activeKey={String(currentOwner)}
          onChange={(key) => onOwnerChange(Number(key))}
          items={MENU_OWNER_OPTIONS.map((opt) => ({
            key: opt.key,
            label: opt.label,
          }))}
        />
        <div className={styles.headerActions}>
          {editing ? (
            <>
              <Button size="small" icon={<SaveOutlined />} type="primary" onClick={onSave}>
                保存
              </Button>
              <Button size="small" icon={<CloseOutlined />} onClick={onCancelEdit}>
                取消
              </Button>
            </>
          ) : (
            <>
              <Button size="small" icon={<EditOutlined />} onClick={onEdit}>
                编辑时效
              </Button>
              <Popconfirm
                title="确定删除此时效组？"
                description="删除后关联的菜单将自动释放"
                onConfirm={onDeleteGroup}
                okText="确定"
                cancelText="取消"
              >
                <Button size="small" danger icon={<DeleteOutlined />}>
                  删除此组
                </Button>
              </Popconfirm>
            </>
          )}
        </div>
      </div>

      {/* 时效信息区 */}
      <div className={styles.validInfoSection}>
        <div className={styles.validInfoHeader}>
          <span className={styles.validInfoTitle}>时效配置</span>
        </div>
        {editing ? (
          <ValidConfigForm form={validForm} initialData={selectedGroup} />
        ) : (
          <div className={styles.validInfoContent}>
            <Tag className={styles.validInfoTag} color="blue">
              {getValidGroupLabel(selectedGroup)}
            </Tag>
            <span className={styles.validInfoText}>
              关联 {selectedGroup.menuCount} 个菜单/按钮
            </span>
          </div>
        )}
      </div>

      {/* 菜单树 */}
      <Spin spinning={loading}>
        <div className={styles.treeSection}>
          <Tree
            checkable={editing}
            checkStrictly
            checkedKeys={checkedMenuIds}
            onCheck={handleCheck}
            treeData={treeData}
            defaultExpandAll
            selectable={false}
            blockNode
          />
        </div>
      </Spin>
    </div>
  );
};

export default MenuTreePanel;
