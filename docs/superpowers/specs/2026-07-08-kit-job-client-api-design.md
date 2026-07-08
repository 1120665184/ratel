# Kit JobClientApi 设计文档

## 背景

当前 `business-kit-api` 已经提供了 job 相关 DTO/VO，但缺少一个面向服务间直接调用的 `JobClientApi`。现状下其他业务模块如果要创建、更新、启停任务，只能依赖页面侧接口认知或自行拼装 `JobInfoCreateDTO`，调用体验和契约清晰度都不够。

参考项目采用了 `RemoteJobService + JobInfoBuild` 模式，但本仓库的跨服务调用标准是 `@ApiClient + @HttpExchange + FallbackFactory`，因此需要基于本仓库规范设计一套一致的 job client 能力。

## 目标

提供一套可直接在服务间调用的任务管理能力，满足以下范围：

- 支持 `BEAN`、`URL`、`GLUE` 三种任务模式
- 支持常用生命周期接口：创建、创建并启动、更新、启动、停止、删除
- 对调用方提供 Builder 风格构造体验
- 服务端直接复用现有 `JobInfoController` 的新增/编辑能力，避免重复建设

## 不做的内容

- 不新增独立的 `JobClientService`
- 不重构现有 `KitJobService` 分层
- 不引入幂等键、任务标签、租户隔离等增强能力
- 不改造页面侧 `JobInfoCreateDTO` 结构

## 设计取舍

### 方案结论

采用“Builder 主导 + 直接复用 `JobInfoController`”方案：

- 对外：新增 `JobClientApi` 和 `JobInfoBuilder`
- 对内：直接复用 `JobInfoController` 的 `addByDTO/updateByDTO/start/stop/remove` 等现有能力
- 控制器层：由 `JobInfoController` 直接实现 `JobClientApi`

### 这样做的收益

- 改动量最小
- 与当前 job 模块实现兼容性最好
- 交付速度最快

### 已知代价

- `JobClientApi` 间接依赖页面型 `JobInfoCreateDTO`
- 后续如果页面侧 DTO 发生字段调整，服务间调用契约也可能受影响
- 当前方案更偏“企业内部复用包装”，不是完全独立的服务间领域契约

## API 设计

### JobClientApi

放置位置：`business/business-kit/business-kit-api/src/main/java/org/quyq/gwsu/kit/api/job/JobClientApi.java`

调用规范：

- 使用 `@ApiClient`
- 使用 `@HttpExchange`
- 提供 fallback factory
- 基础路径沿用 job info 现有 controller 路径：`/job/info`

接口范围：

- `R<String> create(@RequestBody JobInfoCreateDTO dto)`
- `default R<String> createAndStart(JobInfoCreateDTO dto)`
- `R<String> update(@RequestBody JobInfoCreateDTO dto)`
- `R<String> start(@RequestParam("id") String id)`
- `R<String> stop(@RequestParam("id") String id)`
- `R<String> remove(@RequestParam("id") String id)`

说明：

- `create` 对应现有 `addByDTO`
- `update` 对应现有 `updateByDTO`
- `createAndStart` 作为接口默认方法，先创建，再启动
- `createAndStart` 启动失败时不自动回滚删除已创建任务

## Builder 设计

### 目标

调用方不直接手写 `JobInfoCreateDTO`，而是通过 Builder 按任务模式构建，降低误用概率。

### 放置位置

建议放在 `business-kit-api` 的 `org.quyq.gwsu.kit.api.job` 包下。

### 入口

- `JobInfoBuilder.beanModel(String executorHandler)`
- `JobInfoBuilder.urlModel(String prefix, String url)`
- `JobInfoBuilder.glueModel(String glueType, String glueSource)`

### 通用链式能力

- `jobName(String name, String author)`
- `alarmEmail(String alarmEmail)`
- `routeStrategy(String strategy)`
- `misfireStrategy(String strategy)`
- `blockStrategy(String strategy)`
- `executorTimeout(int seconds)`
- `executorFailRetryCount(int count)`
- `scheduleCron(String cron)`
- `scheduleFixRate(int seconds)`
- `scheduleNone()`
- `childJobIds(List<String> ids)`
- `build()`

### 模式映射规则

#### BEAN 模式

- `jobMode = "BEAN"`
- `executorHandler = 调用方传入值`
- `executorParam = 可选`

#### URL 模式

- `jobMode = "URL"`
- `prefix = 调用方传入值`
- `url = 调用方传入值`
- `bodyJson = 可选`

#### GLUE 模式

- `jobMode = "GLUE"`
- `glueType = 调用方传入值`
- `glueSource = 调用方传入值`
- `glueRemark = 可选，未设置时由 builder 默认补“GLUE代码初始化”`

## 服务端实现

### JobInfoController

直接实现 `JobClientApi`，复用现有方法：

- `create` -> `addByDTO`
- `update` -> `updateByDTO`
- `start` -> 现有 `start`
- `stop` -> 现有 `stop`
- `remove` -> 现有 `remove`

为避免影响现有页面接口，可保留原有方法名和映射，再补接口实现方法；若签名可直接复用，则让现有方法直接满足接口契约。

## Fallback 设计

新增 `JobClientApiFallbackFactory`，风格与 `FileClientApiFallbackFactory` 保持一致：

- 记录异常日志
- 普通 `R<T>` 返回 `R.fail("服务暂时不可用: " + cause.getMessage())`
- 默认方法 `createAndStart` 复用已有 `create/start` 结果

## 校验策略

### Builder 侧基础校验

- `name` 必填
- `author` 必填
- `BEAN` 模式要求 `executorHandler` 必填
- `URL` 模式要求 `prefix`、`url` 必填
- `GLUE` 模式要求 `glueType`、`glueSource` 必填

### 服务端最终校验

继续依赖现有 `KitJobServiceImpl.addByDTO/updateByDTO` 和更底层 `add/update` 的校验逻辑，包括：

- 调度配置校验
- handler 校验
- glue 类型校验
- 子任务合法性校验

## 数据流

### 创建任务

1. 调用方通过 `JobInfoBuilder` 构建 `JobInfoCreateDTO`
2. 调用方执行 `jobClientApi.create(dto)`
3. `JobInfoController` 接收请求并调用 `kitJobService.addByDTO(dto)`
4. 服务端完成 DTO 到 `KitJobInfo` 的映射并持久化
5. 返回 `jobId`

### 创建并启动

1. 调用方通过 `JobInfoBuilder` 构建 `JobInfoCreateDTO`
2. 调用方执行 `jobClientApi.createAndStart(dto)`
3. 默认方法先执行 `create`
4. 创建成功后获取 `jobId`
5. 再执行 `start(jobId)`
6. 返回最终结果

## 测试设计

本次实现按 TDD 进行，至少覆盖：

- `JobInfoBuilder` 对 `BEAN`、`URL`、`GLUE` 三种模式的构建结果
- `JobInfoBuilder` 必填参数校验
- `JobClientApi.createAndStart` 默认方法行为
- `JobInfoController` 实现 `JobClientApi` 后的编译和映射兼容性

优先新增 API 模块单元测试；如果现有测试基建允许，再补 controller 侧测试。

## 涉及文件

### API 模块

- 新增 `job/JobClientApi.java`
- 新增 `job/JobInfoBuilder.java`
- 新增 `job/fallback/JobClientApiFallbackFactory.java`
- 可能新增 `job/package-info.java` 或相关枚举/辅助类

### Server 模块

- 修改 `job/controller/JobInfoController.java`

### 测试

- 新增 `business-kit-api` 下 builder / api 默认方法测试

## 风险

- 由于直接复用 `JobInfoCreateDTO`，页面侧字段演进可能影响服务间调用方
- `createAndStart` 不做事务式回滚，启动失败后会保留已创建任务
- `GLUE` 相关默认备注如果由 builder 填充，需要与服务端现有默认值保持一致，避免行为分叉

## 成功标准

- 其他服务可通过 `JobClientApi` 直接创建、更新、启停、删除任务
- 调用方无需手动拼装 `JobInfoCreateDTO`
- 三种任务模式均可通过 Builder 正确创建
- 单体/分布式部署下均可沿用现有 `@ApiClient` 机制正常调用
