package org.quyq.gwsu.security.api.tablemodel.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.quyq.gwsu.common.core.domain.BaseVO;

/**
 * 表基本信息
 *
 * @author Quyq
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "表基本信息")
public class TableModelTableVO extends BaseVO {

    @Schema(description = "主键ID")
    private String id;

    @Schema(description = "表名")
    private String tableName;

    @Schema(description = "模块前缀")
    private String modulePrefix;

    @Schema(description = "数据源")
    private String dataSource;

    @Schema(description = "表注释")
    private String tableComment;
}
