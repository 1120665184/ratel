---
name: gwsu-backend-development
description: GWSU后端项目开发规范与指南，包含目录结构、命名规范、公共模块使用等，只要涉及到后端代码开发必读。
type: skill
---

# GWSU 后端项目开发技能

本技能为 GWSU 后端项目提供开发规范和指导，确保代码风格统一、架构清晰。

## 触发条件

- 涉及后端 Java 代码的任何改动（**必读**）
- 用户要求创建新的业务模块
- 用户要求添加新的 API 接口
- 用户要求创建 Domain、Service、Controller、Mapper 等组件
- 用户要求创建数据库表或 SQL 脚本
- 用户询问项目开发规范或最佳实践
- 用户使用 common 模块的工具类

## 文档索引

本技能由以下子文档组成，改动后端代码时必须阅读相关文档：

| 文档                                                                 | 说明 | 适用场景 |
|--------------------------------------------------------------------|------|---------|
| [01-project-structure.md](reference/01-project-structure.md)       | 目录规范、包命名、模块结构 | 新建模块、了解项目结构 |
| [02-development-conventions.md](reference/02-development-conventions.md)     | 开发规范、命名规范、SQL规范、错误码 | 编写任何业务代码 |
| [03-domain-dto-vo.md](reference/03-domain-dto-vo.md)                         | Domain、DTO、VO 对象规范 | 创建实体类、数据传输对象 |
| [04-mapper-service-controller.md](reference/04-mapper-service-controller.md) | Mapper、Service、Controller 分层规范 | 创建数据访问层和业务逻辑层 |
| [05-api-client.md](reference/05-api-client.md)                               | API Client 规范 | 跨模块服务调用 |
| [06-common-utils.md](reference/06-common-utils.md)                           | 公共模块工具类完整指南 | 使用工具类、缓存、数据库操作、安全等 |
| [07-checklist.md](reference/07-checklist.md)                                 | 开发检查清单 | 新建模块/实体/API后的自查 |

## 快速参考

### 工具类使用优先级

1. **本项目 common 模块工具类**（最高优先级）
2. Spring Boot 自带工具类
3. Apache Commons 工具类
4. Hutool 工具类（最低优先级）

### 统一响应类型

```java
R.ok(data)              // 成功，带数据
R.ok(data, "msg")       // 成功，带数据和自定义消息
R.ok()                  // 成功，无数据
R.fail("msg")           // 失败，带消息
R.fail(exception)       // 失败，带异常
```

### 部署模式

| 模式 | 调用方式 | 检测方式 |
|------|---------|---------|
| 单体 | `LocalApiClientFactory` 直接查找 Bean | `DeployUtils.isSingle()` |
| 分布式 | `RemoteApiClientFactory` HTTP 调用 + 熔断 | 自动检测 `NacosDiscoveryAutoConfiguration` |
