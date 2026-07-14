-- =============================================
-- 用户表
-- 存储用户核心信息，与登录方式无关
-- =============================================
CREATE TABLE sys_user (
    id              VARCHAR(24) PRIMARY KEY,                        -- 主键ID（雪花算法）
    username        VARCHAR(50) NOT NULL,                      -- 用户名（唯一，用于显示）
    nickname        VARCHAR(50),                               -- 昵称
    avatar          VARCHAR(500),                              -- 头像URL
    email           VARCHAR(100),                              -- 邮箱
    phone           VARCHAR(20),                               -- 手机号
    gender          SMALLINT DEFAULT 0,                        -- 性别：0-未知 1-男 2-女
    status          SMALLINT DEFAULT 1,                        -- 状态：0-禁用 1-正常
    last_login_time TIMESTAMP,                                 -- 最后登录时间
    last_login_ip   VARCHAR(50),                               -- 最后登录IP
    tenant_id       VARCHAR(50),                               -- 租户ID
    create_op       VARCHAR(50),                               -- 创建人
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,       -- 创建时间
    modify_op       VARCHAR(50),                               -- 修改人
    modify_time     TIMESTAMP,                                 -- 修改时间
    deleted         INT2 DEFAULT 0,                     -- 删除标识
    delete_op       VARCHAR(50),                               -- 删除人
    delete_time     TIMESTAMP                                  -- 删除时间
);

-- 用户表注释
COMMENT ON TABLE sys_user IS '用户表';

-- 字段注释
COMMENT ON COLUMN sys_user.id IS '主键ID（雪花算法）';
COMMENT ON COLUMN sys_user.username IS '用户名（唯一，用于显示）';
COMMENT ON COLUMN sys_user.nickname IS '昵称';
COMMENT ON COLUMN sys_user.avatar IS '头像URL';
COMMENT ON COLUMN sys_user.email IS '邮箱';
COMMENT ON COLUMN sys_user.phone IS '手机号';
COMMENT ON COLUMN sys_user.gender IS '性别：0-未知 1-男 2-女';
COMMENT ON COLUMN sys_user.status IS '状态：0-禁用 1-正常';
COMMENT ON COLUMN sys_user.last_login_time IS '最后登录时间';
COMMENT ON COLUMN sys_user.last_login_ip IS '最后登录IP';
COMMENT ON COLUMN sys_user.tenant_id IS '租户ID';
COMMENT ON COLUMN sys_user.create_op IS '创建人';
COMMENT ON COLUMN sys_user.create_time IS '创建时间';
COMMENT ON COLUMN sys_user.modify_op IS '修改人';
COMMENT ON COLUMN sys_user.modify_time IS '修改时间';
COMMENT ON COLUMN sys_user.deleted IS '删除标识';
COMMENT ON COLUMN sys_user.delete_op IS '删除人';
COMMENT ON COLUMN sys_user.delete_time IS '删除时间';

-- 索引
CREATE UNIQUE INDEX idx_sys_user_username ON sys_user(username) WHERE deleted = 0;
CREATE INDEX idx_sys_user_phone ON sys_user(phone) WHERE deleted = 0;
CREATE INDEX idx_sys_user_email ON sys_user(email) WHERE deleted = 0;
CREATE INDEX idx_sys_user_tenant ON sys_user(tenant_id);


-- =============================================
-- 账号表
-- 存储登录凭证，支持多种登录方式
-- identity_type 与 LoginHandler.loginType() 返回值匹配
-- =============================================
CREATE TABLE sys_account (
    id              VARCHAR(24) PRIMARY KEY,                        -- 主键ID（雪花算法）
    user_id         VARCHAR(24) NOT NULL,                           -- 关联用户ID
    identity_type   VARCHAR(30) NOT NULL,                      -- 登录类型，与LoginHandler.loginType()匹配
    identifier      VARCHAR(100) NOT NULL,                     -- 登录标识（用户名/手机号/邮箱/OpenID等）
    credential      VARCHAR(500),                              -- 凭证（密码hash/token等）
    status          SMALLINT DEFAULT 1,                        -- 状态：0-禁用 1-正常
    verified        INT2 DEFAULT 0,                     -- 是否已验证
    verified_time   TIMESTAMP,                                 -- 验证时间
    bind_time       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,       -- 绑定时间
    tenant_id       VARCHAR(50),                               -- 租户ID
    create_op       VARCHAR(50),                               -- 创建人
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,       -- 创建时间
    modify_op       VARCHAR(50),                               -- 修改人
    modify_time     TIMESTAMP,                                 -- 修改时间
    deleted         INT2 DEFAULT 0,                     -- 删除标识
    delete_op       VARCHAR(50),                               -- 删除人
    delete_time     TIMESTAMP,                                 -- 删除时间
    CONSTRAINT fk_account_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
);

-- 账号表注释
COMMENT ON TABLE sys_account IS '账号表-存储登录凭证，支持多种登录方式';

-- 字段注释
COMMENT ON COLUMN sys_account.id IS '主键ID（雪花算法）';
COMMENT ON COLUMN sys_account.user_id IS '关联用户ID';
COMMENT ON COLUMN sys_account.identity_type IS '登录类型，与LoginHandler.loginType()匹配（如：password/phone/wechat）';
COMMENT ON COLUMN sys_account.identifier IS '登录标识（用户名/手机号/邮箱/OpenID等）';
COMMENT ON COLUMN sys_account.credential IS '凭证（密码hash/token等）';
COMMENT ON COLUMN sys_account.status IS '状态：0-禁用 1-正常';
COMMENT ON COLUMN sys_account.verified IS '是否已验证';
COMMENT ON COLUMN sys_account.verified_time IS '验证时间';
COMMENT ON COLUMN sys_account.bind_time IS '绑定时间';
COMMENT ON COLUMN sys_account.tenant_id IS '租户ID';
COMMENT ON COLUMN sys_account.create_op IS '创建人';
COMMENT ON COLUMN sys_account.create_time IS '创建时间';
COMMENT ON COLUMN sys_account.modify_op IS '修改人';
COMMENT ON COLUMN sys_account.modify_time IS '修改时间';
COMMENT ON COLUMN sys_account.deleted IS '删除标识';
COMMENT ON COLUMN sys_account.delete_op IS '删除人';
COMMENT ON COLUMN sys_account.delete_time IS '删除时间';

-- 索引
CREATE UNIQUE INDEX idx_sys_account_identity ON sys_account(identity_type, identifier) WHERE deleted = 0;
CREATE INDEX idx_sys_account_user ON sys_account(user_id) WHERE deleted = 0;
-- 外键
ALTER TABLE sys_account add CONSTRAINT account_userid_fk FOREIGN key (user_id) REFERENCES sys_user(id) ON DELETE CASCADE ON UPDATE CASCADE;


-- =============================================
-- API_KEY 表
-- 存储用户创建的持久访问凭证
-- =============================================
CREATE TABLE sys_api_key (
    id              VARCHAR(24) PRIMARY KEY,
    user_id         VARCHAR(24) NOT NULL,
    api_key_name    VARCHAR(128) NOT NULL,
    api_key_hash    CHAR(64) NOT NULL,
    hash_version    INT2 NOT NULL DEFAULT 1,
    masked_key      VARCHAR(512) NOT NULL,
    status          SMALLINT NOT NULL DEFAULT 1,
    expire_time     TIMESTAMP,
    last_used_time  TIMESTAMP,
    last_used_ip    VARCHAR(64),
    remark          VARCHAR(512),
    tenant_id       VARCHAR(50),
    create_op       VARCHAR(50),
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modify_op       VARCHAR(50),
    modify_time     TIMESTAMP,
    deleted         INT2 DEFAULT 0,
    delete_op       VARCHAR(50),
    delete_time     TIMESTAMP
);

COMMENT ON TABLE sys_api_key IS 'API_KEY 表';
COMMENT ON COLUMN sys_api_key.id IS '主键ID（雪花算法）';
COMMENT ON COLUMN sys_api_key.user_id IS '所属用户ID';
COMMENT ON COLUMN sys_api_key.api_key_name IS 'API_KEY 名称';
COMMENT ON COLUMN sys_api_key.api_key_hash IS 'API_KEY 不可逆摘要值（HMAC-SHA256）';
COMMENT ON COLUMN sys_api_key.hash_version IS '摘要版本';
COMMENT ON COLUMN sys_api_key.masked_key IS '脱敏后的 API_KEY';
COMMENT ON COLUMN sys_api_key.status IS '状态：0-停用 1-启用';
COMMENT ON COLUMN sys_api_key.expire_time IS '过期时间，为空表示永不过期';
COMMENT ON COLUMN sys_api_key.last_used_time IS '最近使用时间';
COMMENT ON COLUMN sys_api_key.last_used_ip IS '最近使用IP';
COMMENT ON COLUMN sys_api_key.remark IS '备注';

CREATE INDEX idx_sys_api_key_user_id ON sys_api_key(user_id) WHERE deleted = 0;
CREATE UNIQUE INDEX uk_sys_api_key_hash ON sys_api_key(api_key_hash);
CREATE INDEX idx_sys_api_key_status ON sys_api_key(status) WHERE deleted = 0;
CREATE INDEX idx_sys_api_key_expire_time ON sys_api_key(expire_time) WHERE deleted = 0;
ALTER TABLE sys_api_key add CONSTRAINT api_key_userid_fk FOREIGN key (user_id) REFERENCES sys_user(id) ON DELETE CASCADE ON UPDATE CASCADE;


-- =============================================
-- 部门表
-- 支持矩阵式组织结构
-- =============================================
CREATE TABLE sys_dept (
    id              VARCHAR(24) PRIMARY KEY,
    name            VARCHAR(50) NOT NULL,
    type            SMALLINT NOT NULL,
    parent_id       VARCHAR(24),
    enabled         INT2 DEFAULT 1,
    sort            INT DEFAULT 0,
    path            VARCHAR(500),
    tenant_id       VARCHAR(50),
    create_op       VARCHAR(50),
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modify_op       VARCHAR(50),
    modify_time     TIMESTAMP,
    deleted         INT2 DEFAULT 0,
    delete_op       VARCHAR(50),
    delete_time     TIMESTAMP
);

COMMENT ON TABLE sys_dept IS '部门表-支持矩阵式组织结构';
COMMENT ON COLUMN sys_dept.id IS '主键ID（雪花算法）';
COMMENT ON COLUMN sys_dept.name IS '部门名称';
COMMENT ON COLUMN sys_dept.type IS '类型：1-公司 2-分公司 3-部门 4-小组 5-虚拟团队';
COMMENT ON COLUMN sys_dept.parent_id IS '主父部门ID（根部门为空）';
COMMENT ON COLUMN sys_dept.enabled IS '是否启用';
COMMENT ON COLUMN sys_dept.sort IS '排序号';
COMMENT ON COLUMN sys_dept.path IS '层级路径（如 /1/5/12/）';

CREATE INDEX idx_sys_dept_parent ON sys_dept(parent_id) WHERE deleted = 0;
CREATE INDEX idx_sys_dept_path ON sys_dept(path) WHERE deleted = 0;


-- =============================================
-- 额外父部门表
-- 存储除主父部门外的其他父部门（矩阵结构）
-- =============================================
CREATE TABLE sys_dept_parent (
    id              VARCHAR(24) PRIMARY KEY,
    dept_id         VARCHAR(24) NOT NULL,
    parent_id       VARCHAR(24) NOT NULL,
    tenant_id       VARCHAR(50),
    create_op       VARCHAR(50),
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modify_op       VARCHAR(50),
    modify_time     TIMESTAMP,
    deleted         INT2 DEFAULT 0,
    delete_op       VARCHAR(50),
    delete_time     TIMESTAMP
);

COMMENT ON TABLE sys_dept_parent IS '额外父部门表-矩阵结构';
COMMENT ON COLUMN sys_dept_parent.id IS '主键ID（雪花算法）';
COMMENT ON COLUMN sys_dept_parent.dept_id IS '部门ID';
COMMENT ON COLUMN sys_dept_parent.parent_id IS '额外父部门ID';

CREATE UNIQUE INDEX idx_sys_dept_parent_unique ON sys_dept_parent(dept_id, parent_id) WHERE deleted = 0;
CREATE INDEX idx_sys_dept_parent_parent ON sys_dept_parent(parent_id) WHERE deleted = 0;
-- 外键
ALTER TABLE sys_dept_parent add CONSTRAINT dept_parent_deptid_fk FOREIGN key (dept_id) REFERENCES sys_dept(id) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE sys_dept_parent add CONSTRAINT dept_parent_parentid_fk FOREIGN key (parent_id) REFERENCES sys_dept(id) ON DELETE CASCADE ON UPDATE CASCADE;


-- =============================================
-- 用户部门关联表
-- 用户与部门多对多关系
-- =============================================
CREATE TABLE sys_user_dept (
    id              VARCHAR(24) PRIMARY KEY,
    user_id         VARCHAR(24) NOT NULL,
    dept_id         VARCHAR(24) NOT NULL,
    is_primary      INT2 DEFAULT 0,
    tenant_id       VARCHAR(50),
    create_op       VARCHAR(50),
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modify_op       VARCHAR(50),
    modify_time     TIMESTAMP,
    deleted         INT2 DEFAULT 0,
    delete_op       VARCHAR(50),
    delete_time     TIMESTAMP
);

COMMENT ON TABLE sys_user_dept IS '用户部门关联表-用户与部门多对多关系';
COMMENT ON COLUMN sys_user_dept.id IS '主键ID（雪花算法）';
COMMENT ON COLUMN sys_user_dept.user_id IS '用户ID';
COMMENT ON COLUMN sys_user_dept.dept_id IS '部门ID';
COMMENT ON COLUMN sys_user_dept.is_primary IS '是否主部门';

CREATE UNIQUE INDEX idx_sys_user_dept_unique ON sys_user_dept(user_id, dept_id) WHERE deleted = 0;
CREATE INDEX idx_sys_user_dept_dept ON sys_user_dept(dept_id) WHERE deleted = 0;
CREATE INDEX idx_sys_user_dept_primary ON sys_user_dept(user_id, is_primary) WHERE deleted = 0;
-- 外键
ALTER TABLE sys_user_dept add CONSTRAINT user_dept_userid_fk FOREIGN key (user_id) REFERENCES sys_user(id) ON DELETE CASCADE ON UPDATE CASCADE;
