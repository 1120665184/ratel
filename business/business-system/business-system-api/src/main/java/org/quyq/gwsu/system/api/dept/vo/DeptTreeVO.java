package org.quyq.gwsu.system.api.dept.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.quyq.gwsu.system.api.dept.enums.DeptTypeEnum;

import java.util.List;

/**
 * 部门树节点 VO
 *
 * @author Quyq
 */
@Data
@Schema(description = "部门树节点")
public class DeptTreeVO {

    @Schema(description = "部门ID")
    private String id;

    @Schema(description = "部门名称")
    private String name;

    @Schema(description = "部门类型")
    private DeptTypeEnum type;

    @Schema(description = "是否启用")
    private Boolean enabled;

    @Schema(description = "父部门ID")
    private String parentId;

    @Schema(description = "排序号")
    private Integer sort;

    @Schema(description = "子部门列表")
    private List<DeptTreeVO> children;
}