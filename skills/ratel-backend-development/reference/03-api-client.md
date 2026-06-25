# 三、API Client 规范

## 3.1 何时需要

- 新功能模块无需添加 API Client
- **跨业务模块调用**时需要：如 system 模块调用 security 模块的功能，需 security 开发 API Client

## 3.2 @ApiClient 注解属性

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `value` | `String` | 必填 | 目标服务名称，如 `gwsu-security` |
| `note` | `String` | `""` | 模块描述 |
| `config` | `CircuitBreakerCustomConfig` | 默认配置 | 类级别熔断器自定义配置 |
| `fallbackFactory` | `Class<?>` | `Void.class` | 降级工厂类，需实现 `FallbackFactory<T>` |
| `loadBalancer` | `Class<? extends ReactorServiceInstanceLoadBalancer>` | `NullLoadBalancer.class` | 自定义负载均衡策略，仅分布式模式生效 |

## 3.3 API 接口定义（api 模块）

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

## 3.4 响应式调用（Flux / Mono）

`@ApiClient` 接口支持返回 `Flux<T>` 和 `Mono<T>` 类型，适用于 SSE 流式推送、流式数据传输等场景。

### Flux 示例（SSE 流式推送）

```java
@ApiClient(value = "gwsu-xxx", fallbackFactory = XxxClientApiFallbackFactory.class)
@HttpExchange("/chat")
public interface ChatClientApi {

    @PostExchange("/stream")
    Flux<ServerSentEvent<String>> stream(@RequestBody ChatRequest request);

    @GetExchange("/history")
    Flux<ChatMessage> messageStream(@RequestParam String sessionId);
}
```

Controller 实现：

```java
@RestController
@RequestMapping("chat")
public class ChatController implements ChatClientApi {

    @PostMapping("/stream")
    @Override
    public Flux<ServerSentEvent<String>> stream(@RequestBody ChatRequest request) {
        return chatService.stream(request)
                .map(chunk -> ServerSentEvent.builder(chunk).build());
    }

    @GetMapping("/history")
    @Override
    public Flux<ChatMessage> messageStream(@RequestParam String sessionId) {
        return chatService.getMessageStream(sessionId);
    }
}
```

### Mono 示例（异步单值）

```java
@ApiClient(value = "gwsu-xxx", fallbackFactory = XxxClientApiFallbackFactory.class)
@HttpExchange("/xxx")
public interface XxxClientApi {

    @GetExchange("/{id}")
    Mono<R<XxxVO>> getByIdAsync(@PathVariable("id") String id);
}
```

### 混合同步和响应式方法

同一个接口可以同时包含同步方法和响应式方法：

```java
@ApiClient(value = "gwsu-xxx", fallbackFactory = XxxClientApiFallbackFactory.class)
@HttpExchange("/xxx")
public interface XxxClientApi {

    // 同步调用
    @GetExchange("/{id}")
    R<XxxVO> getById(@PathVariable("id") String id);

    // 响应式调用
    @PostExchange("/stream")
    Flux<XxxVO> streamData(@RequestBody XxxDTO query);
}
```

### Flux 降级工厂

`Flux` 方法的降级需返回 `Flux` 类型：

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
        };
    }
}
```

## 3.5 自定义负载均衡策略

通过 `@ApiClient(loadBalancer = ...)` 指定该接口的负载均衡策略，仅分布式模式生效。

```java
// 使用随机策略
@ApiClient(value = "gwsu-xxx", loadBalancer = RandomLoadBalancer.class)
@HttpExchange("/xxx")
public interface XxxClientApi { ... }

// 使用自定义策略
@ApiClient(value = "gwsu-xxx", loadBalancer = MyCustomLoadBalancer.class)
@HttpExchange("/xxx")
public interface XxxClientApi { ... }
```

自定义负载均衡策略实现类需提供以下构造函数签名：

```java
public class MyCustomLoadBalancer implements ReactorServiceInstanceLoadBalancer {

    public MyCustomLoadBalancer(
            ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider,
            String serviceId) {
        // ...
    }

    @Override
    public Mono<Response<ServiceInstance>> choose(Request request) {
        // 自定义选择逻辑
    }
}
```

不指定 `loadBalancer` 时使用全局默认负载均衡策略（Spring Cloud LoadBalancer 自动配置）。

## 3.6 Controller 实现接口

Controller 必须实现对应的 `XxxClientApi` 接口，确保定义与实现一致：

```java
@RestController
@RequestMapping("xxx")
public class XxxController implements XxxClientApi {
    @Override
    public R<XxxVO> getById(@PathVariable String id) { ... }
}
```

## 3.7 降级工厂

同步方法降级：

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
        };
    }
}
```

Flux 方法降级返回 `Flux`：

```java
@Override
public Flux<XxxVO> streamData(XxxDTO query) {
    return Flux.error(new RuntimeException("XXX服务流式接口不可用: " + cause.getMessage()));
}
```

Mono 方法降级返回 `Mono`：

```java
@Override
public Mono<R<XxxVO>> getByIdAsync(String id) {
    return Mono.just(R.fail("XXX服务暂时不可用: " + cause.getMessage()));
}
```

## 3.8 部署模式

| 模式 | 调用方式 | HTTP 客户端 | 检测方式 |
|------|---------|------------|---------|
| 单体 | `LocalApiClientFactory` 直接查找 Bean | 无（本地调用） | `DeployUtils.isSingle()` |
| 分布式 | `RemoteApiClientFactory` HTTP + 熔断 | `WebClient` | 自动检测 Nacos |

### 熔断配置优先级

方法注解 `@CircuitBreakerCustomConfig` > 类注解 `@ApiClient(config = ...)` > 配置文件

```java
@ApiClient(value = "gwsu-xxx", config = @CircuitBreakerCustomConfig(failureRateThreshold = 50))
public interface XxxClientApi {

    @CircuitBreakerCustomConfig(failureRateThreshold = 30, slidingWindowSize = 20)
    @GetExchange("/critical")
    R<Data> criticalEndpoint();
}
```
