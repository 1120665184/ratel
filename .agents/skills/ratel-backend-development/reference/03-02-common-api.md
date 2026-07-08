# common-api — @ApiClient 跨模块调用框架

## 何时需要

- 新功能模块无需添加 API Client
- **跨业务模块调用**时需要：如 system 模块调用 security 模块的功能，需 security 开发 API Client

## @ApiClient 注解属性

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `value` | `String` | 必填 | 目标服务名称，如 `gwsu-security` |
| `note` | `String` | `""` | 模块描述 |
| `config` | `CircuitBreakerCustomConfig` | 默认配置 | 类级别熔断器自定义配置 |
| `fallbackFactory` | `Class<?>` | `Void.class` | 降级工厂类，需实现 `FallbackFactory<T>` |
| `loadBalancer` | `Class<? extends ReactorServiceInstanceLoadBalancer>` | `NullLoadBalancer.class` | 自定义负载均衡策略，仅分布式模式生效 |

## API 接口定义（api 模块）

```java
@ApiClient(value = "gwsu-xxx", note = "XXX模块API", fallbackFactory = XxxClientApiFallbackFactory.class)
@HttpExchange("/xxx")
public interface XxxClientApi {
    @GetExchange("/{id}")
    R<XxxVO> getById(@PathVariable("id") String id);

    @PostExchange
    R<List<XxxVO>> listByCondition(@RequestBody XxxDTO entity);
}
```

## 响应式调用（Flux / Mono）

`@ApiClient` 接口支持返回 `Flux<T>` 和 `Mono<T>`，适用于 SSE 流式推送等场景。

```java
// Flux 示例（SSE 流式推送）
@PostExchange("/stream")
Flux<ServerSentEvent<String>> stream(@RequestBody ChatRequest request);

// Mono 示例（异步单值）
@GetExchange("/{id}")
Mono<R<XxxVO>> getByIdAsync(@PathVariable("id") String id);
```

同一个接口可混合同步和响应式方法。

## 降级工厂

```java
@Component
public class XxxClientApiFallbackFactory implements FallbackFactory<XxxClientApi> {
    @Override
    public XxxClientApi create(Throwable cause) {
        return new XxxClientApi() {
            @Override
            public R<XxxVO> getById(String id) {
                return R.fail("XXX服务暂时不可用: " + cause.getMessage());
            }
            @Override
            public Flux<XxxVO> streamData(XxxDTO query) {
                return Flux.error(new RuntimeException("XXX服务流式接口不可用: " + cause.getMessage()));
            }
            @Override
            public Mono<R<XxxVO>> getByIdAsync(String id) {
                return Mono.just(R.fail("XXX服务暂时不可用: " + cause.getMessage()));
            }
        };
    }
}
```

## 自定义负载均衡策略

```java
@ApiClient(value = "gwsu-xxx", loadBalancer = RandomLoadBalancer.class)
@HttpExchange("/xxx")
public interface XxxClientApi { ... }
```

自定义策略需提供构造函数 `(ObjectProvider<ServiceInstanceListSupplier>, String serviceId)`。

## Controller 实现接口

Controller 必须实现对应的 `XxxClientApi` 接口：

```java
@RestController
@RequestMapping("xxx")
public class XxxController implements XxxClientApi {
    @Override
    public R<XxxVO> getById(@PathVariable String id) { ... }
}
```

## 熔断配置优先级

方法注解 `@CircuitBreakerCustomConfig` > 类注解 `@ApiClient(config = ...)` > 配置文件

```java
@ApiClient(value = "gwsu-xxx", config = @CircuitBreakerCustomConfig(failureRateThreshold = 50))
public interface XxxClientApi {
    @CircuitBreakerCustomConfig(failureRateThreshold = 30, slidingWindowSize = 20)
    @GetExchange("/critical")
    R<Data> criticalEndpoint();
}
```

## FeignUtils — 微服务响应处理

`FeignUtils.data(R<T> r)` — 获取微服务接口返回数据，失败时抛 `BusinessException`

```java
UserData data = FeignUtils.data(userClientApi.getById(id));
```

## 部署模式

| 模式 | 调用方式 | HTTP 客户端 | 检测方式 |
|------|---------|------------|---------|
| 单体 | `LocalApiClientFactory` 直接查找 Bean | 无（本地调用） | `DeployUtils.isSingle()` |
| 分布式 | `RemoteApiClientFactory` HTTP + 熔断 | `WebClient` | 自动检测 Nacos |
