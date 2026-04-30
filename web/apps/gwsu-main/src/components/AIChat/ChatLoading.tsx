import React from 'react';
import styles from './index.module.less';

/**
 * 加载中消息组件
 * 显示 AI 正在输入的动画效果
 */
const ChatLoading: React.FC = () => {
  return (
    <div className={`${styles.messageItem} ${styles.assistantMessage}`}>
      <div className={`${styles.messageAvatar} ${styles.assistantAvatar}`}>
        <span>AI</span>
      </div>
      <div className={styles.loadingMessage}>
        <div className={styles.loadingDot} />
        <div className={styles.loadingDot} />
        <div className={styles.loadingDot} />
      </div>
    </div>
  );
};

export default ChatLoading;
