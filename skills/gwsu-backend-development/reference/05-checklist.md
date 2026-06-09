# 五、开发检查清单

## 5.1 新建业务模块

- [ ] 创建 `business-xxx` 父模块 + `api` / `server` 子模块
- [ ] 实现 `XxxModuleInfoProvider`
- [ ] 定义错误码枚举 `XxxErrorCode`，在 `ErrorCodeConstants` 添加模块编号
- [ ] 创建 DDL（`docker/initdb/ddl/{postgre,mysql}/{prefix}.sql`）和 DML（`docker/initdb/dml/{prefix}.sql`）
- [ ] 分布式模式下创建对应微服务

## 5.2 新建业务实体

- [ ] Domain 继承 `BaseDO`，添加 `@TableName`、`@Schema`、`@TableId(type = IdType.ASSIGN_ID)`
- [ ] 实现 `toVo()` 和 `static toDo(VO)` 方法
- [ ] 创建 Mapper（继承 `BaseMapper`）+ XML
- [ ] 创建 Service 接口（`I` 前缀，继承 `IService`）+ 实现类
- [ ] Controller 添加 `@Tag`、`@Operation`、**`@TableModelPermission`**
- [ ] 新增方法中用 `AssertUtils` 验证必填字段
- [ ] 敏感字段添加 `@TableModelField` 注解
- [ ] 布尔字段使用整数类型（MySQL: SMALLINT, PostgreSQL: INT2）

## 5.3 新建 API 接口（跨模块调用）

- [ ] api 模块创建 `XxxClientApi` + `@ApiClient`
- [ ] 创建 `XxxClientApiFallbackFactory`
- [ ] Controller 实现接口方法

## 5.4 工具类使用检查

- [ ] 参数校验用 `AssertUtils`，禁止手动 if-else 抛异常
- [ ] Redis 操作用 `CacheUtils`，禁止直接用 `RedisTemplate`
- [ ] 获取 Bean 用 `SpringUtils`，禁止手动从 ApplicationContext 获取
- [ ] HTTP 请求信息用 `ServletUtils`
- [ ] 异常抛出用 `BusinessException` + 错误码枚举
- [ ] 部署模式判断用 `DeployUtils.isSingle()`
- [ ] 微服务响应解包用 `FeignUtils.data()`
