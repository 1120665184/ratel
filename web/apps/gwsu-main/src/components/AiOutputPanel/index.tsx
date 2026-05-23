import { RobotOutlined } from '@ant-design/icons';
import { useEffect, useState, useRef, useCallback } from 'react';
import { Renderer } from '@json-render/react';
import { createSpecStreamCompiler } from '@json-render/core';
import type { Spec } from '@json-render/core';
import { registry } from './registry';
import { onAgentOutput, clearAgentOutput } from '@/services/agent-output';
import styles from './index.module.less';

/**
 * AI 输出面板组件
 * 使用 json-render Renderer 流式渲染 AI 输出的可视化内容
 * 组件不会被销毁，切换 Tab 时仅隐藏
 */
const AiOutputPanel: React.FC = () => {
  const [spec, setSpec] = useState<Spec | null>(null);
  const [isStreaming, setIsStreaming] = useState(false);
  const [hasContent, setHasContent] = useState(false);
  const compilerRef = useRef<ReturnType<typeof createSpecStreamCompiler> | null>(null);

  // 确保编译器只创建一次
  if (!compilerRef.current) {
    compilerRef.current = createSpecStreamCompiler();
  }

  const handleClear = useCallback(() => {
    compilerRef.current = createSpecStreamCompiler();
    setSpec(null);
    setHasContent(false);
    setIsStreaming(false);
    clearAgentOutput();
  }, []);

  useEffect(() => {
    const unsubscribe = onAgentOutput(({ text }) => {
      if (!text) {
        handleClear();
        return;
      }

      setIsStreaming(true);

      try {
        // 尝试作为完整 spec JSON 解析
        const parsed = JSON.parse(text);
        if (parsed && typeof parsed === 'object' && parsed.root && parsed.elements) {
          compilerRef.current = createSpecStreamCompiler();
          setSpec(parsed);
          setHasContent(true);
          setIsStreaming(false);
          return;
        }
      } catch {
        // 不是完整 JSON，尝试作为 JSONL patch
      }

      // 尝试作为 JSONL patch 行处理
      try {
        const { result } = compilerRef.current.push(text);
        if (result && result.root && Object.keys(result.elements || {}).length > 0) {
          setSpec({ ...result });
          setHasContent(true);
        }
      } catch {
        // patch 解析失败，忽略
      }
    });

    return unsubscribe;
  }, [handleClear]);

  return (
    <div className={styles.aiOutputPanel}>
      {hasContent && spec && (
        <Renderer spec={spec} registry={registry} />
      )}
      {isStreaming && (
        <div className={styles.loadingIndicator}>
          <span className={styles.loadingDot} />
          生成中...
        </div>
      )}
      {!hasContent && !isStreaming && (
        <div className={styles.emptyState}>
          <RobotOutlined className={styles.emptyIcon} />
          <div className={styles.emptyTitle}>AI 输出区</div>
          <div className={styles.emptySubtitle}>智能助手的输出结果将在此展示</div>
        </div>
      )}
    </div>
  );
};

export default AiOutputPanel;
