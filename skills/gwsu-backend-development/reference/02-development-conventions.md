# 二、开发规范

## 2.1 工具类使用优先级

1. **本项目 common 模块工具类**（最高优先级）
2. Spring Boot 自带工具类
3. Apache Commons 工具类
4. Hutool 工具类（最低优先级）

## 2.2 业务模块信息提供者

每个业务模块的主包目录下必须实现 `BusinessModuleInfoProvider`：

```java
package org.quyq.gwsu.xxx;

import org.quyq.gwsu.common.core.domain.BusinessModuleInfo;
import org.quyq.gwsu.common.core.provider.BusinessModuleInfoProvider;
import org.springframework.stereotype.Component;

@Component
public class XxxModuleInfoProvider implements BusinessModuleInfoProvider {

    @Override
    public BusinessModuleInfo module() {
        return new BusinessModuleInfo("xxx", "xxx模块");
    }
}
```

**用途**：注册模块前缀标识，用于表命名、错误码生成、前端 API 路由前缀等。

## 2.2.1 前端 API 路由前缀规则（重要）

前端调用后端接口时，**所有 API 路径必须以对应业务模块的 `BusinessModuleInfoProvider` 中定义的 `prefix` 作为统一前缀**。

**规则**：API 路径格式为 `/{模块prefix}/{业务路径}`

**当前模块前缀映射**：

| 业务模块 | ModuleInfoProvider | prefix 值 | API 路径示例 |
|---------|-------------------|-----------|------------|
| business-security | SecurityModuleInfoProvider | `security` | `/security/dept/tree` |
| business-system | SystemModuleInfoProvider | `system` | `/system/user/list` |

**说明**：
- 前端所有接口请求路径必须添加对应模块的 `prefix` 前缀
- `prefix` 值来源于后端 `BusinessModuleInfoProvider` 实现类中 `module().prefix()` 的返回值
- 新增业务模块时，必须在 `BusinessModuleInfoProvider` 中定义唯一的 `prefix`，前端使用该 `prefix` 作为 API 路由前缀
- 前端 API 服务文件中定义的路径应以 `/{prefix}/` 开头，而非直接以业务路径开头

**错误示例**（缺少模块前缀）：
```typescript
// 错误：直接使用业务路径，缺少模块前缀
export async function getDeptTree() {
  const res = await get<DeptTreeNode[]>('/dept/tree');
  return res.data;
}
```

**正确示例**（包含模块前缀）：
```typescript
// 正确：security 模块的接口以 /security 为前缀
export async function getDeptTree() {
  const res = await get<DeptTreeNode[]>('/security/dept/tree');
  return res.data;
}
```

## 2.3 表命名规范

**格式**：`{模块前缀}_{表名}`

**示例**：

- `security_abac` - 安全模块 ABAC 表
- `security_abac_permission` - 安全模块 ABAC 权限表
- `sys_user` - 系统模块用户表
- `sys_account` - 系统模块账户表

## 2.4 SQL 脚本规范

### 2.4.1 SQL 目录结构

所有业务模块的数据库脚本统一放在 `docker/initdb/` 目录下，DDL 按数据库类型分目录，DML 通用不区分数据库：

```
docker/initdb/
├── ddl/                              # DDL（表结构定义，按数据库类型分目录）
│   ├── postgre/                      # PostgreSQL DDL
│   │   ├── system.sql                # 系统模块表结构（CREATE TABLE）
│   │   ├── security.sql              # 安全模块表结构
│   │   ├── log.sql                   # 日志模块表结构
│   │   ├── kit.sql                   # 工具模块表结构
│   │   └── ...                       # 其他模块
│   └── mysql/                        # MySQL DDL
│       ├── system.sql
│       ├── security.sql
│       ├── log.sql
│       ├── kit.sql
│       └── ...
├── dml/                              # DML（初始化数据，通用不区分数据库）
│   ├── system.sql                    # 系统模块初始化数据（INSERT）
│   ├── security.sql                  # 安全模块初始化数据
│   ├── log.sql                       # 日志模块初始化数据
│   └── ...
└── init-postgre.sh                   # PostgreSQL 初始化脚本
```

**命名规则**：`{模块前缀}.sql`

- 模块前缀与 `BusinessModuleInfoProvider` 中定义的 `prefix` 一致
- `ddl/` 目录：表结构定义（CREATE TABLE、索引等），因不同数据库语法不同，需按数据库类型分目录
- `dml/` 目录：初始化数据（INSERT 等），使用通用 SQL 语法，所有数据库共用一份

**Docker 部署自动初始化**：PostgreSQL 容器首次启动时通过 `init-postgre.sh` 脚本，按 DDL → DML 的顺序执行 `/initdb/ddl/postgre/` 和 `/initdb/dml/` 目录下的 `.sql` 文件。

### 2.4.2 布尔字段类型规范（重要）

**规则**：布尔类型字段统一使用整数类型，**禁止使用原生布尔类型**。

| 数据库       | 布尔字段类型 | 说明                |
|-----------|--------|-------------------|
| MySQL     | `TINYINT` 或 `SMALLINT` | 值：0-假，1-真        |
| PostgreSQL | `INT2`  | 值：0-假，1-真（避免使用 BOOLEAN） |

**原因**：
1. 保持跨数据库类型一致性
2. 便于 MyBatis Plus 逻辑删除字段映射（`deleted` 字段）
3. 避免 JDBC 驱动在不同数据库间的类型转换问题

**MySQL 示例**：
```sql
CREATE TABLE sys_user (
    id      VARCHAR(24) PRIMARY KEY,
    status  SMALLINT DEFAULT 1,        -- 状态：0-禁用 1-正常
    deleted SMALLINT DEFAULT 0         -- 删除标识：0-未删除 1-已删除
);
```

**PostgreSQL 示例**：
```sql
CREATE TABLE sys_user (
    id      VARCHAR(24) PRIMARY KEY,
    status  SMALLINT DEFAULT 1,        -- 状态：0-禁用 1-正常
    deleted INT2 DEFAULT 0             -- 删除标识：0-未删除 1-已删除
);
```

### 2.4.3 DDL 脚本规范

- 表名使用小写下划线命名：`sys_user`、`security_abac`
- 主键使用雪花算法生成的 VARCHAR(24)
- 必须包含租户ID、审计字段、逻辑删除字段
- 添加适当的索引和注释

**MySQL DDL 模板**：
```sql
-- =============================================
-- 表名：xxx_entity
-- 说明：XXX实体表
-- =============================================
CREATE TABLE xxx_entity (
    id              VARCHAR(24) PRIMARY KEY COMMENT '主键ID（雪花算法）',
    field_name      VARCHAR(100) NOT NULL COMMENT '字段名称',
    status          SMALLINT DEFAULT 1 COMMENT '状态：0-禁用 1-正常',
    tenant_id       VARCHAR(50) COMMENT '租户ID',
    create_op       VARCHAR(50) COMMENT '创建人',
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modify_op       VARCHAR(50) COMMENT '修改人',
    modify_time     DATETIME COMMENT '修改时间',
    deleted         SMALLINT DEFAULT 0 COMMENT '删除标识：0-未删除 1-已删除',
    delete_op       VARCHAR(50) COMMENT '删除人',
    delete_time     DATETIME COMMENT '删除时间',
    INDEX idx_xxx_entity_field (field_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='XXX实体表';
```

**PostgreSQL DDL 模板**：
```sql
-- =============================================
-- 表名：xxx_entity
-- 说明：XXX实体表
-- =============================================
CREATE TABLE xxx_entity (
    id              VARCHAR(24) PRIMARY KEY,
    field_name      VARCHAR(100) NOT NULL,
    status          SMALLINT DEFAULT 1,
    tenant_id       VARCHAR(50),
    create_op       VARCHAR(50),
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modify_op       VARCHAR(50),
    modify_time     TIMESTAMP,
    deleted         INT2 DEFAULT 0,
    delete_op       VARCHAR(50),
    delete_time     TIMESTAMP
);

-- 表和字段注释
COMMENT ON TABLE xxx_entity IS 'XXX实体表';
COMMENT ON COLUMN xxx_entity.id IS '主键ID（雪花算法）';
COMMENT ON COLUMN xxx_entity.field_name IS '字段名称';
COMMENT ON COLUMN xxx_entity.status IS '状态：0-禁用 1-正常';
COMMENT ON COLUMN xxx_entity.deleted IS '删除标识：0-未删除 1-已删除';

-- 索引
CREATE INDEX idx_xxx_entity_field ON xxx_entity(field_name) WHERE deleted = 0;
```

## 2.5 命名规范总览

| 层级         | 命名规则                   | 示例                                  |
|------------|------------------------|-------------------------------------|
| Domain     | 表名驼峰形式                 | `SecurityAbacPermission`            |
| Mapper     | Domain + Mapper        | `SecurityAbacPermissionMapper`      |
| Service接口  | I + Domain + Service   | `ISecurityAbacPermissionService`    |
| Service实现  | Domain + ServiceImpl   | `SecurityAbacPermissionServiceImpl` |
| Controller | Domain + Controller    | `SecurityAbacPermissionController`  |
| VO         | Domain + VO（或业务名 + VO） | `AbacPermissionVo`                  |
| DTO        | 业务名 + DTO              | `UserQueryDTO`                      |

## 2.6 接口命名规范

所有业务接口统一以 `I` 为前缀：

```java
// 正确
public interface ISecurityRoleService extends IService<SecurityRole> {
}

// 错误
public interface SecurityRoleService extends IService<SecurityRole> {
}
```

## 2.7 错误码定义

每个业务模块在 `errcode` 目录下定义错误码枚举：
错误码命名规范：
- 统一以E开头
- 5位数字长度，前两位代表不同模块如：01,02 ,后三位代表同一模块的不同错误码，递增。

```java
package org.quyq.gwsu.xxx.errcode;

import org.quyq.gwsu.common.core.constants.ErrorCodeConstants;
import org.quyq.gwsu.common.core.domain.ReturnCode;
import org.quyq.gwsu.common.core.exception.errcode.ErrorCodeMeta;

@ErrorCodeMeta(moduleCode = ErrorCodeConstants.XXX_ERROR_CODE_MODULE, notes = "XXX模块错误码")
public enum XxxErrorCode implements ReturnCode {

    E00001("错误描述1"),
    E00002("错误描述2"),
    ;

    private final String msg;

    XxxErrorCode(String msg) {
        this.msg = msg;
    }

    @Override
    public String msg() {
        return msg;
    }
}
```

**错误码常量定义**（在 `ErrorCodeConstants` 中添加）：

```java
String XXX_ERROR_CODE_MODULE = "03";  // 模块编号，递增
```

## 2.8 业务异常抛出

```java
// 标准方式
throw new BusinessException(XxxErrorCode.E00001);

// 带自定义消息
throw new BusinessException(XxxErrorCode.E00001, "自定义错误消息");

// 仅消息（使用默认错误码）
throw new BusinessException("操作失败");
```

## 2.9 其他规范

### 配置类

- 配置类统一放在 `config` 目录
- Properties 对象放在 `config/properties` 目录

### 工具类

- 业务工具类放在 `utils` 目录
- 公共工具类放在 `common-core` 模块的 `utils` 目录

### AOT 兼容性

涉及动态代理、反射的实现必须考虑 AOT 兼容性：

1. 添加 `@ImportRuntimeHints` 注解
2. 提供 `RuntimeHintsRegistrar` 实现
