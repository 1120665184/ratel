import { RobotOutlined } from '@ant-design/icons';
import { Spin } from 'antd';
import { createPortal } from 'react-dom';
import { useForwardedPropsStore } from '@/stores/forwardedProps';
import styles from './index.module.less';

/**
 * AI操作模式遮罩组件
 * 当 operationMode 为 'ai' 时，全屏覆盖操作区和弹框
 * 使用 createPortal 挂载到 body，z-index=1050（高于Ant Design Modal，低于聊天面板）
 */
export function AiModeOverlay() {
  const operationMode = useForwardedPropsStore((s) => s.operationMode);

  if (operationMode !== 'ai') return null;

  return createPortal(
    <div className={styles.overlay}>
      <div className={styles.content}>
        <RobotOutlined className={styles.icon} />
        <Spin size="large" />
        <div className={styles.text}>智能助手控制中...</div>
        <div className={styles.subText}>请在右侧助手面板操作</div>
      </div>
    </div>,
    document.body,
  );
}
