package org.quyq.gwsu.security.api.config.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.quyq.gwsu.common.core.domain.BaseDTO;

/**
 * 配置查询条件
 *
 * @author Quyq
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "配置查询条件")
public class ConfigQueryDTO extends BaseDTO {

    @Schema(description = "配置键（模糊查询）")
    private String configKey;

    @Schema(description = "配置名称（模糊查询）")
    private String configName;

    @Schema(description = "值类型：1-基本类型 2-JSON")
    private Integer valueType;

    @Schema(description = "配置类型：1-系统 2-自定义")
    private Integer configType;

    @Schema(description = "所属模块前缀")
    private String modulePrefix;

}
