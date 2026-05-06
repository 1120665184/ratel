import { CopilotKit } from '@copilotkit/react-core';
import { useAgent } from '@copilotkit/react-core/v2';
import { useUserStore } from '@gwsu/core';
import type { ReactNode } from 'react';
import { useEffect, useRef } from 'react';
import { dispatchWebTool } from '@/services/web-tool';
import type { WebToolExecutePayload } from '@/services/web-tool';
import { WebToolConfirmModal } from '@/services/web-tool/components/WebToolConfirmModal';
// 确保 route-navigation 工具被注册
import '@/services/web-tool/tools/route-navigation';

interface GwsuCopilotKitProviderProps {
  children: ReactNode;
}

/**
 * Agent CUSTOM 事件订阅组件
 * 必须在 CopilotKit 内部使用，因为需要 access to agent context
 */
function WebToolEventListener() {
  const { agent } = useAgent({ agentId: 'brain' });
  const subscriptionRef = useRef<ReturnType<typeof agent.subscribe> | null>(null);

  useEffect(() => {
    if (!agent) return;

    // 清理旧的订阅
    if (subscriptionRef.current) {
      subscriptionRef.current.unsubscribe();
    }

    const subscriber = {
      onCustomEvent: ({ event }: { event: { name: string; value: unknown } }) => {
        if (event.name === 'TOOL_EXECUTE') {
          dispatchWebTool(event.value as WebToolExecutePayload);
        }
      },
    };

    const subscription = agent.subscribe(subscriber);
    subscriptionRef.current = subscription;

    return () => {
      subscription.unsubscribe();
      subscriptionRef.current = null;
    };
  }, [agent]);

  return null;
}

/**
 * CopilotKit Provider 封装
 * 使用 HttpAgent 直接连接到后端 AG-UI 接口
 */
export function GwsuCopilotKitProvider({ children }: GwsuCopilotKitProviderProps) {

  // 动态获取请求头
  const getHeaders = (): Record<string, string> => {
    const tokenInfo = useUserStore.getState().getTokenInfo();
    const headers: Record<string, string> = {};
    if (tokenInfo?.token) {
      headers['Authorization'] = `Bearer ${tokenInfo.token}`;
    }
    return headers;
  };

  return (
    <CopilotKit
      runtimeUrl="/api/security/brain/run/copilotKit"
      headers={getHeaders}
      agent="brain"
      enableInspector={false}
    >
      <WebToolEventListener />
      <WebToolConfirmModal />
      {children}
    </CopilotKit>
  );
}
