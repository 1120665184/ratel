package org.quyq.gwsu.common.ai.distributed.redis;

import io.agentscope.core.state.AgentStateStore;
import io.agentscope.core.state.ListHashUtil;
import io.agentscope.core.state.State;
import io.agentscope.core.util.JsonUtils;
import org.quyq.gwsu.common.ai.AgentException;
import org.quyq.gwsu.common.ai.constants.AIConstants;
import org.quyq.gwsu.common.cache.utils.CacheUtils;
import org.quyq.gwsu.common.core.exception.errcode.CommonErrorCode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * 基于 Redis 的 AgentStateStore 实现。
 */
public class CacheRedisAgentStateStore implements AgentStateStore {

    private static final String ANON_USER = "__anon__";

    private final CacheUtils cacheUtils;

    public CacheRedisAgentStateStore(CacheUtils cacheUtils) {
        this.cacheUtils = Objects.requireNonNull(cacheUtils, "cacheUtils cannot be null");
    }

    @Override
    public void save(String userId, String sessionId, String stateKey, State value) {
        validateSessionId(sessionId);
        validateStateKey(stateKey);

        String slotId = slotId(userId, sessionId);
        String redisStateKey = stateKey(slotId, stateKey);
        String redisKeysKey = keysKey(slotId);
        try {
            cacheUtils.set(redisStateKey, JsonUtils.getJsonCodec().toJson(value));
            cacheUtils.sAdd(redisKeysKey, stateKey);
        } catch (Exception exception) {
            throw new AgentException(CommonErrorCode.E05001,
                    "智能体会话存储失败，sessionId=%s, stateKey=%s".formatted(sessionId, stateKey), exception);
        }
    }

    @Override
    public void save(String userId, String sessionId, String stateKey, List<? extends State> values) {
        validateSessionId(sessionId);
        validateStateKey(stateKey);
        Objects.requireNonNull(values, "values cannot be null");

        String slotId = slotId(userId, sessionId);
        String redisListKey = listKey(slotId, stateKey);
        String redisListHashKey = listHashKey(redisListKey);
        String redisKeysKey = keysKey(slotId);
        try {
            String newHash = ListHashUtil.computeHash(values);
            String currentHash = cacheUtils.get(redisListHashKey);
            Long currentSize = cacheUtils.lSize(redisListKey);
            int existingSize = currentSize == null ? 0 : currentSize.intValue();
            boolean rewriteRequired = ListHashUtil.needsFullRewrite(values, currentHash, existingSize);

            if (rewriteRequired) {
                cacheUtils.delete(redisListKey);
                if (!values.isEmpty()) {
                    for (State item : values) {
                        cacheUtils.rPush(redisListKey, JsonUtils.getJsonCodec().toJson(item));
                    }
                }
            } else if (values.size() > existingSize) {
                for (State item : values.subList(existingSize, values.size())) {
                    cacheUtils.rPush(redisListKey, JsonUtils.getJsonCodec().toJson(item));
                }
            }

            cacheUtils.set(redisListHashKey, newHash);
            cacheUtils.sAdd(redisKeysKey, stateKey + AIConstants.DistributedStoreRedis.STATE_LIST_SUFFIX);
        } catch (Exception exception) {
            throw new AgentException(CommonErrorCode.E05001,
                    "智能体会话列表存储失败，sessionId=%s, stateKey=%s".formatted(sessionId, stateKey), exception);
        }
    }

    @Override
    public <T extends State> Optional<T> get(String userId, String sessionId, String stateKey, Class<T> type) {
        validateSessionId(sessionId);
        validateStateKey(stateKey);
        Objects.requireNonNull(type, "type cannot be null");

        try {
            String raw = cacheUtils.get(stateKey(slotId(userId, sessionId), stateKey));
            if (raw == null) {
                return Optional.empty();
            }
            return Optional.of(JsonUtils.getJsonCodec().fromJson(raw, type));
        } catch (Exception exception) {
            throw new AgentException(CommonErrorCode.E05001,
                    "智能体会话读取失败，sessionId=%s, stateKey=%s".formatted(sessionId, stateKey), exception);
        }
    }

    @Override
    public <T extends State> List<T> getList(String userId, String sessionId, String stateKey, Class<T> itemType) {
        validateSessionId(sessionId);
        validateStateKey(stateKey);
        Objects.requireNonNull(itemType, "itemType cannot be null");

        try {
            List<String> rawList = cacheUtils.lRange(listKey(slotId(userId, sessionId), stateKey), 0, -1);
            if (rawList == null || rawList.isEmpty()) {
                return List.of();
            }

            List<T> result = new ArrayList<>(rawList.size());
            for (String raw : rawList) {
                result.add(JsonUtils.getJsonCodec().fromJson(raw, itemType));
            }
            return result;
        } catch (Exception exception) {
            throw new AgentException(CommonErrorCode.E05001,
                    "智能体会话列表读取失败，sessionId=%s, stateKey=%s".formatted(sessionId, stateKey), exception);
        }
    }

    @Override
    public boolean exists(String userId, String sessionId) {
        validateSessionId(sessionId);

        String slotId = slotId(userId, sessionId);
        String redisKeysKey = keysKey(slotId);
        try {
            Boolean exists = cacheUtils.exists(redisKeysKey);
            Long size = cacheUtils.sSize(redisKeysKey);
            return Boolean.TRUE.equals(exists) && size != null && size > 0;
        } catch (Exception exception) {
            throw new AgentException(CommonErrorCode.E05001,
                    "智能体会话存在性查询失败，sessionId=%s".formatted(sessionId), exception);
        }
    }

    @Override
    public void delete(String userId, String sessionId) {
        validateSessionId(sessionId);

        String slotId = slotId(userId, sessionId);
        String redisKeysKey = keysKey(slotId);
        try {
            Set<String> members = cacheUtils.sMembers(redisKeysKey);
            if (members == null || members.isEmpty()) {
                return;
            }

            Set<String> keysToDelete = new HashSet<>();
            keysToDelete.add(redisKeysKey);
            for (String member : members) {
                if (member == null || member.isBlank()) {
                    continue;
                }
                if (member.endsWith(AIConstants.DistributedStoreRedis.STATE_LIST_SUFFIX)) {
                    String originalStateKey = member.substring(0, member.length()
                            - AIConstants.DistributedStoreRedis.STATE_LIST_SUFFIX.length());
                    String redisListKey = listKey(slotId, originalStateKey);
                    keysToDelete.add(redisListKey);
                    keysToDelete.add(listHashKey(redisListKey));
                } else {
                    keysToDelete.add(stateKey(slotId, member));
                }
            }
            cacheUtils.delete(keysToDelete);
        } catch (Exception exception) {
            throw new AgentException(CommonErrorCode.E05001,
                    "智能体会话删除失败，sessionId=%s".formatted(sessionId), exception);
        }
    }

    @Override
    public void delete(String userId, String sessionId, String stateKey) {
        validateSessionId(sessionId);
        validateStateKey(stateKey);

        String slotId = slotId(userId, sessionId);
        String redisStateKey = stateKey(slotId, stateKey);
        String redisListKey = listKey(slotId, stateKey);
        try {
            cacheUtils.delete(List.of(redisStateKey, redisListKey, listHashKey(redisListKey)));
            String redisKeysKey = keysKey(slotId);
            cacheUtils.sRemove(redisKeysKey, stateKey, stateKey + AIConstants.DistributedStoreRedis.STATE_LIST_SUFFIX);
            Long remaining = cacheUtils.sSize(redisKeysKey);
            if (remaining != null && remaining == 0L) {
                cacheUtils.delete(redisKeysKey);
            }
        } catch (Exception exception) {
            throw new AgentException(CommonErrorCode.E05001,
                    "智能体会话状态删除失败，sessionId=%s, stateKey=%s".formatted(sessionId, stateKey), exception);
        }
    }

    @Override
    public Set<String> listSessionIds(String userId) {
        String normalizedUserId = normalizeUserId(userId);
        String pattern = AIConstants.DistributedStoreRedis.STATE_SESSION_SCAN_PATTERN_TEMPLATE.formatted(normalizedUserId);
        try {
            Set<String> matchedKeys = cacheUtils.scan(pattern);
            if (matchedKeys == null || matchedKeys.isEmpty()) {
                return Set.of();
            }

            String marker = AIConstants.DistributedStoreRedis.STATE_KEY_PREFIX + normalizedUserId + ":";
            Set<String> result = new HashSet<>();
            for (String matchedKey : matchedKeys) {
                int startIndex = matchedKey.indexOf(marker);
                if (startIndex < 0) {
                    continue;
                }
                String suffix = matchedKey.substring(startIndex + marker.length());
                if (!suffix.endsWith(AIConstants.DistributedStoreRedis.STATE_KEYS_SUFFIX)) {
                    continue;
                }
                result.add(suffix.substring(0,
                        suffix.length() - AIConstants.DistributedStoreRedis.STATE_KEYS_SUFFIX.length()));
            }
            return result;
        } catch (Exception exception) {
            throw new AgentException(CommonErrorCode.E05001, "智能体会话列表读取失败", exception);
        }
    }

    private static String normalizeUserId(String userId) {
        return userId == null || userId.isBlank() ? ANON_USER : userId;
    }

    private static String slotId(String userId, String sessionId) {
        return normalizeUserId(userId) + ":" + sessionId;
    }

    private static void validateSessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new AgentException("sessionId cannot be blank");
        }
    }

    private static void validateStateKey(String stateKey) {
        if (stateKey == null || stateKey.isBlank()) {
            throw new AgentException("stateKey cannot be blank");
        }
    }

    private String stateKey(String slotId, String stateKey) {
        return AIConstants.DistributedStoreRedis.STATE_VALUE_KEY_TEMPLATE.formatted(slotId, stateKey);
    }

    private String listKey(String slotId, String stateKey) {
        return AIConstants.DistributedStoreRedis.STATE_LIST_KEY_TEMPLATE.formatted(slotId, stateKey);
    }

    private String keysKey(String slotId) {
        return AIConstants.DistributedStoreRedis.STATE_KEYS_KEY_TEMPLATE.formatted(slotId);
    }

    private String listHashKey(String redisListKey) {
        return redisListKey + AIConstants.DistributedStoreRedis.STATE_LIST_HASH_SUFFIX;
    }
}
