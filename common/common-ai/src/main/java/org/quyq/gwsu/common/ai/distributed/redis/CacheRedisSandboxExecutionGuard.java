package org.quyq.gwsu.common.ai.distributed.redis;

import io.agentscope.harness.agent.sandbox.SandboxExecutionGuard;
import io.agentscope.harness.agent.sandbox.SandboxIsolationKey;
import io.agentscope.harness.agent.sandbox.SandboxLease;
import org.quyq.gwsu.common.ai.AgentException;
import org.quyq.gwsu.common.ai.constants.AIConstants;
import org.quyq.gwsu.common.cache.utils.CacheUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * 基于 Redis 的 Sandbox 执行并发控制。
 */
public class CacheRedisSandboxExecutionGuard implements SandboxExecutionGuard {

    private static final Logger log = LoggerFactory.getLogger(CacheRedisSandboxExecutionGuard.class);

    private final CacheUtils cacheUtils;

    public CacheRedisSandboxExecutionGuard(CacheUtils cacheUtils) {
        this.cacheUtils = Objects.requireNonNull(cacheUtils, "cacheUtils cannot be null");
    }

    @Override
    public SandboxLease tryEnter(SandboxIsolationKey key) throws InterruptedException {
        Objects.requireNonNull(key, "key cannot be null");
        String lockKey = composeKey(key);
        String token = UUID.randomUUID().toString();

        while (true) {
            Boolean acquired = cacheUtils.setIfAbsent(lockKey, token, AIConstants.DistributedStoreRedis.LOCK_TTL);
            if (Boolean.TRUE.equals(acquired)) {
                return () -> release(lockKey, token);
            }
            Thread.sleep(AIConstants.DistributedStoreRedis.LOCK_RETRY_INTERVAL.toMillis());
        }
    }

    private void release(String lockKey, String token) {
        try {
            cacheUtils.deleteIfEquals(lockKey, token);
        } catch (Exception exception) {
            log.warn("Failed to release sandbox lock: {}", lockKey, exception);
        }
    }

    private String composeKey(SandboxIsolationKey key) {
        String scope = key.getScope().name().toLowerCase(Locale.ROOT);
        String value = key.getValue();
        if (value == null || value.isBlank()) {
            throw new AgentException("sandbox isolation value cannot be blank");
        }
        return AIConstants.DistributedStoreRedis.LOCK_KEY_TEMPLATE.formatted(scope, value);
    }
}
