import { create } from 'zustand';
import type { ViewConfig } from '@/components/AIChat/types';

interface ViewConfigState extends ViewConfig {
  /** 设置展示配置 */
  setViewConfig: (config: Partial<ViewConfig>) => void;
}

const DEFAULT_VIEW_CONFIG: ViewConfig = {
  showThinking: true,
  showToolCalls: true,
  showHistory: true,
  enableDragMode: false,
};

export const useViewConfigStore = create<ViewConfigState>((set) => ({
  ...DEFAULT_VIEW_CONFIG,
  setViewConfig: (config) => set((state) => ({ ...state, ...config })),
}));
