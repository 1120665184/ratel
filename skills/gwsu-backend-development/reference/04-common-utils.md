# 四、公共模块工具类

编写后端代码时**优先使用 common 模块工具类**，而非第三方工具。

---

## 4.1 common-core

### AssertUtils — 参数验证

Controller/Service 层参数校验，失败抛 `ArgumentException`。所有方法支持 `(value, error, message, args)` 重载。

| 方法 | 说明 |
|------|------|
| `hasText(T, ReturnCode)` | 字符串不为空白 |
| `notEmpty(T, ReturnCode)` | 字符串/数组/集合/Map 不为空 |
| `notNull(T, ReturnCode)` | 对象不为 null |
| `checkBetween(int/long/double, min, max, ReturnCode)` | 数值范围 |
| `isTrue(boolean, ReturnCode)` | 表达式为 true |
| `equals(Object, Object, ReturnCode)` | 两对象相等 |

```java
AssertUtils.hasText(name, XxxErrorCode.E00001);
AssertUtils.notEmpty(ids, XxxErrorCode.E00002);
AssertUtils.checkBetween(age, 0, 150, XxxErrorCode.E00003);
```

### R\<T\> — 统一响应

```java
R.ok(data)           // 成功带数据
R.ok()               // 成功无数据
R.fail("msg")        // 失败带消息
R.fail(exception)    // 失败带异常
```

### SpringUtils — Spring 上下文

| 方法 | 说明 |
|------|------|
| `getBean(Class<T>)` | 按类型获取 Bean |
| `getBeansOfType(Class<T>)` | 获取某类型所有 Bean |
| `getAopProxy(T)` | 获取 AOP 代理对象 |

### ServletUtils — HTTP 请求

| 方法 | 说明 |
|------|------|
| `getRequest()` | 获取当前 HttpServletRequest |
| `getClientIP()` | 获取客户端 IP |
| `getHeaders()` | 获取当前请求头 Map |
| `LOCAL_HEADERS` | TransmittableThreadLocal，跨线程传递请求头 |

### DeployUtils — 部署模式

`DeployUtils.isSingle()` → 判断是否单应用部署

### ProjectUtils — 项目信息（Spring Bean 注入）

`getServerPrefix()` → `"gwsu:gwsu-security"` 格式的服务前缀

### 异常体系

- `BusinessException` — 业务异常（配合错误码枚举使用）
- `ArgumentException` — 参数校验异常（AssertUtils 内部使用）
- `GlobalExceptionHandler` — 全局异常处理器（自动捕获，无需手动配置）

---

## 4.2 common-api

### FeignUtils — 微服务响应处理

`FeignUtils.data(R<T> r)` — 获取微服务接口返回数据，失败时抛 `BusinessException`

```java
UserData data = FeignUtils.data(userClientApi.getById(id));
```

---

## 4.3 common-cache

### CacheUtils — Redis 缓存（Spring Bean 注入）

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

### IDGenerationUtils — 分布式 ID（Spring Bean 注入）

```java
String id = idGen.generateNextIdStr();           // 默认名称
String id = idGen.generateNextIdStr("order");    // 自定义名称
```

### @DistributedLock — 声明式分布式锁

```java
@DistributedLock(name = "'order:' + #orderId", waitTime = 5, leaseTime = 60)
public void processOrder(String orderId) { ... }
```

---

## 4.4 common-database

### DatabaseHelper — 数据库信息（Spring Bean 注入）

`getCurrentDatabaseType()` → 当前数据库类型枚举

### 多数据源

```java
@DS("master")  public void queryMasterDb() { ... }
@DS("mysql")   public void queryMysqlDb() { ... }
```

### 审计字段自动填充

`DefaultMetaObjectHandler` 自动填充 createOp/createTime/modifyOp/modifyTime，继承 `BaseDO` 自动生效。

### 雪花 ID

`DefaultIdentifierGenerator` 配合 `@TableId(type = IdType.ASSIGN_ID)` 自动生成。

---

## 4.5 common-security

### SecurityUtils — 安全信息

| 方法 | 说明 |
|------|------|
| `getCurrentUsername()` | 当前登录用户名 |
| `getToken()` | 当前认证 Token |
| `getUserInfo()` | 当前用户完整信息 |

### SessionUtils — 会话管理

```java
Optional<User> user = SessionUtils.getValue("currentUser");
SessionUtils.putValue("currentUser", user);
```

---

## 4.6 common-authentication

### LoginInterceptorUtils — 登录拦截器事件

`fireAfterAuthenticated/fireAfterLoginSuccess/fireAfterLoginFailure` — 触发登录流程事件

### LogicUtils — 认证逻辑

`LogicUtils.getLogic()` → 获取当前 `StpLogic` 实例
