-- =============================================
-- 日志模块 DDL（MySQL）
-- =============================================

-- =============================================
-- 表名：log_operation
-- 说明：操作日志表
-- =============================================
CREATE TABLE log_operation (
    id              VARCHAR(24) PRIMARY KEY COMMENT '主键ID（雪花算法）',
    tid             VARCHAR(64) COMMENT '全局日志链路',
    parent_id       VARCHAR(24) COMMENT '父节点',
    module_prefix   VARCHAR(50) COMMENT '所属服务前缀',
    from_app        VARCHAR(50) COMMENT '链路来源服务',
    api_module      VARCHAR(100) COMMENT 'api模块名',
    menu_id         VARCHAR(24) COMMENT '来源菜单',
    oper_subject    SMALLINT DEFAULT 0 COMMENT '界面操作主体：0-人类 1-智能助手',
    api_description VARCHAR(200) COMMENT 'api接口详情注释',
    method          VARCHAR(100) COMMENT '方法名',
    request_url     VARCHAR(500) COMMENT '请求路径',
    request_method  VARCHAR(10) COMMENT '请求方式',
    terminal        VARCHAR(20) COMMENT '请求终端',
    terminal_detail VARCHAR(500) COMMENT '请求终端详情',
    oper_name       VARCHAR(50) COMMENT '操作人',
    token_id        VARCHAR(100) COMMENT 'token',
    request_param   TEXT COMMENT '请求参数',
    response_data   TEXT COMMENT '响应数据',
    error_msg       TEXT COMMENT '错误消息',
    status          SMALLINT DEFAULT 1 COMMENT '状态：0-失败 1-成功',
    request_time    DATETIME COMMENT '请求时间',
    response_time   DATETIME COMMENT '响应时间',
    consume_mill    BIGINT DEFAULT 0 COMMENT '耗时，ms',
    tenant_id       VARCHAR(50) COMMENT '租户ID',
    create_op       VARCHAR(50) COMMENT '创建人',
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modify_op       VARCHAR(50) COMMENT '修改人',
    modify_time     DATETIME COMMENT '修改时间',
    deleted         SMALLINT DEFAULT 0 COMMENT '删除标识：0-未删除 1-已删除',
    delete_op       VARCHAR(50) COMMENT '删除人',
    delete_time     DATETIME COMMENT '删除时间',
    INDEX idx_log_operation_tid (tid),
    INDEX idx_log_operation_module (module_prefix),
    INDEX idx_log_operation_request_time (request_time),
    INDEX idx_log_operation_oper_name (oper_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';
