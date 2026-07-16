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
-- 表名：kit_job_registry
-- 说明：执行器注册表
-- =============================================
CREATE TABLE kit_job_registry
(
    id              VARCHAR(24)  PRIMARY KEY COMMENT '主键ID',
    registry_group  VARCHAR(64)  NOT NULL                COMMENT '执行器AppName（命名空间）',
    registry_key    VARCHAR(255) NOT NULL                COMMENT 'Handler名称',
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

-- =============================================
-- 知识库相关表结构
-- 表名前缀：kit_knowledge_
-- =============================================

CREATE TABLE kit_knowledge_source_document
(
    id               VARCHAR(24)  PRIMARY KEY COMMENT '主键ID',
    file_id          VARCHAR(24)           DEFAULT NULL COMMENT '文件ID',
    file_name        VARCHAR(200)          DEFAULT NULL COMMENT '文件名',
    document_status  VARCHAR(32)  NOT NULL DEFAULT 'UPLOADED' COMMENT '文档处理状态',
    target_page_id   VARCHAR(24)           DEFAULT NULL COMMENT '目标Page ID',
    process_message  VARCHAR(1000)         DEFAULT NULL COMMENT '处理信息',
    processed_at     DATETIME              DEFAULT NULL COMMENT '处理完成时间',
    tenant_id        VARCHAR(50)           DEFAULT NULL COMMENT '租户ID',
    create_op        VARCHAR(50)           DEFAULT NULL COMMENT '创建人',
    create_time      DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modify_op        VARCHAR(50)           DEFAULT NULL COMMENT '修改人',
    modify_time      DATETIME              DEFAULT NULL COMMENT '修改时间',
    deleted          SMALLINT     NOT NULL DEFAULT 0 COMMENT '删除标识：0-未删除 1-已删除',
    delete_op        VARCHAR(50)           DEFAULT NULL COMMENT '删除人',
    delete_time      DATETIME              DEFAULT NULL COMMENT '删除时间'
) COMMENT '知识源文档';

CREATE INDEX idx_kit_knowledge_source_document_file_id ON kit_knowledge_source_document (file_id);
CREATE INDEX idx_kit_knowledge_source_document_status ON kit_knowledge_source_document (document_status);

CREATE TABLE kit_knowledge_source_document_role
(
    id                 VARCHAR(24) PRIMARY KEY COMMENT '主键ID',
    source_document_id VARCHAR(24) NOT NULL COMMENT '源文档ID',
    role_code          VARCHAR(100) NOT NULL COMMENT '角色编码',
    tenant_id          VARCHAR(50) DEFAULT NULL COMMENT '租户ID',
    create_op          VARCHAR(50) DEFAULT NULL COMMENT '创建人',
    create_time        DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modify_op          VARCHAR(50) DEFAULT NULL COMMENT '修改人',
    modify_time        DATETIME    DEFAULT NULL COMMENT '修改时间',
    deleted            SMALLINT    NOT NULL DEFAULT 0 COMMENT '删除标识：0-未删除 1-已删除',
    active_source_document_id VARCHAR(24) GENERATED ALWAYS AS (CASE WHEN deleted = 0 THEN source_document_id ELSE NULL END) STORED COMMENT '未删除源文档唯一键',
    active_role_code   VARCHAR(100) GENERATED ALWAYS AS (CASE WHEN deleted = 0 THEN role_code ELSE NULL END) STORED COMMENT '未删除角色唯一键',
    delete_op          VARCHAR(50) DEFAULT NULL COMMENT '删除人',
    delete_time        DATETIME    DEFAULT NULL COMMENT '删除时间'
) COMMENT '知识源文档角色授权';

CREATE UNIQUE INDEX uk_kit_knowledge_source_document_role_doc_role ON kit_knowledge_source_document_role (active_source_document_id, active_role_code);

CREATE TABLE kit_knowledge_page
(
    id                 VARCHAR(24)  PRIMARY KEY COMMENT '主键ID',
    title              VARCHAR(200)          DEFAULT NULL COMMENT '标题',
    page_status        VARCHAR(32)  NOT NULL DEFAULT 'DRAFT' COMMENT 'Page状态',
    current_version_id VARCHAR(24)           DEFAULT NULL COMMENT '当前版本ID',
    tenant_id          VARCHAR(50)           DEFAULT NULL COMMENT '租户ID',
    create_op          VARCHAR(50)           DEFAULT NULL COMMENT '创建人',
    create_time        DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modify_op          VARCHAR(50)           DEFAULT NULL COMMENT '修改人',
    modify_time        DATETIME              DEFAULT NULL COMMENT '修改时间',
    deleted            SMALLINT     NOT NULL DEFAULT 0 COMMENT '删除标识：0-未删除 1-已删除',
    delete_op          VARCHAR(50)           DEFAULT NULL COMMENT '删除人',
    delete_time        DATETIME              DEFAULT NULL COMMENT '删除时间'
) COMMENT '知识Page';

CREATE INDEX idx_kit_knowledge_page_status ON kit_knowledge_page (page_status);

CREATE TABLE kit_knowledge_page_version
(
    id               VARCHAR(24)  PRIMARY KEY COMMENT '主键ID',
    page_id          VARCHAR(24)  NOT NULL COMMENT 'Page ID',
    version_no       INT          NOT NULL COMMENT '版本号',
    version_status   VARCHAR(32)  NOT NULL DEFAULT 'DRAFT' COMMENT '版本状态',
    markdown_content LONGTEXT              COMMENT 'Markdown内容快照',
    published_at     DATETIME              DEFAULT NULL COMMENT '发布时间',
    tenant_id        VARCHAR(50)           DEFAULT NULL COMMENT '租户ID',
    create_op        VARCHAR(50)           DEFAULT NULL COMMENT '创建人',
    create_time      DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modify_op        VARCHAR(50)           DEFAULT NULL COMMENT '修改人',
    modify_time      DATETIME              DEFAULT NULL COMMENT '修改时间',
    deleted          SMALLINT     NOT NULL DEFAULT 0 COMMENT '删除标识：0-未删除 1-已删除',
    active_page_id    VARCHAR(24) GENERATED ALWAYS AS (CASE WHEN deleted = 0 THEN page_id ELSE NULL END) STORED COMMENT '未删除Page唯一键',
    active_version_no INT GENERATED ALWAYS AS (CASE WHEN deleted = 0 THEN version_no ELSE NULL END) STORED COMMENT '未删除版本号唯一键',
    delete_op        VARCHAR(50)           DEFAULT NULL COMMENT '删除人',
    delete_time      DATETIME              DEFAULT NULL COMMENT '删除时间'
) COMMENT '知识Page版本';

CREATE UNIQUE INDEX uk_kit_knowledge_page_version_page_no ON kit_knowledge_page_version (active_page_id, active_version_no);

CREATE TABLE kit_knowledge_page_block
(
    id              VARCHAR(24) PRIMARY KEY COMMENT '主键ID',
    page_version_id VARCHAR(24) NOT NULL COMMENT 'Page版本ID',
    order_no        INT         NOT NULL COMMENT '排序号',
    block_type      VARCHAR(32) NOT NULL COMMENT 'Block类型',
    content         LONGTEXT             COMMENT 'Block内容',
    tenant_id       VARCHAR(50)          DEFAULT NULL COMMENT '租户ID',
    create_op       VARCHAR(50)          DEFAULT NULL COMMENT '创建人',
    create_time     DATETIME             DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modify_op       VARCHAR(50)          DEFAULT NULL COMMENT '修改人',
    modify_time     DATETIME             DEFAULT NULL COMMENT '修改时间',
    deleted         SMALLINT    NOT NULL DEFAULT 0 COMMENT '删除标识：0-未删除 1-已删除',
    active_page_version_id VARCHAR(24) GENERATED ALWAYS AS (CASE WHEN deleted = 0 THEN page_version_id ELSE NULL END) STORED COMMENT '未删除版本唯一键',
    active_order_no  INT GENERATED ALWAYS AS (CASE WHEN deleted = 0 THEN order_no ELSE NULL END) STORED COMMENT '未删除排序唯一键',
    delete_op       VARCHAR(50)          DEFAULT NULL COMMENT '删除人',
    delete_time     DATETIME             DEFAULT NULL COMMENT '删除时间'
) COMMENT '知识Page Block';

CREATE UNIQUE INDEX uk_kit_knowledge_page_block_version_order ON kit_knowledge_page_block (active_page_version_id, active_order_no);

CREATE TABLE kit_knowledge_page_source_ref
(
    id                 VARCHAR(24) PRIMARY KEY COMMENT '主键ID',
    page_block_id      VARCHAR(24) NOT NULL COMMENT 'Page Block ID',
    source_type        VARCHAR(32) NOT NULL COMMENT '来源类型',
    source_document_id VARCHAR(24) NOT NULL COMMENT '源文档ID',
    source_locator     VARCHAR(500) DEFAULT NULL COMMENT '来源定位',
    tenant_id          VARCHAR(50)  DEFAULT NULL COMMENT '租户ID',
    create_op          VARCHAR(50)  DEFAULT NULL COMMENT '创建人',
    create_time        DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modify_op          VARCHAR(50)  DEFAULT NULL COMMENT '修改人',
    modify_time        DATETIME     DEFAULT NULL COMMENT '修改时间',
    deleted            SMALLINT     NOT NULL DEFAULT 0 COMMENT '删除标识：0-未删除 1-已删除',
    active_page_block_id VARCHAR(24) GENERATED ALWAYS AS (CASE WHEN deleted = 0 THEN page_block_id ELSE NULL END) STORED COMMENT '未删除Block唯一键',
    delete_op          VARCHAR(50)  DEFAULT NULL COMMENT '删除人',
    delete_time        DATETIME     DEFAULT NULL COMMENT '删除时间'
) COMMENT '知识Page Block来源关系';

CREATE UNIQUE INDEX uk_kit_knowledge_page_source_ref_block ON kit_knowledge_page_source_ref (active_page_block_id);
CREATE INDEX idx_kit_knowledge_page_source_ref_document ON kit_knowledge_page_source_ref (source_document_id);

CREATE TABLE kit_knowledge_ingest_task
(
    id                 VARCHAR(24) PRIMARY KEY COMMENT '主键ID',
    source_document_id VARCHAR(24) NOT NULL COMMENT '源文档ID',
    task_status        VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '任务状态',
    current_stage      VARCHAR(32)          DEFAULT NULL COMMENT '当前处理阶段',
    retry_count        INT         NOT NULL DEFAULT 0 COMMENT '重试次数',
    error_message      VARCHAR(2000)        DEFAULT NULL COMMENT '错误信息',
    started_at         DATETIME             DEFAULT NULL COMMENT '开始时间',
    finished_at        DATETIME             DEFAULT NULL COMMENT '完成时间',
    tenant_id          VARCHAR(50)          DEFAULT NULL COMMENT '租户ID',
    create_op          VARCHAR(50)          DEFAULT NULL COMMENT '创建人',
    create_time        DATETIME             DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modify_op          VARCHAR(50)          DEFAULT NULL COMMENT '修改人',
    modify_time        DATETIME             DEFAULT NULL COMMENT '修改时间',
    deleted            SMALLINT    NOT NULL DEFAULT 0 COMMENT '删除标识：0-未删除 1-已删除',
    active_task_source_document_id VARCHAR(24) GENERATED ALWAYS AS (CASE WHEN deleted = 0 AND task_status IN ('PENDING', 'RUNNING') THEN source_document_id ELSE NULL END) STORED COMMENT '活跃任务源文档唯一键',
    delete_op          VARCHAR(50)          DEFAULT NULL COMMENT '删除人',
    delete_time        DATETIME             DEFAULT NULL COMMENT '删除时间'
) COMMENT '知识文档导入任务';

CREATE UNIQUE INDEX uk_kit_knowledge_ingest_task_active_doc ON kit_knowledge_ingest_task (active_task_source_document_id);

-- ================== 初始数据 ==================

INSERT INTO kit_job_lock (lock_name) VALUES ('schedule_lock');
