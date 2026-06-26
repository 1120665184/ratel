package org.quyq.gwsu.headless.core.pool;

/**
 * 缓存 Session 的状态枚举
 * <p>
 * ACTIVE  - 正在执行操作（sendMessage/approval/userAnswer）
 * IDLE    - 操作完成，等待复用
 * EVICTING - 正在被销毁中（防止并发销毁）
 */
public enum SessionState {
    ACTIVE,
    IDLE,
    EVICTING
}
