package org.quyq.gwsu.system.api.dept.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.quyq.gwsu.system.api.dept.enums.DeptTypeEnum;

import java.util.List;

/**
 * 保存部门请求
 *
 * @author Quyq
 */
@Data
@Schema(description = "保存部门请求")
public class DeptSaveDTO {

    @Schema(description = "部门ID（更新时必填）")
    private String id;

    @Schema(description = "部门名称")
    private String name;

    @Schema(description = "部门类型")
    private DeptTypeEnum type;

    @Schema(description = "主父部门ID")
    private String parentId;

    @Schema(description = "额外父部门ID列表")
    private List<String> extraParentIds;

    @Schema(description = "是否启用")
    private Boolean enabled;

    @Schema(description = "排序号")
    private Integer sort;
}