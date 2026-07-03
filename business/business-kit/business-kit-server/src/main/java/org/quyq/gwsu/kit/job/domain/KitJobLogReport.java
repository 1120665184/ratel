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

import java.util.Date;

/**
 * 任务日志报表
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
@TableName("kit_job_log_report")
@Schema(description = "任务日志报表")
public class KitJobLogReport extends BaseDO {

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private String id;

    @TableField("trigger_day")
    @Schema(description = "调度时间")
    private Date triggerDay;

    @TableField("running_count")
    @Schema(description = "运行中-日志数量")
    private int runningCount;

    @TableField("suc_count")
    @Schema(description = "执行成功-日志数量")
    private int sucCount;

    @TableField("fail_count")
    @Schema(description = "执行失败-日志数量")
    private int failCount;

    @TableField("update_time")
    @Schema(description = "更新时间（业务字段）")
    private Date updateTime;

}
