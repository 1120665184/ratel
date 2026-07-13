package org.quyq.gwsu.security.api.dataresource.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.quyq.gwsu.common.core.domain.BaseVO;

import java.util.List;

/**
 * 数据资源配置信息
 *
 * @author Quyq
 * @date 2026/4/20
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "数据资源配置信息")
public class DataResourceVO extends BaseVO {

    @Schema(description = "主键ID")
    private String id;

    @Schema(description = "Catalog 名称，为空时匹配所有 Catalog")
    private String catalogName;

    @Schema(description = "数据库/Schema 名称，为空时匹配所有数据库或 Schema")
    private String schemaName;

    @Schema(description = "表名")
    private String tableName;

    @Schema(description = "规则描述")
    private String description;

    @Schema(description = "是否支持SELF_ONLY过滤")
    private Boolean supportSelfOnly;

    @Schema(description = "SELF_ONLY过滤时使用的字段名，即目标表中记录创建人的字段")
    private String selfOnlyField;

    @Schema(description = "启用状态")
    private Boolean status;

    @Schema(description = "字段条件列表")
    private List<DataResourceConditionVO> conditions;

}
