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
