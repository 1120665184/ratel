---
name: gwsu-backend-development
description: Ratel后端项目开发规范与指南，包含目录结构、命名规范、公共模块使用等，只要涉及到后端代码开发必读。
type: skill
---

# Ratel 后端项目开发技能

## 触发条件

- 涉及后端 Java 代码的任何改动（**必读**）
- 创建业务模块、API 接口、Domain/Service/Controller/Mapper
- 创建数据库表或 SQL 脚本
- 使用 common 模块工具类

## 文档索引

| 文档 | 说明 | 适用场景 |
|------|------|---------|
| [01-project-and-conventions.md](reference/01-project-and-conventions.md) | 项目结构、命名规范、SQL规范、错误码 | 编写任何后端代码 |
| [02-layer-architecture.md](reference/02-layer-architecture.md) | Domain/DTO/VO/Mapper/Service/Controller 分层 | 创建实体类、数据访问层 |
| [03-api-client.md](reference/03-api-client.md) | API Client 跨模块调用 | 跨模块服务调用 |
| [04-common-utils.md](reference/04-common-utils.md) | 公共模块工具类 | 使用工具类、缓存、安全等 |
| [05-checklist.md](reference/05-checklist.md) | 开发检查清单 | 自查 |

## 快速参考

### 工具类使用优先级

1. **本项目 common 模块工具类**（最高优先级）
2. Spring Boot 自带工具类
3. Apache Commons 工具类
4. Hutool 工具类（最低优先级）

### 统一响应

```java
R.ok(data)              // 成功带数据
R.fail("msg")           // 失败带消息
throw new BusinessException(XxxErrorCode.E00001);  // 业务异常
AssertUtils.hasText(name, XxxErrorCode.E00001);     // 参数校验
```

### 部署模式

| 模式 | 调用方式 | 检测方式 |
|------|---------|---------|
| 单体 | `LocalApiClientFactory` | `DeployUtils.isSingle()` |
| 分布式 | `RemoteApiClientFactory` HTTP + 熔断 | 自动检测 Nacos |

### Controller 必须注解

```java
@RestController
@RequestMapping("xxx")
@Tag(name = "XXX管理")
@TableModelPermission({XxxEntity.class})  // 必须声明表模型权限
```
