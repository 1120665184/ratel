# GWSU

[![Java](https://img.shields.io/badge/Java-25-orange.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.3-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)

GWSU (Generic Web Service Utility) 是一个基于 Spring Boot 4.0 的企业级应用开发框架，支持**单体应用**和**分布式微服务**两种部署模式，无需修改代码即可切换。

## 特性

- **双部署模式** - 单体应用与微服务架构无缝切换，通过配置即可切换
- **统一 API 客户端框架** - 类似 OpenFeign 的 `@ApiClient` 注解，自动适配部署模式
- **ABAC 权限控制** - 基于 jCasbin 的属性级访问控制，支持字段级权限过滤
- **多数据源支持** - 动态数据源切换，支持读写分离
- **熔断降级** - 集成 Resilience4j，提供服务容错能力
- **Native Image** - 支持 GraalVM 原生镜像编译

## 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 4.0.3 | 基础框架 |
| Spring Cloud | 2025.1.1 | 微服务组件 |
| Spring Cloud Alibaba | 2025.1.0.0 | Nacos 注册配置中心 |
| MyBatis Plus | 3.5.16 | ORM 框架 |
| Redisson | 4.3.0 | Redis 客户端 |
| Resilience4j | 2.4.0 | 熔断降级 |
| jCasbin | 1.99.0 | 权限控制 |
| Hutool | 5.8.44 | 工具库 |

## 快速开始

### 环境要求

- JDK 25+
- Maven 3.9+
- Redis 6.0+
- PostgreSQL 14+ (或 MySQL)
- Nacos 2.x (分布式模式需要)

### 构建项目

```bash
# 克隆项目
git clone https://github.com/quyq/gwsu.git
cd gwsu

# 构建所有模块
mvn clean install
```

### 运行单体应用

```bash
# 启动单体应用 (默认端口 8888)
mvn spring-boot:run -pl business/application/single/gwsu
```

### 运行分布式微服务

```bash
# 1. 确保 Nacos 已启动 (127.0.0.1:8848)

# 2. 启动网关
mvn spring-boot:run -pl business/application/distributed/gwsu-gateway

# 3. 启动安全服务
mvn spring-boot:run -pl business/application/distributed/gwsu-security

# 4. 启动测试服务
mvn spring-boot:run -pl business/application/distributed/gwsu-test
```

## 模块结构

```
gwsu/
├── root-pom/                    # 根 POM，依赖和插件管理
├── project-pom/                 # 内部模块版本管理
├── common/                      # 公共基础设施模块
│   ├── common-core             # 核心工具类、领域对象
│   ├── common-api              # @ApiClient 框架
│   ├── common-cache            # Redis 缓存封装
│   ├── common-database         # MyBatis Plus、多数据源
│   ├── common-deploy           # 部署模式配置
│   ├── common-security         # ABAC 权限控制
│   └── common-authentication   # 用户认证
└── business/                    # 业务模块
    ├── business-security/       # 安全业务模块
    │   ├── business-security-api
    │   └── business-security-server
    ├── business-test/           # 测试业务模块
    │   ├── business-test-api
    │   └── business-test-server
    └── application/
        ├── single/gwsu          # 单体应用
        └── distributed/         # 分布式微服务
            ├── gwsu-gateway
            ├── gwsu-security
            └── gwsu-test
```

## 核心功能

### 部署模式切换

通过配置 `deploy.single` 属性控制：

```yaml
# 单体模式
deploy:
  single: true

# 分布式模式
deploy:
  single: false
```

### API 客户端框架

定义服务间调用接口：

```java
@ApiClient(value = "gwsu-security", fallbackFactory = SecurityClientApiFallbackFactory.class)
public interface SecurityClientApi {

    @GetExchange("/security/permission/check")
    R<Boolean> checkPermission(@RequestParam("permission") String permission);
}
```

控制器直接实现接口：

```java
@RestController
public class SecurityController implements SecurityClientApi {

    @GetMapping("/security/permission/check")
    @Override
    public R<Boolean> checkPermission(@RequestParam("permission") String permission) {
        return R.ok(true);
    }
}
```

### 多数据源

使用 `@DS` 注解切换数据源：

```java
@Service
public class UserService {

    @DS("master")
    public User queryFromMaster() { ... }

    @DS("slave")
    public User queryFromSlave() { ... }
}
```

### 统一响应格式

所有接口返回统一的 `R<T>` 类型：

```java
// 成功响应
R.ok(data)
R.ok(data, "操作成功")

// 失败响应
R.fail("操作失败")
R.fail(exception)
```

### ABAC 权限控制

支持基于属性的访问控制，可配置字段级权限过滤：

```java
// 权限规则
FieldRule rule = new FieldRule(
    "/api/user/*",     // URL 匹配
    "deny",            // 效果：allow/deny
    Set.of("phone", "address"),  // 字段
    "r.subject.role == 'guest'"   // 表达式
);
```

## 配置说明

### 单体模式配置

配置文件位于 `classpath:` 下：

- `database.yaml` - 数据库配置
- `redis.yaml` - Redis 配置

### 分布式模式配置

配置从 Nacos 导入：

- `common.yaml` - 公共配置
- `common-redis.yaml` - Redis 配置
- `common-database.yaml` - 数据库配置
- `{application.name}.yaml` - 应用专属配置

## Native Image 编译

支持 GraalVM 原生镜像编译：

```bash
# 编译原生镜像
mvn -Pnative package -pl business/application/single/gwsu
```

## 贡献指南

欢迎提交 Issue 和 Pull Request。

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/amazing-feature`)
3. 提交更改 (`git commit -m 'Add some amazing feature'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 创建 Pull Request

## 许可证

本项目基于 [Apache License 2.0](LICENSE) 许可证开源。

## 联系方式

- 作者：Quyq
- GitHub：[https://github.com/quyq/gwsu](https://github.com/quyq/gwsu)
