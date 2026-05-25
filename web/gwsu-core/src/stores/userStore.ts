/**
 * 用户状态管理
 * 统一管理 Token 和用户信息，自动同步 localStorage
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
const STORE_KEY = '__GWSU_USER_STORE__';

/** Token 信息 */
export interface TokenInfo {
    /** 登录 token */
    token: string;
    /** 用户 ID */
    userId: number;
    /** 有效期（秒） */
    expires: number;
    /** 过期时间戳（毫秒） */
    expireTime?: number;
}

/** 用户所属部门 */
export interface UserDept {
    /** 关联记录 ID */
    id?: string;
    /** 部门 ID */
    deptId: string;
    /** 部门名称 */
    deptName: string;
    /** 是否主部门 */
    isPrimary?: boolean;
}

/** 用户信息 */
export interface UserInfo {
    /** 用户 ID */
    userId: number;
    /** 用户名 */
    username: string;
    /** 昵称 */
    nickname?: string;
    /** 头像 */
    avatar?: string;
    /** 邮箱 */
    email?: string;
    /** 手机号 */
    phone?: string;
    /** 性别 */
    gender?: number;
    /** 状态 */
    status?: number;
    /** 部门 ID（主部门） */
    deptId?: number;
    /** 部门名称（主部门） */
    deptName?: string;
    /** 所属部门列表 */
    depts?: UserDept[];
    /** 角色列表 */
    roles?: string[];
    /** 权限列表 */
    permissions?: string[];
    /** 扩展数据 */
    extraData?: Record<string, unknown>;
}

/** localStorage 键名 */
const STORAGE_KEYS = {
    TOKEN: 'gwsu_token',
    USER: 'gwsu_user',
    IS_LOGGED_IN: 'gwsu_isLoggedIn',
} as const;

interface UserState {
    /** Token 信息 */
    tokenInfo: TokenInfo | null;
    /** 用户信息 */
    userInfo: UserInfo | null;
    /** 是否已登录 */
    isLoggedIn: boolean;

    /** 设置 Token 信息（同步到 localStorage） */
    setTokenInfo: (tokenInfo: TokenInfo | null) => void;
    /** 获取 Token 信息（优先从内存，否则从 localStorage） */
    getTokenInfo: () => TokenInfo | null;
    /** 设置用户信息（同步到 localStorage） */
    setUserInfo: (userInfo: UserInfo | null) => void;
    /** 获取用户信息（优先从内存，否则从 localStorage） */
    getUserInfo: () => UserInfo | null;
    /** 检查是否已登录 */
    checkLogin: () => boolean;
    /** 检查 Token 是否过期 */
    isTokenExpired: () => boolean;
    /** 清除所有用户数据 */
    clearUserData: () => void;
    /** 登出 */
    logout: () => void;
}

/**
 * 从 localStorage 读取 Token 信息
 */
function loadTokenFromStorage(): TokenInfo | null {
    try {
        const tokenStr = localStorage.getItem(STORAGE_KEYS.TOKEN);
        if (!tokenStr) return null;
        return JSON.parse(tokenStr) as TokenInfo;
    } catch {
        return null;
    }
}

/**
 * 从 localStorage 读取用户信息
 */
function loadUserFromStorage(): UserInfo | null {
    try {
        const userStr = localStorage.getItem(STORAGE_KEYS.USER);
        if (!userStr) return null;
        return JSON.parse(userStr) as UserInfo;
    } catch {
        return null;
    }
}

/**
 * 保存 Token 到 localStorage
 */
function saveTokenToStorage(tokenInfo: TokenInfo | null): void {
    if (tokenInfo) {
        localStorage.setItem(STORAGE_KEYS.TOKEN, JSON.stringify(tokenInfo));
        localStorage.setItem(STORAGE_KEYS.IS_LOGGED_IN, 'true');
    } else {
        localStorage.removeItem(STORAGE_KEYS.TOKEN);
        localStorage.removeItem(STORAGE_KEYS.IS_LOGGED_IN);
    }
}

/**
 * 保存用户信息到 localStorage
 */
function saveUserToStorage(userInfo: UserInfo | null): void {
    if (userInfo) {
        localStorage.setItem(STORAGE_KEYS.USER, JSON.stringify(userInfo));
    } else {
        localStorage.removeItem(STORAGE_KEYS.USER);
    }
}

/**
 * 创建或获取单例 vanilla Store
 * 通过真实 window 对象挂载，确保主应用和子应用共享同一个 Zustand 实例
 * 使用 vanilla store（无 React 依赖），避免多 React 实例冲突
 */
function createOrGetStore(): StoreApi<UserState> {
    if (rawWindow[STORE_KEY]) {
        return rawWindow[STORE_KEY] as StoreApi<UserState>;
    }

    // 从 localStorage 恢复初始状态，避免页面刷新后丢失
    const initialToken = loadTokenFromStorage();
    const initialUser = loadUserFromStorage();

    const store = createStore<UserState>((set, get) => ({
        tokenInfo: initialToken,
        userInfo: initialUser,
        isLoggedIn: !!initialToken,

        setTokenInfo: (tokenInfo) => {
            saveTokenToStorage(tokenInfo);
            set({
                tokenInfo,
                isLoggedIn: !!tokenInfo,
            });
        },

        getTokenInfo: () => {
            const {tokenInfo} = get();
            if (tokenInfo) return tokenInfo;

            // 从 localStorage 加载
            const storedToken = loadTokenFromStorage();
            if (storedToken) {
                set({tokenInfo: storedToken, isLoggedIn: true});
            }
            return storedToken;
        },

        setUserInfo: (userInfo) => {
            saveUserToStorage(userInfo);
            set({userInfo});
        },

        getUserInfo: () => {
            const {userInfo} = get();
            if (userInfo) return userInfo;

            // 从 localStorage 加载
            const storedUser = loadUserFromStorage();
            if (storedUser) {
                set({userInfo: storedUser});
            }
            return storedUser;
        },

        checkLogin: () => {
            const {tokenInfo, isLoggedIn} = get();
            if (isLoggedIn && tokenInfo) return true;

            // 检查 localStorage
            const storedToken = loadTokenFromStorage();
            if (storedToken) {
                set({tokenInfo: storedToken, isLoggedIn: true});
                return true;
            }
            return false;
        },

        isTokenExpired: () => {
            const tokenInfo = get().getTokenInfo();
            if (!tokenInfo || !tokenInfo.expireTime) return true;
            return Date.now() >= tokenInfo.expireTime;
        },

        clearUserData: () => {
            localStorage.removeItem(STORAGE_KEYS.TOKEN);
            localStorage.removeItem(STORAGE_KEYS.USER);
            localStorage.removeItem(STORAGE_KEYS.IS_LOGGED_IN);
            set({
                tokenInfo: null,
                userInfo: null,
                isLoggedIn: false,
            });
        },

        logout: () => {
            get().clearUserData();
        },
    }));

    rawWindow[STORE_KEY] = store;
    return store;
}

const vanillaStore = createOrGetStore();

/**
 * React Hook — 用户状态管理
 *
 * 每个应用导入此 Hook 时，使用各自的 React 实例创建绑定，
 * 但底层共享同一个 vanilla store，确保状态跨应用同步。
 */
function useUserStore(): UserState;
function useUserStore<U>(selector: (state: UserState) => U): U;
function useUserStore<U>(selector?: (state: UserState) => U): UserState | U {
    return useStore(vanillaStore, selector as (state: UserState) => U);
}

// 挂载 vanilla store 方法，保持向后兼容（useUserStore.getState() 等）
useUserStore.getState = vanillaStore.getState;
useUserStore.setState = vanillaStore.setState;
useUserStore.subscribe = vanillaStore.subscribe;
useUserStore.getInitialState = vanillaStore.getInitialState;

export { useUserStore };

// 导出便捷方法（快照，非响应式）
export const userStore = vanillaStore.getState();
