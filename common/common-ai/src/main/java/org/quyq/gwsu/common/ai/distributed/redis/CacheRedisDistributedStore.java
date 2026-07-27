package org.quyq.gwsu.common.ai.distributed.redis;

import io.agentscope.core.state.AgentStateStore;
import io.agentscope.harness.agent.DistributedStore;
import io.agentscope.harness.agent.filesystem.remote.store.BaseStore;
import io.agentscope.harness.agent.sandbox.SandboxExecutionGuard;
import io.agentscope.harness.agent.sandbox.snapshot.SandboxSnapshotSpec;
import org.quyq.gwsu.common.cache.utils.CacheUtils;

import java.util.Objects;

/**
 * common-ai 自定义 Redis DistributedStore。
 */
public class CacheRedisDistributedStore implements DistributedStore {

    private final AgentStateStore agentStateStore;
    private final BaseStore baseStore;
    private final SandboxSnapshotSpec sandboxSnapshotSpec;
    private final SandboxExecutionGuard sandboxExecutionGuard;

    public CacheRedisDistributedStore(AgentStateStore agentStateStore, CacheUtils cacheUtils) {
        this.agentStateStore = Objects.requireNonNull(agentStateStore, "agentStateStore cannot be null");
        Objects.requireNonNull(cacheUtils, "cacheUtils cannot be null");
        this.baseStore = new CacheRedisBaseStore(cacheUtils);
        this.sandboxSnapshotSpec = new CacheRedisSandboxSnapshotSpec(cacheUtils);
        this.sandboxExecutionGuard = new CacheRedisSandboxExecutionGuard(cacheUtils);
    }

    @Override
    public AgentStateStore agentStateStore() {
        return agentStateStore;
    }

    @Override
    public BaseStore baseStore() {
        return baseStore;
    }

    @Override
    public SandboxSnapshotSpec sandboxSnapshotSpec() {
        return sandboxSnapshotSpec;
    }

    @Override
    public SandboxExecutionGuard sandboxExecutionGuard() {
        return sandboxExecutionGuard;
    }
}
