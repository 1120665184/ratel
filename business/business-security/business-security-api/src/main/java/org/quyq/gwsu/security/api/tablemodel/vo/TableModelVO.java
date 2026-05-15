package org.quyq.gwsu.security.api.tablemodel.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.quyq.gwsu.common.core.domain.BaseVO;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "表模型信息")
public class TableModelVO extends BaseVO {

    @Schema(description = "主键ID")
    private String id;

    @Schema(description = "接口资源ID")
    private String apiId;

    @Schema(description = "模块前缀")
    private String modulePrefix;

    @Schema(description = "数据源名称")
    private String datasource;

    @Schema(description = "表名")
    private String tableName;

    @Schema(description = "字段配置列表")
    private List<TableModelFieldVO> fields;

    @Schema(description = "接口地址")
    private String reqPath;

    @Schema(description = "请求方式")
    private String reqMethod;

    @Schema(description = "接口摘要")
    private String summary;
}
