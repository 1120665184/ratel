import { useState, useEffect, useCallback } from 'react';
import { Button, Input, Space, Popconfirm, message, Empty } from 'antd';
import { PlusOutlined, DeleteOutlined, ArrowUpOutlined, ArrowDownOutlined } from '@ant-design/icons';
import { getDictValues, saveOrUpdateDictValue, deleteDictValues, updateDictValueSort } from '../services/dict';
import type { DictInfo, DictValueInfo } from '../services/dict';
import styles from './index.module.less';

interface DictValueListProps {
  dict: DictInfo | null;
}

const DictValueList: React.FC<DictValueListProps> = ({ dict }) => {
  const [loading, setLoading] = useState(false);
  const [values, setValues] = useState<DictValueInfo[]>([]);
  const [newValue, setNewValue] = useState('');

  const fetchValues = useCallback(async () => {
    if (!dict?.id) {
      setValues([]);
      return;
    }
    setLoading(true);
    try {
      const result = await getDictValues(dict.id!);
      setValues(result);
    } catch {
      // error handled by request util
    } finally {
      setLoading(false);
    }
  }, [dict?.id]);

  useEffect(() => {
    fetchValues();
    setNewValue('');
  }, [dict?.id]);

  const handleAddValue = async () => {
    if (!dict?.id || !newValue.trim()) return;
    const success = await saveOrUpdateDictValue({
      dictId: dict.id!,
      dictValue: newValue.trim(),
    });
    if (success) {
      message.success('添加成功');
      setNewValue('');
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
    if (!dict?.id) return;
    const newIndex = direction === 'up' ? index - 1 : index + 1;
    if (newIndex < 0 || newIndex >= values.length) return;

    const newValues = [...values];
    [newValues[index], newValues[newIndex]] = [newValues[newIndex], newValues[index]];
    const ids = newValues.map((v) => v.id!);
    const success = await updateDictValueSort(ids);
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
        <Space.Compact style={{ width: '100%' }}>
          <Input
            size="small"
            placeholder="输入新值后添加"
            value={newValue}
            onChange={(e) => setNewValue(e.target.value)}
            onPressEnter={handleAddValue}
          />
          <Button size="small" type="primary" icon={<PlusOutlined />} onClick={handleAddValue}>
            添加
          </Button>
        </Space.Compact>
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
              <span className={styles.valueItemText}>{item.dictValue}</span>
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
