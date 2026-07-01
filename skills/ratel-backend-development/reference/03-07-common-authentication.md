# common-authentication — 认证体系

> **注意**：common-authentication 只需在**认证服务（system）**中引入，其他业务服务不需要引用。

## 默认认证接口

`LoginWebConfiguration` 自动注册以下接口（路径在单体模式下自动添加服务前缀）：

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/auth/login/{accountType}` | 用户认证（accountType 见 AccountType 枚举） |
| POST | `/auth/logout` | 退出登录 |
| GET | `/auth/callback/{accountType}/{loginType}` | 三方平台认证回调 |
| GET | `/auth/url/{accountType}/{loginType}` | 获取三方认证地址（前端跳转用） |
| POST | `/workspace/list` | 获取工作区列表和当前工作区 |
| POST | `/workspace/switch/{workspaceId}` | 切换工作区（**等同于切换数据资源权限**） |

### 工作区切换机制

切换工作区时，系统会：
1. 校验目标工作区是否属于当前用户
2. 将新工作区信息存入会话（`SESSION_CURR_WORKSPACE`）
3. 重新计算该工作区下的数据资源（聚合所有 `DataResourceAttributeProvider`）
4. 将新数据资源存入会话（`SESSION_CURR_DATA_RESOURCE`）

后续查询请求经 `DataResourceInterceptor` 拦截，根据会话中的数据资源对 SQL 追加过滤条件。

## 登录处理器体系

```
LoginHandler<T>（接口）
├── loginType()            — 支持的登录类型标识
├── authenticate(T)        — 认证逻辑
└── AbstractLoginHandler<T,U>（抽象基类，封装 Sa-Token 登录流程）
    ├── PasswordLoginHandler   — 账号密码登录
    ├── DingTalkLoginHandler   — 钉钉登录
    └── HeadlessLoginHandler   — 无头智能体登录
```

**自定义登录方式**：实现 `LoginHandler<T extends AbstractLoginDTO>` 接口，注册为 Spring Bean 即可。

## LoginManager — 登录管理器

根据 `loginType` 自动路由到对应的 `LoginHandler`，无需手动选择。

## LoginInterceptor — 登录拦截器

登录流程的拦截器链，支持在认证前后插入自定义逻辑：

```java
public interface LoginInterceptor<U extends UserInfo> {
    boolean afterAuthenticated(LoginInterceptorContext<U> context);  // 认证成功后
    boolean afterLoginSuccess(LoginInterceptorContext<U> context);  // 登录成功后
    void afterLoginFailure(LoginInterceptorContext<U> context);     // 登录失败后
}
```

## LoginInterceptorUtils — 登录拦截器事件触发

```java
LoginInterceptorUtils.fireAfterAuthenticated(loginType, context);
LoginInterceptorUtils.fireAfterLoginSuccess(loginType, context);
LoginInterceptorUtils.fireAfterLoginFailure(loginType, context);
```

## LogicUtils — 认证逻辑

`LogicUtils.getLogic()` → 获取当前 `StpLogic` 实例（Sa-Token 多账号体系支持）

## 工作区与数据资源

### 核心概念

**工作区（Workspace）** 是数据资源权限的隔离单元。切换工作区 = 切换数据资源权限。同一用户在不同工作区下看到的数据范围不同。

- `WorkspaceInfo`（record）：`id`（唯一标识）+ `name`（名称）
- 未配置 `WorkspaceProvider` 时，默认工作区为 `WorkspaceInfo("Default", "默认")`

### WorkspaceProvider — 工作区提供者

决定用户可访问的工作区列表。**必须注册为 Spring Bean**。

```java
@Component
public class MyWorkspaceProvider implements WorkspaceProvider<MyUserInfo> {
    @Override
    public List<WorkspaceInfo> list(MyUserInfo user) {
        // 根据用户信息返回其可访问的工作区列表
        return List.of(
            new WorkspaceInfo("ws-dept-a", "A部门"),
            new WorkspaceInfo("ws-dept-b", "B部门")
        );
    }
}
```

### DataResourceAttributeProvider — 数据资源属性提供者

为每个工作区提供具体的数据资源列表。**必须注册为 Spring Bean**。

```java
@Component
public class DeptAttributeProvider implements DataResourceAttributeProvider {
    @Override
    public ResourceRuleKeyProperties keyInfo() {
        return new ResourceRuleKeyProperties("dept", "部门数据资源");
    }

    @Override
    public List<?> datas(WorkspaceInfo workspace, UserInfo userInfo, DataScope dataScope) {
        // 根据工作区+用户+权限范围，返回该用户在该工作区下可访问的数据资源
        // 如：返回该工作区下的部门ID列表
    }
}
```

### DataResourceScopeManager — 数据资源范围管理器

聚合所有 `DataResourceAttributeProvider`，计算指定工作区的完整数据资源 Map：

```java
// 获取用户可访问的工作区列表
List<WorkspaceInfo> workspaces = DataResourceScopeManager.workspaceList(userInfo);

// 获取指定工作区的数据资源
Map<String, List<?>> resources = DataResourceScopeManager.dataResource(workspaceInfo, userInfo, dataScope);
// 结果示例：{"dept": ["dept-001", "dept-002"], "region": ["east"]}
```

### 配置步骤

1. **实现 `WorkspaceProvider`**（可选）：定义用户可访问的工作区列表，注册为 Bean
2. **实现 `DataResourceAttributeProvider`**（可选，可多个）：为每个工作区提供数据资源，注册为 Bean
3. **无需额外配置**：`DataResourceScopeManager` 自动注入所有 Provider
4. 前端调用 `/workspace/list` 获取工作区列表，调用 `/workspace/switch/{id}` 切换

## 登录领域对象

| 类 | 说明 |
|----|------|
| `AbstractLoginDTO` | 登录参数基类 |
| `LoginVO` | 登录响应（token、用户信息） |
| `LoginToken` | 登录 Token 信息 |
| `WorkspaceInfo` | 工作区信息（record: id + name） |
