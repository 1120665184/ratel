package org.quyq.gwsu.system.api.dept.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.quyq.gwsu.common.core.domain.BaseVO;
import org.quyq.gwsu.system.api.dept.enums.DeptTypeEnum;

import java.util.List;

/**
 * 部门详情 VO
 *
 * @author Quyq
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "部门详情")
public class DeptVO extends BaseVO {

    @Schema(description = "部门ID")
    private String id;

    @Schema(description = "部门名称")
    private String name;

    @Schema(description = "部门类型")
    private DeptTypeEnum type;

    @Schema(description = "主父部门ID")
    private String parentId;

    @Schema(description = "主父部门名称")
    private String parentName;

    @Schema(description = "所有父部门ID列表")
    private List<String> parentIds;

    @Schema(description = "额外父部门列表（带名称）")
    private List<ExtraParentVO> extraParents;

    /**
     * 额外父部门 VO
     */
    @Data
    @Schema(description = "额外父部门")
    public static class ExtraParentVO {
        @Schema(description = "部门ID")
        private String id;

        @Schema(description = "部门名称")
        private String name;
    }

    @Schema(description = "是否启用")
    private Boolean enabled;

    @Schema(description = "排序号")
    private Integer sort;

    @Schema(description = "层级路径")
    private String path;
}