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

import java.util.List;

/**
 * 任务组（执行器）
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
@TableName("kit_job_group")
@Schema(description = "执行器信息")
public class KitJobGroup extends BaseDO {

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private String id;

    @TableField("app_name")
    @Schema(description = "执行器AppName")
    private String appname;

    @Schema(description = "执行器名称")
    private String name;

    @TableField("address_type")
    @Schema(description = "执行器地址类型：0=自动注册、1=手动录入")
    private int addressType;

    @TableField("address_list")
    @Schema(description = "执行器地址列表，多地址逗号分隔")
    private String addressList;

    @TableField("access_token")
    @Schema(description = "执行器AccessToken")
    private String accessToken;

    @TableField("update_time")
    @Schema(description = "更新时间（业务字段）")
    private java.util.Date updateTime;

    @TableField(exist = false)
    private List<String> registryList;

    public List<String> getRegistryList() {
        if (addressList != null && !addressList.trim().isEmpty()) {
            registryList = List.of(addressList.split(","));
        }
        return registryList;
    }

}
