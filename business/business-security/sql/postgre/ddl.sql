-- =============================================
-- 菜单管理相关表结构
-- 数据库：PostgreSQL
-- =============================================

-- =============================================
-- 表名：security_menu
-- 说明：菜单表
-- =============================================
CREATE TABLE security_menu
(
    id          VARCHAR(24) PRIMARY KEY,
    parent_id   VARCHAR(24)               DEFAULT NULL,
    menu_name   VARCHAR(50) NOT NULL,
    menu_type   SMALLINT    NOT NULL DEFAULT 1,
    sort        INT         NOT NULL DEFAULT 0,
    icon        VARCHAR(100)         DEFAULT NULL,
    path        VARCHAR(200)         DEFAULT NULL,
    micro_app   VARCHAR(50)          DEFAULT NULL,
    visible     SMALLINT    NOT NULL DEFAULT 1,
    status      SMALLINT    NOT NULL DEFAULT 1,
    permission  VARCHAR(100)         DEFAULT NULL,
    position    INT                  DEFAULT NULL,
    owner       INT                  DEFAULT NULL,
    button_key  VARCHAR(100)         DEFAULT NULL,
    description VARCHAR(1024)         DEFAULT NULL,
    tenant_id   VARCHAR(50)          DEFAULT NULL,
    create_op   VARCHAR(50)          DEFAULT NULL,
    create_time TIMESTAMP            DEFAULT CURRENT_TIMESTAMP,
    modify_op   VARCHAR(50)          DEFAULT NULL,
    modify_time TIMESTAMP            DEFAULT NULL,
    deleted     INT2        NOT NULL DEFAULT 0,
    delete_op   VARCHAR(50)          DEFAULT NULL,
    delete_time TIMESTAMP            DEFAULT NULL
);

-- 表和字段注释
COMMENT ON TABLE security_menu IS '菜单表';
COMMENT ON COLUMN security_menu.id IS '主键ID';
COMMENT ON COLUMN security_menu.parent_id IS '父菜单ID，NULL表示顶级菜单';
COMMENT ON COLUMN security_menu.menu_name IS '菜单名称';
COMMENT ON COLUMN security_menu.menu_type IS '菜单类型：1-目录 2-菜单 3-按钮';
COMMENT ON COLUMN security_menu.sort IS '排序号';
COMMENT ON COLUMN security_menu.icon IS '菜单图标';
COMMENT ON COLUMN security_menu.path IS '路由路径';
COMMENT ON COLUMN security_menu.micro_app IS '子应用名称';
COMMENT ON COLUMN security_menu.visible IS '是否显示：0-隐藏 1-显示';
COMMENT ON COLUMN security_menu.status IS '状态：0-禁用 1-正常';
COMMENT ON COLUMN security_menu.permission IS '权限标识';
COMMENT ON COLUMN security_menu.position IS '菜单位置类型：1-侧边栏 2-顶部栏';
COMMENT ON COLUMN security_menu.owner IS '菜单所属类型：1-后端管理 2-移动端APP';
COMMENT ON COLUMN security_menu.button_key IS '按钮标识，格式：菜单ID_标识，用于前端按钮显示控制';
COMMENT ON COLUMN security_menu.description IS '功能描述，用于AI提示词构建';
COMMENT ON COLUMN security_menu.tenant_id IS '租户ID';
COMMENT ON COLUMN security_menu.create_op IS '创建人';
COMMENT ON COLUMN security_menu.create_time IS '创建时间';
COMMENT ON COLUMN security_menu.modify_op IS '修改人';
COMMENT ON COLUMN security_menu.modify_time IS '修改时间';
COMMENT ON COLUMN security_menu.deleted IS '删除标识：0-未删除 1-已删除';
COMMENT ON COLUMN security_menu.delete_op IS '删除人';
COMMENT ON COLUMN security_menu.delete_time IS '删除时间';

-- 索引
CREATE INDEX idx_security_menu_parent_id ON security_menu (parent_id) WHERE deleted = 0;
CREATE INDEX idx_security_menu_sort ON security_menu (sort);
CREATE INDEX idx_security_menu_type ON security_menu (menu_type);

-- =============================================
-- 表名：security_role
-- 说明：角色表
-- =============================================
CREATE TABLE security_role
(
    id          VARCHAR(24) PRIMARY KEY,
    role_name   VARCHAR(50) NOT NULL,
    role_code   VARCHAR(50) NOT NULL,
    sort        INT         NOT NULL DEFAULT 0,
    description VARCHAR(200)         DEFAULT NULL,
    status      SMALLINT    NOT NULL DEFAULT 1,
    tenant_id   VARCHAR(50)          DEFAULT NULL,
    create_op   VARCHAR(50)          DEFAULT NULL,
    create_time TIMESTAMP            DEFAULT CURRENT_TIMESTAMP,
    modify_op   VARCHAR(50)          DEFAULT NULL,
    modify_time TIMESTAMP            DEFAULT NULL,
    deleted     INT2        NOT NULL DEFAULT 0,
    delete_op   VARCHAR(50)          DEFAULT NULL,
    delete_time TIMESTAMP            DEFAULT NULL
);

-- 表和字段注释
COMMENT ON TABLE security_role IS '角色表';
COMMENT ON COLUMN security_role.id IS '主键ID';
COMMENT ON COLUMN security_role.role_name IS '角色名称';
COMMENT ON COLUMN security_role.role_code IS '角色编码';
COMMENT ON COLUMN security_role.sort IS '排序号';
COMMENT ON COLUMN security_role.description IS '角色描述';
COMMENT ON COLUMN security_role.status IS '状态：0-禁用 1-正常';
COMMENT ON COLUMN security_role.tenant_id IS '租户ID';
COMMENT ON COLUMN security_role.create_op IS '创建人';
COMMENT ON COLUMN security_role.create_time IS '创建时间';
COMMENT ON COLUMN security_role.modify_op IS '修改人';
COMMENT ON COLUMN security_role.modify_time IS '修改时间';
COMMENT ON COLUMN security_role.deleted IS '删除标识：0-未删除 1-已删除';
COMMENT ON COLUMN security_role.delete_op IS '删除人';
COMMENT ON COLUMN security_role.delete_time IS '删除时间';

-- 唯一索引
CREATE UNIQUE INDEX uk_security_role_code ON security_role (role_code) WHERE deleted = 0;

-- =============================================
-- 表名：security_role_subject
-- 说明：主体角色关联表
-- =============================================
CREATE TABLE security_role_subject
(
    id          VARCHAR(24) PRIMARY KEY,
    subject_id  VARCHAR(24) NOT NULL,
    role_id     VARCHAR(24) NOT NULL,
    tenant_id   VARCHAR(50) DEFAULT NULL,
    create_op   VARCHAR(50) DEFAULT NULL,
    create_time TIMESTAMP   DEFAULT CURRENT_TIMESTAMP
);

-- 表和字段注释
COMMENT ON TABLE security_role_subject IS '主体角色关联表';
COMMENT ON COLUMN security_role_subject.id IS '主键ID';
COMMENT ON COLUMN security_role_subject.subject_id IS '主体ID（用户ID）';
COMMENT ON COLUMN security_role_subject.role_id IS '角色ID';
COMMENT ON COLUMN security_role_subject.tenant_id IS '租户ID';
COMMENT ON COLUMN security_role_subject.create_op IS '创建人';
COMMENT ON COLUMN security_role_subject.create_time IS '创建时间';

-- 索引
CREATE UNIQUE INDEX uk_security_role_subject ON security_role_subject (subject_id, role_id);
CREATE INDEX idx_security_role_subject_id ON security_role_subject (role_id);

-- =============================================
-- 表名：security_role_menu
-- 说明：角色菜单关联表
-- =============================================
CREATE TABLE security_role_menu
(
    id                 VARCHAR(24) PRIMARY KEY,
    role_id            VARCHAR(24) NOT NULL,
    menu_id            VARCHAR(24) NOT NULL,
    abac_permission_id VARCHAR(24)      DEFAULT NULL,
    tenant_id          VARCHAR(50) DEFAULT NULL,
    create_op          VARCHAR(50) DEFAULT NULL,
    create_time        TIMESTAMP   DEFAULT CURRENT_TIMESTAMP
);

-- 表和字段注释
COMMENT ON TABLE security_role_menu IS '角色菜单关联表';
COMMENT ON COLUMN security_role_menu.id IS '主键ID';
COMMENT ON COLUMN security_role_menu.role_id IS '角色ID';
COMMENT ON COLUMN security_role_menu.menu_id IS '菜单ID';
COMMENT ON COLUMN security_role_menu.abac_permission_id IS 'ABAC接口权限ID，关联security_abac_permission表';
COMMENT ON COLUMN security_role_menu.tenant_id IS '租户ID';
COMMENT ON COLUMN security_role_menu.create_op IS '创建人';
COMMENT ON COLUMN security_role_menu.create_time IS '创建时间';

-- 索引
CREATE UNIQUE INDEX uk_security_role_menu ON security_role_menu (role_id, menu_id);
CREATE INDEX idx_security_role_menu_id ON security_role_menu (menu_id);
CREATE INDEX idx_security_role_menu_abac ON security_role_menu (abac_permission_id);

-- =============================================
-- 表名：security_abac
-- 说明：ABAC表达式表
-- =============================================
CREATE TABLE security_abac
(
    id          VARCHAR(24) PRIMARY KEY,
    expression  VARCHAR(500) NOT NULL,
    description VARCHAR(200)          DEFAULT NULL,
    status      INT2         NOT NULL DEFAULT 1,
    tenant_id   VARCHAR(50)           DEFAULT NULL,
    create_op   VARCHAR(50)           DEFAULT NULL,
    create_time TIMESTAMP             DEFAULT CURRENT_TIMESTAMP,
    modify_op   VARCHAR(50)           DEFAULT NULL,
    modify_time TIMESTAMP             DEFAULT NULL,
    deleted     INT2         NOT NULL DEFAULT 0,
    delete_op   VARCHAR(50)           DEFAULT NULL,
    delete_time TIMESTAMP             DEFAULT NULL
);

-- 表和字段注释
COMMENT ON TABLE security_abac IS 'ABAC表达式表';
COMMENT ON COLUMN security_abac.id IS '主键ID';
COMMENT ON COLUMN security_abac.expression IS '表达式内容';
COMMENT ON COLUMN security_abac.description IS '表达式描述';
COMMENT ON COLUMN security_abac.status IS '启用状态：0-禁用 1-启用';
COMMENT ON COLUMN security_abac.tenant_id IS '租户ID';
COMMENT ON COLUMN security_abac.create_op IS '创建人';
COMMENT ON COLUMN security_abac.create_time IS '创建时间';
COMMENT ON COLUMN security_abac.modify_op IS '修改人';
COMMENT ON COLUMN security_abac.modify_time IS '修改时间';
COMMENT ON COLUMN security_abac.deleted IS '删除标识：0-未删除 1-已删除';
COMMENT ON COLUMN security_abac.delete_op IS '删除人';
COMMENT ON COLUMN security_abac.delete_time IS '删除时间';

-- 索引
CREATE INDEX idx_security_abac_expression ON security_abac (expression);

-- =============================================
-- 表名：security_abac_permission
-- 说明：ABAC接口权限关联表
-- =============================================
CREATE TABLE security_abac_permission
(
    id            VARCHAR(24) PRIMARY KEY,
    abac_id       VARCHAR(24)       NOT NULL,
    resource_type VARCHAR(50)  NOT NULL,
    action        VARCHAR(50)  NOT NULL,
    url_pattern   VARCHAR(500) NOT NULL,
    effect        VARCHAR(20)  NOT NULL DEFAULT 'allow',
    status        INT2         NOT NULL DEFAULT 1,
    tenant_id     VARCHAR(50)           DEFAULT NULL,
    create_op     VARCHAR(50)           DEFAULT NULL,
    create_time   TIMESTAMP             DEFAULT CURRENT_TIMESTAMP,
    modify_op     VARCHAR(50)           DEFAULT NULL,
    modify_time   TIMESTAMP             DEFAULT NULL,
    deleted       INT2         NOT NULL DEFAULT 0,
    delete_op     VARCHAR(50)           DEFAULT NULL,
    delete_time   TIMESTAMP             DEFAULT NULL
);

-- 表和字段注释
COMMENT ON TABLE security_abac_permission IS 'ABAC接口权限关联表';
COMMENT ON COLUMN security_abac_permission.id IS '主键ID';
COMMENT ON COLUMN security_abac_permission.abac_id IS '表达式ID';
COMMENT ON COLUMN security_abac_permission.resource_type IS '资源类型';
COMMENT ON COLUMN security_abac_permission.action IS '操作';
COMMENT ON COLUMN security_abac_permission.url_pattern IS 'URL模式';
COMMENT ON COLUMN security_abac_permission.effect IS '效果：allow-允许 deny-拒绝';
COMMENT ON COLUMN security_abac_permission.status IS '状态：0-禁用 1-启用';
COMMENT ON COLUMN security_abac_permission.tenant_id IS '租户ID';
COMMENT ON COLUMN security_abac_permission.create_op IS '创建人';
COMMENT ON COLUMN security_abac_permission.create_time IS '创建时间';
COMMENT ON COLUMN security_abac_permission.modify_op IS '修改人';
COMMENT ON COLUMN security_abac_permission.modify_time IS '修改时间';
COMMENT ON COLUMN security_abac_permission.deleted IS '删除标识：0-未删除 1-已删除';
COMMENT ON COLUMN security_abac_permission.delete_op IS '删除人';
COMMENT ON COLUMN security_abac_permission.delete_time IS '删除时间';

-- 索引
CREATE INDEX idx_security_abac_permission_abac_id ON security_abac_permission (abac_id);
CREATE INDEX idx_security_abac_permission_resource_type ON security_abac_permission (resource_type);
CREATE INDEX idx_security_abac_permission_url_pattern ON security_abac_permission (url_pattern);

-- =============================================
-- 表名：security_abac_field
-- 说明：ABAC字段权限关联表
-- =============================================
CREATE TABLE security_abac_field
(
    id            VARCHAR(24) PRIMARY KEY,
    abac_id       VARCHAR(24)       NOT NULL,
    resource_type VARCHAR(50)  NOT NULL,
    action        VARCHAR(50)  NOT NULL,
    url_pattern   VARCHAR(500) NOT NULL,
    field_mode    VARCHAR(20)  NOT NULL DEFAULT 'allow',
    fields        TEXT                  DEFAULT NULL,
    status        INT2         NOT NULL DEFAULT 1,
    tenant_id     VARCHAR(50)           DEFAULT NULL,
    create_op     VARCHAR(50)           DEFAULT NULL,
    create_time   TIMESTAMP             DEFAULT CURRENT_TIMESTAMP,
    modify_op     VARCHAR(50)           DEFAULT NULL,
    modify_time   TIMESTAMP             DEFAULT NULL,
    deleted       INT2         NOT NULL DEFAULT 0,
    delete_op     VARCHAR(50)           DEFAULT NULL,
    delete_time   TIMESTAMP             DEFAULT NULL
);

-- 表和字段注释
COMMENT ON TABLE security_abac_field IS 'ABAC字段权限关联表';
COMMENT ON COLUMN security_abac_field.id IS '主键ID';
COMMENT ON COLUMN security_abac_field.abac_id IS '表达式ID';
COMMENT ON COLUMN security_abac_field.resource_type IS '资源类型';
COMMENT ON COLUMN security_abac_field.action IS '操作';
COMMENT ON COLUMN security_abac_field.url_pattern IS 'URL';
COMMENT ON COLUMN security_abac_field.field_mode IS '字段模式：allow-允许 deny-拒绝';
COMMENT ON COLUMN security_abac_field.fields IS '字段列表，JSON数组格式';
COMMENT ON COLUMN security_abac_field.status IS '状态：0-禁用 1-启用';
COMMENT ON COLUMN security_abac_field.tenant_id IS '租户ID';
COMMENT ON COLUMN security_abac_field.create_op IS '创建人';
COMMENT ON COLUMN security_abac_field.create_time IS '创建时间';
COMMENT ON COLUMN security_abac_field.modify_op IS '修改人';
COMMENT ON COLUMN security_abac_field.modify_time IS '修改时间';
COMMENT ON COLUMN security_abac_field.deleted IS '删除标识：0-未删除 1-已删除';
COMMENT ON COLUMN security_abac_field.delete_op IS '删除人';
COMMENT ON COLUMN security_abac_field.delete_time IS '删除时间';

-- 索引
CREATE INDEX idx_security_abac_field_abac_id ON security_abac_field (abac_id);
CREATE INDEX idx_security_abac_field_resource_type ON security_abac_field (resource_type);
CREATE INDEX idx_security_abac_field_url_pattern ON security_abac_field (url_pattern);

-- =============================================
-- 表名：security_api_resource
-- 说明：接口资源表
-- =============================================
CREATE TABLE security_api_resource
(
    id                 VARCHAR(64) PRIMARY KEY,
    module_prefix      VARCHAR(50)           DEFAULT '',
    tag_name           VARCHAR(100)          DEFAULT '',
    req_path           VARCHAR(500) NOT NULL,
    req_method         VARCHAR(10)  NOT NULL,
    summary            VARCHAR(200)          DEFAULT '',
    request_class      VARCHAR(500)          DEFAULT '',
    response_class     VARCHAR(500)          DEFAULT '',
    class_name         VARCHAR(200)          DEFAULT NULL,
    method_name        VARCHAR(64)           DEFAULT NULL,
    login_allow_access INT2                  DEFAULT 0,
    tenant_id          VARCHAR(50)           DEFAULT NULL,
    create_op          VARCHAR(50)           DEFAULT NULL,
    create_time        TIMESTAMP             DEFAULT CURRENT_TIMESTAMP,
    modify_op          VARCHAR(50)           DEFAULT NULL,
    modify_time        TIMESTAMP             DEFAULT NULL,
    deleted            INT2         NOT NULL DEFAULT 0,
    delete_op          VARCHAR(50)           DEFAULT NULL,
    delete_time        TIMESTAMP             DEFAULT NULL
);

-- 表和字段注释
COMMENT ON TABLE security_api_resource IS '接口资源表';
COMMENT ON COLUMN security_api_resource.id IS '主键ID';
COMMENT ON COLUMN security_api_resource.module_prefix IS '模块前缀';
COMMENT ON COLUMN security_api_resource.tag_name IS 'Tag标签名称';
COMMENT ON COLUMN security_api_resource.req_path IS '接口地址';
COMMENT ON COLUMN security_api_resource.req_method IS '请求方式(GET/POST/PUT/DELETE等)';
COMMENT ON COLUMN security_api_resource.summary IS '接口摘要';
COMMENT ON COLUMN security_api_resource.request_class IS '请求参数类型全限定名';
COMMENT ON COLUMN security_api_resource.response_class IS '响应类型全限定名';
COMMENT ON COLUMN security_api_resource.class_name IS '类名';
COMMENT ON COLUMN security_api_resource.method_name IS '方法名';
COMMENT ON COLUMN security_api_resource.login_allow_access IS '登录后允许访问';
COMMENT ON COLUMN security_api_resource.tenant_id IS '租户ID';
COMMENT ON COLUMN security_api_resource.create_op IS '创建人';
COMMENT ON COLUMN security_api_resource.create_time IS '创建时间';
COMMENT ON COLUMN security_api_resource.modify_op IS '修改人';
COMMENT ON COLUMN security_api_resource.modify_time IS '修改时间';
COMMENT ON COLUMN security_api_resource.deleted IS '删除标识：0-未删除 1-已删除';
COMMENT ON COLUMN security_api_resource.delete_op IS '删除人';
COMMENT ON COLUMN security_api_resource.delete_time IS '删除时间';

-- 索引
CREATE INDEX idx_security_api_resource_module_prefix ON security_api_resource (module_prefix) WHERE deleted = 0;
CREATE INDEX idx_security_api_resource_req_path_method ON security_api_resource (req_path, req_method) WHERE deleted = 0;
CREATE INDEX idx_security_api_resource_tag_name ON security_api_resource (tag_name) WHERE deleted = 0;

-- =============================================
-- 表名：security_data_resource
-- 说明：数据资源配置主表
-- =============================================
CREATE TABLE security_data_resource
(
    id            VARCHAR(24) PRIMARY KEY,
    database_name VARCHAR(100)          DEFAULT NULL,
    table_name    VARCHAR(100) NOT NULL,
    description   VARCHAR(200)          DEFAULT NULL,
    status        INT2         NOT NULL DEFAULT 1,
    tenant_id     VARCHAR(50)           DEFAULT NULL,
    create_op     VARCHAR(50)           DEFAULT NULL,
    create_time   TIMESTAMP             DEFAULT CURRENT_TIMESTAMP,
    modify_op     VARCHAR(50)           DEFAULT NULL,
    modify_time   TIMESTAMP             DEFAULT NULL,
    deleted       INT2         NOT NULL DEFAULT 0,
    delete_op     VARCHAR(50)           DEFAULT NULL,
    delete_time   TIMESTAMP             DEFAULT NULL
);

-- 表和字段注释
COMMENT ON TABLE security_data_resource IS '数据资源配置主表';
COMMENT ON COLUMN security_data_resource.id IS '主键ID';
COMMENT ON COLUMN security_data_resource.database_name IS '库名，为空时匹配所有库';
COMMENT ON COLUMN security_data_resource.table_name IS '表名';
COMMENT ON COLUMN security_data_resource.description IS '规则描述';
COMMENT ON COLUMN security_data_resource.status IS '启用状态：0-禁用 1-启用';
COMMENT ON COLUMN security_data_resource.tenant_id IS '租户ID';
COMMENT ON COLUMN security_data_resource.create_op IS '创建人';
COMMENT ON COLUMN security_data_resource.create_time IS '创建时间';
COMMENT ON COLUMN security_data_resource.modify_op IS '修改人';
COMMENT ON COLUMN security_data_resource.modify_time IS '修改时间';
COMMENT ON COLUMN security_data_resource.deleted IS '删除标识：0-未删除 1-已删除';
COMMENT ON COLUMN security_data_resource.delete_op IS '删除人';
COMMENT ON COLUMN security_data_resource.delete_time IS '删除时间';

-- 索引
CREATE INDEX idx_security_data_resource_table_name ON security_data_resource (table_name) WHERE deleted = 0;
CREATE INDEX idx_security_data_resource_database_name ON security_data_resource (database_name) WHERE deleted = 0;

-- =============================================
-- 表名：security_data_resource_condition
-- 说明：数据资源字段条件配置表
-- =============================================
CREATE TABLE security_data_resource_condition
(
    id                   VARCHAR(24) PRIMARY KEY,
    data_resource_id     VARCHAR(24)       NOT NULL,
    field_name           VARCHAR(100) NOT NULL,
    show_null            INT2         NOT NULL DEFAULT 0,
    user_resource_fields VARCHAR(500)          DEFAULT NULL,
    assert_type          VARCHAR(20)  NOT NULL DEFAULT 'EQ',
    relationship         VARCHAR(10)  NOT NULL DEFAULT 'AND',
    sort                 INT          NOT NULL DEFAULT 0,
    tenant_id            VARCHAR(50)           DEFAULT NULL,
    create_op            VARCHAR(50)           DEFAULT NULL,
    create_time          TIMESTAMP             DEFAULT CURRENT_TIMESTAMP,
    modify_op            VARCHAR(50)           DEFAULT NULL,
    modify_time          TIMESTAMP             DEFAULT NULL,
    deleted              INT2         NOT NULL DEFAULT 0,
    delete_op            VARCHAR(50)           DEFAULT NULL,
    delete_time          TIMESTAMP             DEFAULT NULL
);

-- 表和字段注释
COMMENT ON TABLE security_data_resource_condition IS '数据资源字段条件配置表';
COMMENT ON COLUMN security_data_resource_condition.id IS '主键ID';
COMMENT ON COLUMN security_data_resource_condition.data_resource_id IS '数据资源配置ID';
COMMENT ON COLUMN security_data_resource_condition.field_name IS '字段名';
COMMENT ON COLUMN security_data_resource_condition.show_null IS '显示过滤字段为null的数据：0-不显示 1-显示';
COMMENT ON COLUMN security_data_resource_condition.user_resource_fields IS '关联的用户数据资源字段，多个用逗号分隔';
COMMENT ON COLUMN security_data_resource_condition.assert_type IS '断言类型：EQ-等于 LIKE-模糊匹配';
COMMENT ON COLUMN security_data_resource_condition.relationship IS '与上一个条件的关联关系：AND-与 OR-或';
COMMENT ON COLUMN security_data_resource_condition.sort IS '排序号';
COMMENT ON COLUMN security_data_resource_condition.tenant_id IS '租户ID';
COMMENT ON COLUMN security_data_resource_condition.create_op IS '创建人';
COMMENT ON COLUMN security_data_resource_condition.create_time IS '创建时间';
COMMENT ON COLUMN security_data_resource_condition.modify_op IS '修改人';
COMMENT ON COLUMN security_data_resource_condition.modify_time IS '修改时间';
COMMENT ON COLUMN security_data_resource_condition.deleted IS '删除标识：0-未删除 1-已删除';
COMMENT ON COLUMN security_data_resource_condition.delete_op IS '删除人';
COMMENT ON COLUMN security_data_resource_condition.delete_time IS '删除时间';

-- 索引
CREATE INDEX idx_security_data_resource_condition_data_resource_id ON security_data_resource_condition (data_resource_id);

-- =============================================
-- 表名：security_brain_sessions
-- 说明：存储智能大脑会话（Session）状态数据
-- =============================================

CREATE TABLE security_brain_sessions
(
    session_id VARCHAR(255) NOT NULL,
    state_key  VARCHAR(255) NOT NULL,
    item_index INT          NOT NULL DEFAULT 0,
    state_data TEXT         NOT NULL,
    user_id    VARCHAR(24)       DEFAULT NULL,
    created_at TIMESTAMP             DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP             DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (session_id, state_key, item_index)
);

-- 添加表注释
COMMENT ON TABLE security_brain_sessions IS '存储智能大脑会话（Session）状态数据，支持单值状态和列表状态';

-- 添加列注释
COMMENT ON COLUMN security_brain_sessions.session_id IS '会话标识符，与 state_key、item_index 共同组成主键，最大长度 255';
COMMENT ON COLUMN security_brain_sessions.state_key IS '状态键名，普通状态直接存储，列表状态会附加 ":_hash" 后缀用于存储哈希值';
COMMENT ON COLUMN security_brain_sessions.item_index IS '列表项索引，普通状态固定为 0，列表状态从 0 开始递增';
COMMENT ON COLUMN security_brain_sessions.state_data IS '序列化后的状态数据，格式为 JSON，使用 LONGTEXT/TEXT 存储';
COMMENT ON COLUMN security_brain_sessions.user_id IS '关联的用户ID';
COMMENT ON COLUMN security_brain_sessions.created_at IS '记录创建时间，默认为当前时间戳';
COMMENT ON COLUMN security_brain_sessions.updated_at IS '记录最后更新时间，默认与创建时间相同，建议通过触发器或应用层自动更新';

