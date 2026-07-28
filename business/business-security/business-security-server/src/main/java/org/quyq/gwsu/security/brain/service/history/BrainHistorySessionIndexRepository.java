package org.quyq.gwsu.security.brain.service.history;

import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.ai.constants.AIConstants;
import org.quyq.gwsu.common.cache.utils.CacheUtils;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 基于 Redis 的历史会话索引仓储。
 */
@Repository
@RequiredArgsConstructor
public class BrainHistorySessionIndexRepository {

    private final CacheUtils cacheUtils;
    private final ObjectMapper objectMapper;

    public void save(String userId, BrainHistorySessionIndexEntry entry) {
        if (entry == null || entry.getSessionId() == null || entry.getSessionId().isBlank()) {
            return;
        }
        try {
            cacheUtils.zAdd(indexKey(userId), entry.getSessionId(), score(entry.getUpdatedAt()));
            cacheUtils.set(detailKey(userId, entry.getSessionId()), objectMapper.writeValueAsString(entry));
        } catch (Exception exception) {
            throw new IllegalStateException("保存历史会话索引失败，sessionId=%s".formatted(entry.getSessionId()), exception);
        }
    }

    public List<BrainHistorySessionIndexEntry> page(String userId, long offset, int limit) {
        if (limit <= 0) {
            return List.of();
        }
        Set<String> sessionIds = cacheUtils.zReverseRange(indexKey(userId), offset, offset + limit - 1L);
        if (CollectionUtils.isEmpty(sessionIds)) {
            return List.of();
        }

        List<BrainHistorySessionIndexEntry> result = new ArrayList<>(sessionIds.size());
        Set<String> missingIds = new LinkedHashSet<>();
        for (String sessionId : sessionIds) {
            BrainHistorySessionIndexEntry entry = get(userId, sessionId);
            if (entry == null) {
                missingIds.add(sessionId);
                continue;
            }
            result.add(entry);
        }

        if (!missingIds.isEmpty()) {
            cacheUtils.zRemove(indexKey(userId), missingIds.toArray());
        }
        return result;
    }

    public BrainHistorySessionIndexEntry get(String userId, String sessionId) {
        try {
            String raw = cacheUtils.get(detailKey(userId, sessionId));
            if (raw == null || raw.isBlank()) {
                return null;
            }
            return objectMapper.readValue(raw, BrainHistorySessionIndexEntry.class);
        } catch (Exception exception) {
            throw new IllegalStateException("读取历史会话索引失败，sessionId=%s".formatted(sessionId), exception);
        }
    }

    public void delete(String userId, String sessionId) {
        cacheUtils.zRemove(indexKey(userId), sessionId);
        cacheUtils.delete(detailKey(userId, sessionId));
    }

    private String indexKey(String userId) {
        return AIConstants.DistributedStoreRedis.HISTORY_INDEX_KEY_TEMPLATE.formatted(Objects.requireNonNullElse(userId, ""));
    }

    private String detailKey(String userId, String sessionId) {
        return AIConstants.DistributedStoreRedis.HISTORY_DETAIL_KEY_TEMPLATE.formatted(
                Objects.requireNonNullElse(userId, ""),
                sessionId);
    }

    private double score(String updatedAt) {
        if (updatedAt == null || updatedAt.isBlank()) {
            return 0D;
        }
        try {
            return java.time.Instant.parse(updatedAt).toEpochMilli();
        } catch (Exception ignored) {
            return 0D;
        }
    }
}
