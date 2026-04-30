# 六、公共模块工具类完整指南

本章节详细列出 common 各模块中所有工具类的使用方式。**编写后端代码时优先使用这些工具类**，而非 Spring Boot / Apache Commons / Hutool 等第三方工具。

---

## 6.1 common-core 模块

> 包路径前缀：`org.quyq.gwsu.common.core`

### 6.1.1 AssertUtils — 统一参数验证

> 包路径：`org.quyq.gwsu.common.core.utils.AssertUtils`

用于 Controller 和 Service 层的参数校验，校验失败时抛出 `ArgumentException`。所有方法均支持传入 `ReturnCode` 错误码，可选传入自定义消息和格式化参数。

| 方法 | 说明 | 示例 |
|------|------|------|
| `hasText(T value, ReturnCode error)` | 断言字符串不为空白 | `AssertUtils.hasText(name, XxxErrorCode.E00001)` |
| `hasText(T value, ReturnCode error, String message, Object... args)` | 断言字符串不为空白，带自定义消息 | `AssertUtils.hasText(name, E00001, "名称不能为空: {}", name)` |
| `notEmpty(T value, ReturnCode error)` | 断言字符串/数组/集合/Map 不为空 | `AssertUtils.notEmpty(list, XxxErrorCode.E00002)` |
| `notEmpty(T[] array, ReturnCode error)` | 断言数组不为空 | `AssertUtils.notEmpty(ids, XxxErrorCode.E00003)` |
| `notEmpty(Collection<E> collection, ReturnCode error)` | 断言集合不为空 | `AssertUtils.notEmpty(userList, XxxErrorCode.E00004)` |
| `notEmpty(Map<K,V> map, ReturnCode error)` | 断言 Map 不为空 | `AssertUtils.notEmpty(params, XxxErrorCode.E00005)` |
| `notNull(T obj, ReturnCode error)` | 断言对象不为 null | `AssertUtils.notNull(entity, XxxErrorCode.E00006)` |
| `checkBetween(int/long/double value, min, max, ReturnCode error)` | 校验数值范围 | `AssertUtils.checkBetween(age, 0, 150, XxxErrorCode.E00007)` |
| `checkBetweenObj(Number value, min, max, ReturnCode error)` | 校验 Number 对象范围 | `AssertUtils.checkBetweenObj(count, 0, 100, E00008)` |
| `equals(Object obj1, Object obj2, ReturnCode error)` | 断言两对象相等 | `AssertUtils.equals(status, 1, XxxErrorCode.E00009)` |
| `notEquals(Object obj1, Object obj2, ReturnCode error)` | 断言两对象不相等 | `AssertUtils.notEquals(type, "admin", E00010)` |
| `isTrue(boolean expression, ReturnCode error)` | 断言表达式为 true | `AssertUtils.isTrue(isValid, XxxErrorCode.E00011)` |
| `isFalse(boolean expression, ReturnCode error)` | 断言表达式为 false | `AssertUtils.isFalse(isLocked, XxxErrorCode.E00012)` |

**所有方法均支持可选的 `(value, error, message, args)` 重载形式**，用于自定义错误消息。

---

### 6.1.2 SpringUtils — Spring 上下文工具

> 包路径：`org.quyq.gwsu.common.core.utils.SpringUtils`

用于在非 Spring 管理的环境中获取 Bean。实现了 `BeanFactoryPostProcessor`，自动注册到 Spring 容器。

| 方法 | 说明 | 示例 |
|------|------|------|
| `getBean(String name)` | 按名称获取 Bean | `SpringUtils.getBean("myService")` |
| `getBean(Class<T> clz)` | 按类型获取 Bean | `SpringUtils.getBean(MyService.class)` |
| `getBeansOfType(Class<T> clz)` | 获取某类型的所有 Bean | `SpringUtils.getBeansOfType(MyService.class)` |
| `containsBean(String name)` | 检查 Bean 是否存在 | `SpringUtils.containsBean("myService")` |
| `isSingleton(String name)` | 判断是否单例 | `SpringUtils.isSingleton("myService")` |
| `getType(String name)` | 获取 Bean 的类型 | `SpringUtils.getType("myService")` |
| `getAliases(String name)` | 获取 Bean 的别名 | `SpringUtils.getAliases("myService")` |
| `getAopProxy(T invoker)` | 获取 AOP 代理对象 | `SpringUtils.getAopProxy(myService)` |

---

### 6.1.3 ServletUtils — HTTP 请求工具

> 包路径：`org.quyq.gwsu.common.core.utils.ServletUtils`

用于获取 HTTP 请求/响应/会话信息，以及客户端 IP 等常用操作。所有方法均为静态方法。

| 方法 | 说明 | 示例 |
|------|------|------|
| `getParameter(String name)` | 获取请求参数（String） | `ServletUtils.getParameter("id")` |
| `getParameter(String name, String defaultValue)` | 获取请求参数，带默认值 | `ServletUtils.getParameter("page", "1")` |
| `getParameterToInt(String name)` | 获取请求参数（Integer） | `ServletUtils.getParameterToInt("size")` |
| `getParameterToInt(String name, Integer defaultValue)` | 获取请求参数，带默认值 | `ServletUtils.getParameterToInt("size", 10)` |
| `getRequest()` | 获取当前 HttpServletRequest | `ServletUtils.getRequest()` |
| `getResponse()` | 获取当前 HttpServletResponse | `ServletUtils.getResponse()` |
| `getSession()` | 获取当前 HttpSession | `ServletUtils.getSession()` |
| `getRequestAttributes()` | 获取 ServletRequestAttributes | `ServletUtils.getRequestAttributes()` |
| `getHeaders(HttpServletRequest request)` | 获取请求头 Map | `ServletUtils.getHeaders(request)` |
| `getHeaders()` | 获取当前请求头 Map（优先从线程本地变量获取） | `ServletUtils.getHeaders()` |
| `getClientIP(String... otherHeaderNames)` | 获取客户端 IP | `ServletUtils.getClientIP()` |
| `getClientIP(HttpServletRequest request, String... otherHeaderNames)` | 获取客户端 IP | `ServletUtils.getClientIP(request)` |
| `getClientIPByHeader(HttpServletRequest request, String... headerNames)` | 按指定头获取客户端 IP | `ServletUtils.getClientIPByHeader(request, "X-Real-IP")` |
| `urlEncode(String str)` | URL 编码（UTF-8） | `ServletUtils.urlEncode("中文")` |
| `urlDecode(String str)` | URL 解码（UTF-8） | `ServletUtils.urlDecode("%E4%B8%AD%E6%96%87")` |

**线程本地变量**：`ServletUtils.LOCAL_HEADERS`（`TransmittableThreadLocal`），用于跨线程传递请求头。

---

### 6.1.4 ProjectUtils — 项目信息工具

> 包路径：`org.quyq.gwsu.common.core.utils.ProjectUtils`

Spring Bean，通过 `@Resource` 注入使用。提供项目标识和服务前缀信息。

| 方法 | 说明 | 示例 |
|------|------|------|
| `getProjectIdent()` | 获取项目标识（如 `gwsu`） | `projectUtils.getProjectIdent()` |
| `getApplicationName()` | 获取当前应用名称 | `projectUtils.getApplicationName()` |
| `getServerPrefix()` | 获取服务统一前缀（格式：`{项目标识}:{服务名}`） | `projectUtils.getServerPrefix()` → `"gwsu:gwsu-security"` |

---

### 6.1.5 DeployUtils — 部署模式工具

> 包路径：`org.quyq.gwsu.common.core.utils.DeployUtils`

静态工具类，用于判断当前部署模式。

| 方法 | 说明 | 示例 |
|------|------|------|
| `isSingle()` | 是否单应用部署模式 | `DeployUtils.isSingle()` → `true`/`false` |

---

### 6.1.6 ProxyUtil — 类检测工具

> 包路径：`org.quyq.gwsu.common.core.utils.ProxyUtil`

静态工具类，用于检测类是否存在于 classpath。

| 方法 | 说明 | 示例 |
|------|------|------|
| `hasClass(String classname)` | 检查类是否存在 | `ProxyUtil.hasClass("com.alibaba.nacos.client.NacosDiscoveryAutoConfiguration")` |

---

### 6.1.7 R\<T\> — 统一响应类型

> 包路径：`org.quyq.gwsu.common.core.domain.R`

所有 API 响应统一使用的 record 类型。

| 方法 | 说明 | 示例 |
|------|------|------|
| `R.ok(data)` | 成功，带数据 | `R.ok(userVo)` |
| `R.ok(data, "msg")` | 成功，带数据和自定义消息 | `R.ok(list, "查询成功")` |
| `R.ok()` | 成功，无数据 | `R.ok()` |
| `R.fail("msg")` | 失败，带消息 | `R.fail("操作失败")` |
| `R.fail(exception)` | 失败，带异常 | `R.fail(basicException)` |

---

### 6.1.8 异常处理体系

#### BusinessException — 业务异常

> 包路径：`org.quyq.gwsu.common.core.exception.BusinessException`

```java
// 标准方式
throw new BusinessException(XxxErrorCode.E00001);

// 带自定义消息
throw new BusinessException(XxxErrorCode.E00001, "自定义错误消息");

// 仅消息（使用默认错误码）
throw new BusinessException("操作失败");
```

#### ArgumentException — 参数校验异常

> 包路径：`org.quyq.gwsu.common.core.exception.ArgumentException`

由 `AssertUtils` 内部使用，一般不需要直接抛出。

#### ExceptionMsgHandler — 异常消息处理

> 包路径：`org.quyq.gwsu.common.core.exception.ExceptionMsgHandler`

| 方法 | 说明 |
|------|------|
| `determineErrorInfo(Throwable ex)` | 根据异常类型确定 HTTP 状态码和响应结果，返回 `ErrorInfo` record |

#### GlobalExceptionHandler — 全局异常处理器

> 包路径：`org.quyq.gwsu.common.core.exception.GlobalExceptionHandler`

自动捕获 REST 控制器抛出的异常并转换为统一 `R` 响应，无需手动配置。

#### GlobalExceptionFunctionHandler — 函数式异常处理

> 包路径：`org.quyq.gwsu.common.core.exception.GlobalExceptionFunctionHandler`

提供函数式风格的异常处理，用于需要精细化异常处理的场景。

---

### 6.1.9 请求处理链

#### ProcessorChain — 责任链

> 包路径：`org.quyq.gwsu.common.core.chain.ProcessorChain`

用于按顺序处理 HTTP 请求/响应，支持注册多个 `RequestResponseProcessor`。

#### RequestResponseContext — 请求上下文

> 包路径：`org.quyq.gwsu.common.core.chain.RequestResponseContext`

封装 `HttpServletRequest` 和 `HttpServletResponse` 的上下文对象。

#### RequestResponseProcessor — 处理器接口

> 包路径：`org.quyq.gwsu.common.core.chain.RequestResponseProcessor`

```java
public interface RequestResponseProcessor {
    void process(RequestResponseContext context);
}
```

---

## 6.2 common-api 模块

> 包路径前缀：`org.quyq.gwsu.common.api`

### 6.2.1 FeignUtils — 微服务响应处理

> 包路径：`org.quyq.gwsu.common.api.utils.FeignUtils`

用于处理微服务间调用返回的 `R<T>` 响应，自动解包数据或抛出异常。

| 方法 | 说明 | 示例 |
|------|------|------|
| `data(R<T> r)` | 获取微服务接口返回数据对象，失败时抛出 `BusinessException` | `UserData data = FeignUtils.data(userClientApi.getById(id))` |

---

### 6.2.2 CircuitBreakerConfigResolver — 熔断器配置解析

> 包路径：`org.quyq.gwsu.common.api.utils.CircuitBreakerConfigResolver`

自动解析 `@ApiClient` 注解和 `@CircuitBreaker` 方法注解上的熔断配置，优先级：方法注解 > 类注解 > 配置文件。一般无需手动调用。

---

## 6.3 common-cache 模块

> 包路径前缀：`org.quyq.gwsu.common.cache`

### 6.3.1 CacheUtils — Redis 缓存操作

> 包路径：`org.quyq.gwsu.common.cache.utils.CacheUtils`

Spring Bean，通过 `@Resource` 注入使用。基于 `RedisTemplate` + `RedissonClient` 的综合 Redis 工具类，**所有 key 操作均自动添加服务前缀**。

#### 跳脱者模式（跨服务 Redis 操作）

```java
// 在 withRebel 内，key 前缀为 "项目标识:key"（如 gwsu:key）
// 在 withRebel 外，key 前缀为 "项目标识:服务名:key"（如 gwsu:gwsu-security:key）
cacheUtils.withRebel(() -> {
    return cacheUtils.get("sharedKey");
});
```

#### String 操作

| 方法 | 说明 | 示例 |
|------|------|------|
| `set(String key, T value)` | 设置值 | `cacheUtils.set("user:1", userVo)` |
| `set(String key, T value, Duration ttl)` | 设置值并指定过期时间 | `cacheUtils.set("token", token, Duration.ofHours(2))` |
| `set(String key, T value, long timeout, TimeUnit unit)` | 设置值，指定过期时间和单位 | `cacheUtils.set("code", code, 30, TimeUnit.MINUTES)` |
| `setIfAbsent(String key, T value, Duration ttl)` | 仅当 key 不存在时设置（防重复） | `cacheUtils.setIfAbsent("lock:order", "1", Duration.ofSeconds(10))` |
| `setIfPresent(String key, T value, Duration ttl)` | 仅当 key 存在时设置 | `cacheUtils.setIfPresent("config", newVal, Duration.ofHours(1))` |
| `get(String key)` | 获取值 | `UserVO user = cacheUtils.get("user:1")` |
| `getAndSet(String key, T value)` | 设置新值并返回旧值 | `String old = cacheUtils.getAndSet("version", "2")` |
| `increment(String key)` | 自增 1 | `cacheUtils.increment("counter")` |
| `increment(String key, long delta)` | 自增指定增量 | `cacheUtils.increment("counter", 10)` |
| `decrement(String key)` | 自减 1 | `cacheUtils.decrement("counter")` |
| `decrement(String key, long delta)` | 自减指定减量 | `cacheUtils.decrement("counter", 5)` |

#### Hash 操作

| 方法 | 说明 | 示例 |
|------|------|------|
| `hSet(String key, String hashKey, T value)` | 设置 Hash 字段 | `cacheUtils.hSet("user:1", "name", "张三")` |
| `hSetAll(String key, Map<String,T> map)` | 批量设置 Hash 字段 | `cacheUtils.hSetAll("user:1", fieldMap)` |
| `hGet(String key, String hashKey)` | 获取 Hash 字段值 | `String name = cacheUtils.hGet("user:1", "name")` |
| `hGetAll(String key, Class<T> type)` | 获取 Hash 所有字段，按类型过滤 | `Map<String, String> map = cacheUtils.hGetAll("user:1", String.class)` |
| `hDelete(String key, Object... hashKeys)` | 删除 Hash 字段 | `cacheUtils.hDelete("user:1", "name", "age")` |
| `hExists(String key, String hashKey)` | 判断 Hash 字段是否存在 | `cacheUtils.hExists("user:1", "name")` |
| `hSize(String key)` | 获取 Hash 字段数量 | `cacheUtils.hSize("user:1")` |
| `hIncrBy(String key, String hashKey, long delta)` | Hash 字段自增 | `cacheUtils.hIncrBy("stats", "views", 1)` |
| `hIncrByFloat(String key, String hashKey, double delta)` | Hash 字段自增浮点数 | `cacheUtils.hIncrByFloat("stats", "score", 0.5)` |
| `hKeys(String key)` | 获取 Hash 所有字段名 | `cacheUtils.hKeys("user:1")` |
| `hValues(String key)` | 获取 Hash 所有值 | `cacheUtils.hValues("user:1")` |

#### List 操作

| 方法 | 说明 | 示例 |
|------|------|------|
| `lPush(String key, Object... values)` | 左侧（头部）添加 | `cacheUtils.lPush("queue", "a", "b")` |
| `rPush(String key, Object... values)` | 右侧（尾部）添加 | `cacheUtils.rPush("queue", "a", "b")` |
| `lPop(String key)` | 左侧弹出 | `String val = cacheUtils.lPop("queue")` |
| `rPop(String key)` | 右侧弹出 | `String val = cacheUtils.rPop("queue")` |
| `rPop(String key, long timeout, TimeUnit unit)` | 右侧弹出，带超时阻塞 | `String val = cacheUtils.rPop("queue", 5, TimeUnit.SECONDS)` |
| `lRange(String key, long start, long end)` | 获取指定范围元素 | `List<String> list = cacheUtils.lRange("queue", 0, -1)` |
| `lSize(String key)` | 获取列表长度 | `cacheUtils.lSize("queue")` |

#### Set 操作

| 方法 | 说明 | 示例 |
|------|------|------|
| `sAdd(String key, Object... values)` | 添加成员 | `cacheUtils.sAdd("tags", "java", "spring")` |
| `sRemove(String key, Object... values)` | 移除成员 | `cacheUtils.sRemove("tags", "java")` |
| `sMembers(String key)` | 获取所有成员 | `Set<String> tags = cacheUtils.sMembers("tags")` |
| `sIsMember(String key, Object value)` | 判断成员是否存在 | `cacheUtils.sIsMember("tags", "java")` |
| `sSize(String key)` | 获取成员数量 | `cacheUtils.sSize("tags")` |
| `sIntersect(String key, String otherKey)` | 获取交集 | `cacheUtils.sIntersect("set1", "set2")` |
| `sUnion(String key, String otherKey)` | 获取并集 | `cacheUtils.sUnion("set1", "set2")` |
| `sDiff(String key, String otherKey)` | 获取差集 | `cacheUtils.sDiff("set1", "set2")` |

#### Sorted Set 操作

| 方法 | 说明 | 示例 |
|------|------|------|
| `zAdd(String key, Object value, double score)` | 添加成员和分数 | `cacheUtils.zAdd("rank", "user1", 100)` |
| `zRemove(String key, Object... values)` | 移除成员 | `cacheUtils.zRemove("rank", "user1")` |
| `zRange(String key, long start, long end)` | 按分数升序获取 | `cacheUtils.zRange("rank", 0, 9)` |
| `zReverseRange(String key, long start, long end)` | 按分数降序获取 | `cacheUtils.zReverseRange("rank", 0, 9)` |
| `zRangeByScore(String key, double min, double max)` | 按分数范围获取 | `cacheUtils.zRangeByScore("rank", 0, 100)` |
| `zScore(String key, Object value)` | 获取成员分数 | `cacheUtils.zScore("rank", "user1")` |
| `zRank(String key, Object value)` | 获取升序排名 | `cacheUtils.zRank("rank", "user1")` |
| `zReverseRank(String key, Object value)` | 获取降序排名 | `cacheUtils.zReverseRank("rank", "user1")` |
| `zSize(String key)` | 获取成员数量 | `cacheUtils.zSize("rank")` |

#### Key 操作

| 方法 | 说明 | 示例 |
|------|------|------|
| `exists(String key)` | 检查 key 是否存在 | `cacheUtils.exists("user:1")` |
| `expire(String key, Duration ttl)` | 设置过期时间 | `cacheUtils.expire("user:1", Duration.ofHours(1))` |
| `expire(String key, long timeout, TimeUnit unit)` | 设置过期时间 | `cacheUtils.expire("user:1", 30, TimeUnit.MINUTES)` |
| `getExpire(String key)` | 获取剩余过期时间（秒） | `cacheUtils.getExpire("user:1")` |
| `getExpire(String key, TimeUnit unit)` | 获取剩余过期时间 | `cacheUtils.getExpire("user:1", TimeUnit.MINUTES)` |
| `persist(String key)` | 移除过期时间，永久存在 | `cacheUtils.persist("user:1")` |
| `delete(String key)` | 删除 key | `cacheUtils.delete("user:1")` |
| `delete(Collection<String> keys)` | 批量删除 | `cacheUtils.delete(List.of("k1", "k2"))` |
| `scan(String pattern)` | 按模式匹配扫描 keys | `cacheUtils.scan("user:*")` |
| `rename(String oldKey, String newKey)` | 重命名 key | `cacheUtils.rename("old", "new")` |

#### 分布式锁

| 方法 | 说明 | 示例 |
|------|------|------|
| `executeWithLock(String key, Supplier<T> action)` | 带锁执行（默认等待10秒，持有30秒） | `cacheUtils.executeWithLock("order:123", () -> processOrder())` |
| `executeWithLock(String key, long waitTime, long leaseTime, TimeUnit unit, Supplier<T> action)` | 带锁执行，自定义时间 | `cacheUtils.executeWithLock("order:123", 5, 60, TimeUnit.SECONDS, () -> processOrder())` |
| `executeWithLock(String key, Runnable action)` | 带锁执行，无返回值 | `cacheUtils.executeWithLock("order:123", () -> doSomething())` |
| `executeWithLock(String key, long waitTime, long leaseTime, TimeUnit unit, Runnable action)` | 带锁执行，无返回值，自定义时间 | `cacheUtils.executeWithLock("order:123", 5, 60, TimeUnit.SECONDS, () -> doSomething())` |

#### 限流器

| 方法 | 说明 | 示例 |
|------|------|------|
| `acquireRateLimit(String key, int maxAttempts, Duration window)` | 尝试获取限流许可 | `cacheUtils.acquireRateLimit("api:login", 5, Duration.ofMinutes(1))` |
| `acquireRateLimit(String key, int maxAttempts, long window, TimeUnit unit)` | 尝试获取限流许可 | `cacheUtils.acquireRateLimit("api:login", 5, 1, TimeUnit.MINUTES)` |

#### 信号量（分布式许可）

| 方法 | 说明 | 示例 |
|------|------|------|
| `getSemaphore(String key)` | 获取信号量对象 | `RSemaphore sem = cacheUtils.getSemaphore("pool")` |
| `tryAcquirePermit(String key, int permits, long waitTime, TimeUnit unit)` | 尝试获取许可 | `cacheUtils.tryAcquirePermit("pool", 1, 5, TimeUnit.SECONDS)` |
| `releasePermit(String key, int permits)` | 释放许可 | `cacheUtils.releasePermit("pool", 1)` |

#### Lua 脚本

| 方法 | 说明 | 示例 |
|------|------|------|
| `executeScript(RedisScript<T> script, List<String> keys, Object... args)` | 执行 Lua 脚本 | `cacheUtils.executeScript(myScript, keys, args)` |
| `executeScript(RedisScript<T> script, RedisSerializer<T> resultSerializer, List<String> keys, Object... args)` | 执行 Lua 脚本，自定义序列化器 | `cacheUtils.executeScript(myScript, StringRedisSerializer.UTF_8, keys, args)` |

#### 缓存加载（Cache-Aside 模式）

| 方法 | 说明 | 示例 |
|------|------|------|
| `getOrLoad(String key, Supplier<T> loader, Duration ttl)` | 获取缓存，不存在则加载并缓存 | `cacheUtils.getOrLoad("user:1", () -> userDao.findById(1), Duration.ofHours(1))` |
| `getOrLoad(String key, Supplier<T> loader)` | 获取缓存，默认1小时过期 | `cacheUtils.getOrLoad("user:1", () -> userDao.findById(1))` |

#### 位操作

| 方法 | 说明 | 示例 |
|------|------|------|
| `setBit(String key, long offset, boolean value)` | 设置指定位 | `cacheUtils.setBit("flags", 0, true)` |
| `getBit(String key, long offset)` | 获取指定位 | `cacheUtils.getBit("flags", 0)` |
| `bitCount(String key)` | 统计位值为1的数量 | `cacheUtils.bitCount("flags")` |
| `bitCount(String key, long start, long end)` | 统计指定范围内位值为1的数量 | `cacheUtils.bitCount("flags", 0, 7)` |

#### HyperLogLog 操作

| 方法 | 说明 | 示例 |
|------|------|------|
| `pfAdd(String key, Object... values)` | 添加元素（基数统计） | `cacheUtils.pfAdd("uv", "user1", "user2")` |
| `pfCount(String... keys)` | 获取基数估计值 | `cacheUtils.pfCount("uv")` |
| `pfMerge(String destKey, String... sourceKeys)` | 合并多个 HyperLogLog | `cacheUtils.pfMerge("uv:total", "uv:day1", "uv:day2")` |

#### 地理坐标操作

| 方法 | 说明 | 示例 |
|------|------|------|
| `geoAdd(String key, double lng, double lat, T member)` | 添加地理坐标 | `cacheUtils.geoAdd("stores", 116.4, 39.9, "store1")` |
| `geoRadius(String key, double lng, double lat, double radius, long count)` | 查询半径内成员 | `cacheUtils.geoRadius("stores", 116.4, 39.9, 5, 10)` |
| `geoDist(String key, T member1, T member2)` | 计算两成员间距离 | `cacheUtils.geoDist("stores", "store1", "store2")` |

#### 消息发布/订阅

| 方法 | 说明 | 示例 |
|------|------|------|
| `convertAndSend(String channel, T value)` | 发送消息到指定频道 | `cacheUtils.convertAndSend("events", eventData)` |
| `addListener(String channel, MessageListener listener)` | 添加消息监听器 | `cacheUtils.addListener("events", listener)` |
| `getSerializer()` | 获取 Redis 值序列化器 | `cacheUtils.getSerializer()` |

---

### 6.3.2 IDGenerationUtils — 分布式 ID 生成

> 包路径：`org.quyq.gwsu.common.cache.utils.IDGenerationUtils`

基于 Redis 的分布式唯一 ID 生成器，Spring Bean 注入使用。

| 方法 | 说明 | 示例 |
|------|------|------|
| `generateNextIdStr()` | 生成下一个 ID（字符串，默认名称） | `String id = idGen.generateNextIdStr()` |
| `generateNextIdStr(String name)` | 生成下一个 ID（字符串，自定义名称） | `String id = idGen.generateNextIdStr("order")` |
| `generateNextIdStr(String name, int sequenceBits)` | 生成下一个 ID（字符串，自定义名称和序列位数） | `String id = idGen.generateNextIdStr("order", 6)` |
| `generateNextId()` | 生成下一个 ID（long） | `long id = idGen.generateNextId()` |
| `generateNextId(String name)` | 生成下一个 ID（long，自定义名称） | `long id = idGen.generateNextId("order")` |
| `generateNextId(String name, int sequenceBits)` | 生成下一个 ID（long，自定义名称和序列位数） | `long id = idGen.generateNextId("order", 6)` |

---

### 6.3.3 @DistributedLock — 分布式锁注解

> 包路径：`org.quyq.gwsu.common.cache.annotation.DistributedLock`

声明式分布式锁，基于 Redisson 实现，支持 SpEL 表达式。

| 属性 | 说明 | 默认值 |
|------|------|--------|
| `name` | 锁名称，支持 SpEL 表达式 | - |
| `waitTime` | 等待获取锁的时间 | 10 |
| `leaseTime` | 锁持有时间 | 30 |
| `timeUnit` | 时间单位 | `TimeUnit.SECONDS` |

**使用示例**：

```java
@DistributedLock(name = "'order:' + #orderId", waitTime = 5, leaseTime = 60)
public void processOrder(String orderId) {
    // 业务逻辑
}
```

---

## 6.4 common-database 模块

> 包路径前缀：`org.quyq.gwsu.common.database`

### 6.4.1 DatabaseHelper — 数据库信息工具

> 包路径：`org.quyq.gwsu.common.database.utils.DatabaseHelper`

Spring Bean，通过 `@Resource` 注入使用。提供当前数据源和数据库类型信息。

| 方法 | 说明 | 示例 |
|------|------|------|
| `getCurrentDatasourceKey()` | 获取当前数据源 Key | `String dsKey = databaseHelper.getCurrentDatasourceKey()` |
| `getCurrentDatabaseType()` | 获取当前数据库类型（枚举） | `DatabaseType dbType = databaseHelper.getCurrentDatabaseType()` |

---

### 6.4.2 多数据源切换

使用 `@DS` 注解切换数据源：

```java
@DS("master")
public void queryMasterDb() { ... }

@DS("mysql")
public void queryMysqlDb() { ... }

@DS("oracle")
public void queryOracleDb() { ... }
```

---

### 6.4.3 ResultSetConverter — ResultSet 转换器

> 包路径：`org.quyq.gwsu.common.database.utils.ResultSetConverter`

将 JDBC `ResultSet` 转换为结构化数据。

| 方法 | 说明 | 示例 |
|------|------|------|
| `convert(ResultSet rs)` | 将 ResultSet 转换为 `List<String[]>` | `List<String[]> rows = ResultSetConverter.convert(rs)` |

---

### 6.4.4 SqlExecutor — SQL 执行工具

> 包路径：`org.quyq.gwsu.common.database.utils.SqlExecutor`

直接执行 SQL 并返回结果数组。

| 方法 | 说明 | 示例 |
|------|------|------|
| `executeSqlAndReturnArr(Connection conn, String sql)` | 执行 SQL 并返回二维字符串数组 | `String[][] result = SqlExecutor.executeSqlAndReturnArr(conn, "SELECT * FROM t")` |
| `executeSqlAndReturnArr(Connection conn, String sql, String database)` | 执行 SQL，指定数据库名 | `SqlExecutor.executeSqlAndReturnArr(conn, sql, "mydb")` |

---

### 6.4.5 DefaultMetaObjectHandler — 审计字段自动填充

> 包路径：`org.quyq.gwsu.common.database.handler.DefaultMetaObjectHandler`

MyBatis-Plus 元对象处理器，自动填充审计字段（createOp、createTime、modifyOp、modifyTime），无需手动调用，继承 `BaseDO` 的实体自动生效。

---

### 6.4.6 DefaultIdentifierGenerator — 雪花 ID 生成器

> 包路径：`org.quyq.gwsu.common.database.id.DefaultIdentifierGenerator`

MyBatis-Plus ID 生成器，生成雪花算法 ID。配合 `@TableId(type = IdType.ASSIGN_ID)` 使用，无需手动调用。

---

### 6.4.7 DynamicDatabaseIdProvider — 动态数据库标识

> 包路径：`org.quyq.gwsu.common.database.provider.DynamicDatabaseIdProvider`

MyBatis 数据库标识提供者，根据当前数据源自动判断数据库产品名称，用于支持多数据源下的 `databaseId` 区分。

| 方法 | 说明 |
|------|------|
| `getDatabaseId(DataSource dataSource)` | 获取数据库产品标识 |

---

## 6.5 common-security 模块

> 包路径前缀：`org.quyq.gwsu.common.security`

### 6.5.1 SecurityUtils — 安全信息工具

> 包路径：`org.quyq.gwsu.common.security.utils.SecurityUtils`

获取当前登录用户的认证信息。

| 方法 | 说明 | 示例 |
|------|------|------|
| 获取当前用户名 | 获取当前登录用户名 | `SecurityUtils.getCurrentUsername()` |
| 获取当前 Token | 获取当前认证 Token | `SecurityUtils.getToken()` |
| 获取客户端信息 | 获取客户端标识 | `SecurityUtils.getClientInfo()` |
| 获取用户信息 | 获取当前用户完整信息 | `SecurityUtils.getUserInfo()` |
| 获取 Subject | 获取认证主体 | `SecurityUtils.getSubject()` |
| Token 验证 | 验证 Token 有效性 | `SecurityUtils.verifyToken()` |
| Token 解析 | 解析 Token 获取信息 | `SecurityUtils.parseToken()` |

---

### 6.5.2 SessionUtils — 会话管理工具

> 包路径：`org.quyq.gwsu.common.security.utils.SessionUtils`

基于 Sa-Token 的会话管理工具，支持 Redis 持久化。

| 方法 | 说明 | 示例 |
|------|------|------|
| `getValue(String key)` | 获取会话值 | `Optional<User> user = SessionUtils.getValue("currentUser")` |
| `putValue(String key, V value)` | 存储会话值 | `SessionUtils.putValue("currentUser", user)` |

---

### 6.5.3 DataResourceRuleUtils — 数据资源规则工具

> 包路径：`org.quyq.gwsu.common.security.utils.DataResourceRuleUtils`

用于获取和管理 ABAC 数据资源权限规则。

| 方法 | 说明 |
|------|------|
| 从 Redis 获取数据资源规则 | 获取 ABAC 数据权限规则 |
| 按表名分组规则 | 将规则按数据库表名分组 |
| 按数据库和表名过滤规则 | 获取特定表的数据权限规则 |

---

## 6.6 common-authentication 模块

> 包路径前缀：`org.quyq.gwsu.common.authentication`

### 6.6.1 LoginInterceptorUtils — 登录拦截器事件工具

> 包路径：`org.quyq.gwsu.common.authentication.utils.LoginInterceptorUtils`

用于触发登录流程中的各种事件（认证成功、登录成功、登录失败）。

| 方法 | 说明 | 示例 |
|------|------|------|
| `setInterceptors(List<LoginInterceptor<?>> interceptorList)` | 设置拦截器列表 | `LoginInterceptorUtils.setInterceptors(interceptors)` |
| `fireAfterAuthenticated(String loginType, LoginInterceptorContext<?> context)` | 触发认证成功事件 | `LoginInterceptorUtils.fireAfterAuthenticated("password", ctx)` |
| `fireAfterLoginSuccess(String loginType, LoginInterceptorContext<?> context)` | 触发登录成功事件 | `LoginInterceptorUtils.fireAfterLoginSuccess("password", ctx)` |
| `fireAfterLoginFailure(String loginType, LoginInterceptorContext<?> context, Throwable exception)` | 触发登录失败事件 | `LoginInterceptorUtils.fireAfterLoginFailure("password", ctx, ex)` |

---

### 6.6.2 LogicUtils — 认证逻辑工具

> 包路径：`org.quyq.gwsu.common.authentication.utils.LogicUtils`

获取当前认证的 Sa-Token 逻辑实例。

| 方法 | 说明 | 示例 |
|------|------|------|
| `getLogic()` | 获取当前认证的 `StpLogic` 实例 | `Optional<StpLogic> logic = LogicUtils.getLogic()` |
