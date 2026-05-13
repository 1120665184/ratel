import { StopOutlined } from '@ant-design/icons';
import { Button } from 'antd';
import { useCallback } from 'react';
import { useForwardedPropsStore } from '@/stores/forwardedProps';
import styles from './index.module.less';

/**
 * AI模式终止控制组件
 * 展示在聊天面板输入框上方，用户可随时终止AI控制
 * 仅当 operationMode === 'ai' 时渲染
 */
export function AiModeControlBar() {
  const operationMode = useForwardedPropsStore((s) => s.operationMode);
  const setOperationMode = useForwardedPropsStore((s) => s.setOperationMode);

  const handleStop = useCallback(() => {
    setOperationMode('human');
  }, [setOperationMode]);

  if (operationMode !== 'ai') return null;

  return (
    <div className={styles.controlBar}>
      <StopOutlined className={styles.controlIcon} />
      <div className={styles.controlInfo}>
        <span className={styles.controlText}>智能助手正在控制界面</span>
      </div>
      <Button
        danger
        size="small"
        className={styles.stopBtn}
        icon={<StopOutlined />}
        onClick={handleStop}
      >
        终止控制
      </Button>
    </div>
  );
}
