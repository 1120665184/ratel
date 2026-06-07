import React, { useState, useCallback } from 'react';
import { Card, Switch, Input, Button, App } from 'antd';
import { PlusOutlined, CloseOutlined } from '@ant-design/icons';
import type { ExtensionFilterConfig } from './types';
import styles from './index.module.less';

interface ExtensionFilterFormProps {
  value: ExtensionFilterConfig;
  onChange: (config: ExtensionFilterConfig) => void;
}

const ExtensionFilterForm: React.FC<ExtensionFilterFormProps> = ({ value, onChange }) => {
  const { message } = App.useApp();
  const [inputValue, setInputValue] = useState('');

  const extensions = value.disable
    ? value.disable.split(',').filter((s) => s.trim() !== '')
    : [];

  const handleEnabledChange = useCallback(
    (enabled: boolean) => {
      onChange({ ...value, enabled });
    },
    [value, onChange],
  );

  const handleAdd = useCallback(() => {
    const raw = inputValue.trim().toLowerCase().replace(/^\./, '');
    if (!raw) return;
    if (extensions.includes(raw)) {
      message.warning(`后缀 .${raw} 已存在`);
      setInputValue('');
      return;
    }
    const newList = [...extensions, raw];
    onChange({ ...value, disable: newList.join(',') });
    setInputValue('');
  }, [inputValue, extensions, value, onChange, message]);

  const handleRemove = useCallback(
    (ext: string) => {
      const newList = extensions.filter((e) => e !== ext);
      onChange({ ...value, disable: newList.join(',') });
    },
    [extensions, value, onChange],
  );

  const handleKeyDown = useCallback(
    (e: React.KeyboardEvent) => {
      if (e.key === 'Enter') {
        e.preventDefault();
        handleAdd();
      }
    },
    [handleAdd],
  );

  return (
    <>
      <Card title="启用文件后缀过滤" className={styles.sectionCard} size="small">
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <Switch checked={value.enabled} onChange={handleEnabledChange} />
          <span style={{ color: value.enabled ? undefined : 'var(--text-secondary-color, #999)' }}>
            {value.enabled ? '已启用，将禁止上传指定后缀的文件' : '未启用，允许上传所有类型文件'}
          </span>
        </div>
      </Card>

      <Card title="禁止上传的后缀" className={styles.sectionCard} size="small">
        {extensions.length > 0 ? (
          <div className={styles.extensionTags}>
            {extensions.map((ext) => (
              <span key={ext} className={styles.extensionTag}>
                .{ext}
                <span className={styles.extensionTagRemove} onClick={() => handleRemove(ext)}>
                  <CloseOutlined />
                </span>
              </span>
            ))}
          </div>
        ) : (
          <div className={styles.emptyHint}>暂无禁止的后缀，在下方输入并添加</div>
        )}

        <div className={styles.addExtensionRow}>
          <Input
            value={inputValue}
            onChange={(e) => setInputValue(e.target.value)}
            onKeyDown={handleKeyDown}
            placeholder="输入后缀，如 exe、bat、sh"
            style={{ width: 240 }}
            disabled={!value.enabled}
          />
          <Button
            icon={<PlusOutlined />}
            onClick={handleAdd}
            disabled={!value.enabled || !inputValue.trim()}
          >
            添加
          </Button>
        </div>
      </Card>
    </>
  );
};

export default ExtensionFilterForm;
