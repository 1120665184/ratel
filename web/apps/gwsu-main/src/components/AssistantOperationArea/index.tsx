import InterfaceOperation from '@/components/InterfaceOperation';
import AiOutputPanel from '@/components/AiOutputPanel';
import { useOperationTabStore } from '@/stores/operationTab';
import styles from './index.module.less';

interface AssistantOperationAreaProps {
  children?: React.ReactNode;
}

/**
 * 助手操作区组件
 * 作为能力容器，承载界面展示和 AI 输出两种能力
 * 通过顶部导航栏 Tab 切换，Tab2（AI 输出）不销毁
 */
const AssistantOperationArea: React.FC<AssistantOperationAreaProps> = () => {
  const { activeTab } = useOperationTabStore();
  const isInterfaceTab = activeTab === 'interface';

  return (
    <div className={styles.operationArea}>
      {/* Tab1: 界面展示 - 条件显示 */}
      <div
        className={styles.tabContent}
        style={{ display: isInterfaceTab ? 'flex' : 'none' }}
      >
        <InterfaceOperation />
      </div>

      {/* Tab2: AI 输出 - 不销毁，仅隐藏 */}
      <div
        className={`${styles.tabContent} ${styles.aiOutputTab}`}
        style={{
          visibility: isInterfaceTab ? 'hidden' : 'visible',
          position: isInterfaceTab ? 'absolute' : 'relative',
          ...(isInterfaceTab
            ? { top: 0, left: 0, width: '100%', height: '100%', pointerEvents: 'none' as const }
            : {}),
        }}
      >
        <AiOutputPanel />
      </div>
    </div>
  );
};

export default AssistantOperationArea;
