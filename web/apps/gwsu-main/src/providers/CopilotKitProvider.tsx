import {CopilotKit} from '@copilotkit/react-core';
import {useUserStore} from '@gwsu/core';
import type {ReactNode} from 'react';

interface GwsuCopilotKitProviderProps {
  children: ReactNode;
}

/**
 * CopilotKit Provider 封装
 * 使用 HttpAgent 直接连接到后端 AG-UI 接口
 * 注意：agents__unsafe_dev_only 仅用于开发环境，生产环境需要使用 CopilotRuntime
 */
export function GwsuCopilotKitProvider({children}: GwsuCopilotKitProviderProps) {


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
      {children}
    </CopilotKit>
  );
}
