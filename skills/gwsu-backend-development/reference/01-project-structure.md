# 一、目录规范

## 1.1 项目整体结构

```
root-pom/                    # 根 POM，依赖和插件管理
project-pom/                 # GWSU 内部模块版本管理
common/                      # 公共基础设施模块
├── common-core              # 核心工具类、领域对象
├── common-api               # @ApiClient 框架，服务间调用
├── common-cache             # Redis/Redisson 配置
├── common-database          # MyBatis Plus、多数据源
├── common-deploy            # 部署模式配置（单体/分布式）
├── common-security          # Casbin/ABAC 安全配置
└── common-authentication    # 认证相关配置
business/                    # 业务领域模块
├── business-xxx/            # 业务模块
│   ├── business-xxx-api/    # API 模块（接口、VO、DTO）
│   └── business-xxx-server/ # 服务模块（实现、控制器）
└── application/
    ├── distributed/         # 微服务应用
    │   ├── gwsu-gateway     # API 网关
    │   ├── gwsu-xxx         # 各业务微服务
    │   └── ...
    └── single/gwsu          # 单体应用
docker/                      # Docker 部署配置
├── nginx/
│   └── gwsu.conf            # nginx 配置
├── initdb/                  # 数据库初始化脚本
│   ├── mysql/               # MySQL 脚本
│   │   ├── system_ddl.sql
│   │   ├── system_dml.sql
│   │   ├── security_ddl.sql
│   │   └── security_dml.sql
│   └── postgre/             # PostgreSQL 脚本
│       ├── system_ddl.sql
│       ├── system_dml.sql
│       ├── security_ddl.sql
│       └── security_dml.sql
├── single/                  # 单机版部署
│   ├── Dockerfile
│   ├── docker-compose.yml
│   ├── entrypoint.sh
│   └── .env
└── distributed/             # 分布式版部署（暂不实现）
```

## 1.2 包命名规范

**格式**：`org.quyq.gwsu.{服务}.{业务名}`

**组成部分**：

- `org.quyq` - 公司域名前缀
- `gwsu` - 项目标识
- `{服务}` - 服务名（如 security、system）
- `{业务名}` - 具体业务名（如 abac、manager）

**示例**：

```
org.quyq.gwsu.security.abac          # 安全模块-ABAC业务
org.quyq.gwsu.system.manager         # 系统模块-管理业务
org.quyq.gwsu.security.api           # 安全模块-API层
```

## 1.3 业务模块结构

每个业务模块（business-xxx）分为两个子模块：

```
business-xxx/
├── business-xxx-api/                    # API 模块
│   └── src/main/java/org/quyq/gwsu/xxx/api/
│       ├── XxxClientApi.java            # API 客户端接口
│       ├── fallback/
│       │   └── XxxClientApiFallbackFactory.java  # 降级工厂
│       ├── vo/                          # VO 对象
│       │   └── XxxVo.java
│       ├── dto/                         # DTO 对象（可选）
│       │   └── XxxDTO.java
│       └── enums/                       # 枚举定义（可选）
│           └── XxxEnum.java
│
├── business-xxx-server/                 # 服务模块
│   └── src/main/
│       ├── java/org/quyq/gwsu/xxx/
│       │   ├── XxxModuleInfoProvider.java  # 模块信息提供者
│       │   ├── config/                     # 配置类
│       │   ├── utils/                      # 工具类
│       │   ├── errcode/                    # 错误码
│       │   │   └── XxxErrorCode.java
│       │   └── {业务名}/
│       │       ├── domain/                 # 领域对象
│       │       │   └── XxxEntity.java
│       │       ├── mapper/                 # Mapper 接口
│       │       │   └── XxxEntityMapper.java
│       │       ├── service/                # 服务层
│       │       │   ├── IXxxService.java
│       │       │   └── impl/
│       │       │       └── XxxServiceImpl.java
│       │       └── controller/             # 控制器
│       │           └── XxxController.java
│       └── resources/
│           └── mapper/{业务名}/            # MyBatis XML
│               └── XxxMapper.xml
```

## 1.4 模块与微服务对应关系

| 业务模块              | 分布式微服务        | 说明   |
|-------------------|---------------|------|
| business-security | gwsu-security | 安全服务 |
| business-system   | gwsu-system   | 系统服务 |
| business-test     | gwsu-test     | 测试服务 |

**重要**：在分布式模式下，每个业务模块对应一个独立的微服务。新建模块前需评估是否有必要独立部署。
