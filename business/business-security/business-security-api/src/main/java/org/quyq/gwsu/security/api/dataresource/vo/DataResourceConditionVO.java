package org.quyq.gwsu.security.api.dataresource.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.quyq.gwsu.common.core.domain.BaseVO;

import java.util.List;

/**
 * 数据资源字段条件信息
 *
 * @author Quyq
 * @date 2026/4/20
 */
@Data
@Schema(description = "数据资源字段条件信息")
public class DataResourceConditionVO extends BaseVO {

    @Schema(description = "主键ID")
    private String id;

    @Schema(description = "字段名")
    private String fieldName;

    @Schema(description = "显示过滤字段为null的数据")
    private Boolean showNull;

    @Schema(description = "关联的用户数据资源字段")
    private List<String> userResourceFields;

    @Schema(description = "断言类型")
    private String assertType;

    @Schema(description = "与上一个条件的关联关系")
    private String relationship;

    @Schema(description = "排序号")
    private Integer sort;

}
