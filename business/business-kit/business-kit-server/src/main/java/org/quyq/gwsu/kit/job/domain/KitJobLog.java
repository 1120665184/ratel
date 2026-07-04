package org.quyq.gwsu.kit.job.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.quyq.gwsu.common.core.domain.BaseDO;

import java.time.LocalDateTime;

/**
 * 任务日志
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
@TableName("kit_job_log")
@Schema(description = "任务日志")
public class KitJobLog extends BaseDO {

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private String id;

    @TableField("job_group")
    @Schema(description = "执行器主键ID")
    private String jobGroup;

    @TableField("job_id")
    @Schema(description = "任务主键ID")
    private String jobId;

    @TableField("executor_address")
    @Schema(description = "执行器地址")
    private String executorAddress;

    @TableField("executor_handler")
    @Schema(description = "任务handler")
    private String executorHandler;

    @TableField("executor_param")
    @Schema(description = "任务参数")
    private String executorParam;

    @TableField("executor_sharding_param")
    @Schema(description = "任务分片参数")
    private String executorShardingParam;

    @TableField("executor_fail_retry_count")
    @Schema(description = "失败重试次数")
    private int executorFailRetryCount;

    @TableField("trigger_time")
    @Schema(description = "调度时间")
    private LocalDateTime triggerTime;

    @TableField("trigger_code")
    @Schema(description = "调度结果")
    private int triggerCode;

    @TableField("trigger_msg")
    @Schema(description = "调度日志")
    private String triggerMsg;

    @TableField("handle_time")
    @Schema(description = "执行时间")
    private LocalDateTime handleTime;

    @TableField("handle_code")
    @Schema(description = "执行状态")
    private int handleCode;

    @TableField("handle_msg")
    @Schema(description = "执行日志")
    private String handleMsg;

    @TableField("alarm_status")
    @Schema(description = "告警状态：0-默认、1-无需告警、2-告警成功、3-告警失败")
    private int alarmStatus;

}
