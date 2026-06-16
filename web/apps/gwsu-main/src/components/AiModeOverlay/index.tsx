import { RobotOutlined } from '@ant-design/icons';
import { Spin } from 'antd';
import { createPortal } from 'react-dom';
import { useForwardedPropsStore } from '@/stores/forwardedProps';
import { useHeadlessStore } from '@gwsu/core';
import styles from './index.module.less';

/**
 * AI操作模式遮罩组件
 * 当 operationMode 为 'ai' 时，全屏覆盖操作区和弹框
 * 无头浏览器模式下不展示遮罩层（由程序控制，无需人工提示）
 * 使用 createPortal 挂载到 body，z-index=1050（高于Ant Design Modal，低于聊天面板）
 */
export function AiModeOverlay() {
  const operationMode = useForwardedPropsStore((s) => s.operationMode);
  const isHeadless = useHeadlessStore((s) => s.isHeadless);

  if (operationMode !== 'ai') return null;
  if (isHeadless) return null;

  return createPortal(
    <div className={styles.overlay}>
      <div className={styles.content}>
        <RobotOutlined className={styles.icon} />
        <Spin size="small" />
        <div className={styles.text}>智能助手控制中...</div>
        <div className={styles.subText}>请在助手面板操作</div>
      </div>
    </div>,
    document.body,
  );
}
