-- =============================================
-- 文件管理相关表结构
-- 数据库：MySQL
-- =============================================

-- =============================================
-- 表名：kit_file_meta_info
-- 说明：文件元信息表
-- =============================================
CREATE TABLE kit_file_meta_info
(
    file_meta_id       VARCHAR(24)  PRIMARY KEY COMMENT '元文件ID',
    unique_id          VARCHAR(64)           DEFAULT NULL COMMENT '文件md5唯一值',
    upload_service_type VARCHAR(20)          DEFAULT NULL COMMENT '上传服务类型：MINIO-OSS-COS-LOCAL',
    file_size          VARCHAR(50)           DEFAULT NULL COMMENT '文件大小',
    media_type         VARCHAR(100)          DEFAULT NULL COMMENT '文件媒体类型',
    file_group         VARCHAR(64)           DEFAULT NULL COMMENT '文件组',
    file_url           VARCHAR(500)          DEFAULT NULL COMMENT '文件路径',
    tenant_id          VARCHAR(50)           DEFAULT NULL COMMENT '租户ID',
    create_op          VARCHAR(50)           DEFAULT NULL COMMENT '创建人',
    create_time        DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modify_op          VARCHAR(50)           DEFAULT NULL COMMENT '修改人',
    modify_time        DATETIME              DEFAULT NULL COMMENT '修改时间',
    deleted            SMALLINT     NOT NULL DEFAULT 0     COMMENT '删除标识：0-未删除 1-已删除',
    delete_op          VARCHAR(50)           DEFAULT NULL COMMENT '删除人',
    delete_time        DATETIME              DEFAULT NULL COMMENT '删除时间'
) COMMENT '文件元信息表';

-- 索引
CREATE UNIQUE INDEX uk_kit_file_meta_info_unique_id ON kit_file_meta_info (unique_id);
CREATE INDEX idx_kit_file_meta_info_upload_service_type ON kit_file_meta_info (upload_service_type);

-- =============================================
-- 表名：kit_file_info
-- 说明：文件信息表，类似文件超链接，多个链接引用同一个文件元信息
-- =============================================
CREATE TABLE kit_file_info
(
    file_id            VARCHAR(24)  PRIMARY KEY COMMENT '文件ID',
    file_meta_id       VARCHAR(24)           DEFAULT NULL COMMENT '元文件ID',
    file_name          VARCHAR(200)          DEFAULT NULL COMMENT '文件名',
    file_size          VARCHAR(50)           DEFAULT NULL COMMENT '文件大小',
    file_suffix        VARCHAR(20)           DEFAULT NULL COMMENT '文件后缀',
    disposable         SMALLINT     NOT NULL DEFAULT 0     COMMENT '是否为一次性文件：0-否 1-是',
    expired_time       DATETIME              DEFAULT NULL COMMENT '文件过期时间',
    scope              VARCHAR(20)  NOT NULL DEFAULT 'PROTECTED' COMMENT '文件作用域：PUBLIC-公共 PROTECTED-受保护 PRIVATE-私有',
    visitors           VARCHAR(500)          DEFAULT NULL COMMENT '作用域为PRIVATE时，允许访问的人员ID，多个逗号分隔',
    tenant_id          VARCHAR(50)           DEFAULT NULL COMMENT '租户ID',
    create_op          VARCHAR(50)           DEFAULT NULL COMMENT '创建人',
    create_time        DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modify_op          VARCHAR(50)           DEFAULT NULL COMMENT '修改人',
    modify_time        DATETIME              DEFAULT NULL COMMENT '修改时间',
    deleted            SMALLINT     NOT NULL DEFAULT 0     COMMENT '删除标识：0-未删除 1-已删除',
    delete_op          VARCHAR(50)           DEFAULT NULL COMMENT '删除人',
    delete_time        DATETIME              DEFAULT NULL COMMENT '删除时间'
) COMMENT '文件信息表';

-- 索引
CREATE INDEX idx_kit_file_info_file_meta_id ON kit_file_info (file_meta_id);
CREATE INDEX idx_kit_file_info_scope ON kit_file_info (scope);
-- 外键
ALTER TABLE kit_file_info ADD CONSTRAINT file_info_meta_fk FOREIGN KEY (file_meta_id) REFERENCES kit_file_meta_info(file_meta_id) ON DELETE CASCADE ON UPDATE CASCADE;

-- =============================================
-- 表名：kit_file_chunk_info
-- 说明：断点续传分片信息表
-- =============================================
CREATE TABLE kit_file_chunk_info
(
    file_chunk_id      VARCHAR(24)  PRIMARY KEY COMMENT '文件分片ID',
    unique_id          VARCHAR(64)           DEFAULT NULL COMMENT '文件标识（md5唯一值）',
    upload_service_type VARCHAR(20)          DEFAULT NULL COMMENT '上传服务类型：MINIO-OSS-COS-LOCAL',
    file_name          VARCHAR(200)          DEFAULT NULL COMMENT '文件名',
    media_type         VARCHAR(100)          DEFAULT NULL COMMENT '文件媒体类型',
    chunk_offset       INT                   DEFAULT NULL COMMENT 'chunk偏移量',
    chunk_stream_size  INT                   DEFAULT NULL COMMENT 'chunk流大小',
    chunk_group        VARCHAR(64)           DEFAULT NULL COMMENT 'chunk组',
    chunk_url          VARCHAR(500)          DEFAULT NULL COMMENT 'chunk上传路径',
    expiry             INT                   DEFAULT NULL COMMENT '到期时长（秒）',
    upload_id          VARCHAR(256)           DEFAULT NULL COMMENT '唯一上传ID',
    notes              VARCHAR(500)          DEFAULT NULL COMMENT '其他信息，用于扩展',
    tenant_id          VARCHAR(50)           DEFAULT NULL COMMENT '租户ID',
    create_op          VARCHAR(50)           DEFAULT NULL COMMENT '创建人',
    create_time        DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modify_op          VARCHAR(50)           DEFAULT NULL COMMENT '修改人',
    modify_time        DATETIME              DEFAULT NULL COMMENT '修改时间',
    deleted            SMALLINT     NOT NULL DEFAULT 0     COMMENT '删除标识：0-未删除 1-已删除',
    delete_op          VARCHAR(50)           DEFAULT NULL COMMENT '删除人',
    delete_time        DATETIME              DEFAULT NULL COMMENT '删除时间'
) COMMENT '断点续传分片信息表';

-- 索引
CREATE INDEX idx_kit_file_chunk_info_unique_id ON kit_file_chunk_info (unique_id);
CREATE INDEX idx_kit_file_chunk_info_upload_id ON kit_file_chunk_info (upload_id);
CREATE INDEX idx_kit_file_chunk_info_chunk_group ON kit_file_chunk_info (chunk_group);

-- =============================================
-- 定时任务相关表结构（基于 xxl-job 改造）
-- 表名前缀：kit_job_
-- 主键：VARCHAR(24) 雪花ID
-- 审计字段：BaseDO（tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time）
-- =============================================

-- =============================================
-- 表名：kit_job_group
-- 说明：执行器信息表
-- =============================================
CREATE TABLE kit_job_group
(
    id            VARCHAR(24)  PRIMARY KEY COMMENT '主键ID',
    app_name      VARCHAR(64)  NOT NULL                COMMENT '执行器AppName',
    name          VARCHAR(64)  NOT NULL                COMMENT '执行器名称',
    address_type  TINYINT      NOT NULL DEFAULT 0      COMMENT '执行器地址类型：0=自动注册、1=手动录入',
    address_list  TEXT         DEFAULT NULL             COMMENT '执行器地址列表，多地址逗号分隔',
    access_token  VARCHAR(255) DEFAULT NULL             COMMENT '执行器AccessToken',
    tenant_id     VARCHAR(50)  DEFAULT NULL             COMMENT '租户ID',
    create_op     VARCHAR(50)  DEFAULT NULL             COMMENT '创建人',
    create_time   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modify_op     VARCHAR(50)  DEFAULT NULL             COMMENT '修改人',
    modify_time   DATETIME     DEFAULT NULL             COMMENT '修改时间',
    deleted       SMALLINT     NOT NULL DEFAULT 0       COMMENT '删除标识：0-未删除 1-已删除',
    delete_op     VARCHAR(50)  DEFAULT NULL             COMMENT '删除人',
    delete_time   DATETIME     DEFAULT NULL             COMMENT '删除时间'
) COMMENT '执行器信息表';

CREATE UNIQUE INDEX i_app_name ON kit_job_group (app_name);

-- =============================================
-- 表名：kit_job_registry
-- 说明：执行器注册表
-- =============================================
CREATE TABLE kit_job_registry
(
    id              VARCHAR(24)  PRIMARY KEY COMMENT '主键ID',
    registry_group  VARCHAR(50)  NOT NULL                COMMENT '注册分组',
    registry_key    VARCHAR(255) NOT NULL                COMMENT '注册标识',
    registry_value  VARCHAR(255) NOT NULL                COMMENT '注册值（地址）',
    tenant_id       VARCHAR(50)  DEFAULT NULL             COMMENT '租户ID',
    create_op       VARCHAR(50)  DEFAULT NULL             COMMENT '创建人',
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modify_op       VARCHAR(50)  DEFAULT NULL             COMMENT '修改人',
    modify_time     DATETIME     DEFAULT NULL             COMMENT '修改时间',
    deleted         SMALLINT     NOT NULL DEFAULT 0       COMMENT '删除标识：0-未删除 1-已删除',
    delete_op       VARCHAR(50)  DEFAULT NULL             COMMENT '删除人',
    delete_time     DATETIME     DEFAULT NULL             COMMENT '删除时间'
) COMMENT '执行器注册表';

CREATE UNIQUE INDEX i_g_k_v ON kit_job_registry (registry_group, registry_key, registry_value);

-- =============================================
-- 表名：kit_job_info
-- 说明：任务信息表
-- =============================================
CREATE TABLE kit_job_info
(
    id                        VARCHAR(24)  PRIMARY KEY COMMENT '主键ID',
    job_group                 VARCHAR(24)  NOT NULL                COMMENT '执行器主键ID',
    name                      VARCHAR(255) NOT NULL                COMMENT '任务名称',
    author                    VARCHAR(64)  DEFAULT NULL             COMMENT '作者',
    alarm_email               VARCHAR(255) DEFAULT NULL             COMMENT '报警邮件',
    schedule_type             VARCHAR(50)  NOT NULL DEFAULT 'NONE'  COMMENT '调度类型',
    schedule_conf             VARCHAR(128) DEFAULT NULL             COMMENT '调度配置',
    misfire_strategy          VARCHAR(50)  NOT NULL DEFAULT 'DO_NOTHING' COMMENT '调度过期策略',
    executor_route_strategy   VARCHAR(50)  DEFAULT NULL             COMMENT '执行器路由策略',
    executor_handler          VARCHAR(255) DEFAULT NULL             COMMENT '任务handler',
    executor_param            TEXT         DEFAULT NULL             COMMENT '任务参数',
    executor_block_strategy   VARCHAR(50)  DEFAULT NULL             COMMENT '阻塞处理策略',
    executor_timeout          INT          NOT NULL DEFAULT 0       COMMENT '任务执行超时时间，单位秒',
    executor_fail_retry_count INT          NOT NULL DEFAULT 0       COMMENT '失败重试次数',
    glue_type                 VARCHAR(50)  NOT NULL                COMMENT 'GLUE类型',
    glue_source               MEDIUMTEXT   DEFAULT NULL             COMMENT 'GLUE源代码',
    glue_remark               VARCHAR(128) DEFAULT NULL             COMMENT 'GLUE备注',
    glue_updatetime           DATETIME     DEFAULT NULL             COMMENT 'GLUE更新时间',
    child_jobid               VARCHAR(255) DEFAULT NULL             COMMENT '子任务ID，多个逗号分隔',
    trigger_status            TINYINT      NOT NULL DEFAULT 0       COMMENT '调度状态：0-停止，1-运行',
    trigger_last_time         BIGINT       NOT NULL DEFAULT 0       COMMENT '上次调度时间',
    trigger_next_time         BIGINT       NOT NULL DEFAULT 0       COMMENT '下次调度时间',
    tenant_id                 VARCHAR(50)  DEFAULT NULL             COMMENT '租户ID',
    create_op                 VARCHAR(50)  DEFAULT NULL             COMMENT '创建人',
    create_time               DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modify_op                 VARCHAR(50)  DEFAULT NULL             COMMENT '修改人',
    modify_time               DATETIME     DEFAULT NULL             COMMENT '修改时间',
    deleted                   SMALLINT     NOT NULL DEFAULT 0       COMMENT '删除标识：0-未删除 1-已删除',
    delete_op                 VARCHAR(50)  DEFAULT NULL             COMMENT '删除人',
    delete_time               DATETIME     DEFAULT NULL             COMMENT '删除时间'
) COMMENT '任务信息表';

CREATE INDEX idx_kit_job_info_job_group ON kit_job_info (job_group);

-- =============================================
-- 表名：kit_job_log_glue
-- 说明：任务GLUE日志表
-- =============================================
CREATE TABLE kit_job_log_glue
(
    id            VARCHAR(24)  PRIMARY KEY COMMENT '主键ID',
    job_id        VARCHAR(24)  NOT NULL                COMMENT '任务主键ID',
    glue_type     VARCHAR(50)  DEFAULT NULL             COMMENT 'GLUE类型',
    glue_source   MEDIUMTEXT   DEFAULT NULL             COMMENT 'GLUE源代码',
    glue_remark   VARCHAR(128) NOT NULL                 COMMENT 'GLUE备注',
    tenant_id     VARCHAR(50)  DEFAULT NULL             COMMENT '租户ID',
    create_op     VARCHAR(50)  DEFAULT NULL             COMMENT '创建人',
    create_time   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modify_op     VARCHAR(50)  DEFAULT NULL             COMMENT '修改人',
    modify_time   DATETIME     DEFAULT NULL             COMMENT '修改时间',
    deleted       SMALLINT     NOT NULL DEFAULT 0       COMMENT '删除标识：0-未删除 1-已删除',
    delete_op     VARCHAR(50)  DEFAULT NULL             COMMENT '删除人',
    delete_time   DATETIME     DEFAULT NULL             COMMENT '删除时间'
) COMMENT '任务GLUE日志表';

CREATE INDEX idx_kit_job_log_glue_job_id ON kit_job_log_glue (job_id);

-- =============================================
-- 表名：kit_job_log
-- 说明：任务执行日志表
-- =============================================
CREATE TABLE kit_job_log
(
    id                        VARCHAR(24)  PRIMARY KEY COMMENT '主键ID',
    job_group                 VARCHAR(24)  NOT NULL                COMMENT '执行器主键ID',
    job_id                    VARCHAR(24)  NOT NULL                COMMENT '任务主键ID',
    executor_address          VARCHAR(255) DEFAULT NULL             COMMENT '执行器地址',
    executor_handler          VARCHAR(255) DEFAULT NULL             COMMENT '任务handler',
    executor_param            TEXT         DEFAULT NULL             COMMENT '任务参数',
    executor_sharding_param   VARCHAR(20)  DEFAULT NULL             COMMENT '任务分片参数',
    executor_fail_retry_count INT          NOT NULL DEFAULT 0       COMMENT '失败重试次数',
    trigger_time              DATETIME     DEFAULT NULL             COMMENT '调度时间',
    trigger_code              INT          NOT NULL                 COMMENT '调度结果',
    trigger_msg               TEXT         DEFAULT NULL             COMMENT '调度日志',
    handle_time               DATETIME     DEFAULT NULL             COMMENT '执行时间',
    handle_code               INT          NOT NULL                 COMMENT '执行状态',
    handle_msg                TEXT         DEFAULT NULL             COMMENT '执行日志',
    alarm_status              TINYINT      NOT NULL DEFAULT 0       COMMENT '告警状态：0-默认、1-无需告警、2-告警成功、3-告警失败',
    tenant_id                 VARCHAR(50)  DEFAULT NULL             COMMENT '租户ID',
    create_op                 VARCHAR(50)  DEFAULT NULL             COMMENT '创建人',
    create_time               DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modify_op                 VARCHAR(50)  DEFAULT NULL             COMMENT '修改人',
    modify_time               DATETIME     DEFAULT NULL             COMMENT '修改时间',
    deleted                   SMALLINT     NOT NULL DEFAULT 0       COMMENT '删除标识：0-未删除 1-已删除',
    delete_op                 VARCHAR(50)  DEFAULT NULL             COMMENT '删除人',
    delete_time               DATETIME     DEFAULT NULL             COMMENT '删除时间'
) COMMENT '任务执行日志表';

CREATE INDEX i_trigger_time ON kit_job_log (trigger_time);
CREATE INDEX i_handle_code ON kit_job_log (handle_code);
CREATE INDEX i_job_group ON kit_job_log (job_group);
CREATE INDEX i_job_id ON kit_job_log (job_id);

-- =============================================
-- 表名：kit_job_log_report
-- 说明：任务日志报表
-- =============================================
CREATE TABLE kit_job_log_report
(
    id            VARCHAR(24)  PRIMARY KEY COMMENT '主键ID',
    trigger_day   DATETIME     DEFAULT NULL             COMMENT '调度时间',
    running_count INT          NOT NULL DEFAULT 0       COMMENT '运行中-日志数量',
    suc_count     INT          NOT NULL DEFAULT 0       COMMENT '执行成功-日志数量',
    fail_count    INT          NOT NULL DEFAULT 0       COMMENT '执行失败-日志数量',
    tenant_id     VARCHAR(50)  DEFAULT NULL             COMMENT '租户ID',
    create_op     VARCHAR(50)  DEFAULT NULL             COMMENT '创建人',
    create_time   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modify_op     VARCHAR(50)  DEFAULT NULL             COMMENT '修改人',
    modify_time   DATETIME     DEFAULT NULL             COMMENT '修改时间',
    deleted       SMALLINT     NOT NULL DEFAULT 0       COMMENT '删除标识：0-未删除 1-已删除',
    delete_op     VARCHAR(50)  DEFAULT NULL             COMMENT '删除人',
    delete_time   DATETIME     DEFAULT NULL             COMMENT '删除时间'
) COMMENT '任务日志报表';

CREATE UNIQUE INDEX i_trigger_day ON kit_job_log_report (trigger_day);

-- =============================================
-- 表名：kit_job_lock
-- 说明：调度锁表
-- =============================================
CREATE TABLE kit_job_lock
(
    lock_name VARCHAR(50) NOT NULL COMMENT '锁名称',
    PRIMARY KEY (lock_name)
) COMMENT '调度锁表';

-- ================== 初始数据 ==================

INSERT INTO kit_job_lock (lock_name) VALUES ('schedule_lock');
