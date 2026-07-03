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
 * 任务日志Glue代码记录
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
@TableName("kit_job_log_glue")
@Schema(description = "任务GLUE日志")
public class KitJobLogGlue extends BaseDO {

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private String id;

    @TableField("job_id")
    @Schema(description = "任务主键ID")
    private String jobId;

    @TableField("glue_type")
    @Schema(description = "GLUE类型")
    private String glueType;

    @TableField("glue_source")
    @Schema(description = "GLUE源代码")
    private String glueSource;

    @TableField("glue_remark")
    @Schema(description = "GLUE备注")
    private String glueRemark;

    @TableField("add_time")
    @Schema(description = "创建时间（业务字段）")
    private Date addTime;

    @TableField("update_time")
    @Schema(description = "更新时间（业务字段）")
    private Date updateTime;

}
