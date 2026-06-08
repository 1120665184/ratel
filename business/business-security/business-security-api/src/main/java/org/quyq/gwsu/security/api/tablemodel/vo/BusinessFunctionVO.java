package org.quyq.gwsu.security.api.tablemodel.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.quyq.gwsu.common.core.domain.BaseVO;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "业务功能信息")
public class BusinessFunctionVO extends BaseVO {

    @Schema(description = "主键ID")
    private String id;

    @Schema(description = "业务名称")
    private String name;

    @Schema(description = "业务简介")
    private String summary;

    @Schema(description = "详细介绍（Markdown格式）")
    private String detail;

    @Schema(description = "排序号")
    private Integer sortOrder;

    @Schema(description = "关联表模型数量")
    private Integer tableCount;

    @Schema(description = "关联的表模型ID列表（保存时使用）")
    private List<String> tableIds;
}
