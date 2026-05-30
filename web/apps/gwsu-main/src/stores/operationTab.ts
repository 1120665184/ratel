/**
 * 操作区 Tab 状态管理
 *
 * 管理操作区的 Tab 切换状态：
 * - interface: 界面展示（路由页面）
 * - ai-output: AI 输出（HTML 渲染）
 * - settings: 设置面板
 */

import { create } from 'zustand';

/** 操作区 Tab 类型 */
export type OperationTab = 'interface' | 'ai-output' | 'settings';

interface OperationTabState {
  /** 当前激活的 Tab */
  activeTab: OperationTab;
  /** 切换 Tab */
  setActiveTab: (tab: OperationTab) => void;
  /** 切换到界面 Tab */
  switchToInterface: () => void;
  /** 切换到 AI 输出 Tab */
  switchToAiOutput: () => void;
  /** 切换到设置 Tab */
  switchToSettings: () => void;
}

export const useOperationTabStore = create<OperationTabState>((set) => ({
  activeTab: 'interface',
  setActiveTab: (tab) => set({ activeTab: tab }),
  switchToInterface: () => set({ activeTab: 'interface' }),
  switchToAiOutput: () => set({ activeTab: 'ai-output' }),
  switchToSettings: () => set({ activeTab: 'settings' }),
}));
