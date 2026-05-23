-- =============================================
-- Catalog管理相关表结构
-- 数据库：MySQL
-- =============================================

-- =============================================
-- 表名：security_catalog
-- 说明：Catalog定义表
-- =============================================
CREATE TABLE security_catalog
(
    id           VARCHAR(24)  PRIMARY KEY COMMENT '主键ID（雪花算法）',
    catalog_key  VARCHAR(100) NOT NULL COMMENT 'Catalog唯一标识',
    catalog_name VARCHAR(200) NOT NULL COMMENT 'Catalog名称',
    description  VARCHAR(500)          DEFAULT NULL COMMENT 'Catalog描述',
    version      VARCHAR(20)           DEFAULT '1.0.0' COMMENT '版本号',
    active       SMALLINT              DEFAULT 0 COMMENT '激活状态：0-未激活 1-激活（全局唯一）',
    status       SMALLINT              DEFAULT 1 COMMENT '状态：0-禁用 1-正常',
    tenant_id    VARCHAR(50)           DEFAULT NULL COMMENT '租户ID',
    create_op    VARCHAR(50)           DEFAULT NULL COMMENT '创建人',
    create_time  DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modify_op    VARCHAR(50)           DEFAULT NULL COMMENT '修改人',
    modify_time  DATETIME              DEFAULT NULL COMMENT '修改时间',
    deleted      SMALLINT     NOT NULL DEFAULT 0 COMMENT '删除标识：0-未删除 1-已删除',
    delete_op    VARCHAR(50)           DEFAULT NULL COMMENT '删除人',
    delete_time  DATETIME              DEFAULT NULL COMMENT '删除时间',
    UNIQUE INDEX uk_catalog_key (catalog_key)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='Catalog定义表';

-- =============================================
-- 表名：security_catalog_component
-- 说明：Catalog组件配置表
-- =============================================
CREATE TABLE security_catalog_component
(
    id            VARCHAR(24)  PRIMARY KEY COMMENT '主键ID（雪花算法）',
    component_name VARCHAR(100) NOT NULL COMMENT '组件名（如 DataTable）',
    description   VARCHAR(500) NOT NULL COMMENT '组件描述（给AI看的）',
    props_schema  TEXT         NOT NULL COMMENT '组件属性JSON Schema定义',
    default_props TEXT                  DEFAULT NULL COMMENT '默认属性值',
    category      VARCHAR(50)           DEFAULT NULL COMMENT '组件分类（display/chart/form）',
    sort_order    INT                   DEFAULT 0 COMMENT '排序号',
    status        SMALLINT              DEFAULT 1 COMMENT '状态：0-禁用 1-正常',
    tenant_id     VARCHAR(50)           DEFAULT NULL COMMENT '租户ID',
    create_op     VARCHAR(50)           DEFAULT NULL COMMENT '创建人',
    create_time   DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modify_op     VARCHAR(50)           DEFAULT NULL COMMENT '修改人',
    modify_time   DATETIME              DEFAULT NULL COMMENT '修改时间',
    deleted       SMALLINT     NOT NULL DEFAULT 0 COMMENT '删除标识：0-未删除 1-已删除',
    delete_op     VARCHAR(50)           DEFAULT NULL COMMENT '删除人',
    delete_time   DATETIME              DEFAULT NULL COMMENT '删除时间',
    UNIQUE INDEX uk_component_name (component_name)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='Catalog组件配置表';

-- =============================================
-- 表名：security_catalog_component_ref
-- 说明：Catalog与组件关联表
-- =============================================
CREATE TABLE security_catalog_component_ref
(
    id           VARCHAR(24) PRIMARY KEY COMMENT '主键ID（雪花算法）',
    catalog_id   VARCHAR(24) NOT NULL COMMENT 'Catalog ID',
    component_id VARCHAR(24) NOT NULL COMMENT '组件ID',
    sort_order   INT                  DEFAULT 0 COMMENT '排序号',
    tenant_id    VARCHAR(50)          DEFAULT NULL COMMENT '租户ID',
    create_op    VARCHAR(50)          DEFAULT NULL COMMENT '创建人',
    create_time  DATETIME             DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modify_op    VARCHAR(50)          DEFAULT NULL COMMENT '修改人',
    modify_time  DATETIME             DEFAULT NULL COMMENT '修改时间',
    deleted      SMALLINT    NOT NULL DEFAULT 0 COMMENT '删除标识：0-未删除 1-已删除',
    delete_op    VARCHAR(50)          DEFAULT NULL COMMENT '删除人',
    delete_time  DATETIME             DEFAULT NULL COMMENT '删除时间',
    UNIQUE INDEX uk_catalog_component (catalog_id, component_id),
    INDEX idx_catalog_id (catalog_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='Catalog与组件关联表';
