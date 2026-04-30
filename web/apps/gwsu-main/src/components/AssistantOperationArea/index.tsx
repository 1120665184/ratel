import InterfaceOperation from '@/components/InterfaceOperation';
import styles from './index.module.less';

/**
 * 助手操作区组件
 * 作为能力容器，可承载多种操作能力
 * 当前包含：界面操作能力
 */
const AssistantOperationArea: React.FC = () => {
  return (
    <div className={styles.operationArea}>
      {/* 界面操作能力 */}
      <InterfaceOperation />

      {/* 后续可添加其他能力组件，例如： */}
      {/* <DataAnalysisOperation /> */}
      {/* <WorkflowOperation /> */}
    </div>
  );
};

export default AssistantOperationArea;
