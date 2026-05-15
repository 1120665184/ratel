# 表模型权限控制系统实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现智能助手AI操作表模型时的权限控制系统，包括注解采集、数据库存储、权限合并、角色配置。

**Architecture:** 基于 common-security 模块的注解和采集机制扩展，在 ApiEndpointCollector 中增加 `@TableModelPermission` 注解采集，通过 Redis 队列推送到 security 服务持久化，权限合并时注解优先级最高，角色配置在注解约束范围内生效。

**Tech Stack:** Spring Boot 4.0.3 / MyBatis Plus 3.5.16 / Redis / Jackson

---

## 文件变更清单

### common/common-security 模块（新增）
- `annotation/TableModelPermission.java` — 表模型权限注解
- `annotation/TableModelField.java` — 字段配置注解
- `annotation/SensitiveStrategy.java` — 脱敏策略枚举
- `domain/TableModelInfo.java` — 表模型信息 record
- `domain/FieldPermission.java` — 字段权限 record

### common/common-security 模块（修改）
- `domain/ApiEndpointInfo.java` — 增加 tableModels 字段
- `collector/ApiEndpointCollector.java` — 增加 @TableModelPermission 采集逻辑
- `config/SecurityRuntimeHintsRegistrar.java` — 注册反射提示

### business/business-security/business-security-api 模块（新增）
- `tablemodel/vo/TableModelVO.java` — 表模型 VO
- `tablemodel/vo/TableModelFieldVO.java` — 表模型字段 VO
- `tablemodel/vo/TableModelConfigVO.java` — 表模型配置 VO
- `tablemodel/vo/RoleTableModelVO.java` — 角色表模型权限 VO
- `tablemodel/dto/TableModelQueryDTO.java` — 表模型查询 DTO
- `tablemodel/dto/TableModelConfigSaveDTO.java` — 表模型配置保存 DTO
- `tablemodel/dto/RoleTableModelSaveDTO.java` — 角色表模型权限保存 DTO

### business/business-security/business-security-server 模块（新增）
- `tablemodel/domain/SecurityApiTableModel.java` — 接口-表模型绑定 domain
- `tablemodel/domain/SecurityApiTableModelConfig.java` — 表模型配置 domain
- `tablemodel/domain/SecurityRoleTableModel.java` — 角色表模型权限 domain
- `tablemodel/mapper/SecurityApiTableModelMapper.java` — Mapper
- `tablemodel/mapper/SecurityApiTableModelConfigMapper.java` — Mapper
- `tablemodel/mapper/SecurityRoleTableModelMapper.java` — Mapper
- `tablemodel/service/ISecurityApiTableModelService.java` — Service 接口
- `tablemodel/service/ISecurityApiTableModelConfigService.java` — Service 接口
- `tablemodel/service/ISecurityRoleTableModelService.java` — Service 接口
- `tablemodel/service/impl/SecurityApiTableModelServiceImpl.java` — Service 实现
- `tablemodel/service/impl/SecurityApiTableModelConfigServiceImpl.java` — Service 实现
- `tablemodel/service/impl/SecurityRoleTableModelServiceImpl.java` — Service 实现
- `tablemodel/controller/SecurityApiTableModelController.java` — Controller
- `tablemodel/controller/SecurityApiTableModelConfigController.java` — Controller
- `tablemodel/controller/SecurityRoleTableModelController.java` — Controller

### business/business-security/business-security-server 模块（修改）
- `apiresource/service/impl/SecurityApiResourceServiceImpl.java` — 增加表模型数据处理
- `errcode/SecurityErrorCode.java` — 增加表模型相关错误码

### SQL 脚本（新增/修改）
- `business/business-security/sql/mysql/ddl.sql` — 追加3张表的DDL

---

## Task 1: common-security 注解与枚举

**Files:**
- Create: `common/common-security/src/main/java/org/quyq/gwsu/common/security/annotation/SensitiveStrategy.java`
- Create: `common/common-security/src/main/java/org/quyq/gwsu/common/security/annotation/TableModelField.java`
- Create: `common/common-security/src/main/java/org/quyq/gwsu/common/security/annotation/TableModelPermission.java`

- [ ] **Step 1: 创建 SensitiveStrategy 枚举**

```java
package org.quyq.gwsu.common.security.annotation;

/**
 * 脱敏策略枚举
 */
public enum SensitiveStrategy {
    /** 无脱敏 */
    NONE,
    /** 用户名：张** */
    USERNAME,
    /** 身份证：3301**********1234 */
    ID_CARD,
    /** 手机号：138****1234 */
    PHONE,
    /** 邮箱：a****b@example.com */
    EMAIL,
    /** 地址：浙江省****杭州市**** */
    ADDRESS,
    /** 自定义脱敏 */
    CUSTOM
}
```

- [ ] **Step 2: 创建 @TableModelField 注解**

```java
package org.quyq.gwsu.common.security.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 表模型字段配置注解，标注在 Domain 类字段上
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface TableModelField {
    /** 是否允许AI查询该字段，默认true */
    boolean show() default true;
    /** 返回给用户时是否脱敏，默认false */
    boolean desensitize() default false;
    /** 脱敏策略，默认NONE */
    SensitiveStrategy strategy() default SensitiveStrategy.NONE;
    /** 自定义脱敏-不脱敏前缀长度，仅 strategy=CUSTOM 时生效 */
    int prefixNoMaskLen() default 1;
    /** 自定义脱敏-不脱敏后缀长度，仅 strategy=CUSTOM 时生效 */
    int suffixNoMaskLen() default 1;
    /** 自定义脱敏-脱敏标识符，仅 strategy=CUSTOM 时生效 */
    String symbol() default "*";
}
```

- [ ] **Step 3: 创建 @TableModelPermission 注解**

```java
package org.quyq.gwsu.common.security.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 表模型权限注解，标注在 Controller 类或方法上
 * <p>
 * 生效规则：
 * - 无注解：无表模型权限
 * - 仅类上：继承类上的表模型权限
 * - 仅方法上：使用方法上的
 * - 类+方法（方法有配置）：方法覆盖类
 * - 类+方法（空注解）：该接口不继承类上的表模型权限
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface TableModelPermission {
    /** 表模型对应的Domain类列表（类需有@TableName注解） */
    Class<?>[] value() default {};
    /** 表名列表（直接指定表名，与value互为补充） */
    String[] tables() default {};
}
```

- [ ] **Step 4: 验证编译**

Run: `mvn compile -pl common/common-security -am -q`
Expected: BUILD SUCCESS

- [ ] **Step 5: 提交**

```bash
git add common/common-security/src/main/java/org/quyq/gwsu/common/security/annotation/SensitiveStrategy.java common/common-security/src/main/java/org/quyq/gwsu/common/security/annotation/TableModelField.java common/common-security/src/main/java/org/quyq/gwsu/common/security/annotation/TableModelPermission.java
git commit -m "feat(common-security): 新增表模型权限注解 @TableModelPermission、@TableModelField 和脱敏策略枚举 SensitiveStrategy"
```

---

## Task 2: common-security 领域对象

**Files:**
- Create: `common/common-security/src/main/java/org/quyq/gwsu/common/security/domain/FieldPermission.java`
- Create: `common/common-security/src/main/java/org/quyq/gwsu/common/security/domain/TableModelInfo.java`

- [ ] **Step 1: 创建 FieldPermission record**

```java
package org.quyq.gwsu.common.security.domain;

import org.quyq.gwsu.common.security.annotation.SensitiveStrategy;

/**
 * 字段权限配置
 *
 * @param show            是否允许查询
 * @param desensitize     是否脱敏
 * @param strategy        脱敏策略
 * @param prefixNoMaskLen 自定义脱敏-不脱敏前缀长度
 * @param suffixNoMaskLen 自定义脱敏-不脱敏后缀长度
 * @param symbol          自定义脱敏-脱敏标识符
 */
public record FieldPermission(
        boolean show,
        boolean desensitize,
        SensitiveStrategy strategy,
        Integer prefixNoMaskLen,
        Integer suffixNoMaskLen,
        String symbol
) {
    /**
     * 默认权限：可查询、不脱敏
     */
    public static FieldPermission defaultPermission() {
        return new FieldPermission(true, false, SensitiveStrategy.NONE, null, null, null);
    }
}
```

- [ ] **Step 2: 创建 TableModelInfo record**

```java
package org.quyq.gwsu.common.security.domain;

import java.util.Map;

/**
 * 表模型信息
 *
 * @param modulePrefix 模块前缀
 * @param tableName    表名
 * @param datasource   数据源名称
 * @param fieldConfig  字段配置，key为字段名（下划线格式）
 */
public record TableModelInfo(
        String modulePrefix,
        String tableName,
        String datasource,
        Map<String, FieldPermission> fieldConfig
) {
    /**
     * 表模型唯一标识：module_prefix:datasource:table_name
     */
    public String uniqueKey() {
        return modulePrefix + ":" + datasource + ":" + tableName;
    }
}
```

- [ ] **Step 3: 验证编译**

Run: `mvn compile -pl common/common-security -am -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: 提交**

```bash
git add common/common-security/src/main/java/org/quyq/gwsu/common/security/domain/FieldPermission.java common/common-security/src/main/java/org/quyq/gwsu/common/security/domain/TableModelInfo.java
git commit -m "feat(common-security): 新增 TableModelInfo 和 FieldPermission 领域对象"
```

---

## Task 3: 扩展 ApiEndpointInfo 和 ApiEndpointCollector

**Files:**
- Modify: `common/common-security/src/main/java/org/quyq/gwsu/common/security/domain/ApiEndpointInfo.java`
- Modify: `common/common-security/src/main/java/org/quyq/gwsu/common/security/collector/ApiEndpointCollector.java`

- [ ] **Step 1: 扩展 ApiEndpointInfo，增加 tableModels 字段**

在 ApiEndpointInfo record 中新增 `List<TableModelInfo> tableModels` 字段：

```java
package org.quyq.gwsu.common.security.domain;

import java.util.List;

/**
 * HTTP 接口信息
 */
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
        List<TableModelInfo> tableModels
) {
}
```

- [ ] **Step 2: 修改 ApiEndpointCollector.buildEndpointInfo，增加表模型采集逻辑**

在 `buildEndpointInfo` 方法中，`loginAllowAccess` 判断之后、构建 `ApiEndpointInfo` 之前，插入表模型权限采集逻辑：

```java
// === 表模型权限采集 ===
List<TableModelInfo> tableModels = collectTableModelPermission(beanType, method, modulePrefix);
```

新增私有方法：

```java
/**
 * 采集表模型权限配置
 *
 * @param beanType     Controller类
 * @param method       接口方法
 * @param modulePrefix 模块前缀
 * @return 表模型信息列表
 */
private List<TableModelInfo> collectTableModelPermission(Class<?> beanType, Method method, String modulePrefix) {
    // 获取类上的注解
    TableModelPermission classAnnotation = AnnotatedElementUtils.findMergedAnnotation(beanType, TableModelPermission.class);
    // 获取方法上的注解
    TableModelPermission methodAnnotation = AnnotatedElementUtils.findMergedAnnotation(method, TableModelPermission.class);

    // 确定最终使用的注解
    TableModelPermission effectiveAnnotation;
    if (methodAnnotation != null) {
        // 方法上有注解
        if (methodAnnotation.value().length == 0 && methodAnnotation.tables().length == 0) {
            // 空注解：显式排除，不继承类上的
            return List.of();
        }
        // 方法上的覆盖类上的
        effectiveAnnotation = methodAnnotation;
    } else if (classAnnotation != null) {
        // 仅类上有注解
        effectiveAnnotation = classAnnotation;
    } else {
        // 都没有
        return List.of();
    }

    // 获取数据源：Controller类上有 @DS 则取其值，否则默认 master
    String datasource = "master";
    // 注意：当前项目未使用 @DS 注解，预留位置
    // DS dsAnnotation = AnnotatedElementUtils.findMergedAnnotation(beanType, DS.class);
    // if (dsAnnotation != null) { datasource = dsAnnotation.value(); }

    // 构建 TableModelInfo 列表
    List<TableModelInfo> result = new ArrayList<>();

    // 处理 value() 中的 Domain 类
    for (Class<?> domainClass : effectiveAnnotation.value()) {
        TableName tableNameAnnotation = domainClass.getAnnotation(TableName.class);
        if (tableNameAnnotation == null) {
            log.warn("@TableModelPermission 引用的类 {} 缺少 @TableName 注解，跳过", domainClass.getName());
            continue;
        }
        String tableName = tableNameAnnotation.value();
        Map<String, FieldPermission> fieldConfig = buildFieldConfig(domainClass);
        result.add(new TableModelInfo(modulePrefix, tableName, datasource, fieldConfig));
    }

    // 处理 tables() 中的直接表名
    for (String tableName : effectiveAnnotation.tables()) {
        result.add(new TableModelInfo(modulePrefix, tableName, datasource, Map.of()));
    }

    return result;
}

/**
 * 构建 Domain 类的字段配置
 * 只记录有 @TableModelField 注解的字段，未标注的字段默认可查询不脱敏
 */
private Map<String, FieldPermission> buildFieldConfig(Class<?> domainClass) {
    Map<String, FieldPermission> fieldConfig = new HashMap<>();
    for (java.lang.reflect.Field field : domainClass.getDeclaredFields()) {
        TableModelField fieldAnnotation = field.getAnnotation(TableModelField.class);
        if (fieldAnnotation == null) {
            continue;
        }
        // 将字段名转为下划线格式
        String columnName = camelToUnderline(field.getName());
        fieldConfig.put(columnName, new FieldPermission(
                fieldAnnotation.show(),
                fieldAnnotation.desensitize(),
                fieldAnnotation.strategy(),
                fieldAnnotation.strategy() == SensitiveStrategy.CUSTOM ? fieldAnnotation.prefixNoMaskLen() : null,
                fieldAnnotation.strategy() == SensitiveStrategy.CUSTOM ? fieldAnnotation.suffixNoMaskLen() : null,
                fieldAnnotation.strategy() == SensitiveStrategy.CUSTOM ? fieldAnnotation.symbol() : null
        ));
    }
    return fieldConfig;
}

/**
 * 驼峰转下划线
 */
private String camelToUnderline(String camelCase) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < camelCase.length(); i++) {
        char c = camelCase.charAt(i);
        if (Character.isUpperCase(c)) {
            if (i > 0) {
                sb.append('_');
            }
            sb.append(Character.toLowerCase(c));
        } else {
            sb.append(c);
        }
    }
    return sb.toString();
}
```

同时需要在 ApiEndpointCollector 顶部添加 import：

```java
import org.quyq.gwsu.common.security.annotation.TableModelPermission;
import org.quyq.gwsu.common.security.annotation.TableModelField;
import org.quyq.gwsu.common.security.annotation.SensitiveStrategy;
import org.quyq.gwsu.common.security.domain.TableModelInfo;
import org.quyq.gwsu.common.security.domain.FieldPermission;
import com.baomidou.mybatisplus.annotation.TableName;
```

修改 `buildEndpointInfo` 方法的返回语句，新增 `tableModels` 参数：

```java
return new ApiEndpointInfo(
        genId(modulePrefix, httpMethod, path),
        modulePrefix,
        tagName,
        path,
        httpMethod,
        summary,
        requestType,
        responseType,
        beanType.getName(),
        method.getName(),
        loginAllowAccess,
        tableModels   // 新增
);
```

- [ ] **Step 3: 更新 SecurityRuntimeHintsRegistrar，注册反射提示**

在 `registerHints` 方法中添加对 `TableModelPermission` 和 `TableModelField` 注解的反射提示：

```java
// 注册表模型权限注解相关类的反射提示
hints.reflection().registerType(TableModelPermission.class, MemberCategory.ACCESS_DECLARED_FIELDS);
hints.reflection().registerType(TableModelField.class, MemberCategory.ACCESS_DECLARED_FIELDS);
```

添加 import：

```java
import org.quyq.gwsu.common.security.annotation.TableModelPermission;
import org.quyq.gwsu.common.security.annotation.TableModelField;
```

- [ ] **Step 4: 验证编译**

Run: `mvn compile -pl common/common-security -am -q`
Expected: BUILD SUCCESS

- [ ] **Step 5: 提交**

```bash
git add common/common-security/
git commit -m "feat(common-security): 扩展 ApiEndpointInfo 和 ApiEndpointCollector，支持 @TableModelPermission 注解采集"
```

---

## Task 4: 数据库 DDL 脚本

**Files:**
- Modify: `business/business-security/sql/mysql/ddl.sql`

- [ ] **Step 1: 在 ddl.sql 末尾追加3张表的DDL**

```sql
-- =============================================
-- 表名：security_api_table_model
-- 说明：接口-表模型绑定表（注解采集，启动时覆盖）
-- =============================================
CREATE TABLE security_api_table_model
(
    id            VARCHAR(64) PRIMARY KEY COMMENT '主键ID，MD5(module_prefix + datasource + table_name + api_id)',
    api_id        VARCHAR(64)  NOT NULL COMMENT '接口资源ID，关联security_api_resource.id',
    module_prefix VARCHAR(50)  NOT NULL DEFAULT '' COMMENT '模块前缀（服务标识）',
    datasource    VARCHAR(50)  NOT NULL DEFAULT 'master' COMMENT '数据源名称',
    table_name    VARCHAR(100) NOT NULL COMMENT '表模型名称',
    field_config  LONGTEXT              DEFAULT NULL COMMENT '字段配置JSON，仅记录注解标识的字段配置',
    tenant_id     VARCHAR(50)           DEFAULT NULL COMMENT '租户ID',
    create_op     VARCHAR(50)           DEFAULT NULL COMMENT '创建人',
    create_time   DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modify_op     VARCHAR(50)           DEFAULT NULL COMMENT '修改人',
    modify_time   DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    deleted       SMALLINT     NOT NULL DEFAULT 0 COMMENT '删除标识：0-未删除 1-已删除',
    delete_op     VARCHAR(50)           DEFAULT NULL COMMENT '删除人',
    delete_time   DATETIME              DEFAULT NULL COMMENT '删除时间',
    INDEX idx_api_id (api_id),
    INDEX idx_table_name (table_name),
    INDEX idx_module_prefix (module_prefix),
    INDEX idx_module_datasource_table (module_prefix, datasource, table_name)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT = '接口-表模型绑定表（注解采集，启动时覆盖）';

-- =============================================
-- 表名：security_api_table_model_config
-- 说明：表模型手动配置表（持久化，启动不覆盖）
-- =============================================
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

-- =============================================
-- 表名：security_role_table_model
-- 说明：角色表模型权限配置表
-- =============================================
CREATE TABLE security_role_table_model
(
    id            VARCHAR(24) PRIMARY KEY COMMENT '主键ID',
    role_id       VARCHAR(24)  NOT NULL COMMENT '角色ID',
    module_prefix VARCHAR(50)  NOT NULL DEFAULT '' COMMENT '模块前缀（服务标识）',
    table_name    VARCHAR(100) NOT NULL COMMENT '表模型名称',
    datasource    VARCHAR(50)  NOT NULL DEFAULT 'master' COMMENT '数据源名称',
    field_config  LONGTEXT              DEFAULT NULL COMMENT '字段限制配置JSON，仅存储限制性配置',
    tenant_id     VARCHAR(50)           DEFAULT NULL COMMENT '租户ID',
    create_op     VARCHAR(50)           DEFAULT NULL COMMENT '创建人',
    create_time   DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modify_op     VARCHAR(50)           DEFAULT NULL COMMENT '修改人',
    modify_time   DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    deleted       SMALLINT     NOT NULL DEFAULT 0 COMMENT '删除标识：0-未删除 1-已删除',
    delete_op     VARCHAR(50)           DEFAULT NULL COMMENT '删除人',
    delete_time   DATETIME              DEFAULT NULL COMMENT '删除时间',
    UNIQUE INDEX uk_role_table_model (role_id, module_prefix, datasource, table_name),
    INDEX idx_role_id (role_id),
    INDEX idx_table_name (table_name)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT = '角色表模型权限配置表';
```

- [ ] **Step 2: 提交**

```bash
git add business/business-security/sql/mysql/ddl.sql
git commit -m "feat(security): 新增表模型权限相关DDL - security_api_table_model、security_api_table_model_config、security_role_table_model"
```

---

## Task 5: business-security-api VO/DTO 定义

**Files:**
- Create: `business/business-security/business-security-api/src/main/java/org/quyq/gwsu/security/api/tablemodel/vo/TableModelVO.java`
- Create: `business/business-security/business-security-api/src/main/java/org/quyq/gwsu/security/api/tablemodel/vo/TableModelFieldVO.java`
- Create: `business/business-security/business-security-api/src/main/java/org/quyq/gwsu/security/api/tablemodel/vo/TableModelConfigVO.java`
- Create: `business/business-security/business-security-api/src/main/java/org/quyq/gwsu/security/api/tablemodel/vo/RoleTableModelVO.java`
- Create: `business/business-security/business-security-api/src/main/java/org/quyq/gwsu/security/api/tablemodel/dto/TableModelQueryDTO.java`
- Create: `business/business-security/business-security-api/src/main/java/org/quyq/gwsu/security/api/tablemodel/dto/TableModelConfigSaveDTO.java`
- Create: `business/business-security/business-security-api/src/main/java/org/quyq/gwsu/security/api/tablemodel/dto/RoleTableModelSaveDTO.java`

- [ ] **Step 1: 创建 TableModelVO**

```java
package org.quyq.gwsu.security.api.tablemodel.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.quyq.gwsu.common.core.domain.BaseVO;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "表模型信息")
public class TableModelVO extends BaseVO {

    @Schema(description = "主键ID")
    private String id;

    @Schema(description = "接口资源ID")
    private String apiId;

    @Schema(description = "模块前缀")
    private String modulePrefix;

    @Schema(description = "数据源名称")
    private String datasource;

    @Schema(description = "表名")
    private String tableName;

    @Schema(description = "字段配置列表")
    private List<TableModelFieldVO> fields;

    @Schema(description = "接口地址")
    private String reqPath;

    @Schema(description = "请求方式")
    private String reqMethod;

    @Schema(description = "接口摘要")
    private String summary;
}
```

- [ ] **Step 2: 创建 TableModelFieldVO**

```java
package org.quyq.gwsu.security.api.tablemodel.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "表模型字段信息")
public class TableModelFieldVO {

    @Schema(description = "字段名")
    private String fieldName;

    @Schema(description = "是否允许查询")
    private Boolean show;

    @Schema(description = "是否脱敏")
    private Boolean desensitize;

    @Schema(description = "脱敏策略")
    private String strategy;

    @Schema(description = "自定义脱敏-不脱敏前缀长度")
    private Integer prefixNoMaskLen;

    @Schema(description = "自定义脱敏-不脱敏后缀长度")
    private Integer suffixNoMaskLen;

    @Schema(description = "自定义脱敏-脱敏标识符")
    private String symbol;

    @Schema(description = "是否为注解硬约束（不可修改）")
    private Boolean annotationLocked;
}
```

- [ ] **Step 3: 创建 TableModelConfigVO**

```java
package org.quyq.gwsu.security.api.tablemodel.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.quyq.gwsu.common.core.domain.BaseVO;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "表模型配置信息")
public class TableModelConfigVO extends BaseVO {

    @Schema(description = "主键ID")
    private String id;

    @Schema(description = "关联的接口-表模型绑定ID")
    private String tableModelId;

    @Schema(description = "表名")
    private String tableName;

    @Schema(description = "模块前缀")
    private String modulePrefix;

    @Schema(description = "数据源名称")
    private String datasource;

    @Schema(description = "配置说明")
    private String description;
}
```

- [ ] **Step 4: 创建 RoleTableModelVO**

```java
package org.quyq.gwsu.security.api.tablemodel.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.quyq.gwsu.common.core.domain.BaseVO;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "角色表模型权限信息")
public class RoleTableModelVO extends BaseVO {

    @Schema(description = "主键ID")
    private String id;

    @Schema(description = "角色ID")
    private String roleId;

    @Schema(description = "模块前缀")
    private String modulePrefix;

    @Schema(description = "表名")
    private String tableName;

    @Schema(description = "数据源名称")
    private String datasource;

    @Schema(description = "字段配置列表")
    private List<TableModelFieldVO> fields;
}
```

- [ ] **Step 5: 创建 TableModelQueryDTO**

```java
package org.quyq.gwsu.security.api.tablemodel.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.quyq.gwsu.common.core.domain.BaseDTO;

@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "表模型查询条件")
public class TableModelQueryDTO extends BaseDTO {

    @Schema(description = "模块前缀")
    private String modulePrefix;

    @Schema(description = "表名（模糊查询）")
    private String tableName;

    @Schema(description = "接口资源ID")
    private String apiId;

    @Schema(description = "角色ID（查询角色可配置的表模型）")
    private String roleId;
}
```

- [ ] **Step 6: 创建 TableModelConfigSaveDTO**

```java
package org.quyq.gwsu.security.api.tablemodel.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "表模型配置保存")
public class TableModelConfigSaveDTO {

    @Schema(description = "关联的接口-表模型绑定ID列表，为空表示独立表模型")
    private List<String> tableModelIds;

    @Schema(description = "表名")
    private String tableName;

    @Schema(description = "模块前缀")
    private String modulePrefix;

    @Schema(description = "数据源名称")
    private String datasource;

    @Schema(description = "配置说明")
    private String description;
}
```

- [ ] **Step 7: 创建 RoleTableModelSaveDTO**

```java
package org.quyq.gwsu.security.api.tablemodel.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "角色表模型权限保存")
public class RoleTableModelSaveDTO {

    @Schema(description = "主键ID（更新时传入）")
    private String id;

    @Schema(description = "角色ID")
    private String roleId;

    @Schema(description = "模块前缀")
    private String modulePrefix;

    @Schema(description = "表名")
    private String tableName;

    @Schema(description = "数据源名称")
    private String datasource;

    @Schema(description = "字段限制配置列表（只存储与默认值不同的限制性配置）")
    private List<FieldConfigItem> fields;

    @Data
    @Schema(description = "字段配置项")
    public static class FieldConfigItem {
        @Schema(description = "字段名")
        private String fieldName;

        @Schema(description = "是否允许查询")
        private Boolean show;

        @Schema(description = "是否脱敏")
        private Boolean desensitize;

        @Schema(description = "脱敏策略")
        private String strategy;

        @Schema(description = "自定义脱敏-不脱敏前缀长度")
        private Integer prefixNoMaskLen;

        @Schema(description = "自定义脱敏-不脱敏后缀长度")
        private Integer suffixNoMaskLen;

        @Schema(description = "自定义脱敏-脱敏标识符")
        private String symbol;
    }
}
```

- [ ] **Step 8: 验证编译**

Run: `mvn compile -pl business/business-security/business-security-api -am -q`
Expected: BUILD SUCCESS

- [ ] **Step 9: 提交**

```bash
git add business/business-security/business-security-api/src/main/java/org/quyq/gwsu/security/api/tablemodel/
git commit -m "feat(security-api): 新增表模型权限相关 VO/DTO 定义"
```

---

## Task 6: business-security-server Domain 实体类

**Files:**
- Create: `business/business-security/business-security-server/src/main/java/org/quyq/gwsu/security/tablemodel/domain/SecurityApiTableModel.java`
- Create: `business/business-security/business-security-server/src/main/java/org/quyq/gwsu/security/tablemodel/domain/SecurityApiTableModelConfig.java`
- Create: `business/business-security/business-security-server/src/main/java/org/quyq/gwsu/security/tablemodel/domain/SecurityRoleTableModel.java`

- [ ] **Step 1: 创建 SecurityApiTableModel domain**

```java
package org.quyq.gwsu.security.tablemodel.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.quyq.gwsu.common.core.domain.BaseDO;
import org.quyq.gwsu.common.security.domain.FieldPermission;
import org.quyq.gwsu.security.api.tablemodel.vo.TableModelVO;

import java.util.Map;

/**
 * 接口-表模型绑定表（注解采集，启动时覆盖）
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
@TableName(value = "security_api_table_model", autoResultMap = true)
@Schema(description = "接口-表模型绑定表")
public class SecurityApiTableModel extends BaseDO {

    @TableId(type = IdType.INPUT)
    @Schema(description = "主键ID")
    private String id;

    @Schema(description = "接口资源ID")
    private String apiId;

    @Schema(description = "模块前缀")
    private String modulePrefix;

    @Schema(description = "数据源名称")
    private String datasource;

    @Schema(description = "表名")
    private String tableName;

    @TableField(typeHandler = JacksonTypeHandler.class)
    @Schema(description = "字段配置，key为字段名（下划线格式）")
    private Map<String, FieldPermission> fieldConfig;

    public TableModelVO toVo() {
        TableModelVO vo = new TableModelVO();
        vo.setId(this.id);
        vo.setApiId(this.apiId);
        vo.setModulePrefix(this.modulePrefix);
        vo.setDatasource(this.datasource);
        vo.setTableName(this.tableName);
        vo.copyBaseProperties(this);
        return vo;
    }

    public static SecurityApiTableModel toDo(TableModelVO vo) {
        SecurityApiTableModel entity = new SecurityApiTableModel();
        entity.setId(vo.getId());
        entity.setApiId(vo.getApiId());
        entity.setModulePrefix(vo.getModulePrefix());
        entity.setDatasource(vo.getDatasource());
        entity.setTableName(vo.getTableName());
        return entity;
    }
}
```

- [ ] **Step 2: 创建 SecurityApiTableModelConfig domain**

```java
package org.quyq.gwsu.security.tablemodel.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.quyq.gwsu.common.core.domain.BaseDO;
import org.quyq.gwsu.security.api.tablemodel.vo.TableModelConfigVO;

/**
 * 表模型手动配置表（持久化，启动不覆盖）
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
@TableName(value = "security_api_table_model_config", autoResultMap = true)
@Schema(description = "表模型手动配置表")
public class SecurityApiTableModelConfig extends BaseDO {

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private String id;

    @Schema(description = "关联的接口-表模型绑定ID")
    private String tableModelId;

    @Schema(description = "表名")
    private String tableName;

    @Schema(description = "模块前缀")
    private String modulePrefix;

    @Schema(description = "数据源名称")
    private String datasource;

    @Schema(description = "配置说明")
    private String description;

    public TableModelConfigVO toVo() {
        TableModelConfigVO vo = new TableModelConfigVO();
        vo.setId(this.id);
        vo.setTableModelId(this.tableModelId);
        vo.setTableName(this.tableName);
        vo.setModulePrefix(this.modulePrefix);
        vo.setDatasource(this.datasource);
        vo.setDescription(this.description);
        vo.copyBaseProperties(this);
        return vo;
    }
}
```

- [ ] **Step 3: 创建 SecurityRoleTableModel domain**

```java
package org.quyq.gwsu.security.tablemodel.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.quyq.gwsu.common.core.domain.BaseDO;
import org.quyq.gwsu.common.security.domain.FieldPermission;
import org.quyq.gwsu.security.api.tablemodel.vo.RoleTableModelVO;

import java.util.Map;

/**
 * 角色表模型权限配置表
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
@TableName(value = "security_role_table_model", autoResultMap = true)
@Schema(description = "角色表模型权限配置表")
public class SecurityRoleTableModel extends BaseDO {

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private String id;

    @Schema(description = "角色ID")
    private String roleId;

    @Schema(description = "模块前缀")
    private String modulePrefix;

    @Schema(description = "表名")
    private String tableName;

    @Schema(description = "数据源名称")
    private String datasource;

    @TableField(typeHandler = JacksonTypeHandler.class)
    @Schema(description = "字段限制配置，key为字段名（下划线格式）")
    private Map<String, FieldPermission> fieldConfig;

    public RoleTableModelVO toVo() {
        RoleTableModelVO vo = new RoleTableModelVO();
        vo.setId(this.id);
        vo.setRoleId(this.roleId);
        vo.setModulePrefix(this.modulePrefix);
        vo.setTableName(this.tableName);
        vo.setDatasource(this.datasource);
        vo.copyBaseProperties(this);
        return vo;
    }

    public static SecurityRoleTableModel toDo(RoleTableModelVO vo) {
        SecurityRoleTableModel entity = new SecurityRoleTableModel();
        entity.setId(vo.getId());
        entity.setRoleId(vo.getRoleId());
        entity.setModulePrefix(vo.getModulePrefix());
        entity.setTableName(vo.getTableName());
        entity.setDatasource(vo.getDatasource());
        return entity;
    }
}
```

- [ ] **Step 4: 验证编译**

Run: `mvn compile -pl business/business-security/business-security-server -am -q`
Expected: BUILD SUCCESS

- [ ] **Step 5: 提交**

```bash
git add business/business-security/business-security-server/src/main/java/org/quyq/gwsu/security/tablemodel/domain/
git commit -m "feat(security-server): 新增 SecurityApiTableModel、SecurityApiTableModelConfig、SecurityRoleTableModel 实体类"
```

---

## Task 7: Mapper 层

**Files:**
- Create: `business/business-security/business-security-server/src/main/java/org/quyq/gwsu/security/tablemodel/mapper/SecurityApiTableModelMapper.java`
- Create: `business/business-security/business-security-server/src/main/java/org/quyq/gwsu/security/tablemodel/mapper/SecurityApiTableModelConfigMapper.java`
- Create: `business/business-security/business-security-server/src/main/java/org/quyq/gwsu/security/tablemodel/mapper/SecurityRoleTableModelMapper.java`

- [ ] **Step 1: 创建 SecurityApiTableModelMapper**

```java
package org.quyq.gwsu.security.tablemodel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.quyq.gwsu.security.tablemodel.domain.SecurityApiTableModel;

/**
 * 接口-表模型绑定 Mapper
 */
public interface SecurityApiTableModelMapper extends BaseMapper<SecurityApiTableModel> {
}
```

- [ ] **Step 2: 创建 SecurityApiTableModelConfigMapper**

```java
package org.quyq.gwsu.security.tablemodel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.quyq.gwsu.security.tablemodel.domain.SecurityApiTableModelConfig;

/**
 * 表模型手动配置 Mapper
 */
public interface SecurityApiTableModelConfigMapper extends BaseMapper<SecurityApiTableModelConfig> {
}
```

- [ ] **Step 3: 创建 SecurityRoleTableModelMapper**

```java
package org.quyq.gwsu.security.tablemodel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.quyq.gwsu.security.tablemodel.domain.SecurityRoleTableModel;

/**
 * 角色表模型权限 Mapper
 */
public interface SecurityRoleTableModelMapper extends BaseMapper<SecurityRoleTableModel> {
}
```

- [ ] **Step 4: 验证编译**

Run: `mvn compile -pl business/business-security/business-security-server -am -q`
Expected: BUILD SUCCESS

- [ ] **Step 5: 提交**

```bash
git add business/business-security/business-security-server/src/main/java/org/quyq/gwsu/security/tablemodel/mapper/
git commit -m "feat(security-server): 新增表模型权限相关 Mapper 接口"
```

---

## Task 8: Service 层

**Files:**
- Create: `business/business-security/business-security-server/src/main/java/org/quyq/gwsu/security/tablemodel/service/ISecurityApiTableModelService.java`
- Create: `business/business-security/business-security-server/src/main/java/org/quyq/gwsu/security/tablemodel/service/ISecurityApiTableModelConfigService.java`
- Create: `business/business-security/business-security-server/src/main/java/org/quyq/gwsu/security/tablemodel/service/ISecurityRoleTableModelService.java`
- Create: `business/business-security/business-security-server/src/main/java/org/quyq/gwsu/security/tablemodel/service/impl/SecurityApiTableModelServiceImpl.java`
- Create: `business/business-security/business-security-server/src/main/java/org/quyq/gwsu/security/tablemodel/service/impl/SecurityApiTableModelConfigServiceImpl.java`
- Create: `business/business-security/business-security-server/src/main/java/org/quyq/gwsu/security/tablemodel/service/impl/SecurityRoleTableModelServiceImpl.java`

- [ ] **Step 1: 创建 ISecurityApiTableModelService**

```java
package org.quyq.gwsu.security.tablemodel.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.quyq.gwsu.common.security.collector.ApiEndpointCollector;
import org.quyq.gwsu.common.security.domain.ApiEndpointInfo;
import org.quyq.gwsu.security.api.tablemodel.dto.TableModelQueryDTO;
import org.quyq.gwsu.security.api.tablemodel.vo.TableModelVO;
import org.quyq.gwsu.security.tablemodel.domain.SecurityApiTableModel;

import java.util.List;

/**
 * 接口-表模型绑定服务接口
 */
public interface ISecurityApiTableModelService extends IService<SecurityApiTableModel> {

    /**
     * 分页查询
     */
    com.baomidou.mybatisplus.core.metadata.IPage<TableModelVO> pageByCondition(TableModelQueryDTO query);

    /**
     * 根据接口资源ID查询表模型列表
     */
    List<TableModelVO> listByApiId(String apiId);

    /**
     * 根据模块前缀查询表模型列表
     */
    List<TableModelVO> listByModulePrefix(String modulePrefix);

    /**
     * 处理接口-表模型绑定数据（启动时全量覆盖）
     *
     * @param applicationName 应用名称
     * @param permissions     接口资源包装
     */
    void handleTableModel(String applicationName, ApiEndpointCollector.ApiEndpointWrapper permissions);
}
```

- [ ] **Step 2: 创建 ISecurityApiTableModelConfigService**

```java
package org.quyq.gwsu.security.tablemodel.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.quyq.gwsu.security.api.tablemodel.dto.TableModelConfigSaveDTO;
import org.quyq.gwsu.security.api.tablemodel.dto.TableModelQueryDTO;
import org.quyq.gwsu.security.api.tablemodel.vo.TableModelConfigVO;
import org.quyq.gwsu.security.tablemodel.domain.SecurityApiTableModelConfig;

import java.util.List;

/**
 * 表模型手动配置服务接口
 */
public interface ISecurityApiTableModelConfigService extends IService<SecurityApiTableModelConfig> {

    /**
     * 分页查询
     */
    IPage<TableModelConfigVO> pageByCondition(TableModelQueryDTO query);

    /**
     * 保存或更新配置
     */
    Boolean saveOrUpdateConfig(TableModelConfigSaveDTO dto);

    /**
     * 根据表模型绑定ID查询有效配置
     */
    TableModelConfigVO getByTableModelId(String tableModelId);

    /**
     * 查询独立表模型列表
     */
    List<TableModelConfigVO> listIndependent();
}
```

- [ ] **Step 3: 创建 ISecurityRoleTableModelService**

```java
package org.quyq.gwsu.security.tablemodel.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.quyq.gwsu.common.security.domain.FieldPermission;
import org.quyq.gwsu.security.api.tablemodel.dto.RoleTableModelSaveDTO;
import org.quyq.gwsu.security.api.tablemodel.dto.TableModelQueryDTO;
import org.quyq.gwsu.security.api.tablemodel.vo.RoleTableModelVO;
import org.quyq.gwsu.security.tablemodel.domain.SecurityRoleTableModel;

import java.util.List;
import java.util.Map;

/**
 * 角色表模型权限服务接口
 */
public interface ISecurityRoleTableModelService extends IService<SecurityRoleTableModel> {

    /**
     * 分页查询
     */
    IPage<RoleTableModelVO> pageByCondition(TableModelQueryDTO query);

    /**
     * 根据角色ID查询表模型权限列表
     */
    List<RoleTableModelVO> listByRoleId(String roleId);

    /**
     * 保存或更新角色表模型权限
     */
    Boolean saveOrUpdateRoleTableModel(RoleTableModelSaveDTO dto);

    /**
     * 批量删除
     */
    Boolean removeByIds(List<String> ids);

    /**
     * 获取指定角色的合并后表模型权限
     * key = "module_prefix:datasource:table_name"
     */
    Map<String, Map<String, FieldPermission>> getMergedRoleTableModelPermission(List<String> roleIds);
}
```

- [ ] **Step 4: 创建 SecurityApiTableModelServiceImpl**

```java
package org.quyq.gwsu.security.tablemodel.service.impl;

import cn.hutool.crypto.digest.MD5;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quyq.gwsu.common.security.collector.ApiEndpointCollector;
import org.quyq.gwsu.common.security.domain.ApiEndpointInfo;
import org.quyq.gwsu.common.security.domain.TableModelInfo;
import org.quyq.gwsu.security.api.tablemodel.dto.TableModelQueryDTO;
import org.quyq.gwsu.security.api.tablemodel.vo.TableModelVO;
import org.quyq.gwsu.security.tablemodel.domain.SecurityApiTableModel;
import org.quyq.gwsu.security.tablemodel.mapper.SecurityApiTableModelMapper;
import org.quyq.gwsu.security.tablemodel.service.ISecurityApiTableModelService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class SecurityApiTableModelServiceImpl extends ServiceImpl<SecurityApiTableModelMapper, SecurityApiTableModel>
        implements ISecurityApiTableModelService {

    @Override
    public IPage<TableModelVO> pageByCondition(TableModelQueryDTO query) {
        Page<SecurityApiTableModel> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<SecurityApiTableModel> wrapper = new LambdaQueryWrapper<>();
        if (query.getModulePrefix() != null) {
            wrapper.eq(SecurityApiTableModel::getModulePrefix, query.getModulePrefix());
        }
        if (query.getTableName() != null) {
            wrapper.like(SecurityApiTableModel::getTableName, query.getTableName());
        }
        if (query.getApiId() != null) {
            wrapper.eq(SecurityApiTableModel::getApiId, query.getApiId());
        }
        return page(page, wrapper).convert(SecurityApiTableModel::toVo);
    }

    @Override
    public List<TableModelVO> listByApiId(String apiId) {
        return list(new LambdaQueryWrapper<SecurityApiTableModel>()
                .eq(SecurityApiTableModel::getApiId, apiId))
                .stream()
                .map(SecurityApiTableModel::toVo)
                .toList();
    }

    @Override
    public List<TableModelVO> listByModulePrefix(String modulePrefix) {
        return list(new LambdaQueryWrapper<SecurityApiTableModel>()
                .eq(SecurityApiTableModel::getModulePrefix, modulePrefix))
                .stream()
                .map(SecurityApiTableModel::toVo)
                .toList();
    }

    @Override
    @Transactional
    public void handleTableModel(String applicationName, ApiEndpointCollector.ApiEndpointWrapper permissions) {
        // 从接口数据中提取所有表模型绑定关系
        List<SecurityApiTableModel> newTableModels = new ArrayList<>();
        for (Map.Entry<String, List<ApiEndpointInfo>> entry : permissions.endpoints().entrySet()) {
            for (ApiEndpointInfo endpointInfo : entry.getValue()) {
                if (endpointInfo.tableModels() == null || endpointInfo.tableModels().isEmpty()) {
                    continue;
                }
                for (TableModelInfo tableModelInfo : endpointInfo.tableModels()) {
                    String id = MD5.create().digestHex(
                            "%s:%s:%s:%s".formatted(
                                    tableModelInfo.modulePrefix(),
                                    tableModelInfo.datasource(),
                                    tableModelInfo.tableName(),
                                    endpointInfo.id()));
                    SecurityApiTableModel model = new SecurityApiTableModel();
                    model.setId(id);
                    model.setApiId(endpointInfo.id());
                    model.setModulePrefix(tableModelInfo.modulePrefix());
                    model.setDatasource(tableModelInfo.datasource());
                    model.setTableName(tableModelInfo.tableName());
                    model.setFieldConfig(tableModelInfo.fieldConfig());
                    newTableModels.add(model);
                }
            }
        }

        // 查询旧数据
        Set<String> modules = permissions.endpoints().keySet();
        List<SecurityApiTableModel> oldTableModels = lambdaQuery()
                .in(SecurityApiTableModel::getModulePrefix, modules)
                .list();

        // 判断是否有变动
        boolean hasChanged = isTableModelChanged(newTableModels, oldTableModels);
        if (!hasChanged) {
            return;
        }

        log.info("模块 {} 的表模型绑定有变动，旧数据: {}, 新数据: {}", applicationName, oldTableModels.size(), newTableModels.size());

        // 删除旧数据
        if (!CollectionUtils.isEmpty(oldTableModels)) {
            List<String> oldIds = oldTableModels.stream().map(SecurityApiTableModel::getId).toList();
            removeByIds(oldIds);
        }

        // 插入新数据
        if (!CollectionUtils.isEmpty(newTableModels)) {
            saveBatch(newTableModels);
        }
    }

    private boolean isTableModelChanged(List<SecurityApiTableModel> newModels, List<SecurityApiTableModel> oldModels) {
        if (newModels.size() != oldModels.size()) {
            return true;
        }
        Map<String, SecurityApiTableModel> oldMap = new HashMap<>();
        for (SecurityApiTableModel m : oldModels) {
            oldMap.put(m.getId(), m);
        }
        for (SecurityApiTableModel newModel : newModels) {
            SecurityApiTableModel oldModel = oldMap.get(newModel.getId());
            if (oldModel == null || !Objects.equals(newModel, oldModel)) {
                return true;
            }
        }
        return false;
    }
}
```

- [ ] **Step 5: 创建 SecurityApiTableModelConfigServiceImpl**

```java
package org.quyq.gwsu.security.tablemodel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.core.utils.AssertUtils;
import org.quyq.gwsu.security.api.tablemodel.dto.TableModelConfigSaveDTO;
import org.quyq.gwsu.security.api.tablemodel.dto.TableModelQueryDTO;
import org.quyq.gwsu.security.api.tablemodel.vo.TableModelConfigVO;
import org.quyq.gwsu.security.errcode.SecurityErrorCode;
import org.quyq.gwsu.security.tablemodel.domain.SecurityApiTableModelConfig;
import org.quyq.gwsu.security.tablemodel.mapper.SecurityApiTableModelConfigMapper;
import org.quyq.gwsu.security.tablemodel.service.ISecurityApiTableModelConfigService;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SecurityApiTableModelConfigServiceImpl extends ServiceImpl<SecurityApiTableModelConfigMapper, SecurityApiTableModelConfig>
        implements ISecurityApiTableModelConfigService {

    @Override
    public IPage<TableModelConfigVO> pageByCondition(TableModelQueryDTO query) {
        Page<SecurityApiTableModelConfig> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<SecurityApiTableModelConfig> wrapper = new LambdaQueryWrapper<>();
        if (query.getModulePrefix() != null) {
            wrapper.eq(SecurityApiTableModelConfig::getModulePrefix, query.getModulePrefix());
        }
        if (query.getTableName() != null) {
            wrapper.like(SecurityApiTableModelConfig::getTableName, query.getTableName());
        }
        return page(page, wrapper).convert(SecurityApiTableModelConfig::toVo);
    }

    @Override
    public Boolean saveOrUpdateConfig(TableModelConfigSaveDTO dto) {
        AssertUtils.hasText(dto.getTableName(), SecurityErrorCode.E03001);
        AssertUtils.hasText(dto.getDatasource(), SecurityErrorCode.E03002);

        if (!CollectionUtils.isEmpty(dto.getTableModelIds())) {
            // 批量创建关联配置
            for (String tableModelId : dto.getTableModelIds()) {
                SecurityApiTableModelConfig config = new SecurityApiTableModelConfig();
                config.setTableModelId(tableModelId);
                config.setTableName(dto.getTableName());
                config.setModulePrefix(dto.getModulePrefix());
                config.setDatasource(dto.getDatasource());
                config.setDescription(dto.getDescription());
                save(config);
            }
            return true;
        }

        // 独立表模型
        SecurityApiTableModelConfig config = new SecurityApiTableModelConfig();
        config.setTableName(dto.getTableName());
        config.setModulePrefix(dto.getModulePrefix());
        config.setDatasource(dto.getDatasource());
        config.setDescription(dto.getDescription());
        return save(config);
    }

    @Override
    public TableModelConfigVO getByTableModelId(String tableModelId) {
        SecurityApiTableModelConfig config = getOne(new LambdaQueryWrapper<SecurityApiTableModelConfig>()
                .eq(SecurityApiTableModelConfig::getTableModelId, tableModelId)
                .last("LIMIT 1"));
        return config != null ? config.toVo() : null;
    }

    @Override
    public List<TableModelConfigVO> listIndependent() {
        return list(new LambdaQueryWrapper<SecurityApiTableModelConfig>()
                .isNull(SecurityApiTableModelConfig::getTableModelId))
                .stream()
                .map(SecurityApiTableModelConfig::toVo)
                .toList();
    }
}
```

- [ ] **Step 6: 创建 SecurityRoleTableModelServiceImpl**

```java
package org.quyq.gwsu.security.tablemodel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quyq.gwsu.common.core.utils.AssertUtils;
import org.quyq.gwsu.common.security.annotation.SensitiveStrategy;
import org.quyq.gwsu.common.security.domain.FieldPermission;
import org.quyq.gwsu.security.api.tablemodel.dto.RoleTableModelSaveDTO;
import org.quyq.gwsu.security.api.tablemodel.dto.TableModelQueryDTO;
import org.quyq.gwsu.security.api.tablemodel.vo.RoleTableModelVO;
import org.quyq.gwsu.security.errcode.SecurityErrorCode;
import org.quyq.gwsu.security.tablemodel.domain.SecurityRoleTableModel;
import org.quyq.gwsu.security.tablemodel.mapper.SecurityRoleTableModelMapper;
import org.quyq.gwsu.security.tablemodel.service.ISecurityRoleTableModelService;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class SecurityRoleTableModelServiceImpl extends ServiceImpl<SecurityRoleTableModelMapper, SecurityRoleTableModel>
        implements ISecurityRoleTableModelService {

    @Override
    public IPage<RoleTableModelVO> pageByCondition(TableModelQueryDTO query) {
        Page<SecurityRoleTableModel> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<SecurityRoleTableModel> wrapper = new LambdaQueryWrapper<>();
        if (query.getRoleId() != null) {
            wrapper.eq(SecurityRoleTableModel::getRoleId, query.getRoleId());
        }
        if (query.getModulePrefix() != null) {
            wrapper.eq(SecurityRoleTableModel::getModulePrefix, query.getModulePrefix());
        }
        if (query.getTableName() != null) {
            wrapper.like(SecurityRoleTableModel::getTableName, query.getTableName());
        }
        return page(page, wrapper).convert(SecurityRoleTableModel::toVo);
    }

    @Override
    public List<RoleTableModelVO> listByRoleId(String roleId) {
        return list(new LambdaQueryWrapper<SecurityRoleTableModel>()
                .eq(SecurityRoleTableModel::getRoleId, roleId))
                .stream()
                .map(SecurityRoleTableModel::toVo)
                .toList();
    }

    @Override
    public Boolean saveOrUpdateRoleTableModel(RoleTableModelSaveDTO dto) {
        AssertUtils.hasText(dto.getRoleId(), SecurityErrorCode.E03003);
        AssertUtils.hasText(dto.getTableName(), SecurityErrorCode.E03001);

        // 构建字段配置
        Map<String, FieldPermission> fieldConfigMap = null;
        if (!CollectionUtils.isEmpty(dto.getFields())) {
            fieldConfigMap = new HashMap<>();
            for (RoleTableModelSaveDTO.FieldConfigItem item : dto.getFields()) {
                fieldConfigMap.put(item.getFieldName(), new FieldPermission(
                        item.getShow() != null ? item.getShow() : true,
                        item.getDesensitize() != null ? item.getDesensitize() : false,
                        item.getStrategy() != null ? SensitiveStrategy.valueOf(item.getStrategy()) : SensitiveStrategy.NONE,
                        item.getPrefixNoMaskLen(),
                        item.getSuffixNoMaskLen(),
                        item.getSymbol()
                ));
            }
        }

        // 查找已有记录（唯一索引：roleId + modulePrefix + datasource + tableName）
        String modulePrefix = dto.getModulePrefix() != null ? dto.getModulePrefix() : "";
        String datasource = dto.getDatasource() != null ? dto.getDatasource() : "master";
        SecurityRoleTableModel existing = getOne(new LambdaQueryWrapper<SecurityRoleTableModel>()
                .eq(SecurityRoleTableModel::getRoleId, dto.getRoleId())
                .eq(SecurityRoleTableModel::getModulePrefix, modulePrefix)
                .eq(SecurityRoleTableModel::getDatasource, datasource)
                .eq(SecurityRoleTableModel::getTableName, dto.getTableName()));

        if (existing != null) {
            existing.setFieldConfig(fieldConfigMap);
            return updateById(existing);
        }

        SecurityRoleTableModel entity = new SecurityRoleTableModel();
        entity.setRoleId(dto.getRoleId());
        entity.setModulePrefix(modulePrefix);
        entity.setDatasource(datasource);
        entity.setTableName(dto.getTableName());
        entity.setFieldConfig(fieldConfigMap);
        return save(entity);
    }

    @Override
    public Boolean removeByIds(List<String> ids) {
        return removeBatchByIds(ids);
    }

    @Override
    public Map<String, Map<String, FieldPermission>> getMergedRoleTableModelPermission(List<String> roleIds) {
        if (CollectionUtils.isEmpty(roleIds)) {
            return Map.of();
        }
        List<SecurityRoleTableModel> roleTableModels = list(new LambdaQueryWrapper<SecurityRoleTableModel>()
                .in(SecurityRoleTableModel::getRoleId, roleIds));

        // 按 "module_prefix:datasource:table_name" 分组
        Map<String, List<SecurityRoleTableModel>> grouped = roleTableModels.stream()
                .collect(Collectors.groupingBy(m -> m.getModulePrefix() + ":" + m.getDatasource() + ":" + m.getTableName()));

        // 多角色取最大权限：show取或，desensitize取与
        Map<String, Map<String, FieldPermission>> result = new HashMap<>();
        for (Map.Entry<String, List<SecurityRoleTableModel>> entry : grouped.entrySet()) {
            Map<String, FieldPermission> mergedFields = new HashMap<>();
            for (SecurityRoleTableModel rtm : entry.getValue()) {
                if (rtm.getFieldConfig() != null) {
                    mergeFieldPermissions(mergedFields, rtm.getFieldConfig());
                }
            }
            result.put(entry.getKey(), mergedFields);
        }
        return result;
    }

    /**
     * 合并字段权限：show取或（任一角色允许则允许），desensitize取与（所有角色都脱敏才脱敏）
     */
    private void mergeFieldPermissions(Map<String, FieldPermission> merged, Map<String, FieldPermission> incoming) {
        for (Map.Entry<String, FieldPermission> entry : incoming.entrySet()) {
            String fieldName = entry.getKey();
            FieldPermission incomingPerm = entry.getValue();
            FieldPermission existingPerm = merged.get(fieldName);

            if (existingPerm == null) {
                merged.put(fieldName, incomingPerm);
            } else {
                // show取或：任一角色允许则允许
                boolean showResult = existingPerm.show() || incomingPerm.show();
                // desensitize取与：所有角色都脱敏才脱敏
                boolean desensitizeResult = existingPerm.desensitize() && incomingPerm.desensitize();
                // strategy：优先取不脱敏的（因为取与后可能不脱敏了），如果都脱敏则取第一个非NONE的
                SensitiveStrategy strategyResult = desensitizeResult
                        ? (existingPerm.strategy() != SensitiveStrategy.NONE ? existingPerm.strategy() : incomingPerm.strategy())
                        : SensitiveStrategy.NONE;
                Integer prefixResult = desensitizeResult
                        ? (existingPerm.prefixNoMaskLen() != null ? existingPerm.prefixNoMaskLen() : incomingPerm.prefixNoMaskLen())
                        : null;
                Integer suffixResult = desensitizeResult
                        ? (existingPerm.suffixNoMaskLen() != null ? existingPerm.suffixNoMaskLen() : incomingPerm.suffixNoMaskLen())
                        : null;
                String symbolResult = desensitizeResult
                        ? (existingPerm.symbol() != null ? existingPerm.symbol() : incomingPerm.symbol())
                        : null;
                merged.put(fieldName, new FieldPermission(showResult, desensitizeResult, strategyResult, prefixResult, suffixResult, symbolResult));
            }
        }
    }
}
```

- [ ] **Step 7: 验证编译**

Run: `mvn compile -pl business/business-security/business-security-server -am -q`
Expected: BUILD SUCCESS

- [ ] **Step 8: 提交**

```bash
git add business/business-security/business-security-server/src/main/java/org/quyq/gwsu/security/tablemodel/service/
git commit -m "feat(security-server): 新增表模型权限相关 Service 接口和实现"
```

---

## Task 9: Controller 层

**Files:**
- Create: `business/business-security/business-security-server/src/main/java/org/quyq/gwsu/security/tablemodel/controller/SecurityApiTableModelController.java`
- Create: `business/business-security/business-security-server/src/main/java/org/quyq/gwsu/security/tablemodel/controller/SecurityApiTableModelConfigController.java`
- Create: `business/business-security/business-security-server/src/main/java/org/quyq/gwsu/security/tablemodel/controller/SecurityRoleTableModelController.java`

- [ ] **Step 1: 创建 SecurityApiTableModelController**

```java
package org.quyq.gwsu.security.tablemodel.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.security.api.tablemodel.dto.TableModelQueryDTO;
import org.quyq.gwsu.security.api.tablemodel.vo.TableModelVO;
import org.quyq.gwsu.security.tablemodel.service.ISecurityApiTableModelService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 接口-表模型绑定控制器
 */
@RestController
@RequestMapping("apiTableModel")
@Tag(name = "接口-表模型绑定管理", description = "接口-表模型绑定管理接口")
@RequiredArgsConstructor
public class SecurityApiTableModelController {

    private final ISecurityApiTableModelService apiTableModelService;

    @Operation(summary = "分页查询")
    @PostMapping("page")
    public R<IPage<TableModelVO>> page(@RequestBody TableModelQueryDTO query) {
        return R.ok(apiTableModelService.pageByCondition(query));
    }

    @Operation(summary = "根据接口资源ID查询表模型列表")
    @GetMapping("list/by-api/{apiId}")
    public R<List<TableModelVO>> listByApiId(@PathVariable String apiId) {
        return R.ok(apiTableModelService.listByApiId(apiId));
    }

    @Operation(summary = "根据模块前缀查询表模型列表")
    @GetMapping("list/by-module/{modulePrefix}")
    public R<List<TableModelVO>> listByModulePrefix(@PathVariable String modulePrefix) {
        return R.ok(apiTableModelService.listByModulePrefix(modulePrefix));
    }
}
```

- [ ] **Step 2: 创建 SecurityApiTableModelConfigController**

```java
package org.quyq.gwsu.security.tablemodel.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.security.api.tablemodel.dto.TableModelConfigSaveDTO;
import org.quyq.gwsu.security.api.tablemodel.dto.TableModelQueryDTO;
import org.quyq.gwsu.security.api.tablemodel.vo.TableModelConfigVO;
import org.quyq.gwsu.security.tablemodel.service.ISecurityApiTableModelConfigService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 表模型手动配置控制器
 */
@RestController
@RequestMapping("apiTableModelConfig")
@Tag(name = "表模型配置管理", description = "表模型手动配置管理接口")
@RequiredArgsConstructor
public class SecurityApiTableModelConfigController {

    private final ISecurityApiTableModelConfigService configService;

    @Operation(summary = "分页查询")
    @PostMapping("page")
    public R<IPage<TableModelConfigVO>> page(@RequestBody TableModelQueryDTO query) {
        return R.ok(configService.pageByCondition(query));
    }

    @Operation(summary = "保存或更新配置")
    @PostMapping
    public R<Boolean> saveOrUpdate(@RequestBody TableModelConfigSaveDTO dto) {
        return R.ok(configService.saveOrUpdateConfig(dto));
    }

    @Operation(summary = "根据表模型绑定ID查询配置")
    @GetMapping("by-table-model/{tableModelId}")
    public R<TableModelConfigVO> getByTableModelId(@PathVariable String tableModelId) {
        return R.ok(configService.getByTableModelId(tableModelId));
    }

    @Operation(summary = "查询独立表模型列表")
    @GetMapping("independent")
    public R<List<TableModelConfigVO>> listIndependent() {
        return R.ok(configService.listIndependent());
    }
}
```

- [ ] **Step 3: 创建 SecurityRoleTableModelController**

```java
package org.quyq.gwsu.security.tablemodel.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.security.api.tablemodel.dto.RoleTableModelSaveDTO;
import org.quyq.gwsu.security.api.tablemodel.dto.TableModelQueryDTO;
import org.quyq.gwsu.security.api.tablemodel.vo.RoleTableModelVO;
import org.quyq.gwsu.security.tablemodel.service.ISecurityRoleTableModelService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 角色表模型权限控制器
 */
@RestController
@RequestMapping("roleTableModel")
@Tag(name = "角色表模型权限管理", description = "角色表模型权限管理接口")
@RequiredArgsConstructor
public class SecurityRoleTableModelController {

    private final ISecurityRoleTableModelService roleTableModelService;

    @Operation(summary = "分页查询")
    @PostMapping("page")
    public R<IPage<RoleTableModelVO>> page(@RequestBody TableModelQueryDTO query) {
        return R.ok(roleTableModelService.pageByCondition(query));
    }

    @Operation(summary = "根据角色ID查询表模型权限列表")
    @GetMapping("list/by-role/{roleId}")
    public R<List<RoleTableModelVO>> listByRoleId(@PathVariable String roleId) {
        return R.ok(roleTableModelService.listByRoleId(roleId));
    }

    @Operation(summary = "保存或更新角色表模型权限")
    @PostMapping
    public R<Boolean> saveOrUpdate(@RequestBody RoleTableModelSaveDTO dto) {
        return R.ok(roleTableModelService.saveOrUpdateRoleTableModel(dto));
    }

    @Operation(summary = "批量删除")
    @DeleteMapping
    public R<Boolean> remove(@RequestBody List<String> ids) {
        return R.ok(roleTableModelService.removeByIds(ids));
    }
}
```

- [ ] **Step 4: 验证编译**

Run: `mvn compile -pl business/business-security/business-security-server -am -q`
Expected: BUILD SUCCESS

- [ ] **Step 5: 提交**

```bash
git add business/business-security/business-security-server/src/main/java/org/quyq/gwsu/security/tablemodel/controller/
git commit -m "feat(security-server): 新增表模型权限相关 Controller"
```

---

## Task 10: 扩展 SecurityApiResourceServiceImpl，增加表模型数据处理

**Files:**
- Modify: `business/business-security/business-security-server/src/main/java/org/quyq/gwsu/security/apiresource/service/impl/SecurityApiResourceServiceImpl.java`
- Modify: `business/business-security/business-security-server/src/main/java/org/quyq/gwsu/security/errcode/SecurityErrorCode.java`

- [ ] **Step 1: 在 SecurityErrorCode 中新增表模型错误码**

```java
E03001("表名不能为空"),
E03002("数据源不能为空"),
E03003("角色ID不能为空"),
```

- [ ] **Step 2: 在 SecurityApiResourceServiceImpl 中注入 ISecurityApiTableModelService**

在类中新增字段：

```java
private final ISecurityApiTableModelService apiTableModelService;
```

添加 import：

```java
import org.quyq.gwsu.security.tablemodel.service.ISecurityApiTableModelService;
```

- [ ] **Step 3: 在 handlePermission 方法中，`if (!hasChanged)` 判断之前增加表模型数据处理**

在 `handlePermission` 方法中，构建完新资源列表和旧资源列表之后、`boolean hasChanged = isResourceChanged(...)` 之前，新增表模型处理调用：

```java
// 处理表模型绑定数据（在 hasChanged 判断之前，与接口资源共享同一事务和分布式锁）
apiTableModelService.handleTableModel(applicationName, permissions);
```

- [ ] **Step 4: 验证编译**

Run: `mvn compile -pl business/business-security/business-security-server -am -q`
Expected: BUILD SUCCESS

- [ ] **Step 5: 提交**

```bash
git add business/business-security/business-security-server/src/main/java/org/quyq/gwsu/security/apiresource/service/impl/SecurityApiResourceServiceImpl.java business/business-security/business-security-server/src/main/java/org/quyq/gwsu/security/errcode/SecurityErrorCode.java
git commit -m "feat(security-server): 扩展 SecurityApiResourceServiceImpl，增加表模型数据处理和错误码"
```

---

## Task 11: 全量编译验证

**Files:**
- 无新增/修改

- [ ] **Step 1: 全量编译**

Run: `mvn clean compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 2: 如有编译错误，修复并重新验证**

- [ ] **Step 3: 最终提交（如有修复）**

```bash
git add -A
git commit -m "fix: 修复表模型权限控制系统编译问题"
```
