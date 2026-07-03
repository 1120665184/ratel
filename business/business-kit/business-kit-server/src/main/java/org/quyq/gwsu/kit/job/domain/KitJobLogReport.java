package org.quyq.gwsu.kit.job.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 任务日志报表
 */
@Data
@TableName("kit_job_log_report")
public class KitJobLogReport {

    @TableId(type = IdType.AUTO)
    private int id;

    @TableField("trigger_day")
    private Date triggerDay;

    @TableField("running_count")
    private int runningCount;

    @TableField("suc_count")
    private int sucCount;

    @TableField("fail_count")
    private int failCount;

    @TableField("update_time")
    private Date updateTime;

}
