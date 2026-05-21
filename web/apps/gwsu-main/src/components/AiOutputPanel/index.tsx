import { RobotOutlined } from '@ant-design/icons';
import { useEffect, useRef, useState } from 'react';
import styles from './index.module.less';

/**
 * AI 输出面板组件
 * 用于展示智能助手输出的 HTML 内容
 * 组件不会被销毁，切换 Tab 时仅隐藏
 */
const AiOutputPanel: React.FC = () => {
  const iframeRef = useRef<HTMLIFrameElement>(null);
  const [hasContent, setHasContent] = useState(false);

  // 监听 AI 输出事件，将 HTML 渲染到 iframe 中
  useEffect(() => {
    const handler = (e: MessageEvent) => {
      if (e.data?.type === 'AI_HTML_OUTPUT') {
        const html = e.data.payload as string;
        if (html && iframeRef.current) {
          const doc = iframeRef.current.contentDocument;
          if (doc) {
            doc.open();
            doc.write(html);
            doc.close();
            setHasContent(true);
          }
        }
      }
    };
    window.addEventListener('message', handler);
    return () => window.removeEventListener('message', handler);
  }, []);

  // 清空输出
  // const handleClear = useCallback(() => {
  //   if (iframeRef.current) {
  //     const doc = iframeRef.current.contentDocument;
  //     if (doc) {
  //       doc.open();
  //       doc.write('');
  //       doc.close();
  //       setHasContent(false);
  //     }
  //   }
  // }, []);

  return (
    <div className={styles.aiOutputPanel}>
      <iframe
        ref={iframeRef}
        className={styles.iframe}
        title="AI 输出内容"
        sandbox="allow-scripts allow-same-origin"
      />
      {!hasContent && (
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
