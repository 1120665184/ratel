package org.quyq.gwsu.common.ai.distributed.redis;

import io.agentscope.harness.agent.sandbox.snapshot.RemoteSnapshotClient;
import org.quyq.gwsu.common.ai.AgentException;
import org.quyq.gwsu.common.ai.constants.AIConstants;
import org.quyq.gwsu.common.cache.utils.CacheUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Base64;
import java.util.Objects;

/**
 * 基于 Redis 的远程快照客户端。
 */
public class CacheRedisRemoteSnapshotClient implements RemoteSnapshotClient {

    private final CacheUtils cacheUtils;

    public CacheRedisRemoteSnapshotClient(CacheUtils cacheUtils) {
        this.cacheUtils = Objects.requireNonNull(cacheUtils, "cacheUtils cannot be null");
    }

    @Override
    public void upload(String snapshotId, InputStream data) throws Exception {
        byte[] bytes = data.readAllBytes();
        cacheUtils.set(
                snapshotKey(snapshotId),
                Base64.getEncoder().encodeToString(bytes),
                AIConstants.DistributedStoreRedis.SNAPSHOT_TTL);
    }

    @Override
    public InputStream download(String snapshotId) {
        String content = cacheUtils.get(snapshotKey(snapshotId));
        if (content == null) {
            throw new AgentException("Snapshot not found: " + snapshotId);
        }
        return new ByteArrayInputStream(Base64.getDecoder().decode(content));
    }

    @Override
    public boolean exists(String snapshotId) {
        return Boolean.TRUE.equals(cacheUtils.exists(snapshotKey(snapshotId)));
    }

    private String snapshotKey(String snapshotId) {
        if (snapshotId == null || snapshotId.isBlank()) {
            throw new AgentException("snapshotId cannot be blank");
        }
        return AIConstants.DistributedStoreRedis.SNAPSHOT_KEY_TEMPLATE.formatted(snapshotId);
    }
}
