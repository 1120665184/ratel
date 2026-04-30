import React from 'react';
import { MessageOutlined, ThunderboltOutlined, BulbOutlined } from '@ant-design/icons';
import styles from './index.module.less';

/**
 * 空状态组件
 * 当没有消息时显示
 */
const ChatEmpty: React.FC = () => {
  return (
    <div className={styles.emptyState}>
      <div className={styles.emptyIcon}>
        <MessageOutlined />
      </div>
      <div className={styles.emptyText}>
        <p className={styles.emptyTitle}>开始与 AI 助手对话</p>
        <p className={styles.emptySubtitle}>输入您的问题，我将为您提供帮助</p>
      </div>
      <div className={styles.emptyHints}>
        <div className={styles.hintItem}>
          <ThunderboltOutlined />
          <span>快速解答问题</span>
        </div>
        <div className={styles.hintItem}>
          <BulbOutlined />
          <span>智能分析建议</span>
        </div>
      </div>
    </div>
  );
};

export default ChatEmpty;
