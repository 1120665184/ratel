package org.quyq.gwsu.kit.job.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 任务日志Glue代码记录
 */
@Data
@TableName("kit_job_log_glue")
public class KitJobLogGlue {

    @TableId(type = IdType.AUTO)
    private int id;

    @TableField("job_id")
    private int jobId;              // 任务主键ID

    @TableField("glue_type")
    private String glueType;        // GLUE类型

    @TableField("glue_source")
    private String glueSource;

    @TableField("glue_remark")
    private String glueRemark;

    @TableField("add_time")
    private Date addTime;

    @TableField("update_time")
    private Date updateTime;

}
