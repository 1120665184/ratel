# common-security — ABAC 权限与安全

## SecurityUtils — 安全信息（Spring Bean 注入）

| 方法 | 说明 |
|------|------|
| `getUsername()` | 当前登录用户名（从请求头获取） |
| `getToken()` | 当前认证 Token（JWT 校验） |
| `userInfo()` | 当前用户信息 `Optional<UserInfo>` |
| `clientInfo()` | 当前客户端信息 `Optional<ClientInfo>` |
| `getSubject()` | 当前登录主体 `Optional<Subject>` |

## SessionUtils — 会话管理（Spring Bean 注入）

| 方法 | 说明 |
|------|------|
| `getValue(key)` | 获取会话属性 `Optional<V>` |
| `putValue(key, value)` | 设置会话属性 |
| `getVisitorType()` | 获取访问者类型（USER/HEADLESS 等） |
| `getLoginType()` | 获取登录类型 |

```java
Optional<User> user = sessionUtils.getValue("currentUser");
sessionUtils.putValue("currentUser", user);
```

## DataPermissionUtils — 数据权限（Spring Bean 注入）

| 方法 | 说明 |
|------|------|
| `getUserDataPermission()` | 获取当前用户数据权限信息 |
| `applyDataPermission(sql)` | 对 SQL 追加数据权限过滤条件 |
| `setDataPermission(info)` / `clearDataPermission()` | ThreadLocal 缓存管理 |

## Casbin 配置

| 类 | 说明 |
|----|------|
| `CasbinConfiguration` | Casbin 执行器自动配置 |
| `RedisAdapter` | Casbin 策略 Redis 存储 |
| `RedisWatcher` | Casbin 策略变更 Redis 监听 |
| `FieldEnforcer` | 字段级权限执行器 |

## 自定义 Casbin 函数

| 函数 | 说明 |
|------|------|
| `ContainsFunction` | 集合包含判断 |
| `IsUserLoginFunction` | 用户登录判断 |
| `TimeInRangeFunction` | 时间范围判断 |
| `CycleWeeklyFunction` | 周循环判断 |
| `CycleMonthlyFunction` | 月循环判断 |

## 注解

| 注解 | 目标 | 说明 |
|------|------|------|
| `@TableModelPermission` | TYPE/METHOD | Controller 表模型权限声明 |
| `@TableModelField` | FIELD | 字段级权限（隐藏/脱敏） |
| `@LoginAllowAccess` | TYPE/METHOD | 登录后即可访问（跳过权限校验） |
| `@SensitiveStrategy` | FIELD（枚举） | 脱敏策略 |

## ApiEndpointCollector — 端点收集器

启动时自动收集所有 `@RestController` 端点信息，供 ABAC 权限策略使用。

## DataResourceInterceptor — 数据资源拦截器

基于 `DataResourceConditionBuilder` 对查询 SQL 自动追加数据资源过滤条件。
