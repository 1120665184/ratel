import React, { useState, useEffect, useCallback } from 'react';
import { Transfer, Button, Spin, message } from 'antd';
import type { TransferProps } from 'antd';
import { getComponentList, getBoundComponentIds, bindComponents } from '../../services/catalog';
import type { CatalogComponentInfo } from '../../types';
import styles from './index.module.less';

interface CatalogComponentBindProps {
  catalogId: string;
  catalogName: string;
  onClose: () => void;
}

interface TransferItem {
  key: string;
  title: string;
  description: string;
  category: string;
}

const CatalogComponentBind: React.FC<CatalogComponentBindProps> = ({
  catalogId,
  catalogName,
  onClose,
}) => {
  const [allComponents, setAllComponents] = useState<CatalogComponentInfo[]>([]);
  const [targetKeys, setTargetKeys] = useState<string[]>([]);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);

  /** 加载数据 */
  const fetchData = useCallback(async () => {
    try {
      setLoading(true);
      const [components, boundIds] = await Promise.all([
        getComponentList(),
        getBoundComponentIds(catalogId),
      ]);
      setAllComponents(components);
      setTargetKeys(boundIds);
    } catch {
      // 请求层自动提示
    } finally {
      setLoading(false);
    }
  }, [catalogId]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  /** Transfer 数据源 */
  const dataSource: TransferItem[] = allComponents.map((comp) => ({
    key: comp.id!,
    title: comp.componentName,
    description: comp.description,
    category: comp.category ?? '',
  }));

  /** 自定义渲染 */
  const renderItem: TransferProps['render'] = (item) => {
    const transferItem = item as TransferItem;
    return (
      <span>
        {transferItem.title}
        {transferItem.category && (
          <span style={{ color: 'var(--text-secondary-color)', marginLeft: 4 }}>
            [{transferItem.category}]
          </span>
        )}
      </span>
    );
  };

  /** Transfer 切换 */
  const handleChange: TransferProps['onChange'] = (newTargetKeys) => {
    setTargetKeys(newTargetKeys as string[]);
  };

  /** 保存绑定 */
  const handleSave = async () => {
    try {
      setSaving(true);
      await bindComponents(catalogId, targetKeys);
      message.success('组件绑定保存成功');
      onClose();
    } catch {
      // 请求层自动提示
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <div className={styles.bindWrapper}>
        <div className={styles.bindContent}>
          <Spin />
        </div>
      </div>
    );
  }

  return (
    <div className={styles.bindWrapper}>
      <div className={styles.bindHeader}>
        <span className={styles.bindTitle}>
          关联组件 - {catalogName}
        </span>
      </div>
      <div className={styles.bindContent}>
        <Transfer
          dataSource={dataSource}
          targetKeys={targetKeys}
          onChange={handleChange}
          render={renderItem}
          showSearch
          listStyle={{ width: 280, height: 400 }}
          titles={['可选组件', '已选组件']}
          oneWay={false}
        />
      </div>
      <div className={styles.bindFooter}>
        <Button onClick={onClose}>取消</Button>
        <Button type="primary" onClick={handleSave} loading={saving} data-ai-approval>
          保存
        </Button>
      </div>
    </div>
  );
};

export default CatalogComponentBind;
