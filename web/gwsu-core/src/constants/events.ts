/**
 * 全局事件类型枚举
 * 整个项目的事件都在此定义，确保类型安全
 */
export enum EventType {
    /** Token 过期，需要重新登录 */
    TOKEN_EXPIRED = 'TOKEN_EXPIRED',

    /** 登录成功 */
    LOGIN_SUCCESS = 'LOGIN_SUCCESS',

    /** 退出登录 */
    LOGOUT = 'LOGOUT',

    /** 主题变更 */
    THEME_CHANGE = 'THEME_CHANGE',

}

/**
 * 事件消息结构
 */
export interface EventMessage<T = unknown> {
    /** 事件类型 */
    type: EventType;
    /** 事件数据 */
    payload?: T;
    /** 时间戳 */
    timestamp?: number;
}

/**
 * 发送全局事件
 */
export function emitEvent<T = unknown>(type: EventType, payload?: T): void {
    const message: EventMessage<T> = {
        type,
        payload,
        timestamp: Date.now(),
    };
    window.postMessage(message, '*');
}

/**
 * 监听全局事件
 */
export function onEvent(
    type: EventType,
    handler: (payload: unknown) => void
): () => void {
    const listener = (event: MessageEvent) => {
        if (event.data && event.data.type === type) {
            handler(event.data.payload);
        }
    };

    window.addEventListener('message', listener);

    // 返回取消监听函数
    return () => {
        window.removeEventListener('message', listener);
    };
}
