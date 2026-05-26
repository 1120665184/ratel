-- =============================================
-- 用户表
-- 存储用户核心信息，与登录方式无关
-- =============================================
CREATE TABLE sys_user (
    id              VARCHAR(24) PRIMARY KEY COMMENT '主键ID（雪花算法）',
    username        VARCHAR(50) NOT NULL              COMMENT '用户名（唯一，用于显示）',
    nickname        VARCHAR(50)           DEFAULT NULL COMMENT '昵称',
    avatar          VARCHAR(500)          DEFAULT NULL COMMENT '头像URL',
    email           VARCHAR(100)          DEFAULT NULL COMMENT '邮箱',
    phone           VARCHAR(20)           DEFAULT NULL COMMENT '手机号',
    gender          SMALLINT   DEFAULT 0              COMMENT '性别：0-未知 1-男 2-女',
    status          SMALLINT   DEFAULT 1              COMMENT '状态：0-禁用 1-正常',
    last_login_time DATETIME              DEFAULT NULL COMMENT '最后登录时间',
    last_login_ip   VARCHAR(50)           DEFAULT NULL COMMENT '最后登录IP',
    tenant_id       VARCHAR(50)           DEFAULT NULL COMMENT '租户ID',
    create_op       VARCHAR(50)           DEFAULT NULL COMMENT '创建人',
    create_time     DATETIME   DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modify_op       VARCHAR(50)           DEFAULT NULL COMMENT '修改人',
    modify_time     DATETIME              DEFAULT NULL COMMENT '修改时间',
    deleted         SMALLINT   DEFAULT 0              COMMENT '删除标识',
    delete_op       VARCHAR(50)           DEFAULT NULL COMMENT '删除人',
    delete_time     DATETIME              DEFAULT NULL COMMENT '删除时间'
) COMMENT '用户表';

-- 索引
CREATE UNIQUE INDEX idx_sys_user_username ON sys_user(username);
CREATE INDEX idx_sys_user_phone ON sys_user(phone);
CREATE INDEX idx_sys_user_email ON sys_user(email);
CREATE INDEX idx_sys_user_tenant ON sys_user(tenant_id);


-- =============================================
-- 账号表
-- 存储登录凭证，支持多种登录方式
-- identity_type 与 LoginHandler.loginType() 返回值匹配
-- =============================================
CREATE TABLE sys_account (
    id              VARCHAR(24) PRIMARY KEY COMMENT '主键ID（雪花算法）',
    user_id         VARCHAR(24) NOT NULL              COMMENT '关联用户ID',
    identity_type   VARCHAR(30) NOT NULL              COMMENT '登录类型，与LoginHandler.loginType()匹配',
    identifier      VARCHAR(100) NOT NULL             COMMENT '登录标识（用户名/手机号/邮箱/OpenID等）',
    credential      VARCHAR(500)          DEFAULT NULL COMMENT '凭证（密码hash/token等）',
    status          SMALLINT   DEFAULT 1              COMMENT '状态：0-禁用 1-正常',
    verified        TINYINT(1) DEFAULT 0              COMMENT '是否已验证',
    verified_time   DATETIME              DEFAULT NULL COMMENT '验证时间',
    bind_time       DATETIME   DEFAULT CURRENT_TIMESTAMP COMMENT '绑定时间',
    tenant_id       VARCHAR(50)           DEFAULT NULL COMMENT '租户ID',
    create_op       VARCHAR(50)           DEFAULT NULL COMMENT '创建人',
    create_time     DATETIME   DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modify_op       VARCHAR(50)           DEFAULT NULL COMMENT '修改人',
    modify_time     DATETIME              DEFAULT NULL COMMENT '修改时间',
    deleted         SMALLINT   DEFAULT 0              COMMENT '删除标识',
    delete_op       VARCHAR(50)           DEFAULT NULL COMMENT '删除人',
    delete_time     DATETIME              DEFAULT NULL COMMENT '删除时间'
) COMMENT '账号表-存储登录凭证，支持多种登录方式';

-- 索引
CREATE UNIQUE INDEX idx_sys_account_identity ON sys_account(identity_type, identifier);
CREATE INDEX idx_sys_account_user ON sys_account(user_id);
-- 外键
ALTER TABLE sys_account ADD CONSTRAINT account_userid_fk FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE ON UPDATE CASCADE;


-- =============================================
-- 部门表
-- 支持矩阵式组织结构
-- =============================================
CREATE TABLE sys_dept (
    id              VARCHAR(24) PRIMARY KEY COMMENT '主键ID（雪花算法）',
    name            VARCHAR(50) NOT NULL              COMMENT '部门名称',
    type            SMALLINT   NOT NULL              COMMENT '类型：1-公司 2-分公司 3-部门 4-小组 5-虚拟团队',
    parent_id       VARCHAR(24)           DEFAULT NULL COMMENT '主父部门ID（根部门为空）',
    enabled         SMALLINT   DEFAULT 1              COMMENT '是否启用',
    sort            INT        DEFAULT 0              COMMENT '排序号',
    path            VARCHAR(500)          DEFAULT NULL COMMENT '层级路径（如 /1/5/12/）',
    tenant_id       VARCHAR(50)           DEFAULT NULL COMMENT '租户ID',
    create_op       VARCHAR(50)           DEFAULT NULL COMMENT '创建人',
    create_time     DATETIME   DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modify_op       VARCHAR(50)           DEFAULT NULL COMMENT '修改人',
    modify_time     DATETIME              DEFAULT NULL COMMENT '修改时间',
    deleted         SMALLINT   DEFAULT 0              COMMENT '删除标识',
    delete_op       VARCHAR(50)           DEFAULT NULL COMMENT '删除人',
    delete_time     DATETIME              DEFAULT NULL COMMENT '删除时间'
) COMMENT '部门表-支持矩阵式组织结构';

CREATE INDEX idx_sys_dept_parent ON sys_dept(parent_id);
CREATE INDEX idx_sys_dept_path ON sys_dept(path);


-- =============================================
-- 额外父部门表
-- 存储除主父部门外的其他父部门（矩阵结构）
-- =============================================
CREATE TABLE sys_dept_parent (
    id              VARCHAR(24) PRIMARY KEY COMMENT '主键ID（雪花算法）',
    dept_id         VARCHAR(24) NOT NULL              COMMENT '部门ID',
    parent_id       VARCHAR(24) NOT NULL              COMMENT '额外父部门ID',
    tenant_id       VARCHAR(50)           DEFAULT NULL COMMENT '租户ID',
    create_op       VARCHAR(50)           DEFAULT NULL COMMENT '创建人',
    create_time     DATETIME   DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modify_op       VARCHAR(50)           DEFAULT NULL COMMENT '修改人',
    modify_time     DATETIME              DEFAULT NULL COMMENT '修改时间',
    deleted         SMALLINT   DEFAULT 0              COMMENT '删除标识',
    delete_op       VARCHAR(50)           DEFAULT NULL COMMENT '删除人',
    delete_time     DATETIME              DEFAULT NULL COMMENT '删除时间'
) COMMENT '额外父部门表-矩阵结构';

CREATE UNIQUE INDEX idx_sys_dept_parent_unique ON sys_dept_parent(dept_id, parent_id);
CREATE INDEX idx_sys_dept_parent_parent ON sys_dept_parent(parent_id);
-- 外键
ALTER TABLE sys_dept_parent ADD CONSTRAINT dept_parent_deptid_fk FOREIGN KEY (dept_id) REFERENCES sys_dept(id) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE sys_dept_parent ADD CONSTRAINT dept_parent_parentid_fk FOREIGN KEY (parent_id) REFERENCES sys_dept(id) ON DELETE CASCADE ON UPDATE CASCADE;


-- =============================================
-- 用户部门关联表
-- 用户与部门多对多关系
-- =============================================
CREATE TABLE sys_user_dept (
    id              VARCHAR(24) PRIMARY KEY COMMENT '主键ID（雪花算法）',
    user_id         VARCHAR(24) NOT NULL              COMMENT '用户ID',
    dept_id         VARCHAR(24) NOT NULL              COMMENT '部门ID',
    is_primary      SMALLINT   DEFAULT 0              COMMENT '是否主部门',
    tenant_id       VARCHAR(50)           DEFAULT NULL COMMENT '租户ID',
    create_op       VARCHAR(50)           DEFAULT NULL COMMENT '创建人',
    create_time     DATETIME   DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modify_op       VARCHAR(50)           DEFAULT NULL COMMENT '修改人',
    modify_time     DATETIME              DEFAULT NULL COMMENT '修改时间',
    deleted         SMALLINT   DEFAULT 0              COMMENT '删除标识',
    delete_op       VARCHAR(50)           DEFAULT NULL COMMENT '删除人',
    delete_time     DATETIME              DEFAULT NULL COMMENT '删除时间'
) COMMENT '用户部门关联表-用户与部门多对多关系';

CREATE UNIQUE INDEX idx_sys_user_dept_unique ON sys_user_dept(user_id, dept_id);
CREATE INDEX idx_sys_user_dept_dept ON sys_user_dept(dept_id);
CREATE INDEX idx_sys_user_dept_primary ON sys_user_dept(user_id, is_primary);
-- 外键
ALTER TABLE sys_user_dept ADD CONSTRAINT user_dept_userid_fk FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE ON UPDATE CASCADE;
