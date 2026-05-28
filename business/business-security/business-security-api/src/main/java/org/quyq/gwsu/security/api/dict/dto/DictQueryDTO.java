package org.quyq.gwsu.security.api.dict.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.quyq.gwsu.common.core.domain.BaseDTO;

/**
 * 字典查询条件
 *
 * @author Quyq
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "字典查询条件")
public class DictQueryDTO extends BaseDTO {

    @Schema(description = "字典键（模糊查询）")
    private String dictKey;

    @Schema(description = "字典名称（模糊查询）")
    private String dictName;

    @Schema(description = "字典类型：1-系统 2-自定义")
    private Integer dictType;

    @Schema(description = "所属模块前缀")
    private String modulePrefix;

}
