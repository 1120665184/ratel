/**
 * 用户状态管理
 * 统一管理 Token 和用户信息，自动同步 localStorage
 */

import {create, type UseBoundStore, type StoreApi} from 'zustand';

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
    /** 状态 */
    status?: number;
    /** 部门 ID */
    deptId?: number;
    /** 部门名称 */
    deptName?: string;
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
 * 创建或获取单例 Store
 * 通过真实 window 对象挂载，确保主应用和子应用共享同一个 Zustand 实例
 */

function createOrGetStore(): UseBoundStore<StoreApi<UserState>> {
    if (rawWindow[STORE_KEY]) {
        return rawWindow[STORE_KEY] as UseBoundStore<StoreApi<UserState>>;
    }

    const store = create<UserState>((set, get) => ({
        tokenInfo: null,
        userInfo: null,
        isLoggedIn: false,

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

export const useUserStore = createOrGetStore();

// 导出便捷方法
export const userStore = useUserStore.getState();
