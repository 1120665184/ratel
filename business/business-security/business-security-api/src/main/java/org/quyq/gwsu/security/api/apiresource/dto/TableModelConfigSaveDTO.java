package org.quyq.gwsu.security.api.apiresource.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "表模型配置保存")
public class TableModelConfigSaveDTO {

    @Schema(description = "关联的接口-表模型绑定ID列表，为空表示独立表模型")
    private List<String> tableModelIds;

    @Schema(description = "表名")
    private String tableName;

    @Schema(description = "模块前缀")
    private String modulePrefix;

    @Schema(description = "数据源名称")
    private String datasource;

    @Schema(description = "配置说明")
    private String description;
}
