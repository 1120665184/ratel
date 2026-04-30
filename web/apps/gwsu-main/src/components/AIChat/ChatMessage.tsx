import React from 'react';
import { UserOutlined, RobotOutlined } from '@ant-design/icons';
import type { ChatMessage } from './types';
import styles from './index.module.less';

interface ChatMessageItemProps {
  /** 消息数据 */
  message: ChatMessage;
}

/**
 * 单条聊天消息组件
 */
const ChatMessageItem: React.FC<ChatMessageItemProps> = ({ message }) => {
  const isUser = message.role === 'user';

  // 格式化时间
  const formatTime = (timestamp: number) => {
    const date = new Date(timestamp);
    return date.toLocaleTimeString('zh-CN', {
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  return (
    <div
      className={`${styles.messageItem} ${
        isUser ? styles.userMessage : styles.assistantMessage
      }`}
    >
      <div
        className={`${styles.messageAvatar} ${
          isUser ? styles.userAvatar : styles.assistantAvatar
        }`}
      >
        {isUser ? <UserOutlined /> : <RobotOutlined />}
      </div>
      <div className={styles.messageContent}>{message.content}</div>
      <div className={styles.messageTime}>{formatTime(message.timestamp)}</div>
    </div>
  );
};

export default ChatMessageItem;
