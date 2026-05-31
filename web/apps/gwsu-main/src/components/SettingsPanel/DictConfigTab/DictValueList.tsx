import { useState, useEffect, useCallback } from 'react';
import { Button, Input, Space, Popconfirm, App, Empty } from 'antd';
import { PlusOutlined, DeleteOutlined, ArrowUpOutlined, ArrowDownOutlined } from '@ant-design/icons';
import { getDictValues, saveOrUpdateDictValue, deleteDictValues, updateDictValueSort } from '../services/dict';
import type { DictInfo, DictValueInfo } from '../services/dict';
import styles from './index.module.less';

interface DictValueListProps {
  dict: DictInfo | null;
}

const DictValueList: React.FC<DictValueListProps> = ({ dict }) => {
  const { message } = App.useApp();
  const [loading, setLoading] = useState(false);
  const [values, setValues] = useState<DictValueInfo[]>([]);
  const [newValue, setNewValue] = useState('');
  const [newLabel, setNewLabel] = useState('');

  const fetchValues = useCallback(async () => {
    if (!dict?.dictKey) {
      setValues([]);
      return;
    }
    setLoading(true);
    try {
      const result = await getDictValues(dict.dictKey);
      setValues(result);
    } catch {
      // error handled by request util
    } finally {
      setLoading(false);
    }
  }, [dict?.dictKey]);

  useEffect(() => {
    fetchValues();
    setNewValue('');
    setNewLabel('');
  }, [dict?.dictKey]);

  const handleAddValue = async () => {
    if (!dict?.dictKey || !newValue.trim()) return;
    const success = await saveOrUpdateDictValue({
      dictKey: dict.dictKey,
      dictValue: newValue.trim(),
      dictLabel: newLabel.trim() || newValue.trim(),
    });
    if (success) {
      message.success('添加成功');
      setNewValue('');
      setNewLabel('');
      fetchValues();
    }
  };

  const handleDeleteValue = async (id: string) => {
    const success = await deleteDictValues([id]);
    if (success) {
      message.success('删除成功');
      fetchValues();
    }
  };

  const handleMoveSort = async (index: number, direction: 'up' | 'down') => {
    if (!dict?.dictKey) return;
    const newIndex = direction === 'up' ? index - 1 : index + 1;
    if (newIndex < 0 || newIndex >= values.length) return;

    const newValues = [...values];
    [newValues[index], newValues[newIndex]] = [newValues[newIndex], newValues[index]];
    const ids = newValues.map((v) => v.id!);
    const success = await updateDictValueSort(dict.dictKey, ids);
    if (success) {
      fetchValues();
    }
  };

  if (!dict) {
    return (
      <div className={styles.rightPanel}>
        <div className={styles.panelHeader}>
          <span className={styles.panelTitle}>字典值</span>
        </div>
        <div className={styles.panelBody}>
          <div className={styles.emptyHint}>请从左侧选择字典</div>
        </div>
      </div>
    );
  }

  return (
    <div className={styles.rightPanel}>
      <div className={styles.panelHeader}>
        <span className={styles.panelTitle}>{dict.dictName} - 字典值</span>
      </div>
      <div style={{ padding: '8px 12px', borderBottom: '1px solid var(--border-color, #f0f0f0)' }}>
        <Space direction="vertical" style={{ width: '100%' }} size={4}>
          <Input
            size="small"
            placeholder="字典值"
            value={newValue}
            onChange={(e) => setNewValue(e.target.value)}
            onPressEnter={handleAddValue}
          />
          <Space.Compact style={{ width: '100%' }}>
            <Input
              size="small"
              placeholder="字典标签（可选，默认同字典值）"
              value={newLabel}
              onChange={(e) => setNewLabel(e.target.value)}
              onPressEnter={handleAddValue}
            />
            <Button size="small" type="primary" icon={<PlusOutlined />} onClick={handleAddValue}>
              添加
            </Button>
          </Space.Compact>
        </Space>
      </div>
      <div className={styles.panelBody}>
        {loading ? (
          <div className={styles.emptyHint}>加载中...</div>
        ) : values.length === 0 ? (
          <Empty description="暂无字典值" image={Empty.PRESENTED_IMAGE_SIMPLE} />
        ) : (
          values.map((item, index) => (
            <div key={item.id} className={styles.valueItem}>
              <span className={styles.valueItemSort}>{item.sort}</span>
              <div className={styles.valueItemContent}>
                <span className={styles.valueItemText}>{item.dictLabel}</span>
                <span className={styles.valueItemSub}>{item.dictValue}</span>
              </div>
              <div className={styles.valueItemActions}>
                <Button
                  type="link"
                  size="small"
                  icon={<ArrowUpOutlined />}
                  disabled={index === 0}
                  onClick={() => handleMoveSort(index, 'up')}
                />
                <Button
                  type="link"
                  size="small"
                  icon={<ArrowDownOutlined />}
                  disabled={index === values.length - 1}
                  onClick={() => handleMoveSort(index, 'down')}
                />
                <Popconfirm title="确定删除？" onConfirm={() => handleDeleteValue(item.id!)}>
                  <Button type="link" size="small" danger icon={<DeleteOutlined />} />
                </Popconfirm>
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
};

export default DictValueList;
