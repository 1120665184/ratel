package org.quyq.gwsu.kit.api.job.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.quyq.gwsu.common.core.domain.BaseDTO;

/**
 * 任务新增/编辑入参对象，支持 URL / BEAN / GLUE 三种任务模式
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "任务新增/编辑入参对象")
public class JobInfoCreateDTO extends BaseDTO {

    // ==================== 通用字段 ====================

    @Schema(description = "主键ID，编辑时必传")
    private String id;

    @Schema(description = "任务名称")
    private String name;

    @Schema(description = "负责人")
    private String author;

    @Schema(description = "告警邮件，多个逗号分隔")
    private String alarmEmail;

    @Schema(description = "调度过期策略：DO_NOTHING / FIRE_ONCE")
    private String misfireStrategy;

    @Schema(description = "路由策略")
    private String executorRouteStrategy;

    @Schema(description = "阻塞策略：SERIAL_EXECUTION / DISCARD_LATER / COVER_EARLY")
    private String executorBlockStrategy;

    @Schema(description = "超时时间，单位秒")
    private int executorTimeout;

    @Schema(description = "失败重试次数")
    private int executorFailRetryCount;

    // ==================== 任务模式字段 ====================

    @Schema(description = "任务模式：URL / BEAN / GLUE")
    private String jobMode;

    // --- URL 模式 ---
    @Schema(description = "URL模式 - 请求前缀，如 http:// 或 https://")
    private String prefix;

    @Schema(description = "URL模式 - 请求地址")
    private String url;

    @Schema(description = "URL模式 - 请求体JSON")
    private String bodyJson;

    // --- BEAN 模式 ---
    @Schema(description = "BEAN模式 - 执行器Handler标识")
    private String executorHandler;

    @Schema(description = "BEAN模式 - 执行参数")
    private String executorParam;

    // --- GLUE 模式 ---
    @Schema(description = "GLUE模式 - 脚本类型，如 GLUE_GROOVY / GLUE_SHELL / GLUE_PYTHON 等")
    private String glueType;

    @Schema(description = "GLUE模式 - 脚本源码")
    private String glueSource;

    @Schema(description = "GLUE模式 - 脚本备注")
    private String glueRemark;

    // ==================== 调度配置字段 ====================

    @Schema(description = "调度类型：NONE / CRON / FIX_RATE")
    private String scheduleType;

    @Schema(description = "调度配置：CRON模式下为cron表达式，FIX_RATE模式下为间隔秒数")
    private String scheduleConf;

    @Schema(description = "子任务ID，多个逗号分隔")
    private String childJobId;

}
