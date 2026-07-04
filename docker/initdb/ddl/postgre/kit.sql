-- =============================================
-- 文件管理相关表结构
-- 数据库：PostgreSQL
-- =============================================

-- =============================================
-- 表名：kit_file_meta_info
-- 说明：文件元信息表
-- =============================================
CREATE TABLE kit_file_meta_info
(
    file_meta_id       VARCHAR(24)  PRIMARY KEY,
    unique_id          VARCHAR(64)           DEFAULT NULL,
    upload_service_type VARCHAR(20)          DEFAULT NULL,
    file_size          VARCHAR(50)           DEFAULT NULL,
    media_type         VARCHAR(100)          DEFAULT NULL,
    file_group         VARCHAR(64)           DEFAULT NULL,
    file_url           VARCHAR(500)          DEFAULT NULL,
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
COMMENT ON TABLE kit_file_meta_info IS '文件元信息表';
COMMENT ON COLUMN kit_file_meta_info.file_meta_id IS '元文件ID';
COMMENT ON COLUMN kit_file_meta_info.unique_id IS '文件md5唯一值';
COMMENT ON COLUMN kit_file_meta_info.upload_service_type IS '上传服务类型：MINIO-OSS-COS-LOCAL';
COMMENT ON COLUMN kit_file_meta_info.file_size IS '文件大小';
COMMENT ON COLUMN kit_file_meta_info.media_type IS '文件媒体类型';
COMMENT ON COLUMN kit_file_meta_info.file_group IS '文件组';
COMMENT ON COLUMN kit_file_meta_info.file_url IS '文件路径';
COMMENT ON COLUMN kit_file_meta_info.tenant_id IS '租户ID';
COMMENT ON COLUMN kit_file_meta_info.create_op IS '创建人';
COMMENT ON COLUMN kit_file_meta_info.create_time IS '创建时间';
COMMENT ON COLUMN kit_file_meta_info.modify_op IS '修改人';
COMMENT ON COLUMN kit_file_meta_info.modify_time IS '修改时间';
COMMENT ON COLUMN kit_file_meta_info.deleted IS '删除标识：0-未删除 1-已删除';
COMMENT ON COLUMN kit_file_meta_info.delete_op IS '删除人';
COMMENT ON COLUMN kit_file_meta_info.delete_time IS '删除时间';

-- 索引
CREATE UNIQUE INDEX uk_kit_file_meta_info_unique_id ON kit_file_meta_info (unique_id) WHERE deleted = 0;
CREATE INDEX idx_kit_file_meta_info_upload_service_type ON kit_file_meta_info (upload_service_type);

-- =============================================
-- 表名：kit_file_info
-- 说明：文件信息表，类似文件超链接，多个链接引用同一个文件元信息
-- =============================================
CREATE TABLE kit_file_info
(
    file_id            VARCHAR(24)  PRIMARY KEY,
    file_meta_id       VARCHAR(24)           DEFAULT NULL,
    file_name          VARCHAR(200)          DEFAULT NULL,
    file_size          VARCHAR(50)           DEFAULT NULL,
    file_suffix        VARCHAR(20)           DEFAULT NULL,
    disposable         INT2         NOT NULL DEFAULT 0,
    expired_time       TIMESTAMP             DEFAULT NULL,
    scope              VARCHAR(20)  NOT NULL DEFAULT 'PROTECTED',
    visitors           VARCHAR(500)          DEFAULT NULL,
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
COMMENT ON TABLE kit_file_info IS '文件信息表';
COMMENT ON COLUMN kit_file_info.file_id IS '文件ID';
COMMENT ON COLUMN kit_file_info.file_meta_id IS '元文件ID';
COMMENT ON COLUMN kit_file_info.file_name IS '文件名';
COMMENT ON COLUMN kit_file_info.file_size IS '文件大小';
COMMENT ON COLUMN kit_file_info.file_suffix IS '文件后缀';
COMMENT ON COLUMN kit_file_info.disposable IS '是否为一次性文件：0-否 1-是';
COMMENT ON COLUMN kit_file_info.expired_time IS '文件过期时间';
COMMENT ON COLUMN kit_file_info.scope IS '文件作用域：PUBLIC-公共 PROTECTED-受保护 PRIVATE-私有';
COMMENT ON COLUMN kit_file_info.visitors IS '作用域为PRIVATE时，允许访问的人员ID，多个逗号分隔';
COMMENT ON COLUMN kit_file_info.tenant_id IS '租户ID';
COMMENT ON COLUMN kit_file_info.create_op IS '创建人';
COMMENT ON COLUMN kit_file_info.create_time IS '创建时间';
COMMENT ON COLUMN kit_file_info.modify_op IS '修改人';
COMMENT ON COLUMN kit_file_info.modify_time IS '修改时间';
COMMENT ON COLUMN kit_file_info.deleted IS '删除标识：0-未删除 1-已删除';
COMMENT ON COLUMN kit_file_info.delete_op IS '删除人';
COMMENT ON COLUMN kit_file_info.delete_time IS '删除时间';

-- 索引
CREATE INDEX idx_kit_file_info_file_meta_id ON kit_file_info (file_meta_id) WHERE deleted = 0;
CREATE INDEX idx_kit_file_info_scope ON kit_file_info (scope);
-- 外键
ALTER TABLE kit_file_info ADD CONSTRAINT file_info_meta_fk FOREIGN KEY (file_meta_id) REFERENCES kit_file_meta_info(file_meta_id) ON DELETE CASCADE ON UPDATE CASCADE;

-- =============================================
-- 表名：kit_file_chunk_info
-- 说明：断点续传分片信息表
-- =============================================
CREATE TABLE kit_file_chunk_info
(
    file_chunk_id      VARCHAR(24)  PRIMARY KEY,
    unique_id          VARCHAR(64)           DEFAULT NULL,
    upload_service_type VARCHAR(20)          DEFAULT NULL,
    file_name          VARCHAR(200)          DEFAULT NULL,
    media_type         VARCHAR(100)          DEFAULT NULL,
    chunk_offset       INT                   DEFAULT NULL,
    chunk_stream_size  INT                   DEFAULT NULL,
    chunk_group        VARCHAR(64)           DEFAULT NULL,
    chunk_url          VARCHAR(500)          DEFAULT NULL,
    expiry             INT                   DEFAULT NULL,
    upload_id          VARCHAR(256)           DEFAULT NULL,
    notes              VARCHAR(500)          DEFAULT NULL,
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
COMMENT ON TABLE kit_file_chunk_info IS '断点续传分片信息表';
COMMENT ON COLUMN kit_file_chunk_info.file_chunk_id IS '文件分片ID';
COMMENT ON COLUMN kit_file_chunk_info.unique_id IS '文件标识（md5唯一值）';
COMMENT ON COLUMN kit_file_chunk_info.upload_service_type IS '上传服务类型：MINIO-OSS-COS-LOCAL';
COMMENT ON COLUMN kit_file_chunk_info.file_name IS '文件名';
COMMENT ON COLUMN kit_file_chunk_info.media_type IS '文件媒体类型';
COMMENT ON COLUMN kit_file_chunk_info.chunk_offset IS 'chunk偏移量';
COMMENT ON COLUMN kit_file_chunk_info.chunk_stream_size IS 'chunk流大小';
COMMENT ON COLUMN kit_file_chunk_info.chunk_group IS 'chunk组';
COMMENT ON COLUMN kit_file_chunk_info.chunk_url IS 'chunk上传路径';
COMMENT ON COLUMN kit_file_chunk_info.expiry IS '到期时长（秒）';
COMMENT ON COLUMN kit_file_chunk_info.upload_id IS '唯一上传ID';
COMMENT ON COLUMN kit_file_chunk_info.notes IS '其他信息，用于扩展';
COMMENT ON COLUMN kit_file_chunk_info.tenant_id IS '租户ID';
COMMENT ON COLUMN kit_file_chunk_info.create_op IS '创建人';
COMMENT ON COLUMN kit_file_chunk_info.create_time IS '创建时间';
COMMENT ON COLUMN kit_file_chunk_info.modify_op IS '修改人';
COMMENT ON COLUMN kit_file_chunk_info.modify_time IS '修改时间';
COMMENT ON COLUMN kit_file_chunk_info.deleted IS '删除标识：0-未删除 1-已删除';
COMMENT ON COLUMN kit_file_chunk_info.delete_op IS '删除人';
COMMENT ON COLUMN kit_file_chunk_info.delete_time IS '删除时间';

-- 索引
CREATE INDEX idx_kit_file_chunk_info_unique_id ON kit_file_chunk_info (unique_id) WHERE deleted = 0;
CREATE INDEX idx_kit_file_chunk_info_upload_id ON kit_file_chunk_info (upload_id);
CREATE INDEX idx_kit_file_chunk_info_chunk_group ON kit_file_chunk_info (chunk_group);

-- =============================================
-- 定时任务相关表结构（基于 xxl-job 改造）
-- 表名前缀：kit_job_
-- =============================================

-- =============================================
-- 表名：kit_job_group
-- 说明：执行器信息表
-- =============================================
CREATE TABLE kit_job_group
(
    id              VARCHAR(24)  PRIMARY KEY,
    app_name        VARCHAR(64)  NOT NULL,
    name            VARCHAR(64)  NOT NULL,
    address_type    INT2         NOT NULL DEFAULT 0,
    address_list    TEXT                  DEFAULT NULL,
    access_token    VARCHAR(255)          DEFAULT NULL,
    tenant_id       VARCHAR(50)           DEFAULT NULL,
    create_op       VARCHAR(50)           DEFAULT NULL,
    create_time     TIMESTAMP             DEFAULT CURRENT_TIMESTAMP,
    modify_op       VARCHAR(50)           DEFAULT NULL,
    modify_time     TIMESTAMP             DEFAULT NULL,
    deleted         INT2         NOT NULL DEFAULT 0,
    delete_op       VARCHAR(50)           DEFAULT NULL,
    delete_time     TIMESTAMP             DEFAULT NULL
);

COMMENT ON TABLE kit_job_group IS '执行器信息表';
COMMENT ON COLUMN kit_job_group.id IS '主键ID';
COMMENT ON COLUMN kit_job_group.app_name IS '执行器AppName';
COMMENT ON COLUMN kit_job_group.name IS '执行器名称';
COMMENT ON COLUMN kit_job_group.address_type IS '执行器地址类型：0=自动注册、1=手动录入';
COMMENT ON COLUMN kit_job_group.address_list IS '执行器地址列表，多地址逗号分隔';
COMMENT ON COLUMN kit_job_group.access_token IS '执行器AccessToken（保留字段，兼容数据结构）';
COMMENT ON COLUMN kit_job_group.tenant_id IS '租户ID';
COMMENT ON COLUMN kit_job_group.create_op IS '创建人';
COMMENT ON COLUMN kit_job_group.create_time IS '创建时间';
COMMENT ON COLUMN kit_job_group.modify_op IS '修改人';
COMMENT ON COLUMN kit_job_group.modify_time IS '修改时间';
COMMENT ON COLUMN kit_job_group.deleted IS '删除标识：0-未删除 1-已删除';
COMMENT ON COLUMN kit_job_group.delete_op IS '删除人';
COMMENT ON COLUMN kit_job_group.delete_time IS '删除时间';

CREATE UNIQUE INDEX i_app_name ON kit_job_group (app_name) WHERE deleted = 0;

-- =============================================
-- 表名：kit_job_registry
-- 说明：执行器注册表
-- =============================================
CREATE TABLE kit_job_registry
(
    id              VARCHAR(24)  PRIMARY KEY,
    registry_group  VARCHAR(50)  NOT NULL,
    registry_key    VARCHAR(255) NOT NULL,
    registry_value  VARCHAR(255) NOT NULL,
    tenant_id       VARCHAR(50)           DEFAULT NULL,
    create_op       VARCHAR(50)           DEFAULT NULL,
    create_time     TIMESTAMP             DEFAULT CURRENT_TIMESTAMP,
    modify_op       VARCHAR(50)           DEFAULT NULL,
    modify_time     TIMESTAMP             DEFAULT NULL,
    deleted         INT2         NOT NULL DEFAULT 0,
    delete_op       VARCHAR(50)           DEFAULT NULL,
    delete_time     TIMESTAMP             DEFAULT NULL
);

COMMENT ON TABLE kit_job_registry IS '执行器注册表';
COMMENT ON COLUMN kit_job_registry.id IS '主键ID';
COMMENT ON COLUMN kit_job_registry.registry_group IS '注册分组';
COMMENT ON COLUMN kit_job_registry.registry_key IS '注册标识';
COMMENT ON COLUMN kit_job_registry.registry_value IS '注册值（地址）';
COMMENT ON COLUMN kit_job_registry.tenant_id IS '租户ID';
COMMENT ON COLUMN kit_job_registry.create_op IS '创建人';
COMMENT ON COLUMN kit_job_registry.create_time IS '创建时间';
COMMENT ON COLUMN kit_job_registry.modify_op IS '修改人';
COMMENT ON COLUMN kit_job_registry.modify_time IS '修改时间';
COMMENT ON COLUMN kit_job_registry.deleted IS '删除标识：0-未删除 1-已删除';
COMMENT ON COLUMN kit_job_registry.delete_op IS '删除人';
COMMENT ON COLUMN kit_job_registry.delete_time IS '删除时间';

ALTER TABLE kit_job_registry ADD CONSTRAINT uk_g_k_v UNIQUE (registry_group, registry_key, registry_value);
CREATE UNIQUE INDEX i_g_k_v ON kit_job_registry (registry_group, registry_key, registry_value) WHERE deleted = 0;

-- =============================================
-- 表名：kit_job_info
-- 说明：任务信息表
-- =============================================
CREATE TABLE kit_job_info
(
    id                        VARCHAR(24)  PRIMARY KEY,
    job_group                 VARCHAR(24)  NOT NULL,
    name                      VARCHAR(255) NOT NULL,
    author                    VARCHAR(64)           DEFAULT NULL,
    alarm_email               VARCHAR(255)          DEFAULT NULL,
    schedule_type             VARCHAR(50)  NOT NULL DEFAULT 'NONE',
    schedule_conf             VARCHAR(128)          DEFAULT NULL,
    misfire_strategy          VARCHAR(50)  NOT NULL DEFAULT 'DO_NOTHING',
    executor_route_strategy   VARCHAR(50)           DEFAULT NULL,
    executor_handler          VARCHAR(255)          DEFAULT NULL,
    executor_param            TEXT                  DEFAULT NULL,
    executor_block_strategy   VARCHAR(50)           DEFAULT NULL,
    executor_timeout          INT          NOT NULL DEFAULT 0,
    executor_fail_retry_count INT          NOT NULL DEFAULT 0,
    glue_type                 VARCHAR(50)  NOT NULL,
    glue_source               TEXT                  DEFAULT NULL,
    glue_remark               VARCHAR(128)          DEFAULT NULL,
    glue_updatetime           TIMESTAMP             DEFAULT NULL,
    child_jobid               VARCHAR(255)          DEFAULT NULL,
    trigger_status            INT2         NOT NULL DEFAULT 0,
    trigger_last_time         BIGINT       NOT NULL DEFAULT 0,
    trigger_next_time         BIGINT       NOT NULL DEFAULT 0,
    tenant_id                 VARCHAR(50)           DEFAULT NULL,
    create_op                 VARCHAR(50)           DEFAULT NULL,
    create_time               TIMESTAMP             DEFAULT CURRENT_TIMESTAMP,
    modify_op                 VARCHAR(50)           DEFAULT NULL,
    modify_time               TIMESTAMP             DEFAULT NULL,
    deleted                   INT2         NOT NULL DEFAULT 0,
    delete_op                 VARCHAR(50)           DEFAULT NULL,
    delete_time               TIMESTAMP             DEFAULT NULL
);

COMMENT ON TABLE kit_job_info IS '任务信息表';
COMMENT ON COLUMN kit_job_info.id IS '主键ID';
COMMENT ON COLUMN kit_job_info.job_group IS '执行器主键ID';
COMMENT ON COLUMN kit_job_info.name IS '任务名称';
COMMENT ON COLUMN kit_job_info.author IS '作者';
COMMENT ON COLUMN kit_job_info.alarm_email IS '报警邮件';
COMMENT ON COLUMN kit_job_info.schedule_type IS '调度类型';
COMMENT ON COLUMN kit_job_info.schedule_conf IS '调度配置，值含义取决于调度类型';
COMMENT ON COLUMN kit_job_info.misfire_strategy IS '调度过期策略';
COMMENT ON COLUMN kit_job_info.executor_route_strategy IS '执行器路由策略';
COMMENT ON COLUMN kit_job_info.executor_handler IS '任务handler';
COMMENT ON COLUMN kit_job_info.executor_param IS '任务参数';
COMMENT ON COLUMN kit_job_info.executor_block_strategy IS '阻塞处理策略';
COMMENT ON COLUMN kit_job_info.executor_timeout IS '任务执行超时时间，单位秒';
COMMENT ON COLUMN kit_job_info.executor_fail_retry_count IS '失败重试次数';
COMMENT ON COLUMN kit_job_info.glue_type IS 'GLUE类型';
COMMENT ON COLUMN kit_job_info.glue_source IS 'GLUE源代码';
COMMENT ON COLUMN kit_job_info.glue_remark IS 'GLUE备注';
COMMENT ON COLUMN kit_job_info.glue_updatetime IS 'GLUE更新时间';
COMMENT ON COLUMN kit_job_info.child_jobid IS '子任务ID，多个逗号分隔';
COMMENT ON COLUMN kit_job_info.trigger_status IS '调度状态：0-停止，1-运行';
COMMENT ON COLUMN kit_job_info.trigger_last_time IS '上次调度时间';
COMMENT ON COLUMN kit_job_info.trigger_next_time IS '下次调度时间';
COMMENT ON COLUMN kit_job_info.tenant_id IS '租户ID';
COMMENT ON COLUMN kit_job_info.create_op IS '创建人';
COMMENT ON COLUMN kit_job_info.create_time IS '创建时间';
COMMENT ON COLUMN kit_job_info.modify_op IS '修改人';
COMMENT ON COLUMN kit_job_info.modify_time IS '修改时间';
COMMENT ON COLUMN kit_job_info.deleted IS '删除标识：0-未删除 1-已删除';
COMMENT ON COLUMN kit_job_info.delete_op IS '删除人';
COMMENT ON COLUMN kit_job_info.delete_time IS '删除时间';

-- =============================================
-- 表名：kit_job_log_glue
-- 说明：任务GLUE日志表
-- =============================================
CREATE TABLE kit_job_log_glue
(
    id            VARCHAR(24)  PRIMARY KEY,
    job_id        VARCHAR(24)  NOT NULL,
    glue_type     VARCHAR(50)           DEFAULT NULL,
    glue_source   TEXT                  DEFAULT NULL,
    glue_remark   VARCHAR(128) NOT NULL,
    tenant_id     VARCHAR(50)           DEFAULT NULL,
    create_op     VARCHAR(50)           DEFAULT NULL,
    create_time   TIMESTAMP             DEFAULT CURRENT_TIMESTAMP,
    modify_op     VARCHAR(50)           DEFAULT NULL,
    modify_time   TIMESTAMP             DEFAULT NULL,
    deleted       INT2         NOT NULL DEFAULT 0,
    delete_op     VARCHAR(50)           DEFAULT NULL,
    delete_time   TIMESTAMP             DEFAULT NULL
);

COMMENT ON TABLE kit_job_log_glue IS '任务GLUE日志表';
COMMENT ON COLUMN kit_job_log_glue.id IS '主键ID';
COMMENT ON COLUMN kit_job_log_glue.job_id IS '任务主键ID';
COMMENT ON COLUMN kit_job_log_glue.glue_type IS 'GLUE类型';
COMMENT ON COLUMN kit_job_log_glue.glue_source IS 'GLUE源代码';
COMMENT ON COLUMN kit_job_log_glue.glue_remark IS 'GLUE备注';
COMMENT ON COLUMN kit_job_log_glue.tenant_id IS '租户ID';
COMMENT ON COLUMN kit_job_log_glue.create_op IS '创建人';
COMMENT ON COLUMN kit_job_log_glue.create_time IS '创建时间';
COMMENT ON COLUMN kit_job_log_glue.modify_op IS '修改人';
COMMENT ON COLUMN kit_job_log_glue.modify_time IS '修改时间';
COMMENT ON COLUMN kit_job_log_glue.deleted IS '删除标识：0-未删除 1-已删除';
COMMENT ON COLUMN kit_job_log_glue.delete_op IS '删除人';
COMMENT ON COLUMN kit_job_log_glue.delete_time IS '删除时间';

-- =============================================
-- 表名：kit_job_log
-- 说明：任务执行日志表
-- =============================================
CREATE TABLE kit_job_log
(
    id                        VARCHAR(24)  PRIMARY KEY,
    job_group                 VARCHAR(24)  NOT NULL,
    job_id                    VARCHAR(24)  NOT NULL,
    executor_address          VARCHAR(255)          DEFAULT NULL,
    executor_handler          VARCHAR(255)          DEFAULT NULL,
    executor_param            TEXT                  DEFAULT NULL,
    executor_sharding_param   VARCHAR(20)           DEFAULT NULL,
    executor_fail_retry_count INT          NOT NULL DEFAULT 0,
    trigger_time              TIMESTAMP             DEFAULT NULL,
    trigger_code              INT          NOT NULL,
    trigger_msg               TEXT                  DEFAULT NULL,
    handle_time               TIMESTAMP             DEFAULT NULL,
    handle_code               INT          NOT NULL,
    handle_msg                TEXT                  DEFAULT NULL,
    alarm_status              INT2         NOT NULL DEFAULT 0,
    tenant_id                 VARCHAR(50)           DEFAULT NULL,
    create_op                 VARCHAR(50)           DEFAULT NULL,
    create_time               TIMESTAMP             DEFAULT CURRENT_TIMESTAMP,
    modify_op                 VARCHAR(50)           DEFAULT NULL,
    modify_time               TIMESTAMP             DEFAULT NULL,
    deleted                   INT2         NOT NULL DEFAULT 0,
    delete_op                 VARCHAR(50)           DEFAULT NULL,
    delete_time               TIMESTAMP             DEFAULT NULL
);

COMMENT ON TABLE kit_job_log IS '任务执行日志表';
COMMENT ON COLUMN kit_job_log.id IS '主键ID';
COMMENT ON COLUMN kit_job_log.job_group IS '执行器主键ID';
COMMENT ON COLUMN kit_job_log.job_id IS '任务主键ID';
COMMENT ON COLUMN kit_job_log.executor_address IS '执行器地址，本次执行的地址';
COMMENT ON COLUMN kit_job_log.executor_handler IS '任务handler';
COMMENT ON COLUMN kit_job_log.executor_param IS '任务参数';
COMMENT ON COLUMN kit_job_log.executor_sharding_param IS '任务分片参数，格式如 1/2';
COMMENT ON COLUMN kit_job_log.executor_fail_retry_count IS '失败重试次数';
COMMENT ON COLUMN kit_job_log.trigger_time IS '调度-时间';
COMMENT ON COLUMN kit_job_log.trigger_code IS '调度-结果';
COMMENT ON COLUMN kit_job_log.trigger_msg IS '调度-日志';
COMMENT ON COLUMN kit_job_log.handle_time IS '执行-时间';
COMMENT ON COLUMN kit_job_log.handle_code IS '执行-状态';
COMMENT ON COLUMN kit_job_log.handle_msg IS '执行-日志';
COMMENT ON COLUMN kit_job_log.alarm_status IS '告警状态：0-默认、1-无需告警、2-告警成功、3-告警失败';
COMMENT ON COLUMN kit_job_log.tenant_id IS '租户ID';
COMMENT ON COLUMN kit_job_log.create_op IS '创建人';
COMMENT ON COLUMN kit_job_log.create_time IS '创建时间';
COMMENT ON COLUMN kit_job_log.modify_op IS '修改人';
COMMENT ON COLUMN kit_job_log.modify_time IS '修改时间';
COMMENT ON COLUMN kit_job_log.deleted IS '删除标识：0-未删除 1-已删除';
COMMENT ON COLUMN kit_job_log.delete_op IS '删除人';
COMMENT ON COLUMN kit_job_log.delete_time IS '删除时间';

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
    id              VARCHAR(24)  PRIMARY KEY,
    trigger_day     TIMESTAMP             DEFAULT NULL,
    running_count   INT          NOT NULL DEFAULT 0,
    suc_count       INT          NOT NULL DEFAULT 0,
    fail_count      INT          NOT NULL DEFAULT 0,
    tenant_id       VARCHAR(50)           DEFAULT NULL,
    create_op       VARCHAR(50)           DEFAULT NULL,
    create_time     TIMESTAMP             DEFAULT CURRENT_TIMESTAMP,
    modify_op       VARCHAR(50)           DEFAULT NULL,
    modify_time     TIMESTAMP             DEFAULT NULL,
    deleted         INT2         NOT NULL DEFAULT 0,
    delete_op       VARCHAR(50)           DEFAULT NULL,
    delete_time     TIMESTAMP             DEFAULT NULL
);

COMMENT ON TABLE kit_job_log_report IS '任务日志报表';
COMMENT ON COLUMN kit_job_log_report.id IS '主键ID';
COMMENT ON COLUMN kit_job_log_report.trigger_day IS '调度-时间';
COMMENT ON COLUMN kit_job_log_report.running_count IS '运行中-日志数量';
COMMENT ON COLUMN kit_job_log_report.suc_count IS '执行成功-日志数量';
COMMENT ON COLUMN kit_job_log_report.fail_count IS '执行失败-日志数量';
COMMENT ON COLUMN kit_job_log_report.tenant_id IS '租户ID';
COMMENT ON COLUMN kit_job_log_report.create_op IS '创建人';
COMMENT ON COLUMN kit_job_log_report.create_time IS '创建时间';
COMMENT ON COLUMN kit_job_log_report.modify_op IS '修改人';
COMMENT ON COLUMN kit_job_log_report.modify_time IS '修改时间';
COMMENT ON COLUMN kit_job_log_report.deleted IS '删除标识：0-未删除 1-已删除';
COMMENT ON COLUMN kit_job_log_report.delete_op IS '删除人';
COMMENT ON COLUMN kit_job_log_report.delete_time IS '删除时间';

ALTER TABLE kit_job_log_report ADD CONSTRAINT uk_trigger_day UNIQUE (trigger_day);
CREATE UNIQUE INDEX i_trigger_day ON kit_job_log_report (trigger_day) WHERE deleted = 0;

-- =============================================
-- 表名：kit_job_lock
-- 说明：调度锁表
-- =============================================
CREATE TABLE kit_job_lock
(
    lock_name VARCHAR(50) PRIMARY KEY
);

COMMENT ON TABLE kit_job_lock IS '调度锁表';
COMMENT ON COLUMN kit_job_lock.lock_name IS '锁名称';

-- ================== 初始数据 ==================

INSERT INTO kit_job_lock (lock_name) VALUES ('schedule_lock');
