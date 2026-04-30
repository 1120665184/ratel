import { CopilotChat } from '@copilotkit/react-ui';
import { useCopilotChat } from '@copilotkit/react-core';
import { useAgent } from '@copilotkit/react-core/v2';
import '@copilotkit/react-ui/styles.css';
import { Button, Tooltip, message } from 'antd';
import { RobotOutlined, CompressOutlined, DragOutlined, CloseOutlined, HistoryOutlined, PlusOutlined } from '@ant-design/icons';
import { useRef, useState, useCallback } from 'react';
import Draggable, { DraggableEvent, DraggableData } from 'react-draggable';
import type { CSSProperties } from 'react';
import type { AIChatPanelMode } from './types';
import { usePanelContext } from './AIChatContext';
import { ChatHistoryPanel } from './ChatHistoryPanel';
import { getSessionMessages, type BrainMessage } from '@/services/brain';
import styles from './copilot-override.module.less';

interface CopilotChatPanelProps {
  /** 自定义类名 */
  className?: string;
  /** 固定模式下的宽度 */
  fixedWidth?: string;
  /** 自定义样式 */
  style?: CSSProperties;
  /** 当前面板模式 */
  mode?: AIChatPanelMode;
  /** 切换到固定模式 */
  onFixed?: () => void;
  /** 切换到拖拽模式 */
  onDraggable?: () => void;
  /** 隐藏面板 */
  onHide?: () => void;
}

/**
 * CopilotChat 组件封装
 * 使用 CopilotKit 的 CopilotChat 组件，支持自定义样式和 header
 * 内部处理拖拽逻辑，避免模式切换时重新初始化
 */
export function CopilotChatPanel({
  className,
  fixedWidth = '100%',
  style,
  mode = 'fixed',
  onFixed,
  onDraggable,
  onHide,
}: CopilotChatPanelProps) {
  const { viewMode, setViewMode, panelState, setPanelPosition, setCurrentThreadId } = usePanelContext();
  const { reset } = useCopilotChat();
  const { agent } = useAgent({ agentId: 'brain' });

  // 拖拽相关状态
  const nodeRef = useRef<HTMLDivElement>(null);
  const [isDragging, setIsDragging] = useState(false);
  // 使用 ref 来跟踪当前模式，避免因模式变化导致重新渲染
  const isDraggableMode = mode === 'draggable';

  // 切换到历史视图
  const handleShowHistory = () => {
    setViewMode('history');
  };

  // 返回聊天视图
  const handleBackToChat = () => {
    setViewMode('chat');
  };

  // 新建会话
  const handleNewSession = () => {
    reset();
    // 生成新的 threadId，让后端创建新的会话
    const newThreadId = crypto.randomUUID();
    setCurrentThreadId(newThreadId);
    agent.threadId = newThreadId;
  };

  const handleLoadSession = async (sessionId: string) => {
    try {
      const messages = await getSessionMessages(sessionId);
      reset();
      agent.threadId = sessionId;
      setCurrentThreadId(sessionId);
      const formattedMessages = messages.map((msg: BrainMessage) => ({
        id: msg.id,
        role: msg.role as 'user' | 'assistant',
        content: typeof msg.content === 'string' ? msg.content : JSON.stringify(msg.content),
      }));
      agent.setMessages(formattedMessages);
    } catch (error) {
      console.error('加载会话消息失败:', error);
      message.error('加载会话消息失败');
    }
  };

  // 拖拽处理
  const handleDragStart = useCallback(() => {
    setIsDragging(true);
  }, []);

  const handleDragStop = useCallback((_e: DraggableEvent, data: DraggableData) => {
    setIsDragging(false);
    setPanelPosition({ x: data.x, y: data.y });
  }, [setPanelPosition]);

  // 渲染聊天内容（不含外层容器）
  const renderChatContent = () => (
    <>
      {/* 自定义 Header - 包含拖动和关闭按钮 */}
      <div className={`${styles.chatHeader} ${isDragging ? styles.dragging : ''}`}>
        <div className={styles.chatHeaderTitle}>
          <RobotOutlined />
          <span>智能助手</span>
        </div>
        <div className={styles.chatHeaderActions}>
          <Tooltip title="新会话" zIndex={10000}>
            <Button
              type="text"
              size="small"
              className={styles.actionButton}
              onClick={handleNewSession}
              icon={<PlusOutlined />}
            />
          </Tooltip>
          <Tooltip title="历史记录" zIndex={10000}>
            <Button
              type="text"
              size="small"
              className={styles.actionButton}
              onClick={handleShowHistory}
              icon={<HistoryOutlined />}
            />
          </Tooltip>
          {isDraggableMode ? (
            <Tooltip title="固定模式" zIndex={10000}>
              <Button
                type="text"
                size="small"
                className={styles.actionButton}
                onClick={onFixed}
                icon={<CompressOutlined />}
              />
            </Tooltip>
          ) : (
            <Tooltip title="拖拽模式" zIndex={10000}>
              <Button
                type="text"
                size="small"
                className={styles.actionButton}
                onClick={onDraggable}
                icon={<DragOutlined />}
              />
            </Tooltip>
          )}
          <Tooltip title="收起面板" zIndex={10000}>
            <Button
              type="text"
              size="small"
              className={styles.actionButton}
              onClick={onHide}
              icon={<CloseOutlined />}
            />
          </Tooltip>
        </div>
      </div>
      {/* CopilotChat 组件 - 隐藏默认 header */}
      <CopilotChat
        labels={{
          title: '智能助手',
          placeholder: '输入消息...',
          initial: '我是你的平台助手，有什么问题可以问我哦^_^',
        }}
        className={styles.copilotChat}
      />
    </>
  );

  // 渲染历史面板内容
  const renderHistoryContent = () => (
    <ChatHistoryPanel
      onBackToChat={handleBackToChat}
      onLoadSession={handleLoadSession}
    />
  );

  // 始终渲染 Draggable，让历史视图和聊天视图都在同一个可拖拽容器中
  // 这样可以避免模式切换时组件被销毁重建，同时保持拖拽位置一致
  const isHidden = mode === 'hidden';

  return (
    <Draggable
      nodeRef={nodeRef}
      handle={`.${styles.chatHeader}`}
      position={isDraggableMode ? panelState.position : { x: 0, y: 0 }}
      onStart={isDraggableMode ? handleDragStart : undefined}
      onStop={isDraggableMode ? handleDragStop : undefined}
      bounds="body"
      disabled={!isDraggableMode}
    >
      <div
        ref={nodeRef}
        className={`${styles.copilotChatWrapper} ${isHidden ? styles.hiddenWrapper : ''} ${isDraggableMode ? styles.draggableWrapper : ''} ${className || ''}`}
        style={isDraggableMode ? {
          width: panelState.width,
          height: panelState.height,
        } : style}
      >
        {viewMode === 'history' ? renderHistoryContent() : renderChatContent()}
      </div>
    </Draggable>
  );
}
