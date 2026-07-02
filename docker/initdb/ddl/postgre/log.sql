-- =============================================
-- 日志模块 DDL（PostgreSQL）
-- =============================================

-- =============================================
-- 表名：log_operation
-- 说明：操作日志表
-- =============================================
CREATE TABLE log_operation (
    id              VARCHAR(24) PRIMARY KEY,
    tid             VARCHAR(64),
    parent_id       VARCHAR(24),
    module_prefix   VARCHAR(50),
    from_app        VARCHAR(50),
    api_module      VARCHAR(100),
    menu_id         VARCHAR(24),
    oper_subject    INT2 DEFAULT 0,
    api_description VARCHAR(200),
    method          VARCHAR(100),
    request_url     VARCHAR(500),
    request_method  VARCHAR(10),
    terminal        VARCHAR(20),
    terminal_detail VARCHAR(500),
    oper_name       VARCHAR(50),
    token_id        VARCHAR(256),
    request_param   TEXT,
    response_data   TEXT,
    error_msg       TEXT,
    status          INT2 DEFAULT 1,
    request_time    TIMESTAMP,
    response_time   TIMESTAMP,
    consume_mill    BIGINT DEFAULT 0,
    tenant_id       VARCHAR(50),
    create_op       VARCHAR(50),
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modify_op       VARCHAR(50),
    modify_time     TIMESTAMP,
    deleted         INT2 DEFAULT 0,
    delete_op       VARCHAR(50),
    delete_time     TIMESTAMP
);

-- 表和字段注释
COMMENT ON TABLE log_operation IS '操作日志表';
COMMENT ON COLUMN log_operation.id IS '主键ID（雪花算法）';
COMMENT ON COLUMN log_operation.tid IS '全局日志链路';
COMMENT ON COLUMN log_operation.parent_id IS '父节点';
COMMENT ON COLUMN log_operation.module_prefix IS '所属服务前缀';
COMMENT ON COLUMN log_operation.from_app IS '链路来源服务';
COMMENT ON COLUMN log_operation.api_module IS 'api模块名';
COMMENT ON COLUMN log_operation.menu_id IS '来源菜单';
COMMENT ON COLUMN log_operation.oper_subject IS '界面操作主体：0-人类 1-智能助手';
COMMENT ON COLUMN log_operation.api_description IS 'api接口详情注释';
COMMENT ON COLUMN log_operation.method IS '方法名';
COMMENT ON COLUMN log_operation.request_url IS '请求路径';
COMMENT ON COLUMN log_operation.request_method IS '请求方式';
COMMENT ON COLUMN log_operation.terminal IS '请求终端';
COMMENT ON COLUMN log_operation.terminal_detail IS '请求终端详情';
COMMENT ON COLUMN log_operation.oper_name IS '操作人';
COMMENT ON COLUMN log_operation.token_id IS 'token';
COMMENT ON COLUMN log_operation.request_param IS '请求参数';
COMMENT ON COLUMN log_operation.response_data IS '响应数据';
COMMENT ON COLUMN log_operation.error_msg IS '错误消息';
COMMENT ON COLUMN log_operation.status IS '状态：0-失败 1-成功';
COMMENT ON COLUMN log_operation.request_time IS '请求时间';
COMMENT ON COLUMN log_operation.response_time IS '响应时间';
COMMENT ON COLUMN log_operation.consume_mill IS '耗时，ms';
COMMENT ON COLUMN log_operation.deleted IS '删除标识：0-未删除 1-已删除';

-- 索引
CREATE INDEX idx_log_operation_tid ON log_operation(tid) WHERE deleted = 0;
CREATE INDEX idx_log_operation_request_time ON log_operation(request_time) WHERE deleted = 0;
