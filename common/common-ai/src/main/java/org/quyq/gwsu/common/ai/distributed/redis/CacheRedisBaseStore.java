package org.quyq.gwsu.common.ai.distributed.redis;

import io.agentscope.harness.agent.filesystem.remote.store.BaseStore;
import io.agentscope.harness.agent.filesystem.remote.store.StoreItem;
import org.quyq.gwsu.common.ai.AgentException;
import org.quyq.gwsu.common.ai.constants.AIConstants;
import org.quyq.gwsu.common.cache.utils.CacheUtils;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 基于 Redis 的 AgentScope BaseStore 实现。
 */
public class CacheRedisBaseStore implements BaseStore {

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().build();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };
    private static final String PUT_IF_VERSION_SCRIPT = """
            local currentVersion = redis.call('get', KEYS[2])
            if ARGV[1] == '0' then
                if currentVersion then
                    return 0
                end
                redis.call('set', KEYS[1], ARGV[2])
                redis.call('set', KEYS[2], '1')
                redis.call('sadd', KEYS[3], ARGV[3])
                return 1
            end
            if (not currentVersion) or tostring(currentVersion) ~= ARGV[1] then
                return 0
            end
            redis.call('set', KEYS[1], ARGV[2])
            redis.call('set', KEYS[2], ARGV[4])
            redis.call('sadd', KEYS[3], ARGV[3])
            return 1
            """;

    private final CacheUtils cacheUtils;

    public CacheRedisBaseStore(CacheUtils cacheUtils) {
        this.cacheUtils = Objects.requireNonNull(cacheUtils, "cacheUtils cannot be null");
    }

    @Override
    public StoreItem get(List<String> namespace, String key) {
        String itemKey = itemKey(namespace, key);
        String itemVersionKey = itemVersionKey(namespace, key);
        String raw = cacheUtils.get(itemKey);
        if (raw == null) {
            return null;
        }

        Map<String, Object> stored = deserialize(raw);
        long version = resolveVersion(stored, cacheUtils.get(itemVersionKey));
        return new StoreItem(key, payloadOf(stored), version);
    }

    @Override
    public void put(List<String> namespace, String key, Map<String, Object> value) {
        validateNamespace(namespace);
        validateKey(key);
        Objects.requireNonNull(value, "value cannot be null");

        StoreItem existing = get(namespace, key);
        long nextVersion = existing == null ? 1L : existing.version() + 1L;
        String now = Instant.now().toString();
        String createdAt = existing == null ? now : String.valueOf(readStoredMap(namespace, key)
                .getOrDefault(AIConstants.DistributedStoreRedis.FIELD_CREATED_AT, now));
        String serialized = serialize(wrapStoredValue(key, value, nextVersion, createdAt, now));

        cacheUtils.set(itemKey(namespace, key), serialized);
        cacheUtils.set(itemVersionKey(namespace, key), String.valueOf(nextVersion));
        cacheUtils.sAdd(indexKey(namespace), key);
    }

    @Override
    public boolean putIfVersion(List<String> namespace, String key, Map<String, Object> value, long expectedVersion) {
        validateNamespace(namespace);
        validateKey(key);
        Objects.requireNonNull(value, "value cannot be null");
        if (expectedVersion < 0) {
            throw new AgentException("expectedVersion cannot be negative");
        }

        String now = Instant.now().toString();
        String createdAt = now;
        if (expectedVersion > 0) {
            Map<String, Object> existing = readStoredMap(namespace, key);
            if (!existing.isEmpty()) {
                createdAt = String.valueOf(existing.getOrDefault(AIConstants.DistributedStoreRedis.FIELD_CREATED_AT, now));
            }
        }
        long nextVersion = expectedVersion == 0 ? 1L : expectedVersion + 1L;
        String serialized = serialize(wrapStoredValue(key, value, nextVersion, createdAt, now));

        Long updated = cacheUtils.executeScriptForServerKeys(
                new DefaultRedisScript<>(PUT_IF_VERSION_SCRIPT, Long.class),
                List.of(itemKey(namespace, key), itemVersionKey(namespace, key), indexKey(namespace)),
                String.valueOf(expectedVersion),
                serialized,
                key,
                String.valueOf(nextVersion));
        return Long.valueOf(1L).equals(updated);
    }

    @Override
    public List<StoreItem> search(List<String> namespace, int limit, int offset) {
        validateNamespace(namespace);
        if (limit <= 0) {
            return List.of();
        }
        int effectiveOffset = Math.max(offset, 0);
        Set<String> members = cacheUtils.sMembers(indexKey(namespace));
        if (members == null || members.isEmpty()) {
            return List.of();
        }

        List<String> sortedKeys = members.stream()
                .filter(Objects::nonNull)
                .map(Object::toString)
                .sorted(Comparator.naturalOrder())
                .toList();

        if (effectiveOffset >= sortedKeys.size()) {
            return List.of();
        }

        int endExclusive = Math.min(sortedKeys.size(), effectiveOffset + limit);
        List<StoreItem> result = new ArrayList<>(endExclusive - effectiveOffset);
        for (String key : sortedKeys.subList(effectiveOffset, endExclusive)) {
            StoreItem item = get(namespace, key);
            if (item != null) {
                result.add(item);
            } else {
                cacheUtils.sRemove(indexKey(namespace), key);
            }
        }
        return result;
    }

    @Override
    public void delete(List<String> namespace, String key) {
        validateNamespace(namespace);
        validateKey(key);
        cacheUtils.delete(itemKey(namespace, key));
        cacheUtils.delete(itemVersionKey(namespace, key));
        cacheUtils.sRemove(indexKey(namespace), key);
    }

    private Map<String, Object> readStoredMap(List<String> namespace, String key) {
        String raw = cacheUtils.get(itemKey(namespace, key));
        return raw == null ? Map.of() : deserialize(raw);
    }

    private String itemKey(List<String> namespace, String key) {
        return AIConstants.DistributedStoreRedis.STORE_ITEM_KEY_TEMPLATE.formatted(namespacePath(namespace), key);
    }

    private String itemVersionKey(List<String> namespace, String key) {
        return AIConstants.DistributedStoreRedis.STORE_ITEM_VERSION_KEY_TEMPLATE.formatted(namespacePath(namespace), key);
    }

    private String indexKey(List<String> namespace) {
        return AIConstants.DistributedStoreRedis.STORE_INDEX_KEY_TEMPLATE.formatted(namespacePath(namespace));
    }

    private String namespacePath(List<String> namespace) {
        return String.join(AIConstants.DistributedStoreRedis.NAMESPACE_SEPARATOR, namespace);
    }

    private void validateNamespace(List<String> namespace) {
        if (namespace == null || namespace.isEmpty()) {
            throw new AgentException("namespace cannot be empty");
        }
        for (String segment : namespace) {
            if (segment == null || segment.isBlank()) {
                throw new AgentException("namespace segment cannot be blank");
            }
        }
    }

    private void validateKey(String key) {
        if (key == null || key.isBlank()) {
            throw new AgentException("key cannot be blank");
        }
    }

    private Map<String, Object> wrapStoredValue(
            String key,
            Map<String, Object> payload,
            long version,
            String createdAt,
            String modifiedAt) {
        Map<String, Object> wrapper = new LinkedHashMap<>();
        wrapper.put(AIConstants.DistributedStoreRedis.FIELD_KEY, key);
        wrapper.put(AIConstants.DistributedStoreRedis.FIELD_VERSION, version);
        wrapper.put(AIConstants.DistributedStoreRedis.FIELD_PAYLOAD, payload);
        wrapper.put(AIConstants.DistributedStoreRedis.FIELD_CREATED_AT, createdAt);
        wrapper.put(AIConstants.DistributedStoreRedis.FIELD_MODIFIED_AT, modifiedAt);
        return wrapper;
    }

    private Map<String, Object> payloadOf(Map<String, Object> stored) {
        Object payload = stored.get(AIConstants.DistributedStoreRedis.FIELD_PAYLOAD);
        if (payload instanceof Map<?, ?> payloadMap) {
            Map<String, Object> result = new LinkedHashMap<>();
            payloadMap.forEach((payloadKey, payloadValue) -> result.put(String.valueOf(payloadKey), payloadValue));
            return result;
        }
        return Map.of();
    }

    private long resolveVersion(Map<String, Object> stored, Object versionObject) {
        if (versionObject != null) {
            return Long.parseLong(String.valueOf(versionObject));
        }
        Object embeddedVersion = stored.get(AIConstants.DistributedStoreRedis.FIELD_VERSION);
        if (embeddedVersion == null) {
            return 0L;
        }
        return Long.parseLong(String.valueOf(embeddedVersion));
    }

    private Map<String, Object> deserialize(String raw) {
        try {
            return OBJECT_MAPPER.readValue(raw, MAP_TYPE);
        } catch (Exception exception) {
            AgentException agentException = new AgentException("Failed to deserialize redis base store value");
            agentException.initCause(exception);
            throw agentException;
        }
    }

    private String serialize(Map<String, Object> value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (Exception exception) {
            AgentException agentException = new AgentException("Failed to serialize redis base store value");
            agentException.initCause(exception);
            throw agentException;
        }
    }
}
