-- =============================================
-- 菜单管理相关表结构
-- 数据库：MySQL
-- =============================================

-- =============================================
-- 表名：security_menu
-- 说明：菜单表
-- =============================================
CREATE TABLE security_menu
(
    id          VARCHAR(24) PRIMARY KEY COMMENT '主键ID',
    parent_id   VARCHAR(24)          DEFAULT NULL COMMENT '父菜单ID，NULL表示顶级菜单',
    menu_name   VARCHAR(50) NOT NULL COMMENT '菜单名称',
    menu_type   SMALLINT    NOT NULL DEFAULT 1 COMMENT '菜单类型：1-目录 2-菜单 3-按钮',
    sort        INT         NOT NULL DEFAULT 0 COMMENT '排序号',
    icon        VARCHAR(100)         DEFAULT NULL COMMENT '菜单图标',
    path        VARCHAR(200)         DEFAULT NULL COMMENT '路由路径',
    micro_app   VARCHAR(50)          DEFAULT NULL COMMENT '子应用名称',
    visible     SMALLINT    NOT NULL DEFAULT 1 COMMENT '是否显示：0-隐藏 1-显示',
    status      SMALLINT    NOT NULL DEFAULT 1 COMMENT '状态：0-禁用 1-正常',
    permission  VARCHAR(100)         DEFAULT NULL COMMENT '权限标识',
    button_key  VARCHAR(100)         DEFAULT NULL COMMENT '按钮标识，格式：菜单ID_标识，用于前端按钮显示控制',
    description VARCHAR(1024)         DEFAULT NULL COMMENT '功能描述，用于AI提示词构建',
    position    INT                  DEFAULT NULL COMMENT '菜单位置类型：1-侧边栏 2-顶部栏',
    owner       INT                  DEFAULT NULL COMMENT '菜单所属类型：1-后端管理 2-移动端APP',
    tenant_id   VARCHAR(50)          DEFAULT NULL COMMENT '租户ID',
    create_op   VARCHAR(50)          DEFAULT NULL COMMENT '创建人',
    create_time DATETIME             DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modify_op   VARCHAR(50)          DEFAULT NULL COMMENT '修改人',
    modify_time DATETIME             DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    deleted     SMALLINT    NOT NULL DEFAULT 0 COMMENT '删除标识：0-未删除 1-已删除',
    delete_op   VARCHAR(50)          DEFAULT NULL COMMENT '删除人',
    delete_time DATETIME             DEFAULT NULL COMMENT '删除时间',
    INDEX idx_parent_id (parent_id),
    INDEX idx_sort (sort),
    INDEX idx_menu_type (menu_type)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='菜单表';

-- =============================================
-- 表名：security_role
-- 说明：角色表
-- =============================================
CREATE TABLE security_role
(
    id          VARCHAR(24) PRIMARY KEY COMMENT '主键ID',
    role_name   VARCHAR(50) NOT NULL COMMENT '角色名称',
    role_code   VARCHAR(50) NOT NULL COMMENT '角色编码',
    sort        INT         NOT NULL DEFAULT 0 COMMENT '排序号',
    description VARCHAR(200)         DEFAULT NULL COMMENT '角色描述',
    status      SMALLINT    NOT NULL DEFAULT 1 COMMENT '状态：0-禁用 1-正常',
    tenant_id   VARCHAR(50)          DEFAULT NULL COMMENT '租户ID',
    create_op   VARCHAR(50)          DEFAULT NULL COMMENT '创建人',
    create_time DATETIME             DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modify_op   VARCHAR(50)          DEFAULT NULL COMMENT '修改人',
    modify_time DATETIME             DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    deleted     SMALLINT    NOT NULL DEFAULT 0 COMMENT '删除标识：0-未删除 1-已删除',
    delete_op   VARCHAR(50)          DEFAULT NULL COMMENT '删除人',
    delete_time DATETIME             DEFAULT NULL COMMENT '删除时间',
    UNIQUE INDEX uk_role_code (role_code)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='角色表';

-- =============================================
-- 表名：security_subject_role
-- 说明：主体角色关联表
-- =============================================
CREATE TABLE security_subject_role
(
    id          VARCHAR(24) PRIMARY KEY COMMENT '主键ID',
    subject_id  VARCHAR(24) NOT NULL COMMENT '主体ID（用户ID）',
    role_id     VARCHAR(24) NOT NULL COMMENT '角色ID',
    tenant_id   VARCHAR(50) DEFAULT NULL COMMENT '租户ID',
    create_op   VARCHAR(50) DEFAULT NULL COMMENT '创建人',
    create_time DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE INDEX uk_subject_role (subject_id, role_id),
    INDEX idx_role_id (role_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='主体角色关联表';

-- =============================================
-- 表名：security_role_menu
-- 说明：角色菜单关联表
-- =============================================
CREATE TABLE security_role_menu
(
    id                 VARCHAR(24) PRIMARY KEY COMMENT '主键ID',
    role_id            VARCHAR(24) NOT NULL COMMENT '角色ID',
    menu_id            VARCHAR(24) NOT NULL COMMENT '菜单ID',
    abac_permission_id VARCHAR(24)      DEFAULT NULL COMMENT 'ABAC接口权限ID，关联security_abac_permission表',
    tenant_id          VARCHAR(50) DEFAULT NULL COMMENT '租户ID',
    create_op          VARCHAR(50) DEFAULT NULL COMMENT '创建人',
    create_time        DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE INDEX uk_role_menu (role_id, menu_id),
    INDEX idx_menu_id (menu_id),
    INDEX idx_abac_permission_id (abac_permission_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='角色菜单关联表';

-- =============================================
-- 表名：security_abac
-- 说明：ABAC表达式表
-- =============================================
CREATE TABLE security_abac
(
    id          VARCHAR(24) PRIMARY KEY COMMENT '主键ID',
    expression  VARCHAR(500) NOT NULL COMMENT '表达式内容',
    description VARCHAR(200)          DEFAULT NULL COMMENT '表达式描述',
    status      TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '启用状态：0-禁用 1-启用',
    tenant_id   VARCHAR(50)           DEFAULT NULL COMMENT '租户ID',
    create_op   VARCHAR(50)           DEFAULT NULL COMMENT '创建人',
    create_time DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modify_op   VARCHAR(50)           DEFAULT NULL COMMENT '修改人',
    modify_time DATETIME              DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    deleted     TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '删除标识：0-未删除 1-已删除',
    delete_op   VARCHAR(50)           DEFAULT NULL COMMENT '删除人',
    delete_time DATETIME              DEFAULT NULL COMMENT '删除时间',
    INDEX idx_expression (expression)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='ABAC表达式表';

-- =============================================
-- 表名：security_abac_permission
-- 说明：ABAC接口权限关联表
-- =============================================
CREATE TABLE security_abac_permission
(
    id            VARCHAR(24) PRIMARY KEY COMMENT '主键ID',
    abac_id       VARCHAR(24)       NOT NULL COMMENT '表达式ID',
    resource_type VARCHAR(50)  NOT NULL COMMENT '资源类型',
    action        VARCHAR(50)  NOT NULL COMMENT '操作',
    url_pattern   VARCHAR(500) NOT NULL COMMENT 'URL模式',
    effect        VARCHAR(20)  NOT NULL DEFAULT 'allow' COMMENT '效果：allow-允许 deny-拒绝',
    status        TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
    tenant_id     VARCHAR(50)           DEFAULT NULL COMMENT '租户ID',
    create_op     VARCHAR(50)           DEFAULT NULL COMMENT '创建人',
    create_time   DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modify_op     VARCHAR(50)           DEFAULT NULL COMMENT '修改人',
    modify_time   DATETIME              DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    deleted       TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '删除标识：0-未删除 1-已删除',
    delete_op     VARCHAR(50)           DEFAULT NULL COMMENT '删除人',
    delete_time   DATETIME              DEFAULT NULL COMMENT '删除时间',
    INDEX idx_abac_id (abac_id),
    INDEX idx_resource_type (resource_type),
    INDEX idx_url_pattern (url_pattern)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='ABAC接口权限关联表';

-- =============================================
-- 表名：security_abac_field
-- 说明：ABAC字段权限关联表
-- =============================================
CREATE TABLE security_abac_field
(
    id            VARCHAR(24) PRIMARY KEY COMMENT '主键ID',
    abac_id       VARCHAR(24)       NOT NULL COMMENT '表达式ID',
    resource_type VARCHAR(50)  NOT NULL COMMENT '资源类型',
    action        VARCHAR(50)  NOT NULL COMMENT '操作',
    url_pattern   VARCHAR(500) NOT NULL COMMENT 'URL',
    field_mode    VARCHAR(20)  NOT NULL DEFAULT 'allow' COMMENT '字段模式：allow-允许 deny-拒绝',
    fields        TEXT                  DEFAULT NULL COMMENT '字段列表，JSON数组格式',
    status        TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
    tenant_id     VARCHAR(50)           DEFAULT NULL COMMENT '租户ID',
    create_op     VARCHAR(50)           DEFAULT NULL COMMENT '创建人',
    create_time   DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modify_op     VARCHAR(50)           DEFAULT NULL COMMENT '修改人',
    modify_time   DATETIME              DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    deleted       TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '删除标识：0-未删除 1-已删除',
    delete_op     VARCHAR(50)           DEFAULT NULL COMMENT '删除人',
    delete_time   DATETIME              DEFAULT NULL COMMENT '删除时间',
    INDEX idx_abac_id (abac_id),
    INDEX idx_resource_type (resource_type),
    INDEX idx_url_pattern (url_pattern)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='ABAC字段权限关联表';

-- =============================================
-- 表名：security_api_resource
-- 说明：接口资源表
-- =============================================
CREATE TABLE security_api_resource
(
    id                 VARCHAR(64) PRIMARY KEY COMMENT '主键ID',
    module_prefix      VARCHAR(50)           DEFAULT '' COMMENT '模块前缀',
    tag_name           VARCHAR(100)          DEFAULT '' COMMENT 'Tag标签名称',
    req_path           VARCHAR(500) NOT NULL COMMENT '接口地址',
    req_method         VARCHAR(10)  NOT NULL COMMENT '请求方式(GET/POST/PUT/DELETE等)',
    summary            VARCHAR(200)          DEFAULT '' COMMENT '接口摘要',
    request_class      VARCHAR(500)          DEFAULT '' COMMENT '请求参数类型全限定名',
    response_class     VARCHAR(500)          DEFAULT '' COMMENT '响应类型全限定名',
    class_name         VARCHAR(200)          DEFAULT NULL COMMENT '类名',
    method_name        VARCHAR(64)           DEFAULT NULL COMMENT '方法名',
    login_allow_access SMALLINT              DEFAULT 0 COMMENT '登录后允许访问',
    tenant_id          VARCHAR(50)           DEFAULT NULL COMMENT '租户ID',
    create_op          VARCHAR(50)           DEFAULT NULL COMMENT '创建人',
    create_time        DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modify_op          VARCHAR(50)           DEFAULT NULL COMMENT '修改人',
    modify_time        DATETIME              DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    deleted            SMALLINT     NOT NULL DEFAULT 0 COMMENT '删除标识：0-未删除 1-已删除',
    delete_op          VARCHAR(50)           DEFAULT NULL COMMENT '删除人',
    delete_time        DATETIME              DEFAULT NULL COMMENT '删除时间',
    INDEX idx_module_prefix (module_prefix),
    INDEX idx_req_path_method (req_path(100), req_method),
    INDEX idx_tag_name (tag_name)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='接口资源表';

-- =============================================
-- 表名：security_data_resource
-- 说明：数据资源配置主表
-- =============================================
CREATE TABLE security_data_resource
(
    id            VARCHAR(24) PRIMARY KEY COMMENT '主键ID',
    database_name VARCHAR(100)          DEFAULT NULL COMMENT '库名，为空时匹配所有库',
    table_name    VARCHAR(100) NOT NULL COMMENT '表名',
    description   VARCHAR(200)          DEFAULT NULL COMMENT '规则描述',
    status        TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '启用状态：0-禁用 1-启用',
    tenant_id     VARCHAR(50)           DEFAULT NULL COMMENT '租户ID',
    create_op     VARCHAR(50)           DEFAULT NULL COMMENT '创建人',
    create_time   DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modify_op     VARCHAR(50)           DEFAULT NULL COMMENT '修改人',
    modify_time   DATETIME              DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    deleted       TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '删除标识：0-未删除 1-已删除',
    delete_op     VARCHAR(50)           DEFAULT NULL COMMENT '删除人',
    delete_time   DATETIME              DEFAULT NULL COMMENT '删除时间',
    INDEX idx_table_name (table_name),
    INDEX idx_database_name (database_name)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='数据资源配置主表';

-- =============================================
-- 表名：security_data_resource_condition
-- 说明：数据资源字段条件配置表
-- =============================================
CREATE TABLE security_data_resource_condition
(
    id                   VARCHAR(24) PRIMARY KEY COMMENT '主键ID',
    data_resource_id     VARCHAR(24)       NOT NULL COMMENT '数据资源配置ID',
    field_name           VARCHAR(100) NOT NULL COMMENT '字段名',
    show_null            TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '显示过滤字段为null的数据：0-不显示 1-显示',
    user_resource_fields VARCHAR(500)          DEFAULT NULL COMMENT '关联的用户数据资源字段，多个用逗号分隔',
    assert_type          VARCHAR(20)  NOT NULL DEFAULT 'EQ' COMMENT '断言类型：EQ-等于 LIKE-模糊匹配',
    relationship         VARCHAR(10)  NOT NULL DEFAULT 'AND' COMMENT '与上一个条件的关联关系：AND-与 OR-或',
    sort                 INT          NOT NULL DEFAULT 0 COMMENT '排序号',
    tenant_id            VARCHAR(50)           DEFAULT NULL COMMENT '租户ID',
    create_op            VARCHAR(50)           DEFAULT NULL COMMENT '创建人',
    create_time          DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modify_op            VARCHAR(50)           DEFAULT NULL COMMENT '修改人',
    modify_time          DATETIME              DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    deleted              TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '删除标识：0-未删除 1-已删除',
    delete_op            VARCHAR(50)           DEFAULT NULL COMMENT '删除人',
    delete_time          DATETIME              DEFAULT NULL COMMENT '删除时间',
    INDEX idx_data_resource_id (data_resource_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='数据资源字段条件配置表';

-- =============================================
-- 表名：security_brain_sessions
-- 说明：存储智能大脑会话（Session）状态数据
-- =============================================
CREATE TABLE IF NOT EXISTS security_brain_sessions
(
    `session_id` VARCHAR(255) NOT NULL COMMENT '会话标识符，与 state_key、item_index 共同组成主键',
    `state_key`  VARCHAR(255) NOT NULL COMMENT '状态键名，普通状态直接存储，列表状态会附加 ":_hash" 后缀用于存储哈希值',
    `item_index` INT          NOT NULL DEFAULT 0 COMMENT '列表项索引，普通状态固定为 0，列表状态从 0 开始递增',
    `state_data` LONGTEXT     NOT NULL COMMENT '序列化后的状态数据，JSON 格式',
    `user_id`    VARCHAR(24)       DEFAULT NULL COMMENT '关联的用户ID',
    `created_at` DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间，自动生成',
    `updated_at` DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录最后更新时间，自动更新',
    PRIMARY KEY (`session_id`, `state_key`, `item_index`)
) DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci COMMENT = '存储智能大脑会话（Session）状态数据，支持单值状态和列表状态';

