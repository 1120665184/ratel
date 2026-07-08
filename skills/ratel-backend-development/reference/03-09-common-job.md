# common-job 模块使用指南

基于 xxl-job 3.3.2 改造的任务调度模块，去掉原始 `xxl-job-core` 依赖，完全自研 Executor 端通信与注册机制，集成项目的 `@ApiClient` 跨模块调用框架和双部署模式。

## 模块定位

| 模块 | 角色 | 说明 |
|------|------|------|
| `common-job` | Executor 端库 | 被所有需要运行 JobHandler 的微服务依赖 |
| `business-kit-server` | Admin 端 | 调度引擎 + 注册中心 + 管理接口 |

## 引入依赖

在需要执行定时任务的微服务模块中添加：

```xml
<dependency>
    <groupId>org.quyq.gwsu</groupId>
    <artifactId>common-job</artifactId>
</dependency>
```

引入后自动生效（`XxlJobAutoConfiguration` 默认启用），无需额外配置。

## 编写 JobHandler

### 方式一：@XxlJob 注解（推荐）

在 Spring Bean 的方法上标注 `@XxlJob` 注解：

```java
@Component
public class MyJobHandler {

    @XxlJob("myJobHandler")
    public void myJobHandler() throws Exception {
        String param = XxlJobHelper.getJobParam();
        XxlJobHelper.log("任务执行开始，参数: {}", param);

        // 业务逻辑...

        XxlJobHelper.handleSuccess("执行成功");
    }
}
```

**注解参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| `value` | String | Handler 名称，**全局唯一**，不允许跨 appname 同名 |
| `init` | String | 初始化方法名（可选），JobThread 创建时调用 |
| `destroy` | String | 销毁方法名（可选），JobThread 销毁时调用 |

**注意事项**：
- Handler 名称必须全局唯一，不同微服务（不同 appname）不能注册同名 Handler，否则注册会被 Admin 拒绝
- 方法签名必须为 `public void methodName() throws Exception`

### 方式二：继承 IJobHandler

```java
@Component
public class MyCustomHandler extends IJobHandler {

    @Override
    public void execute() throws Exception {
        // 业务逻辑
    }

    @Override
    public void init() throws Exception {
        // 初始化（可选）
    }

    @Override
    public void destroy() throws Exception {
        // 销毁（可选）
    }
}
```

> 注意：继承 `IJobHandler` 的方式不会被自动注册，需手动调用 `XxlJobExecutor.getInstance().registryJobHandler()`。

## XxlJobHelper 工具类

任务执行上下文和结果处理的核心工具，在 Handler 方法内部使用。

### 获取任务信息

| 方法 | 返回值 | 说明 |
|------|--------|------|
| `XxlJobHelper.getJobId()` | String | 当前任务 ID |
| `XxlJobHelper.getJobParam()` | String | 当前任务参数（在 Admin 配置） |
| `XxlJobHelper.getLogId()` | String | 当前日志 ID |
| `XxlJobHelper.getLogFileName()` | String | 当前日志文件名 |
| `XxlJobHelper.getShardIndex()` | int | 分片序号（从 0 开始） |
| `XxlJobHelper.getShardTotal()` | int | 分片总数 |

### 日志记录

```java
// 格式化日志
XxlJobHelper.log("处理第 {} 条数据，状态: {}", index, status);

// 异常堆栈日志
XxlJobHelper.log(e);
```

日志同时写入文件（`{user.dir}/logs/{appName}/job/`）和 Admin 日志系统。

### 设置处理结果

```java
// 成功
XxlJobHelper.handleSuccess();
XxlJobHelper.handleSuccess("处理了100条数据");

// 失败
XxlJobHelper.handleFail();
XxlJobHelper.handleFail("数据校验失败");

// 超时
XxlJobHelper.handleTimeout();
XxlJobHelper.handleTimeout("执行超过60秒");
```

> 如果不调用任何 `handleXxx` 方法，默认为成功（`HANDLE_CODE_SUCCESS = 200`）。

## 分片广播

路由策略选择 `SHARDING_BROADCAST` 时，所有 Executor 实例都会收到调度，通过分片参数分配数据：

```java
@XxlJob("shardingJobHandler")
public void shardingJobHandler() throws Exception {
    int shardIndex = XxlJobHelper.getShardIndex();  // 当前分片序号
    int shardTotal = XxlJobHelper.getShardTotal();    // 分片总数

    // 按分片处理数据，例如：
    // SELECT * FROM orders WHERE MOD(id, #{shardTotal}) = #{shardIndex}
    XxlJobHelper.log("分片 {}/{} 开始处理", shardIndex, shardTotal);
}
```

## 阻塞策略

在 Admin 配置任务时选择，当上一次调度尚未完成，新调度到达时的处理方式：

| 策略 | 枚举值 | 说明 |
|------|--------|------|
| 串行执行 | `SERIAL_EXECUTION` | 排队等待（默认） |
| 丢弃后续 | `DISCARD_LATER` | 丢弃本次调度 |
| 覆盖之前 | `COVER_EARLY` | 终止上次调度，执行本次 |

## Glue 模式

支持在线编辑代码，无需重新部署：

| 类型 | 说明 |
|------|------|
| `BEAN` | 模式 Handler（`@XxlJob` 注解），默认方式 |
| `GLUE_GROOVY` | 在线编辑 Java 代码，动态编译执行 |
| `GLUE_SHELL` | Shell 脚本 |
| `GLUE_PYTHON` | Python3 脚本 |
| `GLUE_NODEJS` | Node.js 脚本 |

> Glue 模式需要在 `XxlJobAutoConfiguration` 中启用 `glueEnabled = true`（默认已启用）。

## 自动配置项

### XxlJobAutoConfiguration

所有参数从 Spring 属性自动推导，**无需手动配置**：

| 参数 | 推导规则 | 默认值 |
|------|---------|--------|
| `appname` | `spring.application.name` + `-executor` | `default-job-executor` |
| `logPath` | `{user.dir}/logs/{appName}/job` | - |
| `logRetentionDays` | 固定值 | `30` |
| `glueEnabled` | 固定值 | `true` |


## 注册与调度机制

### 注册流程

1. Executor 启动时，`XxlJobExecutor` 扫描所有 `@XxlJob` 注解方法，注册到本地 `jobHandlerRepository`
2. `ExecutorRegistryHelper` 每 30 秒通过 `JobAdminClientApi.registry()` 向 Admin 发起心跳注册
3. Admin 端 `JobRegistryHelper` 将注册信息持久化到 `kit_job_registry` 表
4. Admin 端 `registryMonitorTask` 每 30 秒从 DB 加载注册信息，构建 `handler2RegistryCache` 内存缓存

### 冲突检测

**同一 Handler 名称不允许跨 appname 注册**：

- 注册时，Admin 检查 DB 中是否已存在不同 appname 的同名 Handler，若存在则拒绝注册
- Executor 端收到注册失败响应后，输出 `warn` 级别告警日志
- 缓存聚合时，如果发现同名 Handler 被多个 appname 注册（数据残留），只保留字母序第一个 appname，其余自动丢弃并输出告警日志

### 调度流程

```
Admin scheduleThread → SELECT FOR UPDATE (分布式锁)
  → 查询 kit_job_info 待触发任务
  → 路由策略选择 Executor 地址
  → TriggerStrategy 触发执行
  → 更新触发时间 → COMMIT
```

### 双部署模式

| 模式 | Executor→Admin | Admin→Executor | 注册地址 |
|------|----------------|----------------|---------|
| 单体 | `LocalApiClientFactory`（本地 Bean 调用） | `LocalTriggerStrategy`（直接方法调用） | `"local"` |
| 分布式 | `RemoteApiClientFactory`（HTTP + Nacos 发现） | `RemoteTriggerStrategy`（WebClient HTTP） | `http://{ip}:{port}/` |

部署模式由 `DeployInitializer` 自动检测 Nacos classpath 决定，无需手动配置。

### 路由策略

Admin 调度时选择目标 Executor 的策略：

| 策略 | 枚举值 | 说明 |
|------|--------|------|
| 第一个 | `FIRST` | 选择地址列表第一个 |
| 最后一个 | `LAST` | 选择地址列表最后一个 |
| 轮询 | `ROUND` | Round-Robin 轮询 |
| 随机 | `RANDOM` | 随机选择 |
| 一致性HASH | `CONSISTENT_HASH` | 基于任务ID的一致性哈希 |
| 最不经常使用 | `LEAST_FREQUENTLY_USED` | LFU 策略 |
| 最近最久未使用 | `LEAST_RECENTLY_USED` | LRU 策略 |
| 故障转移 | `FAILOVER` | 依次心跳探测，选第一个存活 |
| 忙碌转移 | `BUSYOVER` | 依次空闲探测，选第一个空闲 |
| 分片广播 | `SHARDING_BROADCAST` | 广播到所有 Executor |

## Executor Web 端点（分布式模式）

分布式模式下，`ExecutorWebConfiguration` 自动暴露以下 HTTP 端点供 Admin 调用：

| 端点 | 说明 |
|------|------|
| `POST /job-executor/beat` | 心跳探测 |
| `POST /job-executor/idleBeat` | 空闲探测 |
| `POST /job-executor/trigger` | 触发任务 |
| `POST /job-executor/kill` | 终止任务 |
| `POST /job-executor/log` | 查询日志 |

单体模式下不需要这些端点（直接 JVM 内调用）。

## 核心常量

| 常量 | 值 | 说明 |
|------|------|------|
| `HANDLE_CODE_SUCCESS` | 200 | 执行成功 |
| `HANDLE_CODE_FAIL` | 500 | 执行失败 |
| `HANDLE_CODE_TIMEOUT` | 502 | 执行超时 |
| `REGISTRY_BEAT_INTERVAL` | 30 | 心跳间隔（秒） |

## 常见问题

### Q: Handler 名称冲突怎么办？

不同微服务不允许注册同名 Handler。如果出现冲突：
- Admin 端拒绝注册，日志：`handler注册拒绝! handler:xxx, 当前appname:xxx, 冲突appname:xxx`
- Executor 端告警：`xxl-job registry fail, handler:xxx, appname:xxx`
- 解决方式：修改 Handler 名称，确保全局唯一

### Q: 如何调试 Handler？

在 Handler 方法内使用 `XxlJobHelper.log()` 输出的日志会同时写入文件和 Admin 日志系统，可在 Admin 管理界面查看执行日志。

### Q: 任务超时如何设置？

在 Admin 管理界面配置任务的 `executorTimeout` 字段（秒）。超时后 Handler 可通过 `XxlJobHelper.handleTimeout()` 主动标记，或由 Admin 端检测。

### Q: 集群部署下 Admin 调度会重复吗？

不会。Admin 通过 `kit_job_lock` 表的 `SELECT FOR UPDATE` 悲观锁保证同一时刻只有一台 Admin 实例执行调度，不会重复触发。
