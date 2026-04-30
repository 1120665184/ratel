import React from 'react';
import { Button, Tooltip } from 'antd';
import { RobotOutlined, CompressOutlined, DragOutlined, CloseOutlined } from '@ant-design/icons';
import type { AIChatPanelMode } from './types';
import styles from './index.module.less';

interface ChatHeaderProps {
  /** 当前面板模式 */
  mode: AIChatPanelMode;
  /** 切换到固定模式 */
  onFixed: () => void;
  /** 切换到拖拽模式 */
  onDraggable: () => void;
  /** 隐藏面板 */
  onHide: () => void;
  /** 是否在拖拽模式下 */
  isDragging?: boolean;
}

/**
 * AI 聊天面板头部组件
 * 包含标题和控制按钮
 */
const ChatHeader: React.FC<ChatHeaderProps> = ({
  mode,
  onFixed,
  onDraggable,
  onHide,
  isDragging = false,
}) => {
  return (
    <div className={`${styles.chatHeader} ${isDragging ? styles.dragging : ''}`}>
      <div className={styles.chatHeaderTitle}>
        <RobotOutlined />
        <span>AI 助手</span>
      </div>
      <div className={styles.chatHeaderActions}>
        {mode === 'draggable' ? (
          <Tooltip title="固定模式">
            <Button
              type="text"
              size="small"
              className={styles.actionButton}
              onClick={onFixed}
              icon={<CompressOutlined />}
            />
          </Tooltip>
        ) : (
          <Tooltip title="拖拽模式">
            <Button
              type="text"
              size="small"
              className={styles.actionButton}
              onClick={onDraggable}
              icon={<DragOutlined />}
            />
          </Tooltip>
        )}
        <Tooltip title="收起面板">
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
  );
};

export default ChatHeader;
