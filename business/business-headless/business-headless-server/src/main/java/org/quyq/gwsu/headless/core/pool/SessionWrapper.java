package org.quyq.gwsu.headless.core.pool;

import org.quyq.gwsu.headless.core.session.HeadlessBrowserSession;

import java.time.Instant;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 缓存 Session 包装器
 * <p>
 * 封装 HeadlessBrowserSession 的状态、时间信息和并发控制锁，
 * 用于 BrowserContextPool 的 sessionCache 管理。
 */
public class SessionWrapper {

    private final HeadlessBrowserSession session;
    private final String userId;
    private volatile SessionState state;
    private volatile Instant lastActiveTime;
    private final Instant createTime;
    private final ReentrantLock stateLock = new ReentrantLock();

    public SessionWrapper(HeadlessBrowserSession session, String userId) {
        this.session = session;
        this.userId = userId;
        this.state = SessionState.ACTIVE;
        this.lastActiveTime = Instant.now();
        this.createTime = Instant.now();
    }

    public HeadlessBrowserSession getSession() {
        return session;
    }

    public String getUserId() {
        return userId;
    }

    public SessionState getState() {
        return state;
    }

    public Instant getLastActiveTime() {
        return lastActiveTime;
    }

    public Instant getCreateTime() {
        return createTime;
    }

    public ReentrantLock getStateLock() {
        return stateLock;
    }

    /**
     * 安全地变更状态，同时更新 lastActiveTime
     * 必须在持有 stateLock 的情况下调用
     */
    public void transitionTo(SessionState newState) {
        this.state = newState;
        if (newState == SessionState.ACTIVE) {
            this.lastActiveTime = Instant.now();
        }
    }

    /**
     * 计算沉寂时长（秒）
     */
    public long idleDurationSeconds() {
        if (state == SessionState.IDLE) {
            return Instant.now().getEpochSecond() - lastActiveTime.getEpochSecond();
        }
        return 0;
    }

    /**
     * 检查是否沉寂超过指定分钟数
     */
    public boolean isIdleLongerThan(int minutes) {
        return state == SessionState.IDLE && idleDurationSeconds() >= minutes * 60L;
    }
}
