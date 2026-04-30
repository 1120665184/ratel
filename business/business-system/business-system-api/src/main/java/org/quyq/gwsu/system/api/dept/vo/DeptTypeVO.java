package org.quyq.gwsu.system.api.dept.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 部门类型 VO
 *
 * @author Quyq
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "部门类型")
public class DeptTypeVO {

    @Schema(description = "类型编码")
    private Integer code;

    @Schema(description = "类型名称")
    private String name;
}
