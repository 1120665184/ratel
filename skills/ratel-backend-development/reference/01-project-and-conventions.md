# 一、项目结构与开发规范

## 1.1 项目结构

```
root-pom/                    # 根 POM
project-pom/                 # 内部模块版本管理
common/                      # 公共基础设施
├── common-core              # 核心工具类、领域对象
├── common-api               # @ApiClient 框架
├── common-cache             # Redis/Redisson
├── common-database          # MyBatis Plus、多数据源
├── common-deploy            # 部署模式（单体/分布式）
├── common-security          # Casbin/ABAC 安全
└── common-authentication    # 认证
business/                    # 业务模块
├── business-xxx/
│   ├── business-xxx-api/    # API 模块（接口、VO、DTO、枚举）
│   └── business-xxx-server/ # 服务模块（Domain、Mapper、Service、Controller）
└── application/
    ├── distributed/         # 微服务应用
    └── single/gwsu          # 单体应用
docker/initdb/
├── ddl/postgre/             # PostgreSQL DDL
├── ddl/mysql/               # MySQL DDL
└── dml/                     # 通用 DML
```

## 1.2 包命名

格式：`org.quyq.gwsu.{服务}.{业务名}`，如 `org.quyq.gwsu.security.abac`

## 1.3 业务模块结构

```
business-xxx-server/src/main/java/org/quyq/gwsu/xxx/
├── XxxModuleInfoProvider.java   # 模块信息（必须）
├── config/                      # 配置类
├── utils/                       # 业务工具类
├── errcode/XxxErrorCode.java    # 错误码
└── {业务名}/
    ├── domain/XxxEntity.java
    ├── mapper/XxxEntityMapper.java
    ├── service/IXxxService.java
    ├── service/impl/XxxServiceImpl.java
    └── controller/XxxController.java
```

## 1.4 工具类使用优先级

1. **本项目 common 模块工具类**（最高优先级）
2. Spring Boot 自带工具类
3. Apache Commons 工具类
4. Hutool 工具类（最低优先级）

## 1.5 命名规范

| 层级 | 命名规则 | 示例 |
|------|---------|------|
| Domain | 表名驼峰 | `SecurityAbacPermission` |
| Mapper | Domain + Mapper | `SecurityAbacPermissionMapper` |
| Service接口 | I + Domain + Service | `ISecurityAbacPermissionService` |
| Service实现 | Domain + ServiceImpl | `SecurityAbacPermissionServiceImpl` |
| Controller | Domain + Controller | `SecurityAbacPermissionController` |
| VO | 业务名 + VO | `AbacPermissionVo` |
| DTO | 业务名 + DTO | `UserQueryDTO` |
| 表名 | {模块前缀}_{表名} | `security_abac` |

## 1.6 BusinessModuleInfoProvider（必须实现）

每个业务模块主包下必须实现，注册模块前缀标识：

```java
@Component
public class XxxModuleInfoProvider implements BusinessModuleInfoProvider {
    @Override
    public BusinessModuleInfo module() {
        return new BusinessModuleInfo("xxx", "xxx模块");
    }
}
```

## 1.7 前端 API 路由前缀规则

所有 API 路径必须以模块 `prefix` 为前缀：`/{模块prefix}/{业务路径}`

| 模块 | prefix | 示例 |
|------|--------|------|
| business-security | `security` | `/security/dept/tree` |
| business-system | `system` | `/system/user/list` |

## 1.8 SQL 规范

- DDL 按数据库类型分目录：`docker/initdb/ddl/{postgre|mysql}/{模块前缀}.sql`
- DML 通用不区分数据库：`docker/initdb/dml/{模块前缀}.sql`
- **布尔字段禁止使用原生布尔类型**：MySQL 用 `SMALLINT`，PostgreSQL 用 `INT2`（0-假，1-真）
- 主键使用雪花算法 `VARCHAR(24)`
- 必须包含：`tenant_id`、审计字段（`create_op/time`、`modify_op/time`）、逻辑删除（`deleted`、`delete_op/time`）

## 1.9 错误码定义

```java
@ErrorCodeMeta(moduleCode = ErrorCodeConstants.XXX_ERROR_CODE_MODULE, notes = "XXX模块错误码")
public enum XxxErrorCode implements ReturnCode {
    E00001("错误描述"),
    ;
    private final String msg;
    XxxErrorCode(String msg) { this.msg = msg; }
    @Override public String msg() { return msg; }
}
```

- 命名：`E` + 5位数字，前2位为模块编号，后3位递增
- 在 `ErrorCodeConstants` 中添加模块编号常量

## 1.10 业务异常

```java
throw new BusinessException(XxxErrorCode.E00001);                    // 标准方式
throw new BusinessException(XxxErrorCode.E00001, "自定义消息");       // 带自定义消息
throw new BusinessException("操作失败");                               // 仅消息
```
