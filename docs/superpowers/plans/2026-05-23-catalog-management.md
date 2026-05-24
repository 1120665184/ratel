# Catalog 管理功能实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现 Catalog 组件配置的后端存储与管理，以及前端管理界面，支持多 Catalog 配置和唯一激活机制。

**Architecture:** 后端在 `security/catalog` 包下新增 Domain/Mapper/Service/Controller，3 张表（catalog、catalog_component、catalog_component_ref），Controller 提供管理 CRUD + 获取激活 Catalog 定义的接口。前端在 `gwsu-sub-security` 子应用中新增 catalog 管理页面，包含 Catalog 列表、组件池管理、Catalog 组件关联管理。

**Tech Stack:** Spring Boot 4.0.3 / MyBatis Plus / MySQL / React 18 / UmiJS 4 / Ant Design 6 / @gwsu/core

---

## 文件清单

### 后端新建文件

| 文件 | 职责 |
|------|------|
| `business/business-security/sql/mysql/ddl_catalog.sql` | 3 张表的 DDL |
| `business/business-security/business-security-server/src/main/java/org/quyq/gwsu/security/catalog/domain/SecurityCatalog.java` | Catalog 实体 |
| `business/business-security/business-security-server/src/main/java/org/quyq/gwsu/security/catalog/domain/SecurityCatalogComponent.java` | 组件配置实体 |
| `business/business-security/business-security-server/src/main/java/org/quyq/gwsu/security/catalog/domain/SecurityCatalogComponentRef.java` | 关联关系实体 |
| `business/business-security/business-security-server/src/main/java/org/quyq/gwsu/security/catalog/mapper/SecurityCatalogMapper.java` | Catalog Mapper |
| `business/business-security/business-security-server/src/main/java/org/quyq/gwsu/security/catalog/mapper/SecurityCatalogComponentMapper.java` | Component Mapper |
| `business/business-security/business-security-server/src/main/java/org/quyq/gwsu/security/catalog/mapper/SecurityCatalogComponentRefMapper.java` | Ref Mapper |
| `business/business-security/business-security-server/src/main/java/org/quyq/gwsu/security/catalog/service/ISecurityCatalogService.java` | Catalog Service 接口 |
| `business/business-security/business-security-server/src/main/java/org/quyq/gwsu/security/catalog/service/ISecurityCatalogComponentService.java` | Component Service 接口 |
| `business/business-security/business-security-server/src/main/java/org/quyq/gwsu/security/catalog/service/impl/SecurityCatalogServiceImpl.java` | Catalog Service 实现 |
| `business/business-security/business-security-server/src/main/java/org/quyq/gwsu/security/catalog/service/impl/SecurityCatalogComponentServiceImpl.java` | Component Service 实现 |
| `business/business-security/business-security-server/src/main/java/org/quyq/gwsu/security/catalog/controller/SecurityCatalogController.java` | Catalog Controller |
| `business/business-security/business-security-server/src/main/java/org/quyq/gwsu/security/catalog/controller/SecurityCatalogComponentController.java` | Component Controller |
| `business/business-security/business-security-server/src/main/java/org/quyq/gwsu/security/catalog/vo/SecurityCatalogVO.java` | Catalog VO |
| `business/business-security/business-security-server/src/main/java/org/quyq/gwsu/security/catalog/vo/SecurityCatalogComponentVO.java` | Component VO |
| `business/business-security/business-security-server/src/main/java/org/quyq/gwsu/security/catalog/vo/CatalogDefinitionVO.java` | 前端 defineCatalog 用的完整结构 |
| `business/business-security/business-security-server/src/main/java/org/quyq/gwsu/security/errcode/SecurityErrorCode.java` | 追加 Catalog 相关错误码 |
| `business/business-security/business-security-server/src/main/resources/mapper/catalog/SecurityCatalogMapper.xml` | Catalog Mapper XML |
| `business/business-security/business-security-server/src/main/resources/mapper/catalog/SecurityCatalogComponentMapper.xml` | Component Mapper XML |
| `business/business-security/business-security-server/src/main/resources/mapper/catalog/SecurityCatalogComponentRefMapper.xml` | Ref Mapper XML |

### 前端新建文件

| 文件 | 职责 |
|------|------|
| `web/apps/gwsu-sub-security/src/pages/catalog/index.tsx` | Catalog 管理主页 |
| `web/apps/gwsu-sub-security/src/pages/catalog/index.module.less` | 主页样式 |
| `web/apps/gwsu-sub-security/src/pages/catalog/types/index.ts` | 类型定义 |
| `web/apps/gwsu-sub-security/src/pages/catalog/services/catalog.ts` | API 服务 |
| `web/apps/gwsu-sub-security/src/pages/catalog/permissionConstants.ts` | 权限常量 |
| `web/apps/gwsu-sub-security/src/pages/catalog/components/CatalogFormModal/index.tsx` | Catalog 新增/编辑弹窗 |
| `web/apps/gwsu-sub-security/src/pages/catalog/components/CatalogFormModal/index.module.less` | 弹窗样式 |
| `web/apps/gwsu-sub-security/src/pages/catalog/components/ComponentPool/index.tsx` | 组件池管理 |
| `web/apps/gwsu-sub-security/src/pages/catalog/components/ComponentPool/index.module.less` | 组件池样式 |
| `web/apps/gwsu-sub-security/src/pages/catalog/components/ComponentFormModal/index.tsx` | 组件新增/编辑弹窗 |
| `web/apps/gwsu-sub-security/src/pages/catalog/components/ComponentFormModal/index.module.less` | 弹窗样式 |
| `web/apps/gwsu-sub-security/src/pages/catalog/components/CatalogComponentBind/index.tsx` | Catalog 关联组件管理 |
| `web/apps/gwsu-sub-security/src/pages/catalog/components/CatalogComponentBind/index.module.less` | 关联管理样式 |

### 前端修改文件

| 文件 | 修改内容 |
|------|---------|
| `web/apps/gwsu-sub-security/config/routes.ts` | 添加 `/catalog` 路由 |

---

## Task 1: DDL 脚本

**Files:**
- Create: `business/business-security/sql/mysql/ddl_catalog.sql`

- [ ] **Step 1: 创建 DDL 脚本文件**

```sql
-- =============================================
-- 表名：security_catalog
-- 说明：Catalog定义表
-- =============================================
CREATE TABLE security_catalog (
    id              VARCHAR(24) PRIMARY KEY COMMENT '主键ID（雪花算法）',
    catalog_key     VARCHAR(100) NOT NULL COMMENT 'Catalog唯一标识',
    catalog_name    VARCHAR(200) NOT NULL COMMENT 'Catalog名称',
    description     VARCHAR(500) COMMENT 'Catalog描述',
    version         VARCHAR(20) DEFAULT '1.0.0' COMMENT '版本号',
    active          SMALLINT DEFAULT 0 COMMENT '激活状态：0-未激活 1-激活（全局唯一）',
    status          SMALLINT DEFAULT 1 COMMENT '状态：0-禁用 1-正常',
    tenant_id       VARCHAR(50) COMMENT '租户ID',
    create_op       VARCHAR(50) COMMENT '创建人',
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modify_op       VARCHAR(50) COMMENT '修改人',
    modify_time     DATETIME COMMENT '修改时间',
    deleted         SMALLINT DEFAULT 0 COMMENT '删除标识：0-未删除 1-已删除',
    delete_op       VARCHAR(50) COMMENT '删除人',
    delete_time     DATETIME COMMENT '删除时间',
    UNIQUE INDEX uk_catalog_key (catalog_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Catalog定义表';

-- =============================================
-- 表名：security_catalog_component
-- 说明：Catalog组件配置表
-- =============================================
CREATE TABLE security_catalog_component (
    id              VARCHAR(24) PRIMARY KEY COMMENT '主键ID（雪花算法）',
    component_name  VARCHAR(100) NOT NULL COMMENT '组件名（如 DataTable）',
    description     VARCHAR(500) NOT NULL COMMENT '组件描述（给AI看的）',
    props_schema    TEXT NOT NULL COMMENT '组件属性JSON Schema定义',
    default_props   TEXT COMMENT '默认属性值',
    category        VARCHAR(50) COMMENT '组件分类（display/chart/form）',
    sort_order      INT DEFAULT 0 COMMENT '排序号',
    status          SMALLINT DEFAULT 1 COMMENT '状态：0-禁用 1-正常',
    tenant_id       VARCHAR(50) COMMENT '租户ID',
    create_op       VARCHAR(50) COMMENT '创建人',
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modify_op       VARCHAR(50) COMMENT '修改人',
    modify_time     DATETIME COMMENT '修改时间',
    deleted         SMALLINT DEFAULT 0 COMMENT '删除标识：0-未删除 1-已删除',
    delete_op       VARCHAR(50) COMMENT '删除人',
    delete_time     DATETIME COMMENT '删除时间',
    UNIQUE INDEX uk_component_name (component_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Catalog组件配置表';

-- =============================================
-- 表名：security_catalog_component_ref
-- 说明：Catalog与组件关联表
-- =============================================
CREATE TABLE security_catalog_component_ref (
    id              VARCHAR(24) PRIMARY KEY COMMENT '主键ID（雪花算法）',
    catalog_id      VARCHAR(24) NOT NULL COMMENT 'Catalog ID',
    component_id    VARCHAR(24) NOT NULL COMMENT '组件ID',
    sort_order      INT DEFAULT 0 COMMENT '排序号',
    tenant_id       VARCHAR(50) COMMENT '租户ID',
    create_op       VARCHAR(50) COMMENT '创建人',
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modify_op       VARCHAR(50) COMMENT '修改人',
    modify_time     DATETIME COMMENT '修改时间',
    deleted         SMALLINT DEFAULT 0 COMMENT '删除标识：0-未删除 1-已删除',
    delete_op       VARCHAR(50) COMMENT '删除人',
    delete_time     DATETIME COMMENT '删除时间',
    UNIQUE INDEX uk_catalog_component (catalog_id, component_id),
    INDEX idx_catalog_id (catalog_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Catalog与组件关联表';
```

- [ ] **Step 2: 提交**

```bash
git add business/business-security/sql/mysql/ddl_catalog.sql
git commit -m "feat(catalog): 添加 Catalog 管理 DDL 脚本"
```

---

## Task 2: Domain 实体类

**Files:**
- Create: `business/business-security/business-security-server/src/main/java/org/quyq/gwsu/security/catalog/domain/SecurityCatalog.java`
- Create: `business/business-security/business-security-server/src/main/java/org/quyq/gwsu/security/catalog/domain/SecurityCatalogComponent.java`
- Create: `business/business-security/business-security-server/src/main/java/org/quyq/gwsu/security/catalog/domain/SecurityCatalogComponentRef.java`

- [ ] **Step 1: 创建 SecurityCatalog 实体**

```java
package org.quyq.gwsu.security.catalog.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.quyq.gwsu.common.core.domain.BaseDO;
import org.quyq.gwsu.security.catalog.vo.SecurityCatalogVO;

@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
@TableName(value = "security_catalog")
@Schema(description = "Catalog定义表")
public class SecurityCatalog extends BaseDO {

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private String id;

    @Schema(description = "Catalog唯一标识")
    private String catalogKey;

    @Schema(description = "Catalog名称")
    private String catalogName;

    @Schema(description = "Catalog描述")
    private String description;

    @Schema(description = "版本号")
    private String version;

    @Schema(description = "激活状态：0-未激活 1-激活")
    private Integer active;

    @Schema(description = "状态：0-禁用 1-正常")
    private Integer status;

    public SecurityCatalogVO toVo() {
        SecurityCatalogVO vo = new SecurityCatalogVO();
        vo.setId(this.id);
        vo.setCatalogKey(this.catalogKey);
        vo.setCatalogName(this.catalogName);
        vo.setDescription(this.description);
        vo.setVersion(this.version);
        vo.setActive(this.active);
        vo.setStatus(this.status);
        vo.copyBaseProperties(this);
        return vo;
    }

    public static SecurityCatalog toDo(SecurityCatalogVO vo) {
        SecurityCatalog entity = new SecurityCatalog();
        entity.setId(vo.getId());
        entity.setCatalogKey(vo.getCatalogKey());
        entity.setCatalogName(vo.getCatalogName());
        entity.setDescription(vo.getDescription());
        entity.setVersion(vo.getVersion());
        entity.setActive(vo.getActive());
        entity.setStatus(vo.getStatus());
        return entity;
    }
}
```

- [ ] **Step 2: 创建 SecurityCatalogComponent 实体**

```java
package org.quyq.gwsu.security.catalog.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.quyq.gwsu.common.core.domain.BaseDO;
import org.quyq.gwsu.security.catalog.vo.SecurityCatalogComponentVO;

@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
@TableName(value = "security_catalog_component")
@Schema(description = "Catalog组件配置表")
public class SecurityCatalogComponent extends BaseDO {

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private String id;

    @Schema(description = "组件名")
    private String componentName;

    @Schema(description = "组件描述（给AI看的）")
    private String description;

    @Schema(description = "组件属性JSON Schema定义")
    private String propsSchema;

    @Schema(description = "默认属性值")
    private String defaultProps;

    @Schema(description = "组件分类（display/chart/form）")
    private String category;

    @Schema(description = "排序号")
    private Integer sortOrder;

    @Schema(description = "状态：0-禁用 1-正常")
    private Integer status;

    public SecurityCatalogComponentVO toVo() {
        SecurityCatalogComponentVO vo = new SecurityCatalogComponentVO();
        vo.setId(this.id);
        vo.setComponentName(this.componentName);
        vo.setDescription(this.description);
        vo.setPropsSchema(this.propsSchema);
        vo.setDefaultProps(this.defaultProps);
        vo.setCategory(this.category);
        vo.setSortOrder(this.sortOrder);
        vo.setStatus(this.status);
        vo.copyBaseProperties(this);
        return vo;
    }

    public static SecurityCatalogComponent toDo(SecurityCatalogComponentVO vo) {
        SecurityCatalogComponent entity = new SecurityCatalogComponent();
        entity.setId(vo.getId());
        entity.setComponentName(vo.getComponentName());
        entity.setDescription(vo.getDescription());
        entity.setPropsSchema(vo.getPropsSchema());
        entity.setDefaultProps(vo.getDefaultProps());
        entity.setCategory(vo.getCategory());
        entity.setSortOrder(vo.getSortOrder());
        entity.setStatus(vo.getStatus());
        return entity;
    }
}
```

- [ ] **Step 3: 创建 SecurityCatalogComponentRef 实体**

```java
package org.quyq.gwsu.security.catalog.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.quyq.gwsu.common.core.domain.BaseDO;

@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
@TableName(value = "security_catalog_component_ref")
@Schema(description = "Catalog与组件关联表")
public class SecurityCatalogComponentRef extends BaseDO {

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private String id;

    @Schema(description = "Catalog ID")
    private String catalogId;

    @Schema(description = "组件ID")
    private String componentId;

    @Schema(description = "排序号")
    private Integer sortOrder;
}
```

- [ ] **Step 4: 提交**

```bash
git add business/business-security/business-security-server/src/main/java/org/quyq/gwsu/security/catalog/domain/
git commit -m "feat(catalog): 添加 Catalog Domain 实体类"
```

---

## Task 3: VO 对象

**Files:**
- Create: `business/business-security/business-security-server/src/main/java/org/quyq/gwsu/security/catalog/vo/SecurityCatalogVO.java`
- Create: `business/business-security/business-security-server/src/main/java/org/quyq/gwsu/security/catalog/vo/SecurityCatalogComponentVO.java`
- Create: `business/business-security/business-security-server/src/main/java/org/quyq/gwsu/security/catalog/vo/CatalogDefinitionVO.java`

- [ ] **Step 1: 创建 SecurityCatalogVO**

```java
package org.quyq.gwsu.security.catalog.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.quyq.gwsu.common.core.domain.BaseVO;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Catalog信息")
public class SecurityCatalogVO extends BaseVO {

    @Schema(description = "主键ID")
    private String id;

    @Schema(description = "Catalog唯一标识")
    private String catalogKey;

    @Schema(description = "Catalog名称")
    private String catalogName;

    @Schema(description = "Catalog描述")
    private String description;

    @Schema(description = "版本号")
    private String version;

    @Schema(description = "激活状态：0-未激活 1-激活")
    private Integer active;

    @Schema(description = "状态：0-禁用 1-正常")
    private Boolean status;
}
```

- [ ] **Step 2: 创建 SecurityCatalogComponentVO**

```java
package org.quyq.gwsu.security.catalog.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.quyq.gwsu.common.core.domain.BaseVO;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Catalog组件信息")
public class SecurityCatalogComponentVO extends BaseVO {

    @Schema(description = "主键ID")
    private String id;

    @Schema(description = "组件名")
    private String componentName;

    @Schema(description = "组件描述（给AI看的）")
    private String description;

    @Schema(description = "组件属性JSON Schema定义")
    private String propsSchema;

    @Schema(description = "默认属性值")
    private String defaultProps;

    @Schema(description = "组件分类（display/chart/form）")
    private String category;

    @Schema(description = "排序号")
    private Integer sortOrder;

    @Schema(description = "状态：0-禁用 1-正常")
    private Boolean status;
}
```

- [ ] **Step 3: 创建 CatalogDefinitionVO**

```java
package org.quyq.gwsu.security.catalog.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "Catalog完整定义（供前端 defineCatalog 使用）")
public class CatalogDefinitionVO {

    @Schema(description = "Catalog标识")
    private String catalogKey;

    @Schema(description = "Catalog名称")
    private String catalogName;

    @Schema(description = "组件定义列表")
    private List<ComponentDefinition> components;

    @Data
    @Schema(description = "组件定义")
    public static class ComponentDefinition {

        @Schema(description = "组件名")
        private String componentName;

        @Schema(description = "组件描述")
        private String description;

        @Schema(description = "属性JSON Schema（字符串）")
        private String propsSchema;

        @Schema(description = "默认属性（字符串）")
        private String defaultProps;

        @Schema(description = "分类")
        private String category;
    }
}
```

- [ ] **Step 4: 提交**

```bash
git add business/business-security/business-security-server/src/main/java/org/quyq/gwsu/security/catalog/vo/
git commit -m "feat(catalog): 添加 Catalog VO 对象"
```

---

## Task 4: Mapper 层

**Files:**
- Create: `business/business-security/business-security-server/src/main/java/org/quyq/gwsu/security/catalog/mapper/SecurityCatalogMapper.java`
- Create: `business/business-security/business-security-server/src/main/java/org/quyq/gwsu/security/catalog/mapper/SecurityCatalogComponentMapper.java`
- Create: `business/business-security/business-security-server/src/main/java/org/quyq/gwsu/security/catalog/mapper/SecurityCatalogComponentRefMapper.java`
- Create: `business/business-security/business-security-server/src/main/resources/mapper/catalog/SecurityCatalogMapper.xml`
- Create: `business/business-security/business-security-server/src/main/resources/mapper/catalog/SecurityCatalogComponentMapper.xml`
- Create: `business/business-security/business-security-server/src/main/resources/mapper/catalog/SecurityCatalogComponentRefMapper.xml`

- [ ] **Step 1: 创建 SecurityCatalogMapper**

```java
package org.quyq.gwsu.security.catalog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.quyq.gwsu.security.catalog.domain.SecurityCatalog;

public interface SecurityCatalogMapper extends BaseMapper<SecurityCatalog> {
}
```

- [ ] **Step 2: 创建 SecurityCatalogComponentMapper**

```java
package org.quyq.gwsu.security.catalog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.quyq.gwsu.security.catalog.domain.SecurityCatalogComponent;

public interface SecurityCatalogComponentMapper extends BaseMapper<SecurityCatalogComponent> {
}
```

- [ ] **Step 3: 创建 SecurityCatalogComponentRefMapper**

```java
package org.quyq.gwsu.security.catalog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.quyq.gwsu.security.catalog.domain.SecurityCatalogComponentRef;

public interface SecurityCatalogComponentRefMapper extends BaseMapper<SecurityCatalogComponentRef> {
}
```

- [ ] **Step 4: 创建 SecurityCatalogMapper.xml**

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="org.quyq.gwsu.security.catalog.mapper.SecurityCatalogMapper">
</mapper>
```

- [ ] **Step 5: 创建 SecurityCatalogComponentMapper.xml**

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="org.quyq.gwsu.security.catalog.mapper.SecurityCatalogComponentMapper">
</mapper>
```

- [ ] **Step 6: 创建 SecurityCatalogComponentRefMapper.xml**

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="org.quyq.gwsu.security.catalog.mapper.SecurityCatalogComponentRefMapper">
</mapper>
```

- [ ] **Step 7: 提交**

```bash
git add business/business-security/business-security-server/src/main/java/org/quyq/gwsu/security/catalog/mapper/ \
        business/business-security/business-security-server/src/main/resources/mapper/catalog/
git commit -m "feat(catalog): 添加 Catalog Mapper 层"
```

---

## Task 5: 错误码

**Files:**
- Modify: `business/business-security/business-security-server/src/main/java/org/quyq/gwsu/security/errcode/SecurityErrorCode.java`

- [ ] **Step 1: 先读取现有错误码文件，了解已有的错误码编号**

Run: `cat business/business-security/business-security-server/src/main/java/org/quyq/gwsu/security/errcode/SecurityErrorCode.java`

- [ ] **Step 2: 在现有错误码枚举中追加 Catalog 相关错误码**

追加内容（编号接续已有最大值）：

```java
EC00001("Catalog唯一标识已存在"),
EC00002("Catalog名称不能为空"),
EC00003("组件名已存在"),
EC00004("组件描述不能为空"),
EC00005("组件属性Schema不能为空"),
EC00006("激活Catalog失败，不存在ID为{0}的Catalog"),
```

- [ ] **Step 3: 提交**

```bash
git add business/business-security/business-security-server/src/main/java/org/quyq/gwsu/security/errcode/SecurityErrorCode.java
git commit -m "feat(catalog): 添加 Catalog 错误码"
```

---

## Task 6: Service 层

**Files:**
- Create: `business/business-security/business-security-server/src/main/java/org/quyq/gwsu/security/catalog/service/ISecurityCatalogService.java`
- Create: `business/business-security/business-security-server/src/main/java/org/quyq/gwsu/security/catalog/service/ISecurityCatalogComponentService.java`
- Create: `business/business-security/business-security-server/src/main/java/org/quyq/gwsu/security/catalog/service/impl/SecurityCatalogServiceImpl.java`
- Create: `business/business-security/business-security-server/src/main/java/org/quyq/gwsu/security/catalog/service/impl/SecurityCatalogComponentServiceImpl.java`

- [ ] **Step 1: 创建 ISecurityCatalogService 接口**

```java
package org.quyq.gwsu.security.catalog.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.quyq.gwsu.security.catalog.domain.SecurityCatalog;
import org.quyq.gwsu.security.catalog.vo.CatalogDefinitionVO;
import org.quyq.gwsu.security.catalog.vo.SecurityCatalogVO;

import java.util.List;

public interface ISecurityCatalogService extends IService<SecurityCatalog> {

    /** 查询所有Catalog列表 */
    List<SecurityCatalogVO> listAll();

    /** 根据ID查询 */
    SecurityCatalogVO getCatalogById(String id);

    /** 新增或更新Catalog */
    String saveOrUpdateCatalog(SecurityCatalogVO vo);

    /** 批量删除Catalog */
    Boolean removeCatalogs(List<String> ids);

    /** 激活Catalog（全局唯一，事务保证） */
    Boolean activateCatalog(String id);

    /** 获取当前激活的Catalog完整定义 */
    CatalogDefinitionVO getActiveCatalogDefinition();

    /** 根据catalogKey获取Catalog完整定义 */
    CatalogDefinitionVO getCatalogDefinitionByKey(String catalogKey);

    /** 给Catalog绑定组件列表 */
    Boolean bindComponents(String catalogId, List<String> componentIds);

    /** 解绑Catalog的组件 */
    Boolean unbindComponent(String catalogId, String componentId);

    /** 获取Catalog已绑定的组件ID列表 */
    List<String> getBoundComponentIds(String catalogId);
}
```

- [ ] **Step 2: 创建 ISecurityCatalogComponentService 接口**

```java
package org.quyq.gwsu.security.catalog.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.quyq.gwsu.security.catalog.domain.SecurityCatalogComponent;
import org.quyq.gwsu.security.catalog.vo.SecurityCatalogComponentVO;

import java.util.List;

public interface ISecurityCatalogComponentService extends IService<SecurityCatalogComponent> {

    /** 查询所有组件列表 */
    List<SecurityCatalogComponentVO> listAll();

    /** 根据ID查询 */
    SecurityCatalogComponentVO getComponentById(String id);

    /** 新增或更新组件 */
    String saveOrUpdateComponent(SecurityCatalogComponentVO vo);

    /** 批量删除组件 */
    Boolean removeComponents(List<String> ids);

    /** 根据ID列表查询组件 */
    List<SecurityCatalogComponentVO> listByIds(List<String> ids);
}
```

- [ ] **Step 3: 创建 SecurityCatalogServiceImpl 实现**

```java
package org.quyq.gwsu.security.catalog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.quyq.gwsu.common.core.utils.AssertUtils;
import org.quyq.gwsu.security.catalog.domain.SecurityCatalog;
import org.quyq.gwsu.security.catalog.domain.SecurityCatalogComponent;
import org.quyq.gwsu.security.catalog.domain.SecurityCatalogComponentRef;
import org.quyq.gwsu.security.catalog.mapper.SecurityCatalogMapper;
import org.quyq.gwsu.security.catalog.mapper.SecurityCatalogComponentMapper;
import org.quyq.gwsu.security.catalog.mapper.SecurityCatalogComponentRefMapper;
import org.quyq.gwsu.security.catalog.service.ISecurityCatalogService;
import org.quyq.gwsu.security.catalog.service.ISecurityCatalogComponentService;
import org.quyq.gwsu.security.catalog.vo.CatalogDefinitionVO;
import org.quyq.gwsu.security.catalog.vo.SecurityCatalogVO;
import org.quyq.gwsu.security.errcode.SecurityErrorCode;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SecurityCatalogServiceImpl extends ServiceImpl<SecurityCatalogMapper, SecurityCatalog>
        implements ISecurityCatalogService {

    private final SecurityCatalogComponentMapper componentMapper;
    private final SecurityCatalogComponentRefMapper refMapper;
    private final ISecurityCatalogComponentService componentService;

    @Override
    public List<SecurityCatalogVO> listAll() {
        return list(new LambdaQueryWrapper<SecurityCatalog>()
                .eq(SecurityCatalog::getStatus, 1)
                .orderByDesc(SecurityCatalog::getActive)
                .orderByDesc(SecurityCatalog::getCreateTime))
                .stream()
                .map(SecurityCatalog::toVo)
                .toList();
    }

    @Override
    public SecurityCatalogVO getCatalogById(String id) {
        SecurityCatalog entity = super.getById(id);
        return entity != null ? entity.toVo() : null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String saveOrUpdateCatalog(SecurityCatalogVO vo) {
        // 新增时校验 catalogKey 唯一
        if (vo.getId() == null) {
            AssertUtils.hasText(vo.getCatalogKey(), SecurityErrorCode.EC00002);
            AssertUtils.hasText(vo.getCatalogName(), SecurityErrorCode.EC00002);
            long count = count(new LambdaQueryWrapper<SecurityCatalog>()
                    .eq(SecurityCatalog::getCatalogKey, vo.getCatalogKey()));
            AssertUtils.isTrue(count == 0, SecurityErrorCode.EC00001);
        }
        SecurityCatalog entity = SecurityCatalog.toDo(vo);
        saveOrUpdate(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean removeCatalogs(List<String> ids) {
        // 删除关联关系
        for (String id : ids) {
            refMapper.delete(new LambdaQueryWrapper<SecurityCatalogComponentRef>()
                    .eq(SecurityCatalogComponentRef::getCatalogId, id));
        }
        return removeBatchByIds(ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean activateCatalog(String id) {
        SecurityCatalog catalog = super.getById(id);
        AssertUtils.notNull(catalog, SecurityErrorCode.EC00006);
        // 先将所有 Catalog 的 active 置为 0
        update(new LambdaUpdateWrapper<SecurityCatalog>()
                .set(SecurityCatalog::getActive, 0)
                .eq(SecurityCatalog::getActive, 1));
        // 再将目标 Catalog 的 active 置为 1
        update(new LambdaUpdateWrapper<SecurityCatalog>()
                .set(SecurityCatalog::getActive, 1)
                .eq(SecurityCatalog::getId, id));
        return true;
    }

    @Override
    public CatalogDefinitionVO getActiveCatalogDefinition() {
        SecurityCatalog catalog = getOne(new LambdaQueryWrapper<SecurityCatalog>()
                .eq(SecurityCatalog::getActive, 1)
                .eq(SecurityCatalog::getStatus, 1)
                .last("LIMIT 1"));
        if (catalog == null) {
            return null;
        }
        return buildDefinition(catalog);
    }

    @Override
    public CatalogDefinitionVO getCatalogDefinitionByKey(String catalogKey) {
        SecurityCatalog catalog = getOne(new LambdaQueryWrapper<SecurityCatalog>()
                .eq(SecurityCatalog::getCatalogKey, catalogKey)
                .eq(SecurityCatalog::getStatus, 1));
        if (catalog == null) {
            return null;
        }
        return buildDefinition(catalog);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean bindComponents(String catalogId, List<String> componentIds) {
        // 先删除旧的关联
        refMapper.delete(new LambdaQueryWrapper<SecurityCatalogComponentRef>()
                .eq(SecurityCatalogComponentRef::getCatalogId, catalogId));
        // 批量插入新关联
        for (int i = 0; i < componentIds.size(); i++) {
            SecurityCatalogComponentRef ref = new SecurityCatalogComponentRef();
            ref.setCatalogId(catalogId);
            ref.setComponentId(componentIds.get(i));
            ref.setSortOrder(i);
            refMapper.insert(ref);
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean unbindComponent(String catalogId, String componentId) {
        refMapper.delete(new LambdaQueryWrapper<SecurityCatalogComponentRef>()
                .eq(SecurityCatalogComponentRef::getCatalogId, catalogId)
                .eq(SecurityCatalogComponentRef::getComponentId, componentId));
        return true;
    }

    @Override
    public List<String> getBoundComponentIds(String catalogId) {
        List<SecurityCatalogComponentRef> refs = refMapper.selectList(
                new LambdaQueryWrapper<SecurityCatalogComponentRef>()
                        .eq(SecurityCatalogComponentRef::getCatalogId, catalogId)
                        .orderByAsc(SecurityCatalogComponentRef::getSortOrder));
        return refs.stream().map(SecurityCatalogComponentRef::getComponentId).toList();
    }

    /** 根据 Catalog 构建完整定义 */
    private CatalogDefinitionVO buildDefinition(SecurityCatalog catalog) {
        List<String> componentIds = getBoundComponentIds(catalog.getId());
        if (componentIds.isEmpty()) {
            CatalogDefinitionVO def = new CatalogDefinitionVO();
            def.setCatalogKey(catalog.getCatalogKey());
            def.setCatalogName(catalog.getCatalogName());
            def.setComponents(Collections.emptyList());
            return def;
        }
        List<SecurityCatalogComponent> components = componentMapper.selectBatchIds(componentIds);
        List<CatalogDefinitionVO.ComponentDefinition> compDefs = components.stream()
                .sorted((a, b) -> {
                    int idxA = componentIds.indexOf(a.getId());
                    int idxB = componentIds.indexOf(b.getId());
                    return Integer.compare(idxA, idxB);
                })
                .map(comp -> {
                    CatalogDefinitionVO.ComponentDefinition def = new CatalogDefinitionVO.ComponentDefinition();
                    def.setComponentName(comp.getComponentName());
                    def.setDescription(comp.getDescription());
                    def.setPropsSchema(comp.getPropsSchema());
                    def.setDefaultProps(comp.getDefaultProps());
                    def.setCategory(comp.getCategory());
                    return def;
                })
                .toList();

        CatalogDefinitionVO result = new CatalogDefinitionVO();
        result.setCatalogKey(catalog.getCatalogKey());
        result.setCatalogName(catalog.getCatalogName());
        result.setComponents(compDefs);
        return result;
    }
}
```

- [ ] **Step 4: 创建 SecurityCatalogComponentServiceImpl 实现**

```java
package org.quyq.gwsu.security.catalog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.quyq.gwsu.common.core.utils.AssertUtils;
import org.quyq.gwsu.security.catalog.domain.SecurityCatalogComponent;
import org.quyq.gwsu.security.catalog.domain.SecurityCatalogComponentRef;
import org.quyq.gwsu.security.catalog.mapper.SecurityCatalogComponentMapper;
import org.quyq.gwsu.security.catalog.mapper.SecurityCatalogComponentRefMapper;
import org.quyq.gwsu.security.catalog.service.ISecurityCatalogComponentService;
import org.quyq.gwsu.security.catalog.vo.SecurityCatalogComponentVO;
import org.quyq.gwsu.security.errcode.SecurityErrorCode;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SecurityCatalogComponentServiceImpl extends ServiceImpl<SecurityCatalogComponentMapper, SecurityCatalogComponent>
        implements ISecurityCatalogComponentService {

    private final SecurityCatalogComponentRefMapper refMapper;

    @Override
    public List<SecurityCatalogComponentVO> listAll() {
        return list(new LambdaQueryWrapper<SecurityCatalogComponent>()
                .eq(SecurityCatalogComponent::getStatus, 1)
                .orderByAsc(SecurityCatalogComponent::getCategory)
                .orderByAsc(SecurityCatalogComponent::getSortOrder))
                .stream()
                .map(SecurityCatalogComponent::toVo)
                .toList();
    }

    @Override
    public SecurityCatalogComponentVO getComponentById(String id) {
        SecurityCatalogComponent entity = super.getById(id);
        return entity != null ? entity.toVo() : null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String saveOrUpdateComponent(SecurityCatalogComponentVO vo) {
        if (vo.getId() == null) {
            AssertUtils.hasText(vo.getComponentName(), SecurityErrorCode.EC00004);
            AssertUtils.hasText(vo.getPropsSchema(), SecurityErrorCode.EC00005);
            // 校验组件名唯一
            long count = count(new LambdaQueryWrapper<SecurityCatalogComponent>()
                    .eq(SecurityCatalogComponent::getComponentName, vo.getComponentName()));
            AssertUtils.isTrue(count == 0, SecurityErrorCode.EC00003);
        }
        SecurityCatalogComponent entity = SecurityCatalogComponent.toDo(vo);
        saveOrUpdate(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean removeComponents(List<String> ids) {
        // 删除关联关系
        for (String id : ids) {
            refMapper.delete(new LambdaQueryWrapper<SecurityCatalogComponentRef>()
                    .eq(SecurityCatalogComponentRef::getComponentId, id));
        }
        return removeBatchByIds(ids);
    }

    @Override
    public List<SecurityCatalogComponentVO> listByIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return listByIds(ids).stream()
                .map(SecurityCatalogComponent::toVo)
                .toList();
    }
}
```

注意：`listByIds` 方法存在重名问题，MyBatis Plus 的 `IService` 已有 `listByIds`。改为如下签名：

```java
/** 根据ID列表查询组件VO */
List<SecurityCatalogComponentVO> listVoByIds(List<String> ids);
```

实现中也对应修改：

```java
@Override
public List<SecurityCatalogComponentVO> listVoByIds(List<String> ids) {
    if (ids == null || ids.isEmpty()) {
        return List.of();
    }
    return super.listByIds(ids).stream()
            .map(SecurityCatalogComponent::toVo)
            .toList();
}
```

- [ ] **Step 5: 提交**

```bash
git add business/business-security/business-security-server/src/main/java/org/quyq/gwsu/security/catalog/service/
git commit -m "feat(catalog): 添加 Catalog Service 层"
```

---

## Task 7: Controller 层

**Files:**
- Create: `business/business-security/business-security-server/src/main/java/org/quyq/gwsu/security/catalog/controller/SecurityCatalogController.java`
- Create: `business/business-security/business-security-server/src/main/java/org/quyq/gwsu/security/catalog/controller/SecurityCatalogComponentController.java`

- [ ] **Step 1: 创建 SecurityCatalogController**

```java
package org.quyq.gwsu.security.catalog.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.common.security.annotation.TableModelPermission;
import org.quyq.gwsu.security.catalog.domain.SecurityCatalog;
import org.quyq.gwsu.security.catalog.domain.SecurityCatalogComponent;
import org.quyq.gwsu.security.catalog.domain.SecurityCatalogComponentRef;
import org.quyq.gwsu.security.catalog.service.ISecurityCatalogService;
import org.quyq.gwsu.security.catalog.vo.CatalogDefinitionVO;
import org.quyq.gwsu.security.catalog.vo.SecurityCatalogVO;
import org.quyq.gwsu.security.catalog.vo.SecurityCatalogComponentVO;

import java.util.List;

@RestController
@RequestMapping("catalog")
@Tag(name = "Catalog管理", description = "Catalog模块接口")
@RequiredArgsConstructor
@TableModelPermission({SecurityCatalog.class, SecurityCatalogComponentRef.class})
public class SecurityCatalogController {

    private final ISecurityCatalogService catalogService;

    @Operation(summary = "查询Catalog列表")
    @GetMapping("/list")
    public R<List<SecurityCatalogVO>> list() {
        return R.ok(catalogService.listAll());
    }

    @Operation(summary = "根据ID查询Catalog")
    @GetMapping("/{id}")
    public R<SecurityCatalogVO> getById(@PathVariable String id) {
        return R.ok(catalogService.getCatalogById(id));
    }

    @Operation(summary = "新增或更新Catalog")
    @PostMapping
    public R<String> saveOrUpdate(@RequestBody SecurityCatalogVO vo) {
        return R.ok(catalogService.saveOrUpdateCatalog(vo));
    }

    @Operation(summary = "批量删除Catalog")
    @DeleteMapping
    public R<Boolean> remove(@RequestBody List<String> ids) {
        return R.ok(catalogService.removeCatalogs(ids));
    }

    @Operation(summary = "激活Catalog")
    @PutMapping("/activate/{id}")
    public R<Boolean> activate(@PathVariable String id) {
        return R.ok(catalogService.activateCatalog(id));
    }

    @Operation(summary = "获取当前激活的Catalog完整定义")
    @GetMapping("/active-definition")
    public R<CatalogDefinitionVO> getActiveDefinition() {
        return R.ok(catalogService.getActiveCatalogDefinition());
    }

    @Operation(summary = "根据catalogKey获取Catalog完整定义")
    @GetMapping("/definition/{catalogKey}")
    public R<CatalogDefinitionVO> getDefinitionByKey(@PathVariable String catalogKey) {
        return R.ok(catalogService.getCatalogDefinitionByKey(catalogKey));
    }

    @Operation(summary = "给Catalog绑定组件列表")
    @PutMapping("/{catalogId}/components")
    public R<Boolean> bindComponents(@PathVariable String catalogId, @RequestBody List<String> componentIds) {
        return R.ok(catalogService.bindComponents(catalogId, componentIds));
    }

    @Operation(summary = "解绑Catalog的组件")
    @DeleteMapping("/{catalogId}/components/{componentId}")
    public R<Boolean> unbindComponent(@PathVariable String catalogId, @PathVariable String componentId) {
        return R.ok(catalogService.unbindComponent(catalogId, componentId));
    }

    @Operation(summary = "获取Catalog已绑定的组件ID列表")
    @GetMapping("/{catalogId}/component-ids")
    public R<List<String>> getBoundComponentIds(@PathVariable String catalogId) {
        return R.ok(catalogService.getBoundComponentIds(catalogId));
    }

    @Operation(summary = "获取Catalog已绑定的组件详情列表")
    @GetMapping("/{catalogId}/components")
    public R<List<SecurityCatalogComponentVO>> getBoundComponents(@PathVariable String catalogId) {
        List<String> ids = catalogService.getBoundComponentIds(catalogId);
        return R.ok(ids.isEmpty() ? List.of() : List.of());
        // 实际需要调用 componentService.listVoByIds(ids)
    }
}
```

注意：getBoundComponents 方法需要注入 ISecurityCatalogComponentService。修正如下：

```java
private final ISecurityCatalogService catalogService;
private final ISecurityCatalogComponentService componentService;

// ...

@Operation(summary = "获取Catalog已绑定的组件详情列表")
@GetMapping("/{catalogId}/components")
public R<List<SecurityCatalogComponentVO>> getBoundComponents(@PathVariable String catalogId) {
    List<String> ids = catalogService.getBoundComponentIds(catalogId);
    return R.ok(componentService.listVoByIds(ids));
}
```

- [ ] **Step 2: 创建 SecurityCatalogComponentController**

```java
package org.quyq.gwsu.security.catalog.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.common.security.annotation.TableModelPermission;
import org.quyq.gwsu.security.catalog.domain.SecurityCatalogComponent;
import org.quyq.gwsu.security.catalog.service.ISecurityCatalogComponentService;
import org.quyq.gwsu.security.catalog.vo.SecurityCatalogComponentVO;

import java.util.List;

@RestController
@RequestMapping("catalog/component")
@Tag(name = "Catalog组件管理", description = "Catalog组件配置接口")
@RequiredArgsConstructor
@TableModelPermission({SecurityCatalogComponent.class})
public class SecurityCatalogComponentController {

    private final ISecurityCatalogComponentService componentService;

    @Operation(summary = "查询组件列表")
    @GetMapping("/list")
    public R<List<SecurityCatalogComponentVO>> list() {
        return R.ok(componentService.listAll());
    }

    @Operation(summary = "根据ID查询组件")
    @GetMapping("/{id}")
    public R<SecurityCatalogComponentVO> getById(@PathVariable String id) {
        return R.ok(componentService.getComponentById(id));
    }

    @Operation(summary = "新增或更新组件")
    @PostMapping
    public R<String> saveOrUpdate(@RequestBody SecurityCatalogComponentVO vo) {
        return R.ok(componentService.saveOrUpdateComponent(vo));
    }

    @Operation(summary = "批量删除组件")
    @DeleteMapping
    public R<Boolean> remove(@RequestBody List<String> ids) {
        return R.ok(componentService.removeComponents(ids));
    }
}
```

- [ ] **Step 3: 提交**

```bash
git add business/business-security/business-security-server/src/main/java/org/quyq/gwsu/security/catalog/controller/
git commit -m "feat(catalog): 添加 Catalog Controller 层"
```

---

## Task 8: 前端类型定义与服务层

**Files:**
- Create: `web/apps/gwsu-sub-security/src/pages/catalog/types/index.ts`
- Create: `web/apps/gwsu-sub-security/src/pages/catalog/services/catalog.ts`
- Create: `web/apps/gwsu-sub-security/src/pages/catalog/permissionConstants.ts`

- [ ] **Step 1: 创建类型定义**

```typescript
/** Catalog 信息 */
export interface CatalogInfo {
  id?: string;
  catalogKey: string;
  catalogName: string;
  description?: string;
  version?: string;
  active: number;
  status: boolean;
  createTime?: string;
}

/** Catalog 组件信息 */
export interface CatalogComponentInfo {
  id?: string;
  componentName: string;
  description: string;
  propsSchema: string;
  defaultProps?: string;
  category?: string;
  sortOrder?: number;
  status: boolean;
  createTime?: string;
}

/** 组件定义（CatalogDefinitionVO 中的嵌套结构） */
export interface ComponentDefinition {
  componentName: string;
  description: string;
  propsSchema: string;
  defaultProps?: string;
  category?: string;
}

/** Catalog 完整定义 */
export interface CatalogDefinition {
  catalogKey: string;
  catalogName: string;
  components: ComponentDefinition[];
}

/** 组件分类选项 */
export const CATEGORY_OPTIONS = [
  { label: '展示', value: 'display' },
  { label: '图表', value: 'chart' },
  { label: '表单', value: 'form' },
];
```

- [ ] **Step 2: 创建 API 服务**

```typescript
import { get, post, put, del } from '@gwsu/core';
import type {
  CatalogInfo,
  CatalogComponentInfo,
  CatalogDefinition,
} from '../types';

const BASE = '/security/catalog';
const COMP_BASE = '/security/catalog/component';

// =================== Catalog 接口 ===================

/** 查询Catalog列表 */
export async function getCatalogList(): Promise<CatalogInfo[]> {
  const res = await get<CatalogInfo[]>(`${BASE}/list`);
  return res.data ?? [];
}

/** 根据ID查询Catalog */
export async function getCatalogById(id: string): Promise<CatalogInfo> {
  const res = await get<CatalogInfo>(`${BASE}/${id}`);
  return res.data;
}

/** 新增或更新Catalog */
export async function saveOrUpdateCatalog(data: CatalogInfo): Promise<string> {
  const res = await post<string>(BASE, data);
  return res.data;
}

/** 批量删除Catalog */
export async function deleteCatalogs(ids: string[]): Promise<boolean> {
  const res = await del<boolean>(BASE, ids);
  return res.data;
}

/** 激活Catalog */
export async function activateCatalog(id: string): Promise<boolean> {
  const res = await put<boolean>(`${BASE}/activate/${id}`);
  return res.data;
}

/** 获取当前激活的Catalog完整定义 */
export async function getActiveCatalogDefinition(): Promise<CatalogDefinition> {
  const res = await get<CatalogDefinition>(`${BASE}/active-definition`);
  return res.data;
}

/** 根据catalogKey获取Catalog完整定义 */
export async function getCatalogDefinitionByKey(
  catalogKey: string,
): Promise<CatalogDefinition> {
  const res = await get<CatalogDefinition>(`${BASE}/definition/${catalogKey}`);
  return res.data;
}

/** 给Catalog绑定组件列表 */
export async function bindComponents(
  catalogId: string,
  componentIds: string[],
): Promise<boolean> {
  const res = await put<boolean>(`${BASE}/${catalogId}/components`, componentIds);
  return res.data;
}

/** 解绑Catalog的组件 */
export async function unbindComponent(
  catalogId: string,
  componentId: string,
): Promise<boolean> {
  const res = await del<boolean>(`${BASE}/${catalogId}/components/${componentId}`);
  return res.data;
}

/** 获取Catalog已绑定的组件ID列表 */
export async function getBoundComponentIds(
  catalogId: string,
): Promise<string[]> {
  const res = await get<string[]>(`${BASE}/${catalogId}/component-ids`);
  return res.data ?? [];
}

/** 获取Catalog已绑定的组件详情列表 */
export async function getBoundComponents(
  catalogId: string,
): Promise<CatalogComponentInfo[]> {
  const res = await get<CatalogComponentInfo[]>(`${BASE}/${catalogId}/components`);
  return res.data ?? [];
}

// =================== Component 接口 ===================

/** 查询组件列表 */
export async function getComponentList(): Promise<CatalogComponentInfo[]> {
  const res = await get<CatalogComponentInfo[]>(`${COMP_BASE}/list`);
  return res.data ?? [];
}

/** 根据ID查询组件 */
export async function getComponentById(
  id: string,
): Promise<CatalogComponentInfo> {
  const res = await get<CatalogComponentInfo>(`${COMP_BASE}/${id}`);
  return res.data;
}

/** 新增或更新组件 */
export async function saveOrUpdateComponent(
  data: CatalogComponentInfo,
): Promise<string> {
  const res = await post<string>(COMP_BASE, data);
  return res.data;
}

/** 批量删除组件 */
export async function deleteComponents(ids: string[]): Promise<boolean> {
  const res = await del<boolean>(COMP_BASE, ids);
  return res.data;
}
```

- [ ] **Step 3: 创建权限常量**

```typescript
/** Catalog管理 - 按钮权限标识常量 */

/** Catalog操作权限 */
export const PERM_CATALOG_ADD = 'catalog_add';
export const PERM_CATALOG_EDIT = 'catalog_edit';
export const PERM_CATALOG_REMOVE = 'catalog_remove';
export const PERM_CATALOG_ACTIVATE = 'catalog_activate';

/** 组件操作权限 */
export const PERM_COMPONENT_ADD = 'component_add';
export const PERM_COMPONENT_EDIT = 'component_edit';
export const PERM_COMPONENT_REMOVE = 'component_remove';

/** 绑定操作权限 */
export const PERM_BIND_COMPONENT = 'bind_component';
```

- [ ] **Step 4: 提交**

```bash
git add web/apps/gwsu-sub-security/src/pages/catalog/types/ \
        web/apps/gwsu-sub-security/src/pages/catalog/services/ \
        web/apps/gwsu-sub-security/src/pages/catalog/permissionConstants.ts
git commit -m "feat(catalog): 添加前端类型定义、API 服务和权限常量"
```

---

## Task 9: 前端路由配置

**Files:**
- Modify: `web/apps/gwsu-sub-security/config/routes.ts`

- [ ] **Step 1: 添加 catalog 路由**

在现有路由数组中追加：

```typescript
{
  path: '/catalog',
  component: '@/pages/catalog',
},
```

- [ ] **Step 2: 提交**

```bash
git add web/apps/gwsu-sub-security/config/routes.ts
git commit -m "feat(catalog): 添加 Catalog 管理页路由"
```

---

## Task 10: 前端组件池管理页面

**Files:**
- Create: `web/apps/gwsu-sub-security/src/pages/catalog/components/ComponentFormModal/index.tsx`
- Create: `web/apps/gwsu-sub-security/src/pages/catalog/components/ComponentFormModal/index.module.less`
- Create: `web/apps/gwsu-sub-security/src/pages/catalog/components/ComponentPool/index.tsx`
- Create: `web/apps/gwsu-sub-security/src/pages/catalog/components/ComponentPool/index.module.less`

- [ ] **Step 1: 创建 ComponentFormModal 组件**

组件新增/编辑弹窗，包含：组件名、描述、分类、propsSchema（JSON 编辑器）、defaultProps、排序号。

```tsx
import React, { useState, useEffect } from 'react';
import { Modal, Form, Input, Select, InputNumber, message } from 'antd';
import { CATEGORY_OPTIONS, type CatalogComponentInfo } from '../../types';
import { saveOrUpdateComponent } from '../../services/catalog';
import styles from './index.module.less';

interface ComponentFormModalProps {
  visible: boolean;
  data?: CatalogComponentInfo | null;
  onClose: () => void;
  onSuccess: () => void;
}

const ComponentFormModal: React.FC<ComponentFormModalProps> = ({
  visible,
  data,
  onClose,
  onSuccess,
}) => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const isEdit = !!data?.id;

  useEffect(() => {
    if (visible) {
      if (data) {
        form.setFieldsValue(data);
      } else {
        form.resetFields();
      }
    }
  }, [visible, data, form]);

  const handleOk = async () => {
    try {
      const values = await form.validateFields();
      setLoading(true);
      await saveOrUpdateComponent({ ...data, ...values });
      message.success(isEdit ? '编辑成功' : '新增成功');
      onSuccess();
    } catch {
      // 表单校验失败或请求错误
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal
      title={isEdit ? '编辑组件' : '新增组件'}
      open={visible}
      onOk={handleOk}
      onCancel={onClose}
      confirmLoading={loading}
      okButtonProps={{ 'data-ai-approval': true }}
      width={640}
      className={styles.modal}
    >
      <Form form={form} layout="vertical">
        <Form.Item
          name="componentName"
          label="组件名"
          rules={[{ required: true, message: '请输入组件名' }]}
        >
          <Input placeholder="如 DataTable" disabled={isEdit} />
        </Form.Item>
        <Form.Item
          name="description"
          label="组件描述"
          rules={[{ required: true, message: '请输入组件描述' }]}
        >
          <Input.TextArea placeholder="给AI看的组件描述" rows={2} />
        </Form.Item>
        <Form.Item name="category" label="组件分类">
          <Select placeholder="请选择分类" options={CATEGORY_OPTIONS} allowClear />
        </Form.Item>
        <Form.Item
          name="propsSchema"
          label="属性JSON Schema"
          rules={[{ required: true, message: '请输入属性JSON Schema' }]}
        >
          <Input.TextArea
            placeholder='{"type":"object","properties":{...}}'
            rows={8}
            className={styles.codeArea}
          />
        </Form.Item>
        <Form.Item name="defaultProps" label="默认属性值">
          <Input.TextArea placeholder="可选，JSON格式" rows={3} className={styles.codeArea} />
        </Form.Item>
        <Form.Item name="sortOrder" label="排序号">
          <InputNumber min={0} placeholder="0" style={{ width: '100%' }} />
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default ComponentFormModal;
```

- [ ] **Step 2: 创建 ComponentFormModal 样式**

```less
.modal {
  :global {
    .ant-modal-body {
      max-height: 70vh;
      overflow-y: auto;
    }
  }
}

.codeArea {
  font-family: 'SF Mono', 'Menlo', 'Monaco', 'Courier New', monospace;
  font-size: 13px;
}
```

- [ ] **Step 3: 创建 ComponentPool 组件**

组件池列表，展示所有已注册组件，支持新增、编辑、删除。

```tsx
import React, { useState, useCallback, useEffect } from 'react';
import { Button, Table, Tag, Space, Popconfirm, Dropdown, message } from 'antd';
import type { MenuProps, TableProps } from 'antd';
import {
  PlusOutlined,
  DeleteOutlined,
  EditOutlined,
  MoreOutlined,
} from '@ant-design/icons';
import styles from './index.module.less';
import ComponentFormModal from '../ComponentFormModal';
import {
  getComponentList,
  deleteComponents,
} from '../../services/catalog';
import { AuthGate, useAuth } from '@gwsu/core';
import {
  PERM_COMPONENT_ADD,
  PERM_COMPONENT_EDIT,
  PERM_COMPONENT_REMOVE,
} from '../../permissionConstants';
import type { CatalogComponentInfo } from '../../types';

const CATEGORY_COLOR_MAP: Record<string, string> = {
  display: 'blue',
  chart: 'green',
  form: 'orange',
};

const ComponentPool: React.FC = () => {
  const [data, setData] = useState<CatalogComponentInfo[]>([]);
  const [loading, setLoading] = useState(false);
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([]);
  const [formModalVisible, setFormModalVisible] = useState(false);
  const [editData, setEditData] = useState<CatalogComponentInfo | null>(null);

  const canEdit = useAuth(PERM_COMPONENT_EDIT);

  const loadData = useCallback(async () => {
    setLoading(true);
    try {
      const result = await getComponentList();
      setData(result);
    } catch {} finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadData();
  }, [loadData]);

  const handleCreate = useCallback(() => {
    setEditData(null);
    setFormModalVisible(true);
  }, []);

  const handleEdit = useCallback((record: CatalogComponentInfo) => {
    setEditData(record);
    setFormModalVisible(true);
  }, []);

  const handleDelete = useCallback(
    async (ids: string[]) => {
      try {
        await deleteComponents(ids);
        message.success('删除成功');
        setSelectedRowKeys([]);
        void loadData();
      } catch {}
    },
    [loadData],
  );

  const getButtonItem = (
    record: CatalogComponentInfo,
  ): MenuProps['items'] => {
    const buttons = [];
    if (canEdit) {
      buttons.push({
        key: 'edit',
        icon: <EditOutlined />,
        label: '编辑',
        onClick: () => handleEdit(record),
      });
    }
    return buttons;
  };

  const columns: TableProps<CatalogComponentInfo>['columns'] = [
    {
      title: '组件名',
      dataIndex: 'componentName',
      width: 160,
    },
    {
      title: '描述',
      dataIndex: 'description',
      ellipsis: true,
    },
    {
      title: '分类',
      dataIndex: 'category',
      width: 100,
      render: (val: string) =>
        val ? <Tag color={CATEGORY_COLOR_MAP[val] || 'default'}>{val}</Tag> : '-',
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 80,
      render: (val: boolean) => (
        <Tag color={val ? 'green' : 'red'}>{val ? '启用' : '禁用'}</Tag>
      ),
    },
    {
      title: '排序',
      dataIndex: 'sortOrder',
      width: 80,
      align: 'center',
    },
    {
      title: '操作',
      width: 160,
      fixed: 'right',
      render: (_: unknown, record: CatalogComponentInfo) => (
        <div className={styles.actionColumn}>
          <Dropdown
            menu={{ items: getButtonItem(record) }}
            disabled={getButtonItem(record).length === 0}
          >
            <Button
              type="link"
              size="small"
              icon={<MoreOutlined />}
              disabled={getButtonItem(record).length === 0}
            >
              更多
            </Button>
          </Dropdown>
        </div>
      ),
    },
  ];

  return (
    <div className={styles.poolWrapper}>
      <div className={styles.poolHeader}>
        <span className={styles.poolTitle}>组件池</span>
        <Space>
          <AuthGate buttonKey={PERM_COMPONENT_ADD}>
            <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>
              新增组件
            </Button>
          </AuthGate>
          <AuthGate buttonKey={PERM_COMPONENT_REMOVE}>
            <Popconfirm
              title="批量删除"
              description={`确定删除选中的 ${selectedRowKeys.length} 条记录？`}
              onConfirm={() => handleDelete(selectedRowKeys as string[])}
              disabled={selectedRowKeys.length === 0}
            >
              <Button
                danger
                data-ai-approval
                icon={<DeleteOutlined />}
                disabled={selectedRowKeys.length === 0}
              >
                删除
              </Button>
            </Popconfirm>
          </AuthGate>
        </Space>
      </div>
      <Table<CatalogComponentInfo>
        rowKey="id"
        rowSelection={{ selectedRowKeys, onChange: setSelectedRowKeys }}
        columns={columns}
        dataSource={data}
        loading={loading}
        size="small"
        scroll={{ y: 400 }}
        pagination={false}
      />
      <ComponentFormModal
        visible={formModalVisible}
        data={editData}
        onClose={() => setFormModalVisible(false)}
        onSuccess={() => {
          setFormModalVisible(false);
          void loadData();
        }}
      />
    </div>
  );
};

export default ComponentPool;
```

- [ ] **Step 4: 创建 ComponentPool 样式**

```less
.poolWrapper {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: var(--surface-color);
  border-radius: 6px;
  border: 1px solid var(--border-color);
  overflow: hidden;
}

.poolHeader {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid var(--border-color);
}

.poolTitle {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-color);
}

.actionColumn {
  display: flex;
  align-items: center;
  gap: 4px;
}
```

- [ ] **Step 5: 提交**

```bash
git add web/apps/gwsu-sub-security/src/pages/catalog/components/ComponentFormModal/ \
        web/apps/gwsu-sub-security/src/pages/catalog/components/ComponentPool/
git commit -m "feat(catalog): 添加前端组件池管理和编辑弹窗"
```

---

## Task 11: 前端 CatalogFormModal 与 CatalogComponentBind

**Files:**
- Create: `web/apps/gwsu-sub-security/src/pages/catalog/components/CatalogFormModal/index.tsx`
- Create: `web/apps/gwsu-sub-security/src/pages/catalog/components/CatalogFormModal/index.module.less`
- Create: `web/apps/gwsu-sub-security/src/pages/catalog/components/CatalogComponentBind/index.tsx`
- Create: `web/apps/gwsu-sub-security/src/pages/catalog/components/CatalogComponentBind/index.module.less`

- [ ] **Step 1: 创建 CatalogFormModal**

Catalog 新增/编辑弹窗，字段：catalogKey、catalogName、description、version。

```tsx
import React, { useState, useEffect } from 'react';
import { Modal, Form, Input, message } from 'antd';
import type { CatalogInfo } from '../../types';
import { saveOrUpdateCatalog } from '../../services/catalog';
import styles from './index.module.less';

interface CatalogFormModalProps {
  visible: boolean;
  data?: CatalogInfo | null;
  onClose: () => void;
  onSuccess: () => void;
}

const CatalogFormModal: React.FC<CatalogFormModalProps> = ({
  visible,
  data,
  onClose,
  onSuccess,
}) => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const isEdit = !!data?.id;

  useEffect(() => {
    if (visible) {
      if (data) {
        form.setFieldsValue(data);
      } else {
        form.resetFields();
      }
    }
  }, [visible, data, form]);

  const handleOk = async () => {
    try {
      const values = await form.validateFields();
      setLoading(true);
      await saveOrUpdateCatalog({ ...data, ...values });
      message.success(isEdit ? '编辑成功' : '新增成功');
      onSuccess();
    } catch {
      // 表单校验失败或请求错误
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal
      title={isEdit ? '编辑Catalog' : '新增Catalog'}
      open={visible}
      onOk={handleOk}
      onCancel={onClose}
      confirmLoading={loading}
      okButtonProps={{ 'data-ai-approval': true }}
      className={styles.modal}
    >
      <Form form={form} layout="vertical">
        <Form.Item
          name="catalogKey"
          label="Catalog标识"
          rules={[{ required: true, message: '请输入Catalog标识' }]}
        >
          <Input placeholder="如 output-view" disabled={isEdit} />
        </Form.Item>
        <Form.Item
          name="catalogName"
          label="Catalog名称"
          rules={[{ required: true, message: '请输入Catalog名称' }]}
        >
          <Input placeholder="请输入名称" />
        </Form.Item>
        <Form.Item name="description" label="描述">
          <Input.TextArea placeholder="Catalog用途描述" rows={2} />
        </Form.Item>
        <Form.Item name="version" label="版本号">
          <Input placeholder="1.0.0" />
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default CatalogFormModal;
```

- [ ] **Step 2: 创建 CatalogFormModal 样式**

```less
.modal {
  :global {
    .ant-modal-body {
      max-height: 70vh;
      overflow-y: auto;
    }
  }
}
```

- [ ] **Step 3: 创建 CatalogComponentBind 组件**

用于管理 Catalog 与组件的关联关系。使用 Transfer 穿梭框选择组件。

```tsx
import React, { useState, useEffect, useCallback } from 'react';
import { Transfer, Button, message, Spin } from 'antd';
import type { TransferProps } from 'antd';
import { getComponentList, getBoundComponentIds, bindComponents } from '../../services/catalog';
import type { CatalogComponentInfo } from '../../types';
import styles from './index.module.less';

interface CatalogComponentBindProps {
  catalogId: string;
  catalogName: string;
  onClose: () => void;
}

const CatalogComponentBind: React.FC<CatalogComponentBindProps> = ({
  catalogId,
  catalogName,
  onClose,
}) => {
  const [allComponents, setAllComponents] = useState<CatalogComponentInfo[]>([]);
  const [targetKeys, setTargetKeys] = useState<string[]>([]);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);

  const loadData = useCallback(async () => {
    setLoading(true);
    try {
      const [components, boundIds] = await Promise.all([
        getComponentList(),
        getBoundComponentIds(catalogId),
      ]);
      setAllComponents(components);
      setTargetKeys(boundIds);
    } catch {} finally {
      setLoading(false);
    }
  }, [catalogId]);

  useEffect(() => {
    void loadData();
  }, [loadData]);

  const handleChange: TransferProps['onChange'] = (nextTargetKeys) => {
    setTargetKeys(nextTargetKeys as string[]);
  };

  const handleSave = async () => {
    setSaving(true);
    try {
      await bindComponents(catalogId, targetKeys);
      message.success('保存成功');
      onClose();
    } catch {} finally {
      setSaving(false);
    }
  };

  const transferDataSource = allComponents.map((comp) => ({
    key: comp.id!,
    title: comp.componentName,
    description: comp.description,
    category: comp.category,
  }));

  return (
    <div className={styles.bindWrapper}>
      <div className={styles.bindHeader}>
        <span className={styles.bindTitle}>
          关联组件 - {catalogName}
        </span>
      </div>
      <Spin spinning={loading}>
        <div className={styles.bindContent}>
          <Transfer
            dataSource={transferDataSource}
            targetKeys={targetKeys}
            onChange={handleChange}
            render={(item) => (
              <span>
                {item.title}
                {item.category ? (
                  <span style={{ color: 'var(--text-secondary-color)', marginLeft: 8, fontSize: 12 }}>
                    [{item.category}]
                  </span>
                ) : null}
              </span>
            )}
            titles={['可选组件', '已选组件']}
            showSearch
            listStyle={{ width: 280, height: 400 }}
            filterOption={(inputValue, item) =>
              item.title?.toLowerCase().includes(inputValue.toLowerCase()) ?? false
            }
            oneWay={false}
          />
        </div>
      </Spin>
      <div className={styles.bindFooter}>
        <Button onClick={onClose}>取消</Button>
        <Button type="primary" data-ai-approval loading={saving} onClick={handleSave}>
          保存
        </Button>
      </div>
    </div>
  );
};

export default CatalogComponentBind;
```

- [ ] **Step 4: 创建 CatalogComponentBind 样式**

```less
.bindWrapper {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: var(--surface-color);
  border-radius: 6px;
  border: 1px solid var(--border-color);
  overflow: hidden;
}

.bindHeader {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid var(--border-color);
}

.bindTitle {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-color);
}

.bindContent {
  flex: 1;
  display: flex;
  justify-content: center;
  padding: 16px;
  overflow: auto;
}

.bindFooter {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding: 12px 16px;
  border-top: 1px solid var(--border-color);
}
```

- [ ] **Step 5: 提交**

```bash
git add web/apps/gwsu-sub-security/src/pages/catalog/components/CatalogFormModal/ \
        web/apps/gwsu-sub-security/src/pages/catalog/components/CatalogComponentBind/
git commit -m "feat(catalog): 添加 Catalog 表单弹窗和组件关联管理"
```

---

## Task 12: 前端 Catalog 管理主页

**Files:**
- Create: `web/apps/gwsu-sub-security/src/pages/catalog/index.tsx`
- Create: `web/apps/gwsu-sub-security/src/pages/catalog/index.module.less`

- [ ] **Step 1: 创建 Catalog 管理主页**

页面布局：左侧 Catalog 列表，右侧根据选中 Catalog 显示关联组件管理或组件池管理。

```tsx
import React, { useState, useCallback, useEffect } from 'react';
import { Button, Table, Tag, Space, Popconfirm, Dropdown, message } from 'antd';
import type { MenuProps, TableProps } from 'antd';
import {
  PlusOutlined,
  DeleteOutlined,
  EditOutlined,
  MoreOutlined,
  LinkOutlined,
  AppstoreOutlined,
  CheckCircleOutlined,
} from '@ant-design/icons';
import styles from './index.module.less';
import CatalogFormModal from './components/CatalogFormModal';
import CatalogComponentBind from './components/CatalogComponentBind';
import ComponentPool from './components/ComponentPool';
import { getCatalogList, deleteCatalogs, activateCatalog } from './services/catalog';
import { AuthGate, useAuth } from '@gwsu/core';
import {
  PERM_CATALOG_ADD,
  PERM_CATALOG_EDIT,
  PERM_CATALOG_REMOVE,
  PERM_CATALOG_ACTIVATE,
  PERM_BIND_COMPONENT,
} from './permissionConstants';
import type { CatalogInfo } from './types';

const CatalogPage: React.FC = () => {
  const [data, setData] = useState<CatalogInfo[]>([]);
  const [loading, setLoading] = useState(false);
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([]);

  // 弹窗状态
  const [formModalVisible, setFormModalVisible] = useState(false);
  const [editData, setEditData] = useState<CatalogInfo | null>(null);

  // 右侧面板状态
  const [activeTab, setActiveTab] = useState<'pool' | 'bind'>('pool');
  const [selectedCatalog, setSelectedCatalog] = useState<CatalogInfo | null>(null);

  const canEdit = useAuth(PERM_CATALOG_EDIT);
  const canActivate = useAuth(PERM_CATALOG_ACTIVATE);
  const canBind = useAuth(PERM_BIND_COMPONENT);

  const loadData = useCallback(async () => {
    setLoading(true);
    try {
      const result = await getCatalogList();
      setData(result);
    } catch {} finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadData();
  }, [loadData]);

  const handleCreate = useCallback(() => {
    setEditData(null);
    setFormModalVisible(true);
  }, []);

  const handleEdit = useCallback((record: CatalogInfo) => {
    setEditData(record);
    setFormModalVisible(true);
  }, []);

  const handleDelete = useCallback(
    async (ids: string[]) => {
      try {
        await deleteCatalogs(ids);
        message.success('删除成功');
        setSelectedRowKeys([]);
        if (selectedCatalog && ids.includes(selectedCatalog.id!)) {
          setSelectedCatalog(null);
        }
        void loadData();
      } catch {}
    },
    [loadData, selectedCatalog],
  );

  const handleActivate = useCallback(
    async (id: string) => {
      try {
        await activateCatalog(id);
        message.success('激活成功');
        void loadData();
      } catch {}
    },
    [loadData],
  );

  const handleBindComponent = useCallback(
    (record: CatalogInfo) => {
      setSelectedCatalog(record);
      setActiveTab('bind');
    },
    [],
  );

  const getButtonItem = (record: CatalogInfo): MenuProps['items'] => {
    const buttons = [];
    if (canEdit) {
      buttons.push({
        key: 'edit',
        icon: <EditOutlined />,
        label: '编辑',
        onClick: () => handleEdit(record),
      });
    }
    if (canActivate && !record.active) {
      buttons.push({
        key: 'activate',
        icon: <CheckCircleOutlined />,
        label: '激活',
        onClick: () => handleActivate(record.id!),
      });
    }
    if (canBind) {
      buttons.push({
        key: 'bind',
        icon: <LinkOutlined />,
        label: '关联组件',
        onClick: () => handleBindComponent(record),
      });
    }
    return buttons;
  };

  const columns: TableProps<CatalogInfo>['columns'] = [
    {
      title: '标识',
      dataIndex: 'catalogKey',
      width: 140,
    },
    {
      title: '名称',
      dataIndex: 'catalogName',
      width: 160,
    },
    {
      title: '描述',
      dataIndex: 'description',
      ellipsis: true,
    },
    {
      title: '状态',
      dataIndex: 'active',
      width: 80,
      render: (val: number) =>
        val === 1 ? (
          <Tag color="green" icon={<CheckCircleOutlined />}>激活</Tag>
        ) : (
          <Tag>未激活</Tag>
        ),
    },
    {
      title: '操作',
      width: 180,
      fixed: 'right',
      render: (_: unknown, record: CatalogInfo) => (
        <div className={styles.actionColumn}>
          <Dropdown
            menu={{ items: getButtonItem(record) }}
            disabled={getButtonItem(record).length === 0}
          >
            <Button
              type="link"
              size="small"
              icon={<MoreOutlined />}
              disabled={getButtonItem(record).length === 0}
            >
              更多
            </Button>
          </Dropdown>
        </div>
      ),
    },
  ];

  return (
    <div className={styles.catalogPage}>
      {/* 左侧 Catalog 列表 */}
      <div className={styles.leftPanel}>
        <div className={styles.panelHeader}>
          <span className={styles.panelTitle}>Catalog 列表</span>
          <Space>
            <AuthGate buttonKey={PERM_CATALOG_ADD}>
              <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>
                新增
              </Button>
            </AuthGate>
            <AuthGate buttonKey={PERM_CATALOG_REMOVE}>
              <Popconfirm
                title="批量删除"
                description={`确定删除选中的 ${selectedRowKeys.length} 条记录？`}
                onConfirm={() => handleDelete(selectedRowKeys as string[])}
                disabled={selectedRowKeys.length === 0}
              >
                <Button
                  danger
                  data-ai-approval
                  icon={<DeleteOutlined />}
                  disabled={selectedRowKeys.length === 0}
                >
                  删除
                </Button>
              </Popconfirm>
            </AuthGate>
          </Space>
        </div>
        <Table<CatalogInfo>
          rowKey="id"
          rowSelection={{ selectedRowKeys, onChange: setSelectedRowKeys }}
          columns={columns}
          dataSource={data}
          loading={loading}
          size="small"
          scroll={{ y: 'calc(100vh - 280px)' }}
          pagination={false}
        />
      </div>

      {/* 右侧面板 */}
      <div className={styles.rightPanel}>
        <div className={styles.tabBar}>
          <Button
            type={activeTab === 'pool' ? 'primary' : 'default'}
            icon={<AppstoreOutlined />}
            onClick={() => setActiveTab('pool')}
          >
            组件池
          </Button>
          {selectedCatalog && (
            <Button
              type={activeTab === 'bind' ? 'primary' : 'default'}
              icon={<LinkOutlined />}
              onClick={() => setActiveTab('bind')}
            >
              关联组件
            </Button>
          )}
        </div>
        <div className={styles.tabContent}>
          {activeTab === 'pool' && <ComponentPool />}
          {activeTab === 'bind' && selectedCatalog && (
            <CatalogComponentBind
              catalogId={selectedCatalog.id!}
              catalogName={selectedCatalog.catalogName}
              onClose={() => {
                setActiveTab('pool');
                setSelectedCatalog(null);
              }}
            />
          )}
        </div>
      </div>

      {/* Catalog 新增/编辑弹窗 */}
      <CatalogFormModal
        visible={formModalVisible}
        data={editData}
        onClose={() => setFormModalVisible(false)}
        onSuccess={() => {
          setFormModalVisible(false);
          void loadData();
        }}
      />
    </div>
  );
};

export default CatalogPage;
```

- [ ] **Step 2: 创建主页样式**

```less
.catalogPage {
  display: flex;
  gap: 12px;
  height: 100%;
  min-height: 0;
  background: var(--background-color);
  padding: 16px;
}

.leftPanel {
  width: 55%;
  display: flex;
  flex-direction: column;
  background: var(--surface-color);
  border-radius: 6px;
  border: 1px solid var(--border-color);
  overflow: hidden;
}

.rightPanel {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: var(--surface-color);
  border-radius: 6px;
  border: 1px solid var(--border-color);
  overflow: hidden;
}

.panelHeader {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid var(--border-color);
}

.panelTitle {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-color);
}

.tabBar {
  display: flex;
  gap: 8px;
  padding: 12px 16px;
  border-bottom: 1px solid var(--border-color);
}

.tabContent {
  flex: 1;
  min-height: 0;
  overflow: auto;
}

.actionColumn {
  display: flex;
  align-items: center;
  gap: 4px;
}
```

- [ ] **Step 3: 提交**

```bash
git add web/apps/gwsu-sub-security/src/pages/catalog/index.tsx \
        web/apps/gwsu-sub-security/src/pages/catalog/index.module.less
git commit -m "feat(catalog): 添加 Catalog 管理主页"
```

---

## Task 13: 构建验证

- [ ] **Step 1: 后端编译验证**

Run: `cd /Users/quyq/Documents/work/personal/gwsu-basic && mvn clean compile -pl business/business-security/business-security-server -am -DskipTests`

Expected: BUILD SUCCESS

- [ ] **Step 2: 前端编译验证**

Run: `cd /Users/quyq/Documents/work/personal/gwsu-basic/web && pnpm --filter gwsu-sub-security build 2>&1 | tail -20`

Expected: 构建成功无报错

- [ ] **Step 3: 修复编译问题（如有）**

根据编译错误修复代码

- [ ] **Step 4: 最终提交**

```bash
git add -A
git commit -m "feat(catalog): Catalog 管理功能实现完成"
```
