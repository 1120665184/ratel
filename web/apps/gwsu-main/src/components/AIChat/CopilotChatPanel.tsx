import { CopilotChat } from '@copilotkit/react-ui';
import { useAgent } from '@copilotkit/react-core/v2';
import '@copilotkit/react-ui/styles.css';
import { App, Button, Tooltip } from 'antd';
import { RobotOutlined, CompressOutlined, DragOutlined, CloseOutlined, HistoryOutlined, PlusOutlined } from '@ant-design/icons';
import { useRef, useState, useCallback, useEffect } from 'react';
import { createPortal } from 'react-dom';
import Draggable, { DraggableEvent, DraggableData } from 'react-draggable';
import type { CSSProperties } from 'react';
import type { AIChatPanelMode } from './types';
import { usePanelContext } from './AIChatContext';
import { ChatHistoryPanel } from './ChatHistoryPanel';
import { HumanApprovalBar } from './HumanApprovalBar';
import { AskUserQuestionBar } from './AskUserQuestionBar';
import { getSessionMessages, getApprovalStatus, type BrainMessage } from '@/services/brain';
import { dispatchHumanApproval, clearHumanApproval, onHumanApproval } from '@/services/human-approval';
import { dispatchAskUserQuestion, clearAskUserQuestion, onAskUserQuestion } from '@/services/ask-user-question';
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
  style,
  mode = 'fixed',
  onFixed,
  onDraggable,
  onHide,
}: CopilotChatPanelProps) {
  const { viewMode, setViewMode, panelState, setPanelPosition, setCurrentThreadId } = usePanelContext();
  const { agent } = useAgent({ agentId: 'brain' });
  const { message } = App.useApp();

  // CopilotChat 组件容器 ref，用于精确控制其内部输入框
  const copilotChatRef = useRef<HTMLDivElement>(null);

  // 防止 Ant Design 弹框/抽屉的 focus trap 阻止面板内输入框获取焦点
  // Ant Design 的 rc-dialog 在打开时会通过 focusin 全局事件将焦点拉回弹框内部
  // 这里在捕获阶段拦截，允许面板内的元素正常获取焦点
  const wrapperRef = useRef<HTMLDivElement>(null);
  useEffect(() => {
    const el = wrapperRef.current;
    if (!el) return;
    const handleFocusIn = (e: FocusEvent) => {
      // 如果焦点目标是面板内的元素，阻止后续的 focus trap 处理
      if (el.contains(e.target as Node)) {
        e.stopPropagation();
      }
    };
    // 在捕获阶段拦截，优先于 rc-dialog 的冒泡阶段 focusin 监听
    el.addEventListener('focusin', handleFocusIn, true);
    return () => {
      el.removeEventListener('focusin', handleFocusIn, true);
    };
  }, []);

  // 拖拽相关状态
  const nodeRef = useRef<HTMLDivElement>(null);
  const [isDragging, setIsDragging] = useState(false);
  // 使用 ref 来跟踪当前模式，避免因模式变化导致重新渲染
  const isDraggableMode = mode === 'draggable';

  // 弹框与输入框互斥控制
  const [hasApproval, setHasApproval] = useState(false);
  const [hasAskQuestion, setHasAskQuestion] = useState(false);

  useEffect(() => {
    const unsubApproval = onHumanApproval((payload) => setHasApproval(payload !== null));
    const unsubAskQuestion = onAskUserQuestion((payload) => setHasAskQuestion(payload !== null));
    return () => {
      unsubApproval();
      unsubAskQuestion();
    };
  }, []);

  const isInteractionActive = hasApproval || hasAskQuestion;

  // 互斥控制：弹框活跃时通过 DOM 直接禁用 CopilotChat 输入框
  // 仅在 copilotChatRef 范围内查找，避免误禁用审批栏/问题栏的输入
  useEffect(() => {
    const chatEl = copilotChatRef.current;
    if (!chatEl) return;

    const applyState = (active: boolean) => {
      // 仅查找 CopilotChat 内的 textarea（聊天消息输入框）
      const textarea = chatEl.querySelector('textarea');
      if (textarea) {
        (textarea as HTMLTextAreaElement).disabled = active;
        (textarea as HTMLElement).style.opacity = active ? '0.4' : '';
        (textarea as HTMLElement).style.pointerEvents = active ? 'none' : '';
      }

      // 仅禁用 CopilotChat 内的发送按钮
      chatEl.querySelectorAll('button').forEach((btn) => {
        (btn as HTMLButtonElement).disabled = active;
        (btn as HTMLElement).style.opacity = active ? '0.4' : '';
        (btn as HTMLElement).style.pointerEvents = active ? 'none' : '';
      });
    };

    applyState(isInteractionActive);

    // 当弹框活跃时，监听 DOM 变化以应对 CopilotChat 重新渲染
    let observer: MutationObserver | null = null;
    if (isInteractionActive) {
      observer = new MutationObserver(() => applyState(true));
      observer.observe(chatEl, { childList: true, subtree: true });
    }

    return () => {
      observer?.disconnect();
    };
  }, [isInteractionActive]);

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
    agent.setMessages([]);
    clearHumanApproval();
    clearAskUserQuestion();
    // 生成新的 threadId，让后端创建新的会话
    const newThreadId = crypto.randomUUID();
    setCurrentThreadId(newThreadId);
    agent.threadId = newThreadId;
  };

  const handleLoadSession = async (sessionId: string) => {
    try {
      // 先清除旧的弹框状态，避免切换会话后旧弹框残留
      clearHumanApproval();
      clearAskUserQuestion();
      const messages = await getSessionMessages(sessionId);
      agent.setMessages([]);
      agent.threadId = sessionId;
      setCurrentThreadId(sessionId);
      const formattedMessages = messages.map((msg: BrainMessage) => ({
        id: msg.id,
        role: msg.role,
        content: typeof msg.content === 'string' ? msg.content : (msg.content ? JSON.stringify(msg.content) : ''),
        ...(msg.toolCalls && msg.toolCalls.length > 0 ? { toolCalls: msg.toolCalls } : {}),
        ...(msg.toolCallId ? { toolCallId: msg.toolCallId } : {}),
      }));
      agent.setMessages(formattedMessages as Parameters<typeof agent.setMessages>[0]);

      // 检查是否需要恢复 AskUserQuestion 弹框
      // 最新一条消息如果是 AskUserQuestion 工具调用且无对应结果，则恢复弹框
      try {
        const lastMsg = messages[messages.length - 1] as BrainMessage | undefined;
        if (lastMsg?.role === 'assistant' && lastMsg.toolCalls?.length) {
          const askQuestionToolCall = lastMsg.toolCalls.find(
            (tc) => tc.function?.name === 'AskUserQuestion'
          );
          if (askQuestionToolCall) {
            const args = typeof askQuestionToolCall.function?.arguments === 'string'
              ? JSON.parse(askQuestionToolCall.function.arguments)
              : askQuestionToolCall.function?.arguments ?? askQuestionToolCall.args ?? {};
            dispatchAskUserQuestion({
              toolCallId: askQuestionToolCall.id as string,
              questions: Array.isArray(args.questions) ? args.questions.map((q: Record<string, unknown>) => ({
                question: String(q.question ?? ''),
                header: String(q.header ?? ''),
                options: Array.isArray(q.options) ? q.options : (q.options ? [q.options] : []),
                multiSelect: Boolean(q.multiSelect),
              })) : [],
            });
          }
        }
      } catch (e) {
        console.warn('[AskUserQuestion] 历史会话恢复失败:', e);
      }

      // 根据后端实际审批状态决定是否恢复审批弹框
      try {
        const approvalStatus = await getApprovalStatus(sessionId);
        if (approvalStatus.stage) {
          dispatchHumanApproval(approvalStatus as any);
        }
      } catch (e) {
        console.warn('[HumanApproval] 查询审批状态失败:', e);
      }
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
          <Tooltip title="新会话" zIndex={100001}>
            <Button
              type="text"
              size="small"
              className={styles.actionButton}
              onClick={handleNewSession}
              icon={<PlusOutlined />}
            />
          </Tooltip>
          <Tooltip title="历史记录" zIndex={100001}>
            <Button
              type="text"
              size="small"
              className={styles.actionButton}
              onClick={handleShowHistory}
              icon={<HistoryOutlined />}
            />
          </Tooltip>
          {isDraggableMode ? (
            <Tooltip title="固定模式" zIndex={100001}>
              <Button
                type="text"
                size="small"
                className={styles.actionButton}
                onClick={onFixed}
                icon={<CompressOutlined />}
              />
            </Tooltip>
          ) : (
            <Tooltip title="拖拽模式" zIndex={100001}>
              <Button
                type="text"
                size="small"
                className={styles.actionButton}
                onClick={onDraggable}
                icon={<DragOutlined />}
              />
            </Tooltip>
          )}
          <Tooltip title="收起面板" zIndex={100001}>
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
      {/* 人工审批卡片 - 展示在聊天输入框上方 */}
      <HumanApprovalBar />
      {/* AskUserQuestion 选择框 - 展示在审批卡片下方 */}
      <AskUserQuestionBar />
      {/* CopilotChat 组件 - 隐藏默认 header */}
      <div ref={copilotChatRef} style={{ display: 'contents' }}>
        <CopilotChat
          labels={{
            title: '智能助手',
            placeholder: '输入消息...',
            initial: '我是你的平台助手，有什么问题可以问我哦^_^',
          }}
          className={styles.copilotChat}
        />
      </div>
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

  // 使用 createPortal 将面板挂载到 body 最外层，脱离任何父级 stacking context
  // 确保智能助手层级始终高于所有弹框、Modal、Drawer 等 Ant Design 组件
  const panelContent = (
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
        ref={(el) => {
          (nodeRef as React.MutableRefObject<HTMLDivElement | null>).current = el;
          (wrapperRef as React.MutableRefObject<HTMLDivElement | null>).current = el;
        }}
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

  return createPortal(panelContent, document.body);
}
