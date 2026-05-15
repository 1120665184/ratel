# 七、开发检查清单

## 7.1 新建业务模块

- [ ] 创建 `business-xxx` 父模块
- [ ] 创建 `business-xxx-api` 子模块
- [ ] 创建 `business-xxx-server` 子模块
- [ ] 创建 `sql/mysql/` 目录，包含 `ddl.sql` 和 `dml.sql`
- [ ] 创建 `sql/postgre/` 目录，包含 `ddl.sql` 和 `dml.sql`
- [ ] 实现 `XxxModuleInfoProvider`
- [ ] 定义错误码枚举 `XxxErrorCode`
- [ ] 在 `ErrorCodeConstants` 中添加模块编号
- [ ] 在分布式应用模块中创建对应微服务（如需要）

## 7.2 新建业务实体

- [ ] 创建 Domain 类，继承 `BaseDO`
- [ ] 添加 `@TableName`、`@Schema` 注解
- [ ] 实现 `toVo()` 方法
- [ ] 实现 `toDo()` 静态方法（VO 转 DO）
- [ ] 创建 Mapper 接口，继承 `BaseMapper`
- [ ] 创建 Mapper XML 文件
- [ ] 创建 Service 接口，继承 `IService`
- [ ] 创建 Service 实现类
- [ ] 创建 Controller，添加 `@Tag`、`@Operation` 注解
- [ ] 在 Controller 类上添加 `@TableModelPermission` 注解，声明涉及的表模型 Domain 类
- [ ] 在 Controller 新增方法中使用 `AssertUtils` 验证必填字段
- [ ] 在 Domain 类敏感字段上添加 `@TableModelField` 注解（如需字段级权限控制）
- [ ] 在 api 模块创建 VO 类
- [ ] 在 `sql/mysql/` 目录创建 `ddl.sql` 表结构脚本
- [ ] 在 `sql/mysql/` 目录创建 `dml.sql` 初始化数据脚本
- [ ] 在 `sql/postgre/` 目录创建 `ddl.sql` 表结构脚本
- [ ] 在 `sql/postgre/` 目录创建 `dml.sql` 初始化数据脚本
- [ ] 确认布尔字段使用整数类型（MySQL: SMALLINT, PostgreSQL: INT2）

## 7.3 新建 API 接口

- [ ] 在 api 模块创建 `XxxClientApi` 接口
- [ ] 添加 `@ApiClient` 注解
- [ ] 创建 `XxxClientApiFallbackFactory` 降级工厂
- [ ] 在 Controller 中实现接口方法

## 7.4 新增表模型（Domain/数据库表）

- [ ] 在 Domain 类上确保有 `@TableName` 注解
- [ ] 在 Domain 类敏感字段上添加 `@TableModelField` 注解（如需字段级权限控制）
- [ ] 在所属业务模块的 Controller 类上的 `@TableModelPermission` 注解中补充该 Domain 类

## 7.5 工具类使用检查

- [ ] 参数校验使用 `AssertUtils` 而非手动 if-else
- [ ] Redis 操作使用 `CacheUtils` 而非直接使用 `RedisTemplate`
- [ ] 获取 Bean 使用 `SpringUtils` 而非手动从 ApplicationContext 获取
- [ ] HTTP 请求信息获取使用 `ServletUtils`
- [ ] 异常抛出使用 `BusinessException` + 错误码枚举
- [ ] 部署模式判断使用 `DeployUtils.isSingle()`
