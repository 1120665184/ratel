package org.quyq.gwsu.security.catalog.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.quyq.gwsu.common.core.domain.BaseVO;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Catalog组件信息")
public class SecurityCatalogComponentVO extends BaseVO {

    @Schema(description = "主键ID")
    private String id;

    @Schema(description = "组件名")
    private String componentName;

    @Schema(description = "组件描述（给AI看的）")
    private String description;

    @Schema(description = "组件属性JSON Schema定义")
    private String propsSchema;

    @Schema(description = "默认属性值")
    private String defaultProps;

    @Schema(description = "组件分类（display/chart/form）")
    private String category;

    @Schema(description = "排序号")
    private Integer sortOrder;

    @Schema(description = "状态：0-禁用 1-正常")
    private Boolean status;
}
