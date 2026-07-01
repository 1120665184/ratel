# common-cache — Redis 缓存与分布式锁

## CacheUtils — Redis 缓存（Spring Bean 注入）

**所有 key 操作自动添加服务前缀**。跨服务操作用 `withRebel()`：

```java
cacheUtils.withRebel(() -> cacheUtils.get("sharedKey"));
```

**常用方法**：

| 类别 | 方法 | 说明 |
|------|------|------|
| String | `set(key, value, Duration)` / `get(key)` | 基础读写 |
| String | `setIfAbsent(key, value, Duration)` | 防重复设置 |
| String | `increment(key)` / `decrement(key)` | 自增/自减 |
| Hash | `hSet/hGet/hGetAll/hDelete` | Hash 操作 |
| List | `lPush/rPush/lPop/rPop/lRange` | List 操作 |
| Set | `sAdd/sMembers/sIsMember/sRemove` | Set 操作 |
| SortedSet | `zAdd/zRange/zReverseRange/zScore` | 有序集合 |
| Key | `exists/expire/delete/scan` | Key 管理 |
| 缓存 | `getOrLoad(key, loader, Duration)` | Cache-Aside 模式 |
| 锁 | `executeWithLock(key, Supplier)` | 分布式锁（默认等10s持30s） |
| 限流 | `acquireRateLimit(key, maxAttempts, Duration)` | 限流器 |

```java
@Resource
private CacheUtils cacheUtils;

cacheUtils.set("user:1", userVo, Duration.ofHours(1));
UserVO user = cacheUtils.get("user:1");
UserVO user = cacheUtils.getOrLoad("user:1", () -> userDao.findById(1), Duration.ofHours(1));
cacheUtils.executeWithLock("order:123", () -> processOrder());
```

## IDGenerationUtils — 分布式 ID（Spring Bean 注入）

```java
String id = idGen.generateNextIdStr();           // 默认名称
String id = idGen.generateNextIdStr("order");    // 自定义名称
```

## @DistributedLock — 声明式分布式锁

```java
@DistributedLock(name = "'order:' + #orderId", waitTime = 5, leaseTime = 60)
public void processOrder(String orderId) { ... }
```
