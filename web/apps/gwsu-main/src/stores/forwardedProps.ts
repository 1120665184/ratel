/**
 * 传递到 AG-UI forwardedProps 的动态参数
 * 通过 Zustand 管理，路由/操作模式变化时自动同步到 CopilotKit
 */

import { create } from 'zustand';

/** 操作模式：人类操作 / AI操作 */
export type OperationMode = 'human' | 'ai';

interface ForwardedPropsState {
  /** 当前路由路径 */
  currentPath: string;
  /** 操作模式：human=人类操作, ai=AI操作，默认人类 */
  operationMode: OperationMode;
  /**
   * 扩展属性
   */
  extras: Record<string, any>;
  /** 更新路由路径 */
  setCurrentPath: (path: string) => void;
  /** 设置操作模式 */
  setOperationMode: (mode: OperationMode) => void;
  /** 更新扩展参数 */
  setExtras: (extras: Record<string, any>) => void;
}

export const useForwardedPropsStore = create<ForwardedPropsState>((set: any) => ({
  currentPath: '/',
  operationMode: 'human',
  extras: {},
  setCurrentPath: (path: string) => set({ currentPath: path }),
  setOperationMode: (mode: OperationMode) => set({ operationMode: mode }),
  setExtras: (extras) => set({ extras }),
}));
