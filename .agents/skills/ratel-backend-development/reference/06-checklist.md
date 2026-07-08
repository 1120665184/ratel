# 六、开发检查清单

> 按开发任务类型分组，只检查对应部分即可。

## 新建业务模块

- [ ] 创建 `business-xxx` 父模块 + `api` / `server` 子模块
- [ ] 实现 `XxxModuleInfoProvider`
- [ ] 定义错误码枚举 `XxxErrorCode`，在 `ErrorCodeConstants` 添加模块编号
- [ ] 创建 DDL（`docker/initdb/ddl/{postgre,mysql}/{prefix}.sql`）和 DML（`docker/initdb/dml/{prefix}.sql`）
- [ ] 分布式模式下创建对应微服务应用

## 新建业务实体

- [ ] Domain 继承 `BaseDO`，添加 `@TableName`、`@Schema`、`@TableId(type = IdType.ASSIGN_ID)`
- [ ] 实现 `toVo()` 和 `static toDo(VO)` 方法，`toVo()` 中调用 `vo.copyBaseProperties(this)`
- [ ] 创建 Mapper（继承 `BaseMapper`）+ XML
- [ ] 创建 Service 接口（`I` 前缀，继承 `IService`）+ 实现类
- [ ] Controller 添加 `@Tag`、`@Operation`、**`@TableModelPermission`**
- [ ] 新增方法中用 `AssertUtils` 验证必填字段
- [ ] 敏感字段添加 `@TableModelField` 注解
- [ ] 布尔字段使用整数类型（MySQL: SMALLINT, PostgreSQL: INT2）
- [ ] SQL 包含 tenant_id、审计字段、逻辑删除字段

## 新建跨模块 API 接口

- [ ] api 模块创建 `XxxClientApi` + `@ApiClient`
- [ ] 创建 `XxxClientApiFallbackFactory`
- [ ] Controller 实现接口方法
- [ ] 响应式方法降级返回对应 `Flux`/`Mono` 类型

## 涉及安全权限

- [ ] Controller 类必须有 `@TableModelPermission`
- [ ] 敏感字段标注 `@TableModelField(desensitize=true, strategy=...)`
- [ ] AI 不可见字段标注 `@TableModelField(show=false)`
- [ ] 登录即可访问的接口标注 `@LoginAllowAccess`
- [ ] 数据资源查询是否需要 `DataPermissionUtils.applyDataPermission()` 过滤
- [ ] 自定义 Casbin 函数需在 `CasbinConfiguration` 中注册
- [ ] 反射/AOP 相关类需 `@ImportRuntimeHints` + `RuntimeHintsRegistrar`

## 涉及认证

- [ ] common-authentication 只在认证服务（system）中引入，其他服务不需要
- [ ] 新增登录方式实现 `LoginHandler<T>` 接口并注册为 Bean
- [ ] 登录拦截器实现 `LoginInterceptor<U>` 接口
- [ ] 需要工作区隔离时实现 `WorkspaceProvider` 并注册为 Bean
- [ ] 需要数据资源权限时实现 `DataResourceAttributeProvider` 并注册为 Bean
- [ ] 切换工作区后数据资源会自动重新计算，无需手动处理

## 涉及数据库

- [ ] 多数据源方法标注 `@DS("dataSourceName")`
- [ ] 多数据库 SQL 使用 `databaseId` 区分
- [ ] 布尔字段使用 SMALLINT/INT2 + `BooleanTypeHandler`

## 涉及日志

- [ ] 不记录日志的接口标注 `@LogIgnore`

## 通用禁忌（任何改动都需检查）

- [ ] 参数校验用 `AssertUtils`，禁止手动 if-else 抛异常
- [ ] Redis 操作用 `CacheUtils`，禁止直接用 `RedisTemplate`
- [ ] 获取 Bean 用 `SpringUtils`，禁止手动从 ApplicationContext 获取
- [ ] 异常抛出用 `BusinessException` + 错误码枚举
- [ ] 线程池创建用 `ThreadPoolUtil`，禁止直接用 `Executors`
- [ ] 分布式 ID 用 `IDGenerationUtils`，禁止自行生成
