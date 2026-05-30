package org.quyq.gwsu.security.api.config.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.quyq.gwsu.security.api.config.enums.ConfigValueType;

/**
 * 配置保存请求
 *
 * @author Quyq
 */
@Data
@Schema(description = "配置保存请求")
public class ConfigSaveDTO {

    @Schema(description = "主键ID，新增时为空")
    private String id;

    @Schema(description = "配置键")
    private String configKey;

    @Schema(description = "配置名称")
    private String configName;

    @Schema(description = "配置值")
    private String configValue;

    @Schema(description = "值类型：1-STR 2-NUMBER 3-BOOL 4-JSON")
    private ConfigValueType valueType;

    @Schema(description = "描述")
    private String description;

}
