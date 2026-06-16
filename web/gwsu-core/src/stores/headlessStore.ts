/**
 * 无头浏览器状态管理
 * 管理无头浏览器会话中的 threadId，用于恢复历史聊天记录
 *
 * 使用 vanilla store 共享状态（无 React 依赖），每个应用用自己的 React 创建 Hook 绑定。
 * 这样避免了 qiankun 微前端中多 React 实例导致 "Invalid hook call" 的问题。
 */

import { createStore } from 'zustand/vanilla';
import type { StoreApi } from 'zustand/vanilla';
import { useStore } from 'zustand';

/**
 * 获取真实 window 对象，绕过 qiankun JS 沙箱的 Proxy 代理
 * 确保主应用和子应用共享同一个全局状态
 */
const rawWindow: Window & typeof globalThis & Record<string, unknown> = (0, eval)('window');

/** Store 在真实 window 上的挂载键 */
const STORE_KEY = '__GWSU_HEADLESS_STORE__';

/** localStorage 键名 */
const STORAGE_KEY = 'gwsu_headless_thread_id';

interface HeadlessState {
  /** 无头浏览器会话的 threadId，用于恢复聊天记录 */
  threadId: string | null;
  /** 是否为无头浏览器模式 */
  isHeadless: boolean;
  /** 设置 threadId（同步到 localStorage） */
  setThreadId: (threadId: string | null) => void;
  /** 获取 threadId（优先从内存，否则从 localStorage） */
  getThreadId: () => string | null;
  /** 清除 threadId（同步清除 localStorage） */
  clearThreadId: () => void;
  /** 设置是否为无头浏览器模式 */
  setHeadless: (isHeadless: boolean) => void;
}

/**
 * 从 localStorage 读取 threadId
 */
function loadThreadIdFromStorage(): string | null {
  try {
    return localStorage.getItem(STORAGE_KEY);
  } catch {
    return null;
  }
}

/**
 * 保存 threadId 到 localStorage
 */
function saveThreadIdToStorage(threadId: string | null): void {
  if (threadId) {
    localStorage.setItem(STORAGE_KEY, threadId);
  } else {
    localStorage.removeItem(STORAGE_KEY);
  }
}

/**
 * 创建或获取单例 vanilla Store
 * 通过真实 window 对象挂载，确保主应用和子应用共享同一个 Zustand 实例
 */
function createOrGetStore(): StoreApi<HeadlessState> {
  if (rawWindow[STORE_KEY]) {
    return rawWindow[STORE_KEY] as StoreApi<HeadlessState>;
  }

  const initialThreadId = loadThreadIdFromStorage();

  const store = createStore<HeadlessState>((set, get) => ({
    threadId: initialThreadId,
    isHeadless: false,

    setThreadId: (threadId) => {
      saveThreadIdToStorage(threadId);
      set({ threadId });
    },

    getThreadId: () => {
      const { threadId } = get();
      if (threadId) return threadId;
      const stored = loadThreadIdFromStorage();
      if (stored) {
        set({ threadId: stored });
      }
      return stored;
    },

    clearThreadId: () => {
      localStorage.removeItem(STORAGE_KEY);
      set({ threadId: null });
    },

    setHeadless: (isHeadless) => {
      set({ isHeadless });
    },
  }));

  rawWindow[STORE_KEY] = store;
  return store;
}

const vanillaStore = createOrGetStore();

/**
 * React Hook — 无头浏览器状态管理
 */
function useHeadlessStore(): HeadlessState;
function useHeadlessStore<U>(selector: (state: HeadlessState) => U): U;
function useHeadlessStore<U>(selector?: (state: HeadlessState) => U): HeadlessState | U {
  return useStore(vanillaStore, selector as (state: HeadlessState) => U);
}

// 挂载 vanilla store 方法
useHeadlessStore.getState = vanillaStore.getState;
useHeadlessStore.setState = vanillaStore.setState;
useHeadlessStore.subscribe = vanillaStore.subscribe;
useHeadlessStore.getInitialState = vanillaStore.getInitialState;

export { useHeadlessStore };
