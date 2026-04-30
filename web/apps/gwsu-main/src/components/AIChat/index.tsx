import React, { useRef, useEffect, useCallback } from 'react';
import Draggable, { DraggableEvent, DraggableData } from 'react-draggable';
import { Button, Tooltip } from 'antd';
import { RobotOutlined } from '@ant-design/icons';
import ChatHeader from './ChatHeader';
import ChatMessageItem from './ChatMessage';
import ChatLoading from './ChatLoading';
import ChatInput from './ChatInput';
import ChatEmpty from './ChatEmpty';
import { useAIChatContext } from './AIChatContext';
import type { AIChatPanelMode } from './types';
import styles from './index.module.less';

interface AIChatPanelProps {
  /** 自定义类名 */
  className?: string;
  /** 固定模式下的宽度（CSS 值） */
  fixedWidth?: string;
}

/**
 * AI 聊天面板组件
 * 支持固定模式和拖拽模式
 */
const AIChatPanel: React.FC<AIChatPanelProps> = ({
  className,
  fixedWidth = '100%',
}) => {
  const {
    messages,
    sendMessage,
    panelState,
    setPanelMode,
    setPanelPosition,
    togglePanel,
    isLoading,
  } = useAIChatContext();

  const messagesEndRef = useRef<HTMLDivElement>(null);
  const nodeRef = useRef<HTMLDivElement>(null);
  const [isDragging, setIsDragging] = React.useState(false);

  // 滚动到底部
  const scrollToBottom = useCallback(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, []);

  // 消息更新时滚动到底部
  useEffect(() => {
    scrollToBottom();
  }, [messages, scrollToBottom]);

  // 处理拖拽
  const handleDragStart = () => {
    setIsDragging(true);
  };

  const handleDragStop = (_e: DraggableEvent, data: DraggableData) => {
    setIsDragging(false);
    setPanelPosition({ x: data.x, y: data.y });
  };

  // 切换到固定模式
  const handleFixed = () => {
    setPanelMode('fixed');
  };

  // 切换到拖拽模式
  const handleDraggable = () => {
    setPanelMode('draggable');
  };

  // 隐藏面板
  const handleHide = () => {
    setPanelMode('hidden');
  };

  // 渲染消息列表
  const renderMessages = () => {
    if (messages.length === 0) {
      return <ChatEmpty />;
    }

    return (
      <>
        {messages.map((message) => (
          <ChatMessageItem key={message.id} message={message} />
        ))}
        {isLoading && <ChatLoading />}
        <div ref={messagesEndRef} />
      </>
    );
  };

  // 渲染面板内容
  const renderPanelContent = (mode: AIChatPanelMode) => (
    <>
      <ChatHeader
        mode={mode}
        onFixed={handleFixed}
        onDraggable={handleDraggable}
        onHide={handleHide}
        isDragging={isDragging}
      />
      <div className={styles.chatMessages}>{renderMessages()}</div>
      <ChatInput onSend={sendMessage} isLoading={isLoading} />
    </>
  );

  // 隐藏状态 - 不显示任何内容（顶部导航栏已有展开按钮）
  if (panelState.mode === 'hidden') {
    return null;
  }

  // 拖拽模式
  if (panelState.mode === 'draggable') {
    return (
      <Draggable
        nodeRef={nodeRef}
        handle={`.${styles.chatHeader}`}
        position={panelState.position}
        onStart={handleDragStart}
        onStop={handleDragStop}
        bounds="parent"
      >
        <div
          ref={nodeRef}
          className={`${styles.aiChatPanelDraggable} ${isDragging ? styles.dragging : ''} ${className || ''}`}
          style={{
            width: panelState.width,
            height: panelState.height,
          }}
        >
          {renderPanelContent('draggable')}
        </div>
      </Draggable>
    );
  }

  // 固定模式
  return (
    <div
      className={`${styles.aiChatPanelFixed} ${className || ''}`}
      style={{ width: fixedWidth }}
    >
      {renderPanelContent('fixed')}
    </div>
  );
};

export default AIChatPanel;
