package org.quyq.gwsu.security.api.dict.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.quyq.gwsu.common.core.domain.BaseVO;

/**
 * 字典信息
 *
 * @author Quyq
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "字典信息")
public class DictVO extends BaseVO {

    @Schema(description = "主键ID")
    private String id;

    @Schema(description = "字典键")
    private String dictKey;

    @Schema(description = "字典名称")
    private String dictName;

    @Schema(description = "字典类型：1-系统 2-自定义")
    private Integer dictType;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "所属模块前缀")
    private String modulePrefix;

    @Schema(description = "字典值数量")
    private Integer valueCount;

}
