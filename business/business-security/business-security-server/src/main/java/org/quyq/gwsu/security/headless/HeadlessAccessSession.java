package org.quyq.gwsu.security.headless;

import java.io.Serializable;

/**
 * 无头浏览器分布式会话信息
 * <p>
 * 存储在 Redis 中，key 为 headless_access_session:{userId}
 * 支持分布式场景下不同服务器共享会话状态
 *
 * @param userId   用户ID
 * @param token    登录 token（认证成功后存储）
 * @param threadId 智能助手聊天线程ID（可能为空，首次监听到后更新）
 */
public record HeadlessAccessSession(
        String userId,
        String token,
        String threadId
) implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Hash 字段名 */
    public static final String HASH_KEY_USER_ID = "userId";
    public static final String HASH_KEY_TOKEN = "token";
    public static final String HASH_KEY_THREAD_ID = "threadId";

    /**
     * 从 Redis Hash 数据构建实例
     */
    public static HeadlessAccessSession fromMap(java.util.Map<String, String> map) {
        if (map == null || map.isEmpty()) return null;
        return new HeadlessAccessSession(
                map.get(HASH_KEY_USER_ID),
                map.get(HASH_KEY_TOKEN),
                map.get(HASH_KEY_THREAD_ID)
        );
    }

    /**
     * 转换为 Redis Hash 数据
     */
    public java.util.Map<String, String> toMap() {
        java.util.Map<String, String> map = new java.util.LinkedHashMap<>();
        if (userId != null) map.put(HASH_KEY_USER_ID, userId);
        if (token != null) map.put(HASH_KEY_TOKEN, token);
        if (threadId != null) map.put(HASH_KEY_THREAD_ID, threadId);
        return map;
    }

    /**
     * 创建带 threadId 的新实例
     */
    public HeadlessAccessSession withThreadId(String newThreadId) {
        return new HeadlessAccessSession(this.userId, this.token, newThreadId);
    }

    /**
     * 创建带 token 的新实例
     */
    public HeadlessAccessSession withToken(String newToken) {
        return new HeadlessAccessSession(this.userId, newToken, this.threadId);
    }
}
