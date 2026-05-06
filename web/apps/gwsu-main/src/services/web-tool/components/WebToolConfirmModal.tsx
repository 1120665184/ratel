import { Descriptions, Modal, Tag } from 'antd';
import { useCallback, useEffect, useRef, useState } from 'react';
import { onWebToolConfirm } from '../dispatcher';
import type { WebToolConfirmEvent } from '../types';
import styles from './WebToolConfirmModal.module.less';

/**
 * Web工具确认对话框
 * 当 toolType 为 INTERACTIVE 时，展示此对话框让用户确认后再执行
 */
export function WebToolConfirmModal() {
  const [pendingEvent, setPendingEvent] = useState<WebToolConfirmEvent | null>(
    null,
  );
  // 用 ref 追踪最新事件，避免闭包问题
  const eventRef = useRef<WebToolConfirmEvent | null>(null);

  const handleConfirm = useCallback(() => {
    eventRef.current?.onConfirm();
    eventRef.current = null;
    setPendingEvent(null);
  }, []);

  const handleCancel = useCallback(() => {
    eventRef.current?.onCancel();
    eventRef.current = null;
    setPendingEvent(null);
  }, []);

  // 监听确认事件
  useEffect(() => {
    const unsubscribe = onWebToolConfirm((event) => {
      eventRef.current = event;
      setPendingEvent(event);
    });
    return unsubscribe;
  }, []);

  // 事件变更时弹出确认框
  useEffect(() => {
    if (!pendingEvent) return;

    const paramsEntries = Object.entries(pendingEvent.params);
    const paramsContent = (
      <Descriptions
        column={1}
        size="small"
        bordered
        className={styles.paramsDesc}
      >
        {paramsEntries.map(([key, value]) => (
          <Descriptions.Item key={key} label={key}>
            {String(value)}
          </Descriptions.Item>
        ))}
      </Descriptions>
    );

    const instance = Modal.confirm({
      title: (
        <span className={styles.modalTitle}>
          <Tag color="orange" className={styles.interactiveTag}>
            需确认
          </Tag>
          {pendingEvent.description}
        </span>
      ),
      content: paramsContent,
      okText: '确认执行',
      cancelText: '取消',
      onOk: handleConfirm,
      onCancel: handleCancel,
      centered: true,
      mask: {
        closable: false,
      },
    });

    return () => {
      instance.destroy();
    };
  }, [pendingEvent, handleConfirm, handleCancel]);

  return null;
}
