import React, { useState, useRef, useEffect } from 'react';
import { Button } from 'antd';
import { SendOutlined } from '@ant-design/icons';
import styles from './index.module.less';

interface ChatInputProps {
  /** 发送消息回调 */
  onSend: (content: string) => void;
  /** 是否正在加载 */
  isLoading?: boolean;
}

/**
 * 聊天输入组件
 */
const ChatInput: React.FC<ChatInputProps> = ({ onSend, isLoading = false }) => {
  const [inputValue, setInputValue] = useState('');
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  // 自动调整文本框高度
  useEffect(() => {
    const textarea = textareaRef.current;
    if (textarea) {
      textarea.style.height = 'auto';
      textarea.style.height = `${Math.min(textarea.scrollHeight, 100)}px`;
    }
  }, [inputValue]);

  // 发送消息
  const handleSend = () => {
    const trimmedValue = inputValue.trim();
    if (trimmedValue && !isLoading) {
      onSend(trimmedValue);
      setInputValue('');
    }
  };

  // 键盘事件处理
  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  return (
    <div className={styles.chatInput}>
      <div className={styles.inputWrapper}>
        <textarea
          ref={textareaRef}
          className={styles.textarea}
          placeholder="输入消息... (Enter 发送, Shift+Enter 换行)"
          value={inputValue}
          onChange={(e) => setInputValue(e.target.value)}
          onKeyDown={handleKeyDown}
          disabled={isLoading}
          rows={1}
        />
      </div>
      <Button
        type="primary"
        className={styles.sendButton}
        onClick={handleSend}
        disabled={!inputValue.trim() || isLoading}
        icon={<SendOutlined />}
      />
    </div>
  );
};

export default ChatInput;
