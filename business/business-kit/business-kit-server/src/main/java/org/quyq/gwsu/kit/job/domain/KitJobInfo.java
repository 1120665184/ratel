package org.quyq.gwsu.kit.job.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 任务信息
 */
@Data
@TableName("kit_job_info")
public class KitJobInfo {

    @TableId(type = IdType.AUTO)
    private int id;                 // 主键ID

    @TableField("job_group")
    private int jobGroup;           // 执行器主键ID

    private String name;

    private String author;          // 负责人

    @TableField("alarm_email")
    private String alarmEmail;      // 报警邮件

    @TableField("schedule_type")
    private String scheduleType;            // 调度类型：ScheduleTypeEnum

    @TableField("schedule_conf")
    private String scheduleConf;            // 调度配置，值含义取决于调度类型

    @TableField("misfire_strategy")
    private String misfireStrategy;         // 调度过期策略：MisfireStrategyEnum

    @TableField("executor_route_strategy")
    private String executorRouteStrategy;   // 执行器路由策略：ExecutorRouteStrategyEnum

    @TableField("executor_handler")
    private String executorHandler;         // 执行器，任务Handler名称

    @TableField("executor_param")
    private String executorParam;           // 执行器，任务参数

    @TableField("executor_block_strategy")
    private String executorBlockStrategy;   // 阻塞处理策略：ExecutorBlockStrategyEnum

    @TableField("executor_timeout")
    private int executorTimeout;            // 任务执行超时时间，单位秒

    @TableField("executor_fail_retry_count")
    private int executorFailRetryCount;     // 失败重试次数

    @TableField("glue_type")
    private String glueType;        // GLUE类型：GlueTypeEnum

    @TableField("glue_source")
    private String glueSource;      // GLUE源代码

    @TableField("glue_remark")
    private String glueRemark;      // GLUE备注

    @TableField("glue_updatetime")
    private Date glueUpdatetime;    // GLUE更新时间

    @TableField("child_jobid")
    private String childJobId;      // 子任务ID，多个逗号分隔

    @TableField("trigger_status")
    private int triggerStatus;      // 调度状态：TriggerStatus

    @TableField("trigger_last_time")
    private long triggerLastTime;   // 上次调度时间

    @TableField("trigger_next_time")
    private long triggerNextTime;   // 下次调度时间

    @TableField("add_time")
    private Date addTime;

    @TableField("update_time")
    private Date updateTime;

}
