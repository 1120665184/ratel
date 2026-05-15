package org.quyq.gwsu.security.api.apiresource.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.quyq.gwsu.common.core.domain.BaseVO;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "表模型配置信息")
public class TableModelConfigVO extends BaseVO {

    @Schema(description = "主键ID")
    private String id;

    @Schema(description = "关联的接口-表模型绑定ID")
    private String tableModelId;

    @Schema(description = "表名")
    private String tableName;

    @Schema(description = "模块前缀")
    private String modulePrefix;

    @Schema(description = "数据源名称")
    private String datasource;

    @Schema(description = "配置说明")
    private String description;
}
