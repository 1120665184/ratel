# common-core — 核心工具与领域基类

> 编写后端代码时**优先使用 common 模块工具类**，而非第三方工具。

## 领域基类

| 类 | 说明 | 关键字段 |
|----|------|---------|
| `BaseVO` | VO/DO 公共基类 | modifyOp/Time, createOp/Time, `copyBaseProperties()` |
| `BaseDO` | Domain 基类，继承 BaseVO | tenantId, deleted, deleteOp/Time |
| `BaseDTO` | 查询参数基类 | pageNum=1, pageSize=10, orderByColumn, asc, `getOrderBy()` |
| `R<T>` | 统一响应 record | `R.ok(data)`, `R.fail("msg")`, `R.fail(exception)` |
| `BusinessModuleInfo` | 模块信息 | prefix, description |

## AssertUtils — 参数验证

Controller/Service 层参数校验，失败抛 `ArgumentException`。

| 方法 | 说明 |
|------|------|
| `hasText(T, ReturnCode)` | 字符串不为空白 |
| `notEmpty(T, ReturnCode)` | 字符串/数组/集合/Map 不为空 |
| `notNull(T, ReturnCode)` | 对象不为 null |
| `checkBetween(int/long/double, min, max, ReturnCode)` | 数值范围 |
| `isTrue(boolean, ReturnCode)` | 表达式为 true |
| `equals(Object, Object, ReturnCode)` | 两对象相等 |

```java
AssertUtils.hasText(name, XxxErrorCode.E00001);
AssertUtils.notEmpty(ids, XxxErrorCode.E00002);
AssertUtils.checkBetween(age, 0, 150, XxxErrorCode.E00003);
```

## SpringUtils — Spring 上下文

| 方法 | 说明 |
|------|------|
| `getBean(Class<T>)` | 按类型获取 Bean |
| `getBeansOfType(Class<T>)` | 获取某类型所有 Bean |
| `getAopProxy(T)` | 获取 AOP 代理对象 |

## ServletUtils — HTTP 请求

| 方法 | 说明 |
|------|------|
| `getRequest()` | 获取当前 HttpServletRequest |
| `getClientIP()` | 获取客户端 IP |
| `getHeaders()` | 获取当前请求头 Map |
| `LOCAL_HEADERS` | TransmittableThreadLocal，跨线程传递请求头 |

## DeployUtils — 部署模式

`DeployUtils.isSingle()` → 判断是否单应用部署

## ProjectUtils — 项目信息（Spring Bean 注入）

`getServerPrefix()` → `"gwsu:gwsu-security"` 格式的服务前缀

## ProxyUtil — AOT 兼容类检测

`ProxyUtil.hasClass(className)` → 类路径中是否存在指定类（用于 AOT 条件判断）

## ThreadPoolUtil — 上下文传播线程池

创建的线程池自动传播 Reactor/ThreadLocal 上下文（替代 `Executors` 直接创建）：

```java
ExecutorService pool = ThreadPoolUtil.newFixedThreadPool(4);
ExecutorService virtual = ThreadPoolUtil.newVirtualThreadPerTaskExecutor();
```

## ProcessorChain — 请求处理责任链

基于 `RequestResponseProcessor` 接口的响应式责任链，按 `@Order` 排序执行：

```java
public interface RequestResponseProcessor {
    Mono<Boolean> preHandle(RequestResponseContext context);   // 前置，返回 false 中断
    Mono<Void> postHandle(RequestResponseContext context);     // 后置（逆序执行）
    boolean needsResponseBody(RequestResponseContext context); // 是否需要响应体
}
```

## 异常体系

| 类 | 说明 |
|----|------|
| `BusinessException` | 业务异常（配合错误码枚举使用） |
| `ArgumentException` | 参数校验异常（AssertUtils 内部使用） |
| `GlobalExceptionHandler` | 全局异常处理器（自动捕获，无需手动配置） |
