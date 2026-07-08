# common-deploy — 双部署模式

## DeployInitializer — 自动检测

启动时检测 `NacosDiscoveryAutoConfiguration` 是否存在：
- 存在 → 分布式模式（`deploy.single: false`）
- 不存在 → 单体模式（`deploy.single: true`）

## ProcessorFilter 责任链

| 模式 | 过滤器 | 说明 |
|------|--------|------|
| 单体 | `SingleProcessorFilter` | 本地 `ProcessorChain` 责任链处理 |
| 分布式 | `DistributedGatewayProcessorFilter` | 网关层 `ProcessorChain` 责任链处理 |

## 路由处理

| 模式 | 类 | 说明 |
|------|----|------|
| 单体 | `SingleAppRouteHandlerConfiguration` | 所有模块路由在同一应用内处理 |
| 分布式 | 网关路由 | 通过 Spring Cloud Gateway 转发到各微服务 |

## 分布式 SQL 代理执行

`DistributedSqlExecutionController` 提供跨服务 SQL 执行能力，单体模式下通过 `SingleModuleController` 暴露模块信息。

## ReactorContextCaptureAspect

响应式上下文捕获切面，确保 Reactor 链中 ThreadLocal 上下文正确传播。
