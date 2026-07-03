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
 * 执行器注册信息
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
@TableName("kit_job_registry")
@Schema(description = "执行器注册信息")
public class KitJobRegistry extends BaseDO {

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private String id;

    @TableField("registry_group")
    @Schema(description = "注册分组")
    private String registryGroup;

    @TableField("registry_key")
    @Schema(description = "注册标识")
    private String registryKey;

    @TableField("registry_value")
    @Schema(description = "注册值（地址）")
    private String registryValue;

    @TableField("update_time")
    @Schema(description = "更新时间（业务字段）")
    private Date updateTime;

}
