package org.quyq.gwsu.headless.core.pool;

import lombok.Getter;
import org.quyq.gwsu.headless.core.session.HeadlessBrowserSession;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 缓存 Session 包装器
 * <p>
 * 封装 HeadlessBrowserSession 的状态和时间信息，使用 AtomicReference + CAS
 * 实现无锁状态转换，彻底避免虚拟线程环境下锁的线程归属问题。
 */
@Getter
public class SessionWrapper {

    private final HeadlessBrowserSession session;
    private final String userId;
    private final AtomicReference<SessionState> stateRef;
    private volatile Instant lastActiveTime;
    private final Instant createTime;

    public SessionWrapper(HeadlessBrowserSession session, String userId) {
        this.session = session;
        this.userId = userId;
        this.stateRef = new AtomicReference<>(SessionState.ACTIVE);
        this.lastActiveTime = Instant.now();
        this.createTime = Instant.now();
    }

    public SessionState getState() {
        return stateRef.get();
    }

    /**
     * CAS 状态转换：从期望状态转换到新状态
     *
     * @param expected 期望的当前状态
     * @param newState 目标状态
     * @return true=转换成功，false=当前状态不是 expected（并发冲突）
     */
    public boolean compareAndSetState(SessionState expected, SessionState newState) {
        boolean success = stateRef.compareAndSet(expected, newState);
        if (success && newState == SessionState.ACTIVE) {
            this.lastActiveTime = Instant.now();
        }
        return success;
    }

    /**
     * 强制设置状态（仅用于 EVICTING 等必须成功的场景）
     */
    public void setState(SessionState newState) {
        stateRef.set(newState);
        if (newState == SessionState.ACTIVE) {
            this.lastActiveTime = Instant.now();
        }
    }

    /**
     * 计算沉寂时长（秒）
     */
    public long idleDurationSeconds() {
        if (stateRef.get() == SessionState.IDLE) {
            return Instant.now().getEpochSecond() - lastActiveTime.getEpochSecond();
        }
        return 0;
    }

    /**
     * 检查是否沉寂超过指定分钟数
     */
    public boolean isIdleLongerThan(int minutes) {
        return stateRef.get() == SessionState.IDLE && idleDurationSeconds() >= minutes * 60L;
    }
}
