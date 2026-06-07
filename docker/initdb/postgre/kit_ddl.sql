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
    upload_id          VARCHAR(64)           DEFAULT NULL,
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
