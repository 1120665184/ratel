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
 * 任务信息
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
@TableName("kit_job_info")
@Schema(description = "任务信息")
public class KitJobInfo extends BaseDO {

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private String id;

    @Schema(description = "任务名称")
    private String name;

    @Schema(description = "负责人")
    private String author;

    @TableField("alarm_email")
    @Schema(description = "报警邮件")
    private String alarmEmail;

    @TableField("schedule_type")
    @Schema(description = "调度类型")
    private String scheduleType;

    @TableField("schedule_conf")
    @Schema(description = "调度配置")
    private String scheduleConf;

    @TableField("misfire_strategy")
    @Schema(description = "调度过期策略")
    private String misfireStrategy;

    @TableField("executor_route_strategy")
    @Schema(description = "执行器路由策略")
    private String executorRouteStrategy;

    @TableField("executor_handler")
    @Schema(description = "任务handler")
    private String executorHandler;

    @TableField("executor_param")
    @Schema(description = "任务参数")
    private String executorParam;

    @TableField("executor_block_strategy")
    @Schema(description = "阻塞处理策略")
    private String executorBlockStrategy;

    @TableField("executor_timeout")
    @Schema(description = "任务执行超时时间，单位秒")
    private int executorTimeout;

    @TableField("executor_fail_retry_count")
    @Schema(description = "失败重试次数")
    private int executorFailRetryCount;

    @TableField("glue_type")
    @Schema(description = "GLUE类型")
    private String glueType;

    @TableField("glue_source")
    @Schema(description = "GLUE源代码")
    private String glueSource;

    @TableField("glue_remark")
    @Schema(description = "GLUE备注")
    private String glueRemark;

    @TableField("glue_updatetime")
    @Schema(description = "GLUE更新时间")
    private LocalDateTime glueUpdatetime;

    @TableField("child_jobid")
    @Schema(description = "子任务ID，多个逗号分隔")
    private String childJobId;

    @TableField("trigger_status")
    @Schema(description = "调度状态：0-停止，1-运行")
    private int triggerStatus;

    @TableField("trigger_last_time")
    @Schema(description = "上次调度时间")
    private long triggerLastTime;

    @TableField("trigger_next_time")
    @Schema(description = "下次调度时间")
    private long triggerNextTime;

}
