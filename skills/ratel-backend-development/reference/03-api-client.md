# 三、API Client 规范

## 3.1 何时需要

- 新功能模块无需添加 API Client
- **跨业务模块调用**时需要：如 system 模块调用 security 模块的功能，需 security 开发 API Client

## 3.2 API 接口定义（api 模块）

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

## 3.3 Controller 实现接口

Controller 必须实现对应的 `XxxClientApi` 接口，确保定义与实现一致：

```java
@RestController
@RequestMapping("xxx")
public class XxxController implements XxxClientApi {
    @Override
    public R<XxxVO> getById(@PathVariable String id) { ... }
}
```

## 3.4 降级工厂

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

## 3.5 部署模式

| 模式 | 调用方式 | 检测方式 |
|------|---------|---------|
| 单体 | `LocalApiClientFactory` 直接查找 Bean | `DeployUtils.isSingle()` |
| 分布式 | `RemoteApiClientFactory` HTTP + 熔断 | 自动检测 Nacos |
