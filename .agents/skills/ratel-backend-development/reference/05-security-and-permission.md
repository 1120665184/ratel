# 五、安全与权限体系

## 5.1 体系总览

```
请求 → AuthenticationFilter（认证） → Casbin（ABAC策略） → DataResourceInterceptor（数据资源） → Controller
                                              ↓                                    ↓
                                        表模型权限校验                       SQL 条件追加
                                              ↓
                                        字段脱敏/隐藏（FieldEnforcer）
```

## 5.2 ABAC 权限模型

基于 jCasbin 实现，策略存储在 Redis 中，支持运行时热更新。

### 策略加载

| 类 | 说明 |
|----|------|
| `RoleBindingMenuAbacLoading` | 角色-菜单权限策略加载 |
| `LoginAllowLoginAbacLoading` | @LoginAllowAccess 标记的放行策略加载 |
| `MenuApiChangeAbacReLoading` | 菜单 API 变更时重新加载策略 |

### 策略存储

- `RedisAdapter`：Casbin 策略 Redis 持久化
- `RedisWatcher`：策略变更 Redis Pub/Sub 监听，触发实时重载

### 自定义函数

| 函数 | 签名 | 说明 |
|------|------|------|
| `ContainsFunction` | `contains(collection, item)` | 集合包含判断 |
| `IsUserLoginFunction` | `isUserLogin()` | 当前用户是否已登录 |
| `TimeInRangeFunction` | `timeInRange(start, end)` | 当前时间在范围内 |
| `CycleWeeklyFunction` | `cycleWeekly(weekdays)` | 周循环判断 |
| `CycleMonthlyFunction` | `cycleMonthly(days)` | 月循环判断 |

### 权限变更

实现 `IAbacAlterationProvider` 接口注册权限变更监听器，`PermissionAlterationManager` 统一管理变更通知。

## 5.3 @TableModelPermission — 表模型权限

声明 Controller 操作的数据库表模型，用于 ABAC 权限校验和 AI 查询权限控制。

### 生效规则

| 场景 | 行为 |
|------|------|
| 无注解 | 无表模型权限 |
| 仅类上 | 类所有方法继承该权限 |
| 仅方法上 | 使用方法上的声明 |
| 类+方法（有配置） | 方法覆盖类 |
| 类+方法（空注解） | 该方法不继承类上的权限 |

### 使用方式

```java
// 类级别（推荐）
@RestController
@TableModelPermission({SecurityRole.class, SecurityRoleMenu.class})
public class SecurityRoleController { ... }

// 方法级别覆盖
@TableModelPermission(SecurityRole.class)  // 覆盖类上的配置
@PostMapping
public R<String> save(...) { ... }

// 方法级别排除
@TableModelPermission   // 空注解：该方法不做表模型权限校验
@GetMapping("/check")
public R<Boolean> check() { ... }

// 直接指定表名（无 Domain 类时）
@TableModelPermission(tables = {"custom_table"})
```

## 5.4 @TableModelField — 字段级权限

标注在 Domain 字段上，控制 AI 查询可见性和脱敏。

### 属性

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `show` | boolean | true | 是否允许 AI 查询该字段 |
| `desensitize` | boolean | false | 返回给用户时是否脱敏 |
| `strategy` | SensitiveStrategy | NONE | 脱敏策略 |
| `prefixNoMaskLen` | int | 1 | 自定义脱敏-不脱敏前缀长度 |
| `suffixNoMaskLen` | int | 1 | 自定义脱敏-不脱敏后缀长度 |
| `symbol` | String | "*" | 自定义脱敏-脱敏标识符 |

### 脱敏策略

| 策略 | 效果 | 示例 |
|------|------|------|
| NONE | 不脱敏 | 原值 |
| USERNAME | 保留首字符 | 张** |
| ID_CARD | 保留前4后4 | 3301**********1234 |
| PHONE | 保留前3后4 | 138****1234 |
| EMAIL | 保留首尾字符 | a****b@example.com |
| ADDRESS | 保留首3后2 | 浙江省****杭州市**** |
| CUSTOM | 自定义前后保留长度 | 配合 prefixNoMaskLen/suffixNoMaskLen |

### 使用示例

```java
@TableName("security_user")
public class SecurityUser extends BaseDO {
    @Schema(description = "用户名")
    private String username;

    @Schema(description = "手机号")
    @TableModelField(desensitize = true, strategy = SensitiveStrategy.PHONE)
    private String phone;

    @Schema(description = "身份证号")
    @TableModelField(desensitize = true, strategy = SensitiveStrategy.ID_CARD)
    private String idCard;

    @Schema(description = "密码")
    @TableModelField(show = false)  // AI 不可查询
    private String password;
}
```

## 5.5 数据资源拦截

### DataResourceInterceptor

拦截查询请求，根据用户数据权限范围自动对 SQL 追加过滤条件。

### 工作区与数据资源的关系

**工作区是数据资源权限的隔离单元**。用户切换工作区时，系统重新计算该工作区下的数据资源并存入会话，后续查询自动按新资源过滤。

- 前端调用 `POST /workspace/list` 获取可访问的工作区列表
- 前端调用 `POST /workspace/switch/{workspaceId}` 切换工作区
- 切换后，`DataResourceScopeManager` 聚合所有 `DataResourceAttributeProvider` 重新计算数据资源

详见 [03-07-common-authentication.md](03-07-common-authentication.md) 工作区与数据资源章节。

### DataScope — 数据权限范围

| 范围 | 说明 |
|------|------|
| ALL | 全部数据，不追加条件 |
| SELF_ONLY | 仅本人数据，追加 `username = 当前用户` |
| 自定义范围 | 根据 DataResourceAttributeProvider 提供的资源追加条件 |

### DataPermissionUtils — 数据权限工具

```java
// 获取当前用户数据权限
DataPermissionInfo info = dataPermissionUtils.getUserDataPermission();

// 对 SQL 追加数据权限过滤
String filteredSql = dataPermissionUtils.applyDataPermission(originalSql);

// ThreadLocal 缓存管理（通常由拦截器自动管理）
dataPermissionUtils.setDataPermission(info);
dataPermissionUtils.clearDataPermission();
```

### DataResourceAttributeProvider — 数据资源属性提供者

为每个工作区提供具体的数据资源列表，注册为 Spring Bean：

```java
@Component
public class DeptAttributeProvider implements DataResourceAttributeProvider {
    @Override
    public ResourceRuleKeyProperties keyInfo() {
        return new ResourceRuleKeyProperties("dept", "部门数据资源");
    }

    @Override
    public List<?> datas(WorkspaceInfo workspace, UserInfo userInfo, DataScope scope) {
        // 根据工作区+用户+权限范围，返回可访问的数据资源
    }
}
```

### DataResourceScopeManager — 数据资源范围管理器

聚合所有 `DataResourceAttributeProvider`，计算指定工作区的完整数据资源 Map，存入会话。

## 5.6 @LoginAllowAccess — 登录放行

标注在 Controller 类或方法上，用户认证后即可访问，跳过 ABAC 权限校验：

```java
@LoginAllowAccess
@GetMapping("/public-config")
public R<ConfigVO> getPublicConfig() { ... }
```

## 5.7 ApiEndpointCollector — 端点收集器

启动时自动收集所有 `@RestController` 端点信息（路径、HTTP 方法、所属模块），供 ABAC 权限策略匹配使用。无需手动配置。

## 5.8 AOT 兼容

`SecurityRuntimeHintsRegistrar` 注册 Casbin、数据资源等涉及的反射元数据，确保 Native Image 编译正常。
