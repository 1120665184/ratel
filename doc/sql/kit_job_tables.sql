-- ============================================================================
-- Kit Job 定时任务模块建表脚本
-- 基于 xxl-job 改造，适配 Ratel 项目
-- 表名前缀：kit_job_（原 xxl_job_）
-- 已移除 xxl_job_user 表（项目使用自身用户体系）
-- ============================================================================

-- ================== 执行器与注册信息 ==================

CREATE TABLE `kit_job_group`
(
    `id`            INT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `app_name`      VARCHAR(64)  NOT NULL                COMMENT '执行器AppName',
    `name`          VARCHAR(64)  NOT NULL                COMMENT '执行器名称',
    `address_type`  TINYINT      NOT NULL DEFAULT 0      COMMENT '执行器地址类型：0=自动注册、1=手动录入',
    `address_list`  TEXT         DEFAULT NULL             COMMENT '执行器地址列表，多地址逗号分隔',
    `access_token`  VARCHAR(255) DEFAULT NULL             COMMENT '执行器AccessToken（保留字段，兼容数据结构）',
    `update_time`   DATETIME     DEFAULT NULL             COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `i_app_name` (`app_name`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT = '执行器信息表';

CREATE TABLE `kit_job_registry`
(
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `registry_group`  VARCHAR(50)  NOT NULL                COMMENT '注册分组',
    `registry_key`    VARCHAR(255) NOT NULL                COMMENT '注册标识',
    `registry_value`  VARCHAR(255) NOT NULL                COMMENT '注册值（地址）',
    `update_time`     DATETIME     DEFAULT NULL             COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `i_g_k_v` (`registry_group`, `registry_key`, `registry_value`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT = '执行器注册表';

-- ================== 任务信息 ==================

CREATE TABLE `kit_job_info`
(
    `id`                        INT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `job_group`                 INT          NOT NULL                COMMENT '执行器主键ID',
    `name`                      VARCHAR(255) NOT NULL                COMMENT '任务名称',
    `author`                    VARCHAR(64)  DEFAULT NULL             COMMENT '作者',
    `alarm_email`               VARCHAR(255) DEFAULT NULL             COMMENT '报警邮件',
    `schedule_type`             VARCHAR(50)  NOT NULL DEFAULT 'NONE'  COMMENT '调度类型',
    `schedule_conf`             VARCHAR(128) DEFAULT NULL             COMMENT '调度配置，值含义取决于调度类型',
    `misfire_strategy`          VARCHAR(50)  NOT NULL DEFAULT 'DO_NOTHING' COMMENT '调度过期策略',
    `executor_route_strategy`   VARCHAR(50)  DEFAULT NULL             COMMENT '执行器路由策略',
    `executor_handler`          VARCHAR(255) DEFAULT NULL             COMMENT '任务handler',
    `executor_param`            TEXT         DEFAULT NULL             COMMENT '任务参数',
    `executor_block_strategy`   VARCHAR(50)  DEFAULT NULL             COMMENT '阻塞处理策略',
    `executor_timeout`          INT          NOT NULL DEFAULT 0       COMMENT '任务执行超时时间，单位秒',
    `executor_fail_retry_count` INT          NOT NULL DEFAULT 0       COMMENT '失败重试次数',
    `glue_type`                 VARCHAR(50)  NOT NULL                COMMENT 'GLUE类型',
    `glue_source`               MEDIUMTEXT   DEFAULT NULL             COMMENT 'GLUE源代码',
    `glue_remark`               VARCHAR(128) DEFAULT NULL             COMMENT 'GLUE备注',
    `glue_updatetime`           DATETIME     DEFAULT NULL             COMMENT 'GLUE更新时间',
    `child_jobid`               VARCHAR(255) DEFAULT NULL             COMMENT '子任务ID，多个逗号分隔',
    `trigger_status`            TINYINT      NOT NULL DEFAULT 0       COMMENT '调度状态：0-停止，1-运行',
    `trigger_last_time`         BIGINT       NOT NULL DEFAULT 0       COMMENT '上次调度时间',
    `trigger_next_time`         BIGINT       NOT NULL DEFAULT 0       COMMENT '下次调度时间',
    `add_time`                  DATETIME     DEFAULT NULL             COMMENT '创建时间',
    `update_time`               DATETIME     DEFAULT NULL             COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT = '任务信息表';

CREATE TABLE `kit_job_log_glue`
(
    `id`            INT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `job_id`        INT          NOT NULL                COMMENT '任务主键ID',
    `glue_type`     VARCHAR(50)  DEFAULT NULL             COMMENT 'GLUE类型',
    `glue_source`   MEDIUMTEXT   DEFAULT NULL             COMMENT 'GLUE源代码',
    `glue_remark`   VARCHAR(128) NOT NULL                 COMMENT 'GLUE备注',
    `add_time`      DATETIME     DEFAULT NULL             COMMENT '创建时间',
    `update_time`   DATETIME     DEFAULT NULL             COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT = '任务GLUE日志表';

-- ================== 任务日志与报表 ==================

CREATE TABLE `kit_job_log`
(
    `id`                        BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `job_group`                 INT          NOT NULL                COMMENT '执行器主键ID',
    `job_id`                    INT          NOT NULL                COMMENT '任务主键ID',
    `executor_address`          VARCHAR(255) DEFAULT NULL             COMMENT '执行器地址，本次执行的地址',
    `executor_handler`          VARCHAR(255) DEFAULT NULL             COMMENT '任务handler',
    `executor_param`            TEXT         DEFAULT NULL             COMMENT '任务参数',
    `executor_sharding_param`   VARCHAR(20)  DEFAULT NULL             COMMENT '任务分片参数，格式如 1/2',
    `executor_fail_retry_count` INT          NOT NULL DEFAULT 0       COMMENT '失败重试次数',
    `trigger_time`              DATETIME     DEFAULT NULL             COMMENT '调度-时间',
    `trigger_code`              INT          NOT NULL                 COMMENT '调度-结果',
    `trigger_msg`               TEXT         DEFAULT NULL             COMMENT '调度-日志',
    `handle_time`               DATETIME     DEFAULT NULL             COMMENT '执行-时间',
    `handle_code`               INT          NOT NULL                 COMMENT '执行-状态',
    `handle_msg`                TEXT         DEFAULT NULL             COMMENT '执行-日志',
    `alarm_status`              TINYINT      NOT NULL DEFAULT 0       COMMENT '告警状态：0-默认、1-无需告警、2-告警成功、3-告警失败',
    PRIMARY KEY (`id`),
    KEY `i_trigger_time` (`trigger_time`),
    KEY `i_handle_code` (`handle_code`),
    KEY `i_job_group` (`job_group`),
    KEY `i_job_id` (`job_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT = '任务执行日志表';

CREATE TABLE `kit_job_log_report`
(
    `id`            INT      NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `trigger_day`   DATETIME DEFAULT NULL             COMMENT '调度-时间',
    `running_count` INT      NOT NULL DEFAULT 0       COMMENT '运行中-日志数量',
    `suc_count`     INT      NOT NULL DEFAULT 0       COMMENT '执行成功-日志数量',
    `fail_count`    INT      NOT NULL DEFAULT 0       COMMENT '执行失败-日志数量',
    `update_time`   DATETIME DEFAULT NULL             COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `i_trigger_day` (`trigger_day`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT = '任务日志报表';

-- ================== 调度锁 ==================

CREATE TABLE `kit_job_lock`
(
    `lock_name` VARCHAR(50) NOT NULL COMMENT '锁名称',
    PRIMARY KEY (`lock_name`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT = '调度锁表';

-- ================== 初始数据 ==================

START TRANSACTION;

INSERT INTO `kit_job_lock` (`lock_name`)
VALUES ('schedule_lock');

COMMIT;
