import { useState, useEffect, useRef } from 'react';
import { RightOutlined, LoadingOutlined } from '@ant-design/icons';
import styles from './ReasoningMessageItem.module.less';

interface ReasoningMessageItemProps {
  /** 消息 ID */
  id: string;
  /** 消息内容 */
  content: string;
  /** 是否正在流式输出 */
  isStreaming?: boolean;
}

/**
 * 推理/思考消息渲染组件
 * 可折叠的思考卡片，流式输出时自动展开，完成后自动折叠
 */
export function ReasoningMessageItem({
  content,
  isStreaming = false,
}: ReasoningMessageItemProps) {
  const [isOpen, setIsOpen] = useState(true);
  const userToggledRef = useRef(false);
  const startTimeRef = useRef<number | null>(null);
  const [elapsed, setElapsed] = useState(0);

  // 流式输出时自动展开，完成后自动折叠（用户手动操作除外）
  useEffect(() => {
    if (isStreaming) {
      userToggledRef.current = false;
      setIsOpen(true);
    } else if (!userToggledRef.current) {
      setIsOpen(false);
    }
  }, [isStreaming]);

  // 计时器：流式输出时持续更新耗时
  useEffect(() => {
    if (isStreaming) {
      if (startTimeRef.current === null) {
        startTimeRef.current = Date.now();
      }
      // 每 200ms 更新一次，确保计时准确
      const timer = setInterval(() => {
        if (startTimeRef.current !== null) {
          setElapsed((Date.now() - startTimeRef.current) / 1000);
        }
      }, 200);
      return () => clearInterval(timer);
    }

    // 流式结束：计算最终耗时
    if (startTimeRef.current !== null) {
      setElapsed((Date.now() - startTimeRef.current) / 1000);
    }
  }, [isStreaming]);

  const hasContent = !!(content && content.length > 0);

  const handleToggle = () => {
    userToggledRef.current = true;
    setIsOpen((prev) => !prev);
  };

  // 格式化耗时
  const formatDuration = (seconds: number): string => {
    if (seconds <= 0) return '';
    if (seconds < 1) return '1秒';
    if (seconds < 60) return `${Math.round(seconds)}秒`;
    const mins = Math.floor(seconds / 60);
    const secs = Math.round(seconds % 60);
    if (secs === 0) return `${mins}分钟`;
    return `${mins}分${secs}秒`;
  };

  const durationText = formatDuration(elapsed);
  const label = isStreaming
    ? (durationText ? `思考中… ${durationText}` : '思考中…')
    : (durationText ? `思考了 ${durationText}` : '已思考');

  return (
    <div className={styles.reasoningCard} data-message-role="reasoning">
      {/* 头部：标签 + 折叠控制 */}
      <button
        type="button"
        className={`${styles.header} ${hasContent ? styles.headerExpandable : ''}`}
        onClick={hasContent ? handleToggle : undefined}
        aria-expanded={hasContent ? isOpen : undefined}
      >
        <span className={styles.label}>{label}</span>
        {isStreaming && !hasContent && (
          <span className={styles.streamingDot}>
            <LoadingOutlined spin />
          </span>
        )}
        {hasContent && (
          <span className={`${styles.chevron} ${isOpen ? styles.chevronOpen : ''}`}>
            <RightOutlined />
          </span>
        )}
      </button>

      {/* 可折叠内容区域 */}
      <div
        className={`${styles.contentWrapper} ${isOpen ? styles.contentWrapperOpen : ''}`}
      >
        <div className={styles.contentInner}>
          {hasContent && (
            <div className={styles.content}>
              {content}
              {isStreaming && (
                <span className={styles.streamingCursor}>
                  <LoadingOutlined spin />
                </span>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
