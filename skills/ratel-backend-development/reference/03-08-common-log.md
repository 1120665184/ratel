# common-log — 操作日志

## 自动采集

通过 `LogAspectInterceptor`（AOP 切面）自动采集 Controller 方法的请求/响应信息，异步写入日志服务。

## AccessLogHandlerService — 异步日志处理

多队列 + 哈希分片消费，确保相同 operId 的请求/响应日志路由到同一队列，由同一虚拟线程顺序消费。

## @LogIgnore — 忽略日志

标注在 Controller 方法上，该接口不记录操作日志：

```java
@LogIgnore
@GetMapping("/check")
public R<Boolean> check() { ... }
```

## ILogClientApi — 日志服务接口

日志写入的跨模块调用接口，由 `business-log` 模块实现。

## 日志领域对象

| 类 | 说明 |
|----|------|
| `LogOperationVO` | 操作日志 VO |
| `LogLifeCycle` | 日志生命周期配置 |
| `LogStorage` | 日志存储配置 |
| `SaveMedium` | 存储介质枚举 |
| `TableLogSourceType` | 日志来源类型 |
| `ViewOperationSubject` | 操作主体枚举 |

## 配置

通过 `LogInfoConfigProperties` 配置日志行为（是否记录请求体/响应体、排除路径等）。
