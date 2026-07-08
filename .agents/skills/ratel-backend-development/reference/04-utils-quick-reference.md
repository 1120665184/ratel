# 四、工具类与注解速查表

> 一目了然所有可用工具，详见 → 对应 common 模块文档

## 工具类速查

| 工具类 | 所属模块 | 注入方式 | 简介 | 详见 |
|--------|---------|---------|------|------|
| AssertUtils | common-core | 静态方法 | 参数校验，失败抛 ArgumentException | → [03-01](03-01-common-core.md) |
| R\<T\> | common-core | 静态方法 | 统一 API 响应封装 | → [03-01](03-01-common-core.md) |
| SpringUtils | common-core | 静态方法 | Spring 上下文，获取 Bean/代理 | → [03-01](03-01-common-core.md) |
| ServletUtils | common-core | 静态方法 | HTTP 请求信息，跨线程传递 Header | → [03-01](03-01-common-core.md) |
| DeployUtils | common-core | 静态方法 | 部署模式判断 isSingle() | → [03-01](03-01-common-core.md) |
| ProjectUtils | common-core | Bean 注入 | 项目服务前缀信息 | → [03-01](03-01-common-core.md) |
| ProxyUtil | common-core | 静态方法 | AOT 兼容类检测 hasClass() | → [03-01](03-01-common-core.md) |
| ThreadPoolUtil | common-core | 静态方法 | 上下文传播线程池创建 | → [03-01](03-01-common-core.md) |
| ProcessorChain | common-core | 构造注入 | 请求处理响应式责任链 | → [03-01](03-01-common-core.md) |
| FeignUtils | common-api | 静态方法 | 微服务响应解包 data() | → [03-02](03-02-common-api.md) |
| CacheUtils | common-cache | Bean 注入 | Redis 全操作 + 分布式锁 + 限流 | → [03-03](03-03-common-cache.md) |
| IDGenerationUtils | common-cache | Bean 注入 | 分布式雪花 ID | → [03-03](03-03-common-cache.md) |
| DatabaseHelper | common-database | Bean 注入 | 当前数据库类型判断 | → [03-04](03-04-common-database.md) |
| SqlExecutor | common-database | 静态方法 | 通用 SQL 执行器 | → [03-04](03-04-common-database.md) |
| ResultSetConverter | common-database | 静态方法 | ResultSet 转结构化数据 | → [03-04](03-04-common-database.md) |
| SecurityUtils | common-security | Bean 注入 | 当前用户名/Token/用户信息/主体 | → [03-06](03-06-common-security.md) |
| SessionUtils | common-security | Bean 注入 | 会话属性存取/访问者类型 | → [03-06](03-06-common-security.md) |
| DataPermissionUtils | common-security | Bean 注入 | 数据权限获取与 SQL 过滤 | → [03-06](03-06-common-security.md) |
| LoginInterceptorUtils | common-authentication | 静态方法 | 登录流程事件触发 | → [03-07](03-07-common-authentication.md) |
| LogicUtils | common-authentication | 静态方法 | 获取当前 StpLogic 实例 | → [03-07](03-07-common-authentication.md) |
| AccessLogHandlerService | common-log | Bean 注入 | 操作日志异步处理 | → [03-08](03-08-common-log.md) |

## 注解速查

| 注解 | 所属模块 | 目标 | 简介 | 详见 |
|------|---------|------|------|------|
| @ApiClient | common-api | TYPE | 跨模块服务调用声明 | → [03-02](03-02-common-api.md) |
| @CircuitBreakerCustomConfig | common-api | TYPE/METHOD | 熔断器自定义配置 | → [03-02](03-02-common-api.md) |
| @DistributedLock | common-cache | METHOD | 声明式分布式锁 | → [03-03](03-03-common-cache.md) |
| @DS | common-database | TYPE/METHOD | 多数据源切换 | → [03-04](03-04-common-database.md) |
| @TableModelPermission | common-security | TYPE/METHOD | Controller 表模型权限声明（必须） | → [03-06](03-06-common-security.md) |
| @TableModelField | common-security | FIELD | 字段级权限（隐藏/脱敏） | → [03-06](03-06-common-security.md) |
| @LoginAllowAccess | common-security | TYPE/METHOD | 登录后即可访问 | → [03-06](03-06-common-security.md) |
| @SensitiveStrategy | common-security | FIELD | 脱敏策略（配合 @TableModelField） | → [03-06](03-06-common-security.md) |
| @LogIgnore | common-log | METHOD | 忽略操作日志记录 | → [03-08](03-08-common-log.md) |
| @ErrorCodeMeta | common-core | TYPE | 错误码枚举元信息 | → [01](01-project-and-conventions.md) |

## 领域基类速查

| 类 | 所属模块 | 简介 | 关键字段 |
|----|---------|------|---------|
| BaseVO | common-core | VO/DO 公共基类 | modifyOp/Time, createOp/Time |
| BaseDO | common-core | Domain 基类 | + tenantId, deleted, deleteOp/Time |
| BaseDTO | common-core | 查询参数基类 | pageNum, pageSize, orderByColumn, asc |
| R\<T\> | common-core | 统一响应 | ok/fail 静态工厂 |
| BusinessModuleInfo | common-core | 模块信息 | prefix, description |

## 枚举速查

| 枚举 | 所属模块 | 简介 |
|------|---------|------|
| DatabaseType | common-database | 数据库类型（POSTGRESQL/MYSQL） |
| DataScope | common-security | 数据权限范围（ALL/SELF_ONLY 等） |
| DataResourceAssertType | common-security | 数据资源断言类型 |
| DataResourceFieldConditionType | common-security | 数据资源字段条件类型 |
| AccountType | common-security | 账号类型 |
| VisitorType | common-security | 访问者类型（USER/HEADLESS 等） |
| SensitiveStrategy | common-security | 脱敏策略（NONE/USERNAME/ID_CARD/PHONE/EMAIL/ADDRESS/CUSTOM） |
| TerminalType | common-core | 终端类型 |
| SaveMedium | common-log | 日志存储介质 |
| TableLogSourceType | common-log | 日志来源类型 |
| ViewOperationSubject | common-log | 操作主体 |
