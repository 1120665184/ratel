package org.quyq.gwsu.common.cache.utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.quyq.gwsu.common.cache.exceptions.CacheException;
import org.quyq.gwsu.common.core.utils.ProjectUtils;
import org.redisson.api.RLock;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.util.CollectionUtils;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class CacheUtils {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedissonClient redissonClient;
    private final ProjectUtils projectUtils;

    private static final String LOCK_PREFIX = "lock:";
    private static final String RATE_LIMIT_PREFIX = "rate_limit:";

    private static final ScopedValue<Boolean> USE_PROJECT_PREFIX = ScopedValue.newInstance();


    /**
     * 跳脱者模式下执行Redis操作
     * 在此模式下，key的前缀将使用 项目标识:key 的形式
     * 注意：redis值在跨服务的情况下使用时才需要使用该方法，服务内的redis值操作无需使用该方法包裹
     *
     * @param operation 要执行的操作
     * @param <T>       返回值类型
     * @return 操作执行结果
     */
    public <T> T withRebel(Supplier<T> operation) {

        return ScopedValue.where(USE_PROJECT_PREFIX, true).call(operation::get);
    }

    // ================================ String 操作 ================================

    /**
     * 设置String类型值
     *
     * @param key   键
     * @param value 值
     */
    public <T> void set(String key, T value) {
        redisTemplate.opsForValue().set(clusterKey(key), value);
    }

    /**
     * 设置String类型值并设置过期时间
     *
     * @param key   键
     * @param value 值
     * @param ttl   过期时间
     */
    public <T> void set(String key, T value, Duration ttl) {
        redisTemplate.opsForValue().set(clusterKey(key), value, ttl);
    }

    /**
     * 设置String类型值并设置过期时间和时间单位
     *
     * @param key     键
     * @param value   值
     * @param timeout 过期时间
     * @param unit    时间单位
     */
    public <T> void set(String key, T value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(clusterKey(key), value, timeout, unit);
    }

    /**
     * 设置String类型值（仅当key不存在时）
     *
     * @param key   键
     * @param value 值
     * @param ttl   过期时间
     * @return 设置成功返回true，key已存在返回false
     */
    public <T> Boolean setIfAbsent(String key, T value, Duration ttl) {
        return redisTemplate.opsForValue().setIfAbsent(clusterKey(key), value, ttl);
    }

    /**
     * 设置String类型值（仅当key存在时）
     *
     * @param key   键
     * @param value 值
     * @param ttl   过期时间
     * @return 设置成功返回true，key不存在返回false
     */
    public <T> Boolean setIfPresent(String key, T value, Duration ttl) {
        return redisTemplate.opsForValue().setIfPresent(clusterKey(key), value, ttl);
    }

    /**
     * 获取String类型值
     *
     * @param key 键
     * @return 值，不存在返回null
     */
    @Nullable
    public <T> T get(String key) {
        return (T) redisTemplate.opsForValue().get(clusterKey(key));
    }


    /**
     * 设置新值并返回旧值
     *
     * @param key   键
     * @param value 新值
     * @return 旧值，不存在返回null
     */
    public <T> T getAndSet(String key, T value) {
        return (T) redisTemplate.opsForValue().getAndSet(clusterKey(key), value);
    }

    /**
     * 自增1
     *
     * @param key 键
     * @return 自增后的值
     */
    public Long increment(String key) {
        return redisTemplate.opsForValue().increment(clusterKey(key));
    }

    /**
     * 自增指定增量
     *
     * @param key   键
     * @param delta 增量
     * @return 自增后的值
     */
    public Long increment(String key, long delta) {
        return redisTemplate.opsForValue().increment(clusterKey(key), delta);
    }

    /**
     * 自减1
     *
     * @param key 键
     * @return 自减后的值
     */
    public Long decrement(String key) {
        return redisTemplate.opsForValue().decrement(clusterKey(key));
    }

    /**
     * 自减指定减量
     *
     * @param key   键
     * @param delta 减量
     * @return 自减后的值
     */
    public Long decrement(String key, long delta) {
        return redisTemplate.opsForValue().decrement(clusterKey(key), delta);
    }

    // ================================ Hash 操作 ================================

    /**
     * 设置Hash中指定字段的值
     *
     * @param key     键
     * @param hashKey Hash字段名
     * @param value   值
     */
    public <T> void hSet(String key, String hashKey, T value) {
        redisTemplate.opsForHash().put(clusterKey(key), hashKey, value);
    }

    /**
     * 批量设置Hash中的字段和值
     *
     * @param key 键
     * @param map 字段和值的映射
     */
    public <T> void hSetAll(String key, Map<String, T> map) {
        redisTemplate.opsForHash().putAll(clusterKey(key), map);
    }

    /**
     * 获取Hash中指定字段的值
     *
     * @param key     键
     * @param hashKey Hash字段名
     * @return 值，不存在返回null
     */
    @Nullable
    public <T> T hGet(String key, String hashKey) {
        return (T) redisTemplate.opsForHash().get(clusterKey(key), hashKey);
    }


    /**
     * 获取Hash中所有字段和值，并转换为指定类型
     *
     * @param key  键
     * @param type 目标类型
     * @param <T>  泛型类型
     * @return 字段和值的映射
     */
    @SuppressWarnings("unchecked")
    public <T> Map<String, T> hGetAll(String key, Class<T> type) {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(clusterKey(key));
        Map<String, T> result = new HashMap<>();
        entries.forEach((k, v) -> {
            if (type.isInstance(v)) {
                result.put(String.valueOf(k), (T) v);
            }
        });
        return result;
    }

    /**
     * 删除Hash中指定的一个或多个字段
     *
     * @param key      键
     * @param hashKeys Hash字段名
     * @return 删除的字段数量
     */
    public Long hDelete(String key, Object... hashKeys) {
        return redisTemplate.opsForHash().delete(clusterKey(key), hashKeys);
    }

    /**
     * 判断Hash中指定字段是否存在
     *
     * @param key     键
     * @param hashKey Hash字段名
     * @return 存在返回true，不存在返回false
     */
    public Boolean hExists(String key, String hashKey) {
        return redisTemplate.opsForHash().hasKey(clusterKey(key), hashKey);
    }

    /**
     * 获取Hash中字段的数量
     *
     * @param key 键
     * @return 字段数量
     */
    public Long hSize(String key) {
        return redisTemplate.opsForHash().size(clusterKey(key));
    }

    /**
     * Hash中指定字段自增指定增量
     *
     * @param key     键
     * @param hashKey Hash字段名
     * @param delta   增量
     * @return 自增后的值
     */
    public Long hIncrBy(String key, String hashKey, long delta) {
        return redisTemplate.opsForHash().increment(clusterKey(key), hashKey, delta);
    }

    /**
     * Hash中指定字段自增指定浮点数增量
     *
     * @param key     键
     * @param hashKey Hash字段名
     * @param delta   浮点数增量
     * @return 自增后的值
     */
    public Double hIncrByFloat(String key, String hashKey, double delta) {
        return redisTemplate.opsForHash().increment(clusterKey(key), hashKey, delta);
    }

    /**
     * 获取Hash中所有字段
     *
     * @param key 键
     * @return 字段集合
     */
    public Set<Object> hKeys(String key) {
        return redisTemplate.opsForHash().keys(clusterKey(key));
    }

    /**
     * 获取Hash中所有值
     *
     * @param key 键
     * @return 值列表
     */
    public List<Object> hValues(String key) {
        return redisTemplate.opsForHash().values(clusterKey(key));
    }

    // ================================ List 操作 ================================

    /**
     * 向List左侧（头部）添加一个或多个值
     *
     * @param key    键
     * @param values 值
     * @return 列表长度
     */
    public Long lPush(String key, Object... values) {
        return redisTemplate.opsForList().leftPushAll(clusterKey(key), values);
    }

    /**
     * 向List右侧（尾部）添加一个或多个值
     *
     * @param key    键
     * @param values 值
     * @return 列表长度
     */
    public Long rPush(String key, Object... values) {
        return redisTemplate.opsForList().rightPushAll(clusterKey(key), values);
    }

    /**
     * 移除并返回List左侧（头部）的值
     *
     * @param key 键
     * @return 头部值，列表为空返回null
     */
    @Nullable
    public <T> T lPop(String key) {
        return (T) redisTemplate.opsForList().leftPop(clusterKey(key));
    }

    /**
     * 移除并返回List右侧（尾部）的值
     *
     * @param key 键
     * @return 尾部值，列表为空返回null
     */
    @Nullable
    public <T> T rPop(String key) {
        return (T) redisTemplate.opsForList().rightPop(clusterKey(key));
    }

    public <T> T rPop(String key, long timeout, TimeUnit unit) {
        return (T) redisTemplate.opsForList().rightPop(clusterKey(key), timeout, unit);
    }

    /**
     * 获取List中指定范围的元素
     *
     * @param key   键
     * @param start 起始索引
     * @param end   结束索引
     * @return 元素列表
     */
    public <T> List<T> lRange(String key, long start, long end) {
        return (List<T>) redisTemplate.opsForList().range(clusterKey(key), start, end);
    }

    /**
     * 获取List的长度
     *
     * @param key 键
     * @return 列表长度
     */
    public Long lSize(String key) {
        return redisTemplate.opsForList().size(clusterKey(key));
    }

    // ================================ Set 操作 ================================

    /**
     * 向Set中添加一个或多个成员
     *
     * @param key    键
     * @param values 成员值
     * @return 添加成功的成员数量
     */
    public Long sAdd(String key, Object... values) {
        return redisTemplate.opsForSet().add(clusterKey(key), values);
    }

    /**
     * 从Set中移除一个或多个成员
     *
     * @param key    键
     * @param values 成员值
     * @return 移除成功的成员数量
     */
    public Long sRemove(String key, Object... values) {
        return redisTemplate.opsForSet().remove(clusterKey(key), values);
    }

    /**
     * 获取Set中所有成员
     *
     * @param key 键
     * @return 成员集合
     */
    public <T> Set<T> sMembers(String key) {
        return (Set<T>) redisTemplate.opsForSet().members(clusterKey(key));
    }

    /**
     * 判断Set中是否存在指定成员
     *
     * @param key   键
     * @param value 成员值
     * @return 存在返回true，不存在返回false
     */
    public Boolean sIsMember(String key, Object value) {
        return redisTemplate.opsForSet().isMember(clusterKey(key), value);
    }

    /**
     * 获取Set的成员数量
     *
     * @param key 键
     * @return 成员数量
     */
    public Long sSize(String key) {
        return redisTemplate.opsForSet().size(clusterKey(key));
    }

    /**
     * 获取两个Set的交集
     *
     * @param key      第一个Set的键
     * @param otherKey 第二个Set的键
     * @return 交集成员集合
     */
    public <T> Set<T> sIntersect(String key, String otherKey) {
        return (Set<T>) redisTemplate.opsForSet().intersect(clusterKey(key), clusterKey(otherKey));
    }

    /**
     * 获取两个Set的并集
     *
     * @param key      第一个Set的键
     * @param otherKey 第二个Set的键
     * @return 并集成员集合
     */
    public <T> Set<T> sUnion(String key, String otherKey) {
        return (Set<T>) redisTemplate.opsForSet().union(clusterKey(key), clusterKey(otherKey));
    }

    /**
     * 获取两个Set的差集
     *
     * @param key      第一个Set的键
     * @param otherKey 第二个Set的键
     * @return 差集成员集合（key中有但otherKey中没有的成员）
     */
    public <T> Set<T> sDiff(String key, String otherKey) {
        return (Set<T>) redisTemplate.opsForSet().difference(clusterKey(key), clusterKey(otherKey));
    }

    // ================================ Sorted Set 操作
    // ================================

    /**
     * 向Sorted Set中添加成员及其分数
     *
     * @param key   键
     * @param value 成员值
     * @param score 分数
     * @return 添加成功返回true，成员已存在更新分数返回false
     */
    public Boolean zAdd(String key, Object value, double score) {
        return redisTemplate.opsForZSet().add(clusterKey(key), value, score);
    }

    /**
     * 从Sorted Set中移除一个或多个成员
     *
     * @param key    键
     * @param values 成员值
     * @return 移除成功的成员数量
     */
    public Long zRemove(String key, Object... values) {
        return redisTemplate.opsForZSet().remove(clusterKey(key), values);
    }

    /**
     * 获取Sorted Set中指定索引范围的成员
     *
     * @param key   键
     * @param start 起始索引
     * @param end   结束索引
     * @return 成员集合（按分数升序）
     */
    public <T> Set<T> zRange(String key, long start, long end) {
        return (Set<T>) redisTemplate.opsForZSet().range(clusterKey(key), start, end);
    }

    /**
     * 获取Sorted Set中指定索引范围的成员（按分数降序）
     *
     * @param key   键
     * @param start 起始索引
     * @param end   结束索引
     * @return 成员集合（按分数降序）
     */
    public <T> Set<T> zReverseRange(String key, long start, long end) {
        return (Set<T>) redisTemplate.opsForZSet().reverseRange(clusterKey(key), start, end);
    }

    /**
     * 获取Sorted Set中指定分数范围的成员
     *
     * @param key 键
     * @param min 最小分数
     * @param max 最大分数
     * @return 成员集合
     */
    public <T> Set<T> zRangeByScore(String key, double min, double max) {
        return (Set<T>) redisTemplate.opsForZSet().rangeByScore(clusterKey(key), min, max);
    }

    /**
     * 获取Sorted Set中指定成员的分数
     *
     * @param key   键
     * @param value 成员值
     * @return 分数，成员不存在返回null
     */
    @Nullable
    public Double zScore(String key, Object value) {
        return redisTemplate.opsForZSet().score(clusterKey(key), value);
    }

    /**
     * 获取Sorted Set中指定成员的索引（按分数升序）
     *
     * @param key   键
     * @param value 成员值
     * @return 索引，成员不存在返回null
     */
    public Long zRank(String key, Object value) {
        return redisTemplate.opsForZSet().rank(clusterKey(key), value);
    }

    /**
     * 获取Sorted Set中指定成员的索引（按分数降序）
     *
     * @param key   键
     * @param value 成员值
     * @return 索引，成员不存在返回null
     */
    public Long zReverseRank(String key, Object value) {
        return redisTemplate.opsForZSet().reverseRank(clusterKey(key), value);
    }

    /**
     * 获取Sorted Set的成员数量
     *
     * @param key 键
     * @return 成员数量
     */
    public Long zSize(String key) {
        return redisTemplate.opsForZSet().size(clusterKey(key));
    }

    // ================================ Key 操作 ================================

    /**
     * 检查key是否存在
     *
     * @param key 键
     * @return 存在返回true，不存在返回false
     */
    public Boolean exists(String key) {
        return redisTemplate.hasKey(clusterKey(key));
    }

    /**
     * 设置key的过期时间（Duration）
     *
     * @param key 键
     * @param ttl 过期时间
     * @return 设置成功返回true，失败返回false
     */
    public Boolean expire(String key, Duration ttl) {
        return redisTemplate.expire(clusterKey(key), ttl);
    }

    /**
     * 设置key的过期时间和时间单位
     *
     * @param key     键
     * @param timeout 过期时间
     * @param unit    时间单位
     * @return 设置成功返回true，失败返回false
     */
    public Boolean expire(String key, long timeout, TimeUnit unit) {
        return redisTemplate.expire(clusterKey(key), timeout, unit);
    }

    /**
     * 获取key的剩余过期时间（秒）
     *
     * @param key 键
     * @return 剩余过期时间（秒），永久存在返回-1，不存在返回-2
     */
    public Long getExpire(String key) {
        return redisTemplate.getExpire(clusterKey(key));
    }

    /**
     * 获取key的剩余过期时间（指定时间单位）
     *
     * @param key  键
     * @param unit 时间单位
     * @return 剩余过期时间，永久存在返回-1，不存在返回-2
     */
    public Long getExpire(String key, TimeUnit unit) {
        return redisTemplate.getExpire(clusterKey(key), unit);
    }

    /**
     * 移除key的过期时间，使其永久存在
     *
     * @param key 键
     * @return 移除成功返回true，失败返回false
     */
    public Boolean persist(String key) {
        return redisTemplate.persist(clusterKey(key));
    }

    /**
     * 删除指定的key
     *
     * @param key 键
     * @return 删除成功返回true，失败返回false
     */
    public Boolean delete(String key) {
        return redisTemplate.delete(clusterKey(key));
    }

    /**
     * 批量删除指定的keys
     *
     * @param keys 键集合
     * @return 删除成功的key数量
     */
    public Long delete(Collection<String> keys) {
        Set<String> clusterKeys = keys.stream()
                .map(this::clusterKey)
                .collect(Collectors.toSet());
        return redisTemplate.delete(clusterKeys);
    }

    /**
     * 根据模式匹配扫描keys
     *
     * @param pattern 匹配模式
     * @return 匹配的key集合
     */
    public Set<String> scan(String pattern) {
        Set<String> result = new HashSet<>();
        ScanOptions options = ScanOptions.scanOptions().match(clusterKey(pattern)).count(1000).build();
        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            cursor.forEachRemaining(result::add);
        }
        return result;
    }

    /**
     * 修改key名称
     *
     * @param oldKey 原键名
     * @param newKey 新键名
     */
    public void rename(String oldKey, String newKey) {
        redisTemplate.rename(clusterKey(oldKey), clusterKey(newKey));
    }

    // ================================ 分布式锁 ================================

    /**
     * 执行业务逻辑（带分布式锁）
     *
     * @param key       锁的键
     * @param waitTime  等待获取锁的时间
     * @param leaseTime 锁持有时间
     * @param unit      时间单位
     * @param action    业务逻辑执行器
     * @param <T>       返回值类型
     * @return 业务逻辑执行结果
     */
    public <T> T executeWithLock(String key, long waitTime, long leaseTime, TimeUnit unit, Supplier<T> action) {
        RLock lock = null;
        boolean acquired = false;
        try {
            lock = redissonClient.getLock(lockKey(key));
            acquired = lock.tryLock(waitTime, leaseTime, unit);
            if (!acquired) {
                throw new CacheException("Failed to acquire lock: " + key);
            }
            return action.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CacheException(e);
        } finally {
            if (lock != null && acquired && lock.isHeldByCurrentThread()) {
                try {
                    lock.unlock();
                } catch (Exception e) {
                    log.error("Unlock error, key: {}", key, e);
                }
            }
        }
    }

    /**
     * 执行业务逻辑（带分布式锁），默认等待10秒，自动续锁
     *
     * @param key    锁的键
     * @param action 业务逻辑执行器
     * @param <T>    返回值类型
     * @return 业务逻辑执行结果
     */
    public <T> T executeWithLock(String key, Supplier<T> action) {
        return executeWithLock(key, 10, -1, TimeUnit.SECONDS, action);
    }

    /**
     * 执行业务逻辑（带分布式锁），无返回值
     *
     * @param key       锁的键
     * @param waitTime  等待获取锁的时间
     * @param leaseTime 锁持有时间
     * @param unit      时间单位
     * @param action    业务逻辑执行器
     */
    public void executeWithLock(String key, long waitTime, long leaseTime, TimeUnit unit, Runnable action) {
        executeWithLock(key, waitTime, leaseTime, unit, () -> {
            action.run();
            return null;
        });
    }

    /**
     * 执行业务逻辑（带分布式锁），无返回值，默认10秒等待，30秒持有
     *
     * @param key    锁的键
     * @param action 业务逻辑执行器
     */
    public void executeWithLock(String key, Runnable action) {
        executeWithLock(key, 10, 30, TimeUnit.SECONDS, action);
    }

    /**
     * 判断指定锁是否正在被持有（任何线程）
     *
     * @param key 锁的键
     * @return 锁被持有时返回 true，否则返回 false
     */
    public boolean isLocked(String key) {
        RLock lock = redissonClient.getLock(lockKey(key));
        return lock.isLocked();
    }

    // ================================ Lua 脚本 ================================

    /**
     * 执行Lua脚本
     *
     * @param script Lua脚本
     * @param keys   键列表
     * @param args   参数
     * @param <T>    返回类型
     * @return 脚本执行结果
     */
    public <T> T executeScript(RedisScript<T> script, List<String> keys, Object... args) {
        keys = createScriptKey(keys);
        return redisTemplate.execute(script, keys, args);
    }


    /**
     * 执行Lua脚本
     *
     * @param script Lua脚本
     * @param keys   键列表
     * @param args   参数
     * @param <T>    返回类型
     * @return 脚本执行结果
     */
    public <T> T executeScript(RedisScript<T> script, @NonNull RedisSerializer<T> resultSerializer, List<String> keys, Object... args) {
        keys = createScriptKey(keys);
        return redisTemplate.execute(script, this.redisTemplate.getValueSerializer(),
                resultSerializer
                , keys, args);
    }


    // ================================ 限流器 ================================

    /**
     * 尝试获取限流许可
     *
     * @param key         限流键
     * @param maxAttempts 最大尝试次数
     * @param window      时间窗口
     * @return 允许访问返回true，超过限制返回false
     */
    public boolean acquireRateLimit(String key, int maxAttempts, Duration window) {
        String rateLimitKey = clusterKey(RATE_LIMIT_PREFIX + key);
        Long count = redisTemplate.opsForValue().increment(rateLimitKey);
        if (count == null) {
            return false;
        }
        if (count == 1) {
            redisTemplate.expire(rateLimitKey, window);
        }
        return count <= maxAttempts;
    }

    /**
     * 尝试获取限流许可（指定时间单位）
     *
     * @param key         限流键
     * @param maxAttempts 最大尝试次数
     * @param window      时间窗口
     * @param unit        时间单位
     * @return 允许访问返回true，超过限制返回false
     */
    public boolean acquireRateLimit(String key, int maxAttempts, long window, TimeUnit unit) {
        return acquireRateLimit(key, maxAttempts, Duration.ofMillis(unit.toMillis(window)));
    }

    // ================================ 信号量（分布式许可） ================================

    /**
     * 获取信号量对象
     *
     * @param key 信号量键
     * @return 信号量对象
     */
    public RSemaphore getSemaphore(String key) {
        return redissonClient.getSemaphore(clusterKey(key));
    }

    /**
     * 尝试获取信号量许可
     *
     * @param key      信号量键
     * @param permits  许可数量
     * @param waitTime 等待时间
     * @param unit     时间单位
     * @return 获取成功返回true，失败返回false
     */
    public boolean tryAcquirePermit(String key, int permits, long waitTime, TimeUnit unit) {
        try {
            RSemaphore semaphore = redissonClient.getSemaphore(clusterKey(key));
            return semaphore.tryAcquire(permits, Duration.of(waitTime, unit.toChronoUnit()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Redis tryAcquirePermit error, key: {}", key, e);
            return false;
        } catch (Exception e) {
            log.error("Redis tryAcquirePermit error, key: {}", key, e);
            return false;
        }
    }

    /**
     * 释放信号量许可
     *
     * @param key     信号量键
     * @param permits 许可数量
     */
    public void releasePermit(String key, int permits) {
        RSemaphore semaphore = redissonClient.getSemaphore(clusterKey(key));
        semaphore.release(permits);
    }

    // ================================ 缓存加载（Cache-Aside 模式）
    // ================================

    /**
     * 获取缓存，如果不存在则调用loader加载并缓存
     *
     * @param key    键
     * @param loader 数据加载器
     * @param ttl    过期时间
     * @param <T>    泛型类型
     * @return 缓存值或加载的值
     */
    public <T> T getOrLoad(String key, Supplier<T> loader, Duration ttl) {
        T value = get(key);
        if (value != null) {
            return value;
        }
        value = loader.get();
        if (value != null) {
            set(key, value, ttl);
        }
        return value;
    }


    /**
     * 获取缓存并转换为指定类型，默认1小时过期
     *
     * @param key    键
     * @param loader 数据加载器
     * @param <T>    泛型类型
     * @return 缓存值或加载的值
     */
    public <T> T getOrLoad(String key, Supplier<T> loader) {
        return getOrLoad(key, loader, Duration.ofHours(1));
    }

    // ================================ 位操作 ================================

    /**
     * 设置指定偏移位置的位值
     *
     * @param key    键
     * @param offset 偏移位置
     * @param value  位值
     * @return 原来的位值
     */
    public Boolean setBit(String key, long offset, boolean value) {
        return redisTemplate.opsForValue().setBit(clusterKey(key), offset, value);
    }

    /**
     * 获取指定偏移位置的位值
     *
     * @param key    键
     * @param offset 偏移位置
     * @return 位值
     */
    public Boolean getBit(String key, long offset) {
        return redisTemplate.opsForValue().getBit(clusterKey(key), offset);
    }

    /**
     * 统计位值为1的数量
     *
     * @param key 键
     * @return 位值为1的数量
     */
    public Long bitCount(String key) {
        return bitCount(key, 0, -1);
    }

    /**
     * 统计指定范围内位值为1的数量
     *
     * @param key   键
     * @param start 起始字节位置
     * @param end   结束字节位置
     * @return 位值为1的数量
     */
    public Long bitCount(String key, long start, long end) {
        byte[] keyBytes = clusterKey(key).getBytes();
        return redisTemplate.execute((RedisCallback<Long>) connection ->
                connection.stringCommands().bitCount(keyBytes, start, end));
    }

    // ================================ HyperLogLog 操作
    // ================================

    /**
     * 向HyperLogLog添加元素（用于基数统计）
     *
     * @param key    键
     * @param values 元素值
     * @return 添加成功返回1，元素已存在返回0
     */
    public Long pfAdd(String key, Object... values) {
        return redisTemplate.opsForHyperLogLog().add(clusterKey(key), values);
    }

    /**
     * 获取一个或多个HyperLogLog的基数（不重复元素数量）
     *
     * @param keys 键
     * @return 基数估计值
     */
    public Long pfCount(String... keys) {
        String[] clusterKeys = Arrays.stream(keys).map(this::clusterKey).toArray(String[]::new);
        return redisTemplate.opsForHyperLogLog().size(clusterKeys);
    }

    /**
     * 合并多个HyperLogLog
     *
     * @param destKey    目标键
     * @param sourceKeys 源键
     */
    public void pfMerge(String destKey, String... sourceKeys) {
        String clusterDestKey = clusterKey(destKey);
        String[] clusterSourceKeys = Arrays.stream(sourceKeys).map(this::clusterKey).toArray(String[]::new);
        redisTemplate.opsForHyperLogLog().union(clusterDestKey, clusterSourceKeys);
    }

    // ================================ 地理坐标操作 ================================

    /**
     * 添加地理坐标
     *
     * @param key       键
     * @param longitude 经度
     * @param latitude  纬度
     * @param member    成员名称
     * @return 添加成功返回1，更新返回0
     */
    public <T> Long geoAdd(String key, double longitude, double latitude, T member) {
        return redisTemplate.opsForGeo().add(clusterKey(key),
                new Point(longitude, latitude), member);
    }

    /**
     * 查询指定坐标半径内的成员
     *
     * @param key       键
     * @param longitude 中心经度
     * @param latitude  中心纬度
     * @param radius    半径
     * @param count     最大返回数量
     * @return 成员名称列表
     */
    public <T> List<T> geoRadius(String key, double longitude, double latitude, double radius, long count) {
        Circle circle = new Circle(
                new Point(longitude, latitude),
                new Distance(radius));
        var results = redisTemplate.opsForGeo().radius(clusterKey(key), circle);
        if (results == null) {
            return Collections.emptyList();
        }
        return (List<T>) results.getContent().stream()
                .map(r -> r.getContent().getName())
                .toList();
    }

    /**
     * 计算两个成员之间的距离
     *
     * @param key     键
     * @param member1 第一个成员
     * @param member2 第二个成员
     * @return 距离值
     */
    public <T> Double geoDist(String key, T member1, T member2) {
        return redisTemplate.opsForGeo().distance(clusterKey(key), member1, member2).getValue();
    }

    /**
     * 给指定topic发送消息
     *
     * @param channel
     * @param value
     * @param <T>
     */
    public <T> Long convertAndSend(String channel, T value) {
        return redisTemplate.convertAndSend(clusterKey(channel), value);
    }

    /**
     * 添加监听器
     *
     * @param channel
     * @param listener
     * @return
     */
    public RedisMessageListenerContainer addListener(String channel, MessageListener listener) {
        RedisMessageListenerContainer listenerContainer = new RedisMessageListenerContainer();
        if (redisTemplate.getConnectionFactory() != null) {
            listenerContainer.setConnectionFactory(redisTemplate.getConnectionFactory());
        }
        listenerContainer.addMessageListener(listener, new ChannelTopic(clusterKey(channel)));
        listenerContainer.afterPropertiesSet();
        listenerContainer.start();

        return listenerContainer;
    }

    public RedisSerializer<?> getSerializer() {
        return redisTemplate.getValueSerializer();
    }


    /**
     * 构建Redis key
     * 根据USE_PROJECT_PREFIX标志决定前缀：
     * - true：使用项目标识前缀（如 gwsu:key）
     * - false：使用服务前缀（如 gwsu:service-name:key）
     *
     * @param key 原始key
     * @return 带前缀的完整key
     */
    private String clusterKey(String key) {
        if (Boolean.TRUE.equals(USE_PROJECT_PREFIX.orElse(false))) {
            return getProjectPrefix() + key;
        }
        return projectUtils.getServerPrefix() + ":" + key;
    }

    /**
     * 构建带锁前缀的Redis key
     *
     * @param key 原始key
     * @return 带锁前缀的完整key
     */
    private String lockKey(String key) {
        return getProjectPrefix() + LOCK_PREFIX + key;
    }

    /**
     * 获取项目前缀
     *
     * @return 项目前缀字符串
     */
    private String getProjectPrefix() {
        return projectUtils.getProjectIdent() + ":";
    }

    /**
     * 统一给脚本key添加{项目标识}前缀
     *
     * @param keys
     * @return
     */
    private List<String> createScriptKey(List<String> keys) {

        if (CollectionUtils.isEmpty(keys)) {
            return keys;
        }

        String prefix = "{%s}:".formatted(projectUtils.getProjectIdent());
        if (!USE_PROJECT_PREFIX.orElse(false)) {
            prefix += projectUtils.getApplicationName() + ":";
        }
        List<String> finKeys = new ArrayList<>(keys.size());
        for (String key : keys) {
            finKeys.add(prefix + key);
        }
        return finKeys;

    }

}
