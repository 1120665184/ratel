import InterfaceOperation from '@/components/InterfaceOperation';
import AiOutputPanel from '@/components/AiOutputPanel';
import SettingsPanel from '@/components/SettingsPanel';
import { useOperationTabStore } from '@/stores/operationTab';
import styles from './index.module.less';

/** 操作区 Tab 注册配置 */
interface OperationTabConfig {
  key: string;
  component: React.ComponentType;
  keepAlive: boolean;
}

const operationTabRegistry: OperationTabConfig[] = [
  { key: 'interface', component: InterfaceOperation, keepAlive: false },
  { key: 'ai-output', component: AiOutputPanel, keepAlive: true },
  { key: 'settings', component: SettingsPanel, keepAlive: false },
];

/**
 * 助手操作区组件
 * 基于注册表架构，承载界面展示、AI 输出、设置等多种能力
 * 通过顶部导航栏 Tab 切换，keepAlive 的 Tab 不销毁仅隐藏
 */
const AssistantOperationArea: React.FC = () => {
  const { activeTab } = useOperationTabStore();

  return (
    <div className={styles.operationArea}>
      {operationTabRegistry.map((tab) => {
        const isActive = activeTab === tab.key;
        const TabComponent = tab.component;

        if (tab.keepAlive) {
          // keepAlive 模式：始终渲染，通过 CSS 控制显隐
          return (
            <div
              key={tab.key}
              className={`${styles.tabContent} ${tab.key === 'ai-output' ? styles.aiOutputTab : ''}`}
              style={{
                visibility: isActive ? 'visible' : 'hidden',
                position: isActive ? 'relative' : 'absolute',
                ...(!isActive ? { top: 0, left: 0, width: '100%', height: '100%', pointerEvents: 'none' as const } : {}),
              }}
            >
              <TabComponent />
            </div>
          );
        }

        // 非 keepAlive 模式：条件渲染
        if (!isActive) return null;
        return (
          <div key={tab.key} className={styles.tabContent}>
            <TabComponent />
          </div>
        );
      })}
    </div>
  );
};

export default AssistantOperationArea;
