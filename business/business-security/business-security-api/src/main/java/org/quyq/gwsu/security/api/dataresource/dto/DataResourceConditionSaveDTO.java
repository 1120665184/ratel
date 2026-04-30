package org.quyq.gwsu.security.api.dataresource.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 数据资源字段条件保存/更新 DTO
 *
 * @author Quyq
 * @date 2026/4/20
 */
@Data
@Schema(description = "数据资源字段条件保存/更新DTO")
public class DataResourceConditionSaveDTO {

    @Schema(description = "主键ID（更新时可选）")
    private String id;

    @Schema(description = "字段名")
    private String fieldName;

    @Schema(description = "显示过滤字段为null的数据")
    private Boolean showNull;

    @Schema(description = "关联的用户数据资源字段")
    private List<String> userResourceFields;

    @Schema(description = "断言类型：EQ-等于 LIKE-模糊匹配")
    private String assertType;

    @Schema(description = "与上一个条件的关联关系：AND-与 OR-或")
    private String relationship;

    @Schema(description = "排序号")
    private Integer sort;

}
