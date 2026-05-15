# 表模型权限控制系统设计文档

## 一、概述

智能助手AI操作表模型时，需要一套权限控制系统来管理：
- 哪些表模型可以被查询
- 表中哪些字段可以被查询
- 哪些字段返回给用户时需要脱敏

权限来源有两个层级：
1. **注解采集**：开发者在代码中通过注解声明表模型权限（硬约束，最高优先级）
2. **角色配置**：管理员在角色管理中自定义表模型字段权限（软约束，可在注解约束范围内配置）

### 核心概念：表模型唯一标识

表模型名称（`table_name`）不能唯一确定一条记录，以下场景会导致同名表存在：
- 不同服务模块可能有同名的表（如 `system` 模块和 `security` 模块都有 `sys_user`）
- 同一服务的不同数据源也可能有同名的表

因此，**表模型唯一标识 = `module_prefix + datasource + table_name`**（三元组）。

本文档中所有引用"表模型"作为Map key或唯一标识的地方，均使用此三元组。

## 二、注解设计

### 2.1 `@TableModelPermission` — 表模型权限注解

**所在模块**：`common/common-security`

```java
package org.quyq.gwsu.common.security.annotation;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface TableModelPermission {
    /**
     * 表模型对应的Domain类列表（类需有@TableName注解）
     */
    Class<?>[] value() default {};

    /**
     * 表名列表（直接指定表名，与value互为补充）
     */
    String[] tables() default {};
}
```

**生效规则**：

| 场景 | 类上注解 | 方法上注解 | 结果 |
|------|---------|-----------|------|
| 默认 | 无 | 无 | 无表模型权限 |
| 仅类上 | 有 | 无 | 继承类上的表模型权限 |
| 仅方法上 | 无 | 有 | 使用方法上标注的表模型 |
| 类+方法 | 有 | 有（配置了表模型） | 方法上的覆盖类上的 |
| 类+方法 | 有 | 有（空注解，未配置任何表模型） | 该接口不继承类上的表模型权限（显式排除） |

### 2.2 `@TableModelField` — 表模型字段配置注解

**所在模块**：`common/common-security`

```java
package org.quyq.gwsu.common.security.annotation;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface TableModelField {
    /**
     * 是否允许AI查询该字段，默认true
     */
    boolean show() default true;

    /**
     * 返回给用户时是否脱敏，默认false
     */
    boolean desensitize() default false;

    /**
     * 脱敏策略，默认NONE
     */
    SensitiveStrategy strategy() default SensitiveStrategy.NONE;

    /**
     * 自定义脱敏-不脱敏前缀长度，仅 strategy=CUSTOM 时生效
     */
    int prefixNoMaskLen() default 1;

    /**
     * 自定义脱敏-不脱敏后缀长度，仅 strategy=CUSTOM 时生效
     */
    int suffixNoMaskLen() default 1;

    /**
     * 自定义脱敏-脱敏标识符，仅 strategy=CUSTOM 时生效
     */
    String symbol() default "*";
}
```

### 2.3 `SensitiveStrategy` — 脱敏策略枚举

**所在模块**：`common/common-security`

参考 `SensitiveStrategyEnum` 的设计，适配本项目：

```java
package org.quyq.gwsu.common.security.annotation;

public enum SensitiveStrategy {
    /**
     * 无脱敏
     */
    NONE,

    /**
     * 用户名：张**
     */
    USERNAME,

    /**
     * 身份证：3301**********1234
     */
    ID_CARD,

    /**
     * 手机号：138****1234
     */
    PHONE,

    /**
     * 邮箱：a****b@example.com
     */
    EMAIL,

    /**
     * 地址：浙江省****杭州市****
     */
    ADDRESS,

    /**
     * 自定义脱敏：通过 prefixNoMaskLen / suffixNoMaskLen / symbol 配置
     */
    CUSTOM;
}
```

### 2.4 使用示例

```java
@RestController
@RequestMapping("user")
@Tag(name = "用户管理")
@TableModelPermission(value = {SysUser.class}, tables = {"sys_user_dept"})
public class UserController {

    // 继承类上的表模型权限：sys_user + sys_user_dept

    @GetMapping("/list")
    @TableModelPermission(tables = {"sys_account"})
    // 方法级覆盖：只有 sys_account
    public R<List<UserVO>> list() { ... }

    @GetMapping("/detail")
    @TableModelPermission
    // 空注解：不继承类上的表模型权限，该接口无表模型权限
    public R<UserVO> detail() { ... }

    @PostMapping
    public R<Boolean> create() { ... }
    // 继承类上的：sys_user + sys_user_dept
}

@TableName("sys_user")
public class SysUser extends BaseDO {

    @TableModelField(show = false)
    private String lastLoginIp;   // AI禁止查询

    @TableModelField(desensitize = true, strategy = SensitiveStrategy.PHONE)
    private String phone;         // 脱敏显示

    @TableModelField(desensitize = true, strategy = SensitiveStrategy.EMAIL)
    private String email;         // 脱敏显示

    @TableModelField(desensitize = true, strategy = SensitiveStrategy.CUSTOM,
                     prefixNoMaskLen = 2, suffixNoMaskLen = 1, symbol = "*")
    private String username;      // 自定义脱敏：张*

    private Integer gender;       // 默认：可查询，不脱敏
}
```

## 三、采集流程

### 3.1 扩展 ApiEndpointCollector

在 `ApiEndpointCollector.buildEndpointInfo()` 中增加 `@TableModelPermission` 注解的采集逻辑：

1. 获取类上的 `@TableModelPermission`，解析 `value()` 和 `tables()` 得到表模型列表
2. 获取方法上的 `@TableModelPermission`，按优先级规则确定最终表模型列表
3. 对于通过 `value()` 指定的Domain类，通过反射获取字段上的 `@TableModelField` 注解，构建字段配置
4. 确定数据源：Controller类上有 `@DS` 则取其值，否则取主数据源
5. 将表模型信息附加到 `ApiEndpointInfo` 中，随接口信息一起推送到 Redis

### 3.2 扩展 ApiEndpointInfo

```java
public record ApiEndpointInfo(
        String id,
        String modulePrefix,
        String tagName,
        String reqPath,
        String reqMethod,
        String summary,
        String requestClass,
        String responseClass,
        String className,
        String methodName,
        boolean allowLoginAccess,
        List<TableModelInfo> tableModels  // 新增
) {}
```

### 3.3 新增 TableModelInfo

```java
public record TableModelInfo(
        String modulePrefix,
        String tableName,
        String datasource,
        Map<String, FieldPermission> fieldConfig
) {}
```

**唯一标识**：`modulePrefix + datasource + tableName` 三元组

### 3.4 新增 FieldPermission

```java
public record FieldPermission(
        /**
         * 是否允许查询，默认true
         */
        boolean show,
        /**
         * 是否脱敏，默认false
         */
        boolean desensitize,
        /**
         * 脱敏策略
         */
        SensitiveStrategy strategy,
        /**
         * 自定义脱敏-不脱敏前缀长度
         */
        Integer prefixNoMaskLen,
        /**
         * 自定义脱敏-不脱敏后缀长度
         */
        Integer suffixNoMaskLen,
        /**
         * 自定义脱敏-脱敏标识符
         */
        String symbol
) {}
```

入库时 `fieldConfig` 序列化为JSON存储，格式示例：

```json
{
  "last_login_ip": {"show": false, "desensitize": false, "strategy": "NONE"},
  "phone": {"show": true, "desensitize": true, "strategy": "PHONE"},
  "email": {"show": true, "desensitize": true, "strategy": "EMAIL"},
  "username": {"show": true, "desensitize": true, "strategy": "CUSTOM", "prefixNoMaskLen": 2, "suffixNoMaskLen": 1, "symbol": "*"}
}
```

注：未在注解中标识的字段不在JSON中出现，默认可查询、不脱敏。

### 3.5 采集处理流程

与现有 `SecurityApiResourceServiceImpl.handlePermission()` 一致：
1. 各服务启动时采集接口+表模型信息，推送到 Redis 队列
2. Security服务监听队列，全量覆盖该模块的 `security_api_table_model` 数据
3. 手动配置的数据源存储在独立的 `security_api_table_model_config` 表中，不受采集覆盖影响

## 四、数据库表设计

### 4.1 `security_api_table_model` — 接口-表模型绑定表（注解采集）

启动时按模块全量覆盖，与 `security_api_resource` 生命周期一致。

```sql
CREATE TABLE security_api_table_model
(
    id            VARCHAR(64) PRIMARY KEY COMMENT '主键ID，MD5(module_prefix + datasource + table_name + api_id)',
    api_id        VARCHAR(64)  NOT NULL COMMENT '接口资源ID，关联security_api_resource.id',
    module_prefix VARCHAR(50)  NOT NULL DEFAULT '' COMMENT '模块前缀（服务标识）',
    datasource    VARCHAR(50)  NOT NULL DEFAULT 'master' COMMENT '数据源名称',
    table_name    VARCHAR(100) NOT NULL COMMENT '表模型名称',
    field_config  LONGTEXT              DEFAULT NULL COMMENT '字段配置JSON，仅记录注解标识的字段配置',
    tenant_id     VARCHAR(50)           DEFAULT NULL COMMENT '租户ID',
    create_op    VARCHAR(50)           DEFAULT NULL COMMENT '创建人',
    create_time  DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modify_op    VARCHAR(50)           DEFAULT NULL COMMENT '修改人',
    modify_time  DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    deleted      SMALLINT     NOT NULL DEFAULT 0 COMMENT '删除标识：0-未删除 1-已删除',
    delete_op    VARCHAR(50)           DEFAULT NULL COMMENT '删除人',
    delete_time  DATETIME              DEFAULT NULL COMMENT '删除时间',
    INDEX idx_api_id (api_id),
    INDEX idx_table_name (table_name),
    INDEX idx_module_prefix (module_prefix),
    INDEX idx_module_datasource_table (module_prefix, datasource, table_name)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT = '接口-表模型绑定表（注解采集，启动时覆盖）';
```

**ID生成规则**：`MD5(module_prefix + ":" + datasource + ":" + table_name + ":" + api_id)`

**表模型唯一标识**：`module_prefix + datasource + table_name`（三元组）。同一张表被两个接口引用时，会有两条不同ID的记录。

### 4.2 `security_api_table_model_config` — 表模型手动配置表（持久化）

此表数据**不会被启动采集覆盖**，用于存储：
1. 数据源覆盖（修改已采集的API-表模型的数据源）
2. 独立表模型（不绑定任何API，供角色管理选择表时搜索）

```sql
CREATE TABLE security_api_table_model_config
(
    id             VARCHAR(24)  PRIMARY KEY COMMENT '主键ID',
    table_model_id VARCHAR(64)  DEFAULT NULL COMMENT '关联security_api_table_model的ID，有值表示关联的表模型，NULL表示独立表模型',
    table_name     VARCHAR(100) NOT NULL COMMENT '表模型名称',
    module_prefix  VARCHAR(50)  NOT NULL DEFAULT '' COMMENT '模块前缀',
    datasource     VARCHAR(50)  NOT NULL COMMENT '数据源名称',
    description    VARCHAR(200) DEFAULT NULL COMMENT '配置说明',
    tenant_id      VARCHAR(50)  DEFAULT NULL COMMENT '租户ID',
    create_op      VARCHAR(50)  DEFAULT NULL COMMENT '创建人',
    create_time    DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modify_op      VARCHAR(50)  DEFAULT NULL COMMENT '修改人',
    modify_time    DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    deleted        SMALLINT     NOT NULL DEFAULT 0 COMMENT '删除标识：0-未删除 1-已删除',
    delete_op      VARCHAR(50)  DEFAULT NULL COMMENT '删除人',
    delete_time    DATETIME              DEFAULT NULL COMMENT '删除时间',
    INDEX idx_table_model_id (table_model_id),
    INDEX idx_table_name (table_name),
    INDEX idx_module_prefix (module_prefix)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT = '表模型手动配置表（持久化，启动不覆盖）';
```

**数据源修改的两种方式**：

1. **表模型级别修改**：在配置界面选择表模型 → 配置数据源 → 不选择指定接口 → 所有绑定该表模型的API一起变更
   - 插入多条 `table_model_id` 有值的记录，每条对应 `security_api_table_model` 中该表模型的一条绑定记录

2. **API级别修改**：在配置界面选择表模型 → 配置数据源 → 选择指定接口 → 只修改这些接口的表模型数据源
   - 只插入选中接口对应的 `table_model_id` 记录

3. **独立表模型**：`table_model_id` 为NULL，`module_prefix` + `datasource` + `table_name` 三元组描述一个不在代码注解中配置的表
   - 主要作用是在角色管理配置时，表模型列表中可以搜索到该表
   - 独立表模型也需要 `module_prefix`，用于区分不同服务下的同名表

**数据源解析优先级**：

```
查询某接口的某表模型的数据源：
  ├─ security_api_table_model_config 中 table_model_id 匹配？
  │   └─ 是 → 使用该 datasource（最高优先级）
  └─ 使用 security_api_table_model 中的 datasource（采集默认值）
```

### 4.3 `security_role_table_model` — 角色表模型权限配置表

存储角色对表模型的自定义权限，**只存储限制性配置**（不可查询的字段、脱敏配置），默认全部可查询。

```sql
CREATE TABLE security_role_table_model
(
    id           VARCHAR(24) PRIMARY KEY COMMENT '主键ID',
    role_id      VARCHAR(24)  NOT NULL COMMENT '角色ID',
    module_prefix VARCHAR(50) NOT NULL DEFAULT '' COMMENT '模块前缀（服务标识）',
    table_name   VARCHAR(100) NOT NULL COMMENT '表模型名称',
    datasource   VARCHAR(50)  NOT NULL DEFAULT 'master' COMMENT '数据源名称',
    field_config LONGTEXT              DEFAULT NULL COMMENT '字段限制配置JSON，仅存储限制性配置',
    tenant_id    VARCHAR(50)           DEFAULT NULL COMMENT '租户ID',
    create_op    VARCHAR(50)           DEFAULT NULL COMMENT '创建人',
    create_time  DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modify_op    VARCHAR(50)           DEFAULT NULL COMMENT '修改人',
    modify_time  DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    deleted      SMALLINT     NOT NULL DEFAULT 0 COMMENT '删除标识：0-未删除 1-已删除',
    delete_op    VARCHAR(50)           DEFAULT NULL COMMENT '删除人',
    delete_time  DATETIME              DEFAULT NULL COMMENT '删除时间',
    UNIQUE INDEX uk_role_table_model (role_id, module_prefix, datasource, table_name),
    INDEX idx_role_id (role_id),
    INDEX idx_table_name (table_name)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT = '角色表模型权限配置表';
```

**唯一标识**：`role_id + module_prefix + datasource + table_name`。一个角色对同一个表模型（三元组确定）只有一条配置记录。

**field_config JSON 示例（只存储限制性配置）**：

```json
{
  "salary": {"show": false, "desensitize": false, "strategy": "NONE"},
  "address": {"show": true, "desensitize": true, "strategy": "ADDRESS"}
}
```

注：
- 不在JSON中的字段默认可查询、不脱敏
- 库表新增字段自动可查询
- `show=false` 的字段记录了不可查询
- `desensitize=true` 的字段记录了需要脱敏

## 五、权限合并逻辑

### 5.1 合并入口

在对应的Service实现类中提供方法加载合并当前用户的表模型权限：

```java
/**
 * 获取当前登录用户的表模型权限
 * @return Map<表模型唯一标识, TableModelPermissionInfo>
 *         key = module_prefix + ":" + datasource + ":" + table_name
 */
Map<String, TableModelPermissionInfo> getCurrentUserTableModelPermission();
```

### 5.2 合并步骤

#### Step 1：获取接口关联的表模型权限

```
用户拥有的接口权限（菜单/API）
  → security_api_resource
  → security_api_table_model
  → 得到：接口级别的表模型列表 + 注解字段配置
```

#### Step 2：获取角色自定义表模型权限

```
用户的所有角色
  → security_role_table_model
  → 得到：角色级别的表模型字段限制配置
```

#### Step 3：合并同一表模型的多角色权限（取最大权限）

**字段查询权限**：取并集（任一角色允许则允许）

| 场景 | 角色A | 角色B | 合并结果 |
|------|-------|-------|----------|
| 字段查询权限 | 甲、乙 | 甲、乙、丙、丁 | 甲、乙、丙、丁 |
| 字段脱敏 | 甲字段脱敏 | 甲字段不脱敏 | 甲字段不脱敏（任一角色不脱敏则最终不脱敏） |

**合并规则**：
- `show`：多角色取**或**（任一角色show=true则show=true）
- `desensitize`：多角色取**与**（所有角色都脱敏才脱敏，任一角色不脱敏则不脱敏）

#### Step 4：注解优先级覆盖

注解采集的配置（`security_api_table_model.field_config`）具有**最高优先级**：

- 注解标注 `show=false` → 无论角色如何配置，该字段AI不可查询
- 注解标注 `desensitize=true` → 无论角色如何配置，该字段必须脱敏
- 角色配置界面中，注解禁止查询的字段**禁用选择**，注解标注脱敏的字段**不可修改**

### 5.3 合并伪代码

**表模型唯一标识**：`module_prefix + ":" + datasource + ":" + table_name`（三元组），作为Map的key。

```java
public Map<String, TableModelPermissionInfo> getCurrentUserTableModelPermission() {
    // 1. 接口权限 → 表模型列表 + 注解字段配置
    //    key = "module_prefix:datasource:table_name"
    Map<String, Map<String, FieldPermission>> annotationConfig = getAnnotationConfigByApis(user.getApiIds());

    // 2. 角色权限 → 字段限制配置（多角色取最大权限）
    //    key = "module_prefix:datasource:table_name"
    Map<String, Map<String, FieldPermission>> roleConfig = getMergedRoleConfig(user.getRoleIds());

    // 3. 收集所有表模型唯一标识（来自注解采集 + 独立表模型）
    Set<String> allTableModelKeys = getAllTableModelKeys(annotationConfig, roleConfig);

    // 4. 合并：以注解为最高优先级
    Map<String, TableModelPermissionInfo> result = new HashMap<>();
    for (String tableModelKey : allTableModelKeys) {
        Map<String, FieldPermission> annotation = annotationConfig.getOrDefault(tableModelKey, Map.of());
        Map<String, FieldPermission> role = roleConfig.getOrDefault(tableModelKey, Map.of());
        result.put(tableModelKey, mergeWithAnnotationPriority(annotation, role));
    }
    return result;
}

private TableModelPermissionInfo mergeWithAnnotationPriority(
        Map<String, FieldPermission> annotation,
        Map<String, FieldPermission> role) {

    // 收集所有字段名
    Set<String> allFields = new HashSet<>();
    allFields.addAll(annotation.keySet());
    allFields.addAll(role.keySet());

    Map<String, FieldPermission> merged = new HashMap<>();
    for (String field : allFields) {
        FieldPermission a = annotation.get(field);
        FieldPermission r = role.get(field);

        if (a != null && !a.show()) {
            // 注解禁止查询 → 最高优先级，直接禁止
            merged.put(field, a);
        } else if (a != null && a.desensitize()) {
            // 注解要求脱敏 → 最高优先级，必须脱敏
            merged.put(field, a);
        } else if (r != null) {
            // 使用角色配置
            merged.put(field, r);
        } else if (a != null) {
            // 仅有注解配置
            merged.put(field, a);
        }
        // 既无注解也无角色配置 → 默认可查询不脱敏，不需要记录
    }
    return new TableModelPermissionInfo(merged);
}
```

### 5.4 数据源解析流程

```
查询某接口的某表模型的数据源：
  ├─ security_api_table_model_config 中 table_model_id 匹配？
  │   └─ 是 → 使用该 datasource（最高优先级）
  └─ 使用 security_api_table_model 中的 datasource（采集默认值）
```

## 六、SQL执行流程

智能助手生成SQL后的处理流程：

1. AI生成SQL → 只允许 SELECT
2. 根据表模型权限过滤SQL：
   - 从合并后的权限中获取 `show=false` 的字段列表
   - 从SQL的SELECT子句中移除这些字段（如果SELECT *，则替换为具体字段列表）
3. 通过MyBatis执行SQL（已有 `DataResourceInterceptor` 追加数据条件过滤）
4. 根据脱敏配置对结果集脱敏处理：
   - 遍历结果集，对 `desensitize=true` 的字段应用对应的脱敏策略
5. 返回给用户

## 七、角色管理界面交互

### 7.1 表模型配置

1. 选择角色 → 回显已配置的表模型名称列表（懒加载，不查字段详情）
2. 点击某个表模型 → 查询该表字段结构（通过datasource动态查询 `INFORMATION_SCHEMA.COLUMNS`）
3. 字段配置界面规则：
   - 注解 `show=false` 的字段：禁用选择（灰色，不可修改）
   - 注解 `desensitize=true` 的字段：脱敏勾选禁用（灰色，不可修改脱敏相关配置）
   - 其他字段：可自由配置是否可查询、是否脱敏、脱敏策略
4. 存储时只保存限制性配置（与默认值不同的部分），确保新增字段默认可查询

### 7.2 API资源管理界面

1. 支持查看每个接口绑定的表模型列表及数据源
2. 支持修改表模型的数据源：
   - 选择表模型 → 配置数据源 → 可选指定接口（不选则该表所有API绑定一起变更）
3. 支持添加独立表模型（不绑定任何API）

## 八、模块归属

| 代码 | 所属模块 |
|------|---------|
| `@TableModelPermission` 注解 | `common/common-security` |
| `@TableModelField` 注解 | `common/common-security` |
| `SensitiveStrategy` 枚举 | `common/common-security` |
| `TableModelInfo` record | `common/common-security` |
| `FieldPermission` record | `common/common-security` |
| `ApiEndpointCollector` 扩展 | `common/common-security` |
| `ApiEndpointInfo` 扩展 | `common/common-security` |
| `SecurityApiTableModel` domain | `business-security-server` |
| `SecurityApiTableModelConfig` domain | `business-security-server` |
| `SecurityRoleTableModel` domain | `business-security-server` |
| 对应的 mapper / service / controller | `business-security-server` |
| DDL | `business-security/sql/mysql/ddl.sql` |

## 九、表关系总览

```
security_api_resource (1) ──→ (N) security_api_table_model
        │                              │
        │                              │ table_model_id (可选关联)
        │                              ↓
        │                    security_api_table_model_config
        │                     (数据源覆盖 / 独立表模型)
        │
security_role (1) ──→ (N) security_role_table_model
                           (角色自定义字段权限)

表模型唯一标识：module_prefix + datasource + table_name（三元组）
  - security_api_table_model: 通过 module_prefix + datasource + table_name 确定唯一表模型
  - security_api_table_model_config: 独立表模型通过 module_prefix + datasource + table_name 标识
  - security_role_table_model: 唯一索引 (role_id, module_prefix, datasource, table_name)

权限合并时（key = "module_prefix:datasource:table_name"）：
  接口权限 → security_api_table_model (注解配置，最高优先级)
  角色权限 → security_role_table_model (角色配置，在注解约束范围内)
  数据源   → security_api_table_model_config (手动覆盖) > security_api_table_model (采集默认)
```
