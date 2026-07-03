package org.quyq.gwsu.kit.job.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 执行器注册信息
 */
@Data
@TableName("kit_job_registry")
public class KitJobRegistry {

    @TableId(type = IdType.AUTO)
    private long id;

    @TableField("registry_group")
    private String registryGroup;

    @TableField("registry_key")
    private String registryKey;

    @TableField("registry_value")
    private String registryValue;

    @TableField("update_time")
    private Date updateTime;

}
