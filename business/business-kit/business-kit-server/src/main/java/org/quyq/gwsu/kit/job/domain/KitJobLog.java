package org.quyq.gwsu.kit.job.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 任务日志
 */
@Data
@TableName("kit_job_log")
public class KitJobLog {

    @TableId(type = IdType.AUTO)
    private long id;

    // job info
    @TableField("job_group")
    private int jobGroup;

    @TableField("job_id")
    private int jobId;

    // execute info
    @TableField("executor_address")
    private String executorAddress;

    @TableField("executor_handler")
    private String executorHandler;

    @TableField("executor_param")
    private String executorParam;

    @TableField("executor_sharding_param")
    private String executorShardingParam;

    @TableField("executor_fail_retry_count")
    private int executorFailRetryCount;

    // trigger info
    @TableField("trigger_time")
    private Date triggerTime;

    @TableField("trigger_code")
    private int triggerCode;

    @TableField("trigger_msg")
    private String triggerMsg;

    // handle info
    @TableField("handle_time")
    private Date handleTime;

    @TableField("handle_code")
    private int handleCode;

    @TableField("handle_msg")
    private String handleMsg;

    // alarm info
    @TableField("alarm_status")
    private int alarmStatus;

}
