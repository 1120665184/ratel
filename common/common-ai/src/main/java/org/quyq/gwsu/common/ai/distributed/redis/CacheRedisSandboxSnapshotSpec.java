package org.quyq.gwsu.common.ai.distributed.redis;

import io.agentscope.harness.agent.sandbox.snapshot.RemoteSnapshotSpec;
import org.quyq.gwsu.common.cache.utils.CacheUtils;

/**
 * 基于 Redis 的 Sandbox 快照规范。
 */
public class CacheRedisSandboxSnapshotSpec extends RemoteSnapshotSpec {

    public CacheRedisSandboxSnapshotSpec(CacheUtils cacheUtils) {
        super(new CacheRedisRemoteSnapshotClient(cacheUtils));
    }
}
