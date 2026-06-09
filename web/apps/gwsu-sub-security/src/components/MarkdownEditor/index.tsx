import React, { useState, useCallback } from 'react';
import { Segmented } from 'antd';
import styles from './index.module.less';

interface MarkdownEditorProps {
  value?: string;
  onChange?: (value: string) => void;
  placeholder?: string;
  minHeight?: number;
}

const MarkdownEditor: React.FC<MarkdownEditorProps> = ({
  value = '',
  onChange,
  placeholder,
  minHeight = 300,
}) => {
  const [mdMode, setMdMode] = useState<'edit' | 'preview' | 'split'>('edit');

  const handleChange = useCallback(
    (e: React.ChangeEvent<HTMLTextAreaElement>) => {
      onChange?.(e.target.value);
    },
    [onChange],
  );

  const renderMarkdownPreview = useCallback((content: string) => {
    return content
      .replace(/^### (.+)$/gm, '<h3>$1</h3>')
      .replace(/^## (.+)$/gm, '<h2>$1</h2>')
      .replace(/^# (.+)$/gm, '<h1>$1</h1>')
      .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
      .replace(/\*(.+?)\*/g, '<em>$1</em>')
      .replace(/`(.+?)`/g, '<code>$1</code>')
      .replace(/^- (.+)$/gm, '<li>$1</li>')
      .replace(/^> (.+)$/gm, '<blockquote>$1</blockquote>')
      .replace(/\n/g, '<br/>');
  }, []);

  return (
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
      <div className={styles.mdContent} style={{ minHeight }}>
        {(mdMode === 'edit' || mdMode === 'split') && (
          <textarea
            className={styles.mdEditArea}
            value={value}
            onChange={handleChange}
            placeholder={placeholder}
            style={{ width: mdMode === 'split' ? '50%' : '100%' }}
          />
        )}
        {(mdMode === 'preview' || mdMode === 'split') && (
          <div
            className={styles.mdPreview}
            style={{ width: mdMode === 'split' ? '50%' : '100%' }}
            dangerouslySetInnerHTML={{ __html: renderMarkdownPreview(value) }}
          />
        )}
      </div>
    </div>
  );
};

export default MarkdownEditor;
