package org.quyq.gwsu.common.security.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.quyq.gwsu.common.core.domain.BaseVO;

/**
 * 配置信息
 *
 * @author Quyq
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "配置信息")
public class ConfigVO extends BaseVO {

    @Schema(description = "主键ID")
    private String id;

    @Schema(description = "配置键")
    private String configKey;

    @Schema(description = "配置名称")
    private String configName;

    @Schema(description = "配置值")
    private String configValue;

    @Schema(description = "值类型：1-STR 2-NUMBER 3-BOOL 4-JSON")
    private int valueType;

    @Schema(description = "配置类型：1-系统 2-自定义")
    private Integer configType;

    @Schema(description = "描述")
    private String description;


}
