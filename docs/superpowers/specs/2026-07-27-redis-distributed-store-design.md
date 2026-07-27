# common-ai 自定义 Redis DistributedStore 设计

## 目标

在 `common/common-ai` 模块内提供一套自定义的 `DistributedStore` 实现，用于替代 `agentscope-extensions-redis` 中的 Redis 分布式能力接入。新实现必须复用项目现有的 Redis 使用习惯和基础设施：

- `AgentStateStore` 继续使用现有 `DatabaseStateStore`
- `BaseStore`、`SandboxSnapshotSpec`、`SandboxExecutionGuard` 基于 `CacheUtils` 实现
- 后续业务模块只依赖 `common-ai`，不再需要直接依赖 `agentscope-extensions-redis`

## 范围

本次设计覆盖以下三个 AgentScope 扩展点：

- `BaseStore`：用于远程工作区文件系统的底层 KV 存储
- `SandboxSnapshotSpec`：用于 sandbox 快照的持久化与恢复
- `SandboxExecutionGuard`：用于 sandbox 隔离槽位的分布式并发控制

本次不改动以下内容：

- `AgentStateStore` 的数据库表结构与读写逻辑
- 业务模块中 `HarnessAgent.builder().stateStore(...)` 的使用方式
- `CacheUtils` 之外的 Redis 客户端接入方式

## 方案

### 总体结构

在 `common/common-ai/src/main/java/org/quyq/gwsu/common/ai/distributed/redis/` 下新增以下组件：

- `CacheRedisDistributedStore`：实现 `DistributedStore`，统一装配 3 个子组件，并复用外部注入的 `AgentStateStore`
- `CacheRedisBaseStore`：实现 `BaseStore`
- `CacheRedisSandboxSnapshotSpec`：实现 `SandboxSnapshotSpec`
- `CacheRedisSandboxSnapshot`：表示单个 `snapshotId` 对应的快照实例
- `CacheRedisSandboxExecutionGuard`：实现 `SandboxExecutionGuard`
- `CacheRedisDistributedStoreProperties`：集中定义 key 前缀、TTL、锁等待间隔等参数
- `CacheRedisDistributedStoreConstants`：定义 Redis key 模板、字段名和脚本常量

如 `CacheUtils` 现有能力不足以支撑 CAS 或脚本执行，本次允许在 `common-cache` 中补充最小必要方法，但不绕开 `CacheUtils` 直接在 `common-ai` 中操作 `RedisTemplate`。

### Bean 装配

在 `AgentscopeConfiguration` 中新增默认 Bean：

- 注入现有 `AgentStateStore`
- 注入 `CacheUtils`
- 构造 `CacheRedisDistributedStore`

装配原则：

- `AgentStateStore` 由外部传入，默认是 `DatabaseStateStore`
- `DistributedStore` 的 Redis 能力只负责除 `AgentStateStore` 外的其它分布式组件
- 业务代码需要显式调用 `.distributedStore(distributedStoreBean)`，不通过静态工厂隐藏依赖

## Redis 数据模型

### 全局前缀

统一使用 Redis key 前缀：

`agentscope:distributed:*`

不额外套用项目业务语义前缀，避免与普通缓存混淆。由于 `CacheUtils` 已有项目前缀能力，最终实际写入 Redis 的 key 仍会自动带上项目隔离前缀。

### BaseStore

`BaseStore` 负责存储远程文件系统中的“单个文件项”和“namespace 索引”。

建议使用两类 key：

- `agentscope:distributed:store:item:{namespacePath}:{itemKey}`
- `agentscope:distributed:store:index:{namespacePath}`

其中：

- `namespacePath` 为 namespace 数组按固定分隔符拼接后的结果
- `itemKey` 为单个文件或目录项的逻辑 key

单项 value 存储内容：

- `key`
- `version`
- `payload`
- `createdAt`
- `modifiedAt`

索引 key 保存当前 namespace 下所有 itemKey 的集合，用于支持 `search(namespace, limit, offset)`。

策略：

- `BaseStore` 默认不过期
- `put` 每次写入时递增 `version`
- `putIfVersion` 使用 Lua 脚本做原子 CAS
- `delete` 同时删除单项内容和索引项

### SandboxSnapshot

快照使用单 key 存储：

`agentscope:distributed:snapshot:{snapshotId}`

value 保存：

- `snapshotId`
- `content`
- `updatedAt`

策略：

- 默认设置 TTL
- TTL 使用配置值，默认建议 24 小时
- 每次保存快照都刷新 TTL
- 删除快照时直接删除对应 key

### SandboxExecutionGuard

执行锁使用单 key：

`agentscope:distributed:lock:{scope}:{value}`

value 保存随机 token。

策略：

- 通过 `SET NX PX` 或等价 Lua 语义实现获取锁
- 通过“比对 token 后删除”的 Lua 脚本释放锁，防止误删别人的锁
- 锁默认带 TTL，避免进程崩溃后永久死锁
- 获取失败时按固定重试间隔阻塞轮询，直到成功或线程中断

默认参数建议：

- 锁 TTL：30 秒
- 重试间隔：200 毫秒
- 快照 TTL：24 小时

以上参数通过 `CacheRedisDistributedStoreProperties` 提供默认值，并允许后续外部配置覆盖。

## 组件职责

### CacheRedisDistributedStore

职责：

- 暴露 `agentStateStore()`、`baseStore()`、`sandboxSnapshotSpec()`、`sandboxExecutionGuard()`
- 复用外部注入的 `AgentStateStore`
- 惰性或一次性创建 Redis 相关子组件

约束：

- 不承载具体 Redis 读写逻辑
- 只做装配与参数分发

### CacheRedisBaseStore

职责：

- 实现 `get/put/putIfVersion/search/delete`
- 负责 namespace 编码、Redis key 组装、序列化/反序列化
- 维护 item 与 namespace 索引的一致性

实现要求：

- 统一校验空 namespace、空 key、非法分隔符
- `search` 按稳定顺序返回结果，避免分页漂移
- `putIfVersion` 必须原子执行

`search` 的实现采用“索引集合 + 排序 + 分页切片”策略即可，不要求 Redis 侧原生分页。

### CacheRedisSandboxSnapshotSpec / CacheRedisSandboxSnapshot

职责：

- `CacheRedisSandboxSnapshotSpec.build(snapshotId)` 返回与该 `snapshotId` 绑定的快照对象
- 快照对象负责保存、读取、删除具体内容

实现要求：

- 快照内容作为完整对象存储，不做字段级拆分
- 读到不存在 key 时返回空结果
- 保存后自动刷新 TTL

### CacheRedisSandboxExecutionGuard

职责：

- 基于 `SandboxIsolationKey` 生成分布式锁 key
- 在 `tryEnter` 中阻塞等待锁
- 返回 `SandboxLease`，在 `close()` 中安全释放锁

实现要求：

- 每次加锁生成唯一 token
- 释放锁时校验 token
- 收到 `InterruptedException` 时立即恢复中断并终止等待

## 对 CacheUtils 的要求

当前 `CacheUtils` 已具备以下可复用能力：

- 普通 key 的 `get/set/setIfAbsent/exists/expire/delete/scan`
- 分布式锁 `getLock/executeWithLock`
- Lua 脚本 `executeScript`

本次优先复用以上能力。

如需要补充能力，允许新增以下最小方法：

- 获取原始锁对象以外的更细粒度 TTL/状态方法时，优先补 `CacheUtils` 包装，不在业务模块直接用 `RedisTemplate`
- 如 `scan` 返回的 pattern 匹配与分页切片不足以支撑稳定 `search`，则在 `CacheUtils` 中补面向当前场景的最小封装

不新增通用性过强、与当前设计无关的 Redis 工具接口。

## 错误处理

- 统一将 Redis 访问异常转换为 `common-ai` 内部异常，错误信息包含操作类型和目标 key
- 不向上层暴露底层 Redis 客户端实现细节
- 对参数错误使用快速失败策略，在进入 Redis 前完成校验

## 验证

至少完成以下验证：

- `BaseStore`：
  - `put/get/delete` 正常
  - `putIfVersion` 成功与失败路径正确
  - `search` 能稳定分页
- `SandboxSnapshot`：
  - 保存后可读取
  - 读取不存在快照返回空
  - TTL 能在保存后刷新
- `SandboxExecutionGuard`：
  - 同一 key 并发进入时串行
  - 释放锁时不会误删非本持有者的锁
- Spring 装配：
  - `DistributedStore` Bean 能正常创建
  - `AgentStateStore` 仍指向 `DatabaseStateStore`

## 实施顺序

1. 在 `common-ai` 新增 `distributed.redis` 包及核心类骨架
2. 在 `common-cache` 中补齐实现 CAS/脚本所需的最小工具方法
3. 实现 `CacheRedisBaseStore`
4. 实现 `CacheRedisSandboxSnapshotSpec` 与快照对象
5. 实现 `CacheRedisSandboxExecutionGuard`
6. 在 `AgentscopeConfiguration` 中新增 `DistributedStore` Bean
7. 补充测试与最小装配验证
