package org.quyq.gwsu.security.api.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.quyq.gwsu.common.core.domain.BaseDTO;

/**
 * 组件查询条件
 *
 * @author Quyq
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "组件查询条件")
public class ComponentQueryDTO extends BaseDTO {

    @Schema(description = "组件名称（模糊查询）")
    private String componentName;

    @Schema(description = "组件分类：display/chart/form")
    private String category;

    @Schema(description = "状态：true-启用 false-禁用")
    private Boolean status;
}
