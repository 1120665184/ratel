import { CopilotKit, useRenderToolCall } from '@copilotkit/react-core';
import { useAgent } from '@copilotkit/react-core/v2';
import { useUserStore } from '@gwsu/core';
import type { ReactNode } from 'react';
import { useEffect, useRef } from 'react';
import { dispatchWebTool } from '@/services/web-tool';
import type { WebToolExecutePayload } from '@/services/web-tool';
import { dispatchHumanApproval } from '@/services/human-approval';
import type { HumanApprovalPayload } from '@/services/human-approval';
import { WebToolConfirmModal } from '@/services/web-tool/components/WebToolConfirmModal';
import { ToolCallItem } from '@/components/AIChat/ToolCallItem';
// 确保 route-navigation 工具被注册
import '@/services/web-tool/tools/route-navigation';
import { AgentSubscriber } from '@ag-ui/client';

interface GwsuCopilotKitProviderProps {
  children: ReactNode;
}

/**
 * 工具调用渲染注册组件
 * 注册通配符(*)渲染器，使所有工具调用在聊天面板中展示
 * 必须在 CopilotKit 内部使用
 */
function ToolCallRendererRegistration() {
  // @ts-ignore
  useRenderToolCall({
    name: '*',
    render: (props: any) => <ToolCallItem {...props} />,
  });
  return null;
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

    const subscriber: AgentSubscriber = {
      onCustomEvent: ({ event }):void => {
        //web工具调用
        if (event.name === 'TOOL_EXECUTE') {
          dispatchWebTool(event.value as WebToolExecutePayload);
        }
        //人工干预审批
        else if (event.name === 'HUMAN_APPROVAL') {
          dispatchHumanApproval(event.value as HumanApprovalPayload);
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
      <ToolCallRendererRegistration />
      <WebToolEventListener />
      <WebToolConfirmModal />
      {children}
    </CopilotKit>
  );
}
