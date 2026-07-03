import { createStore } from 'zustand/vanilla';
import type { StoreApi } from 'zustand/vanilla';
import { useStore } from 'zustand';
import { fetchConfigsBatch } from '../services/config';
import type { ConfigVO } from '../types/config';

const rawWindow: Window & typeof globalThis & Record<string, unknown> = (0, eval)('window');

const STORE_KEY = '__GWSU_PROJECT_CONFIG_STORE__';

/** 基础地址配置 key（与 GeneralConfigTab/types.ts 保持一致） */
const BASE_URL_CONFIG_KEY = 'basic_url_config';

interface BaseUrlConfig {
  projectName: string;
  viewBaseUrl: string;
  apiBaseUrl: string;
}

const DEFAULT_BASE_URL_CONFIG: BaseUrlConfig = {
  projectName: 'Ratel',
  viewBaseUrl: 'http://127.0.0.1:8000',
  apiBaseUrl: 'http://127.0.0.1:8888',
};

interface ProjectConfigState {
  /** 项目名称 */
  projectName: string;
  /** 基础地址配置 */
  baseUrlConfig: BaseUrlConfig;
  /** 是否已初始化 */
  initialized: boolean;

  /** 设置项目名称 */
  setProjectName: (name: string) => void;
  /** 设置基础地址配置 */
  setBaseUrlConfig: (config: BaseUrlConfig) => void;
  /** 从后端加载配置 */
  loadConfig: () => Promise<void>;
}

function createOrGetStore(): StoreApi<ProjectConfigState> {
  if (rawWindow[STORE_KEY]) {
    return rawWindow[STORE_KEY] as StoreApi<ProjectConfigState>;
  }

  const store = createStore<ProjectConfigState>((set) => ({
    projectName: DEFAULT_BASE_URL_CONFIG.projectName,
    baseUrlConfig: DEFAULT_BASE_URL_CONFIG,
    initialized: false,

    setProjectName: (name) => set({ projectName: name }),

    setBaseUrlConfig: (config) => set({
      baseUrlConfig: config,
      projectName: config.projectName,
    }),

    loadConfig: async () => {
      try {
        const configMap = await fetchConfigsBatch([BASE_URL_CONFIG_KEY]);
        const urlInfo = configMap[BASE_URL_CONFIG_KEY] as ConfigVO | undefined;
        if (urlInfo?.configValue) {
          try {
            const parsed = JSON.parse(urlInfo.configValue) as Partial<BaseUrlConfig>;
            const merged: BaseUrlConfig = { ...DEFAULT_BASE_URL_CONFIG, ...parsed };
            set({
              baseUrlConfig: merged,
              projectName: merged.projectName,
              initialized: true,
            });
          } catch {
            // 解析失败，使用默认值
            set({ initialized: true });
          }
        } else {
          set({ initialized: true });
        }
      } catch {
        // 请求失败，使用默认值
        set({ initialized: true });
      }
    },
  }));

  rawWindow[STORE_KEY] = store;
  return store;
}

const vanillaStore = createOrGetStore();

function useProjectConfigStore(): ProjectConfigState;
function useProjectConfigStore<U>(selector: (state: ProjectConfigState) => U): U;
function useProjectConfigStore<U>(selector?: (state: ProjectConfigState) => U): ProjectConfigState | U {
  return useStore(vanillaStore, selector as (state: ProjectConfigState) => U);
}

useProjectConfigStore.getState = vanillaStore.getState;
useProjectConfigStore.setState = vanillaStore.setState;
useProjectConfigStore.subscribe = vanillaStore.subscribe;
useProjectConfigStore.getInitialState = vanillaStore.getInitialState;

export { useProjectConfigStore };
