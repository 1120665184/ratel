package org.quyq.gwsu.security.api.dict.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 字典保存请求
 *
 * @author Quyq
 */
@Data
@Schema(description = "字典保存请求")
public class DictSaveDTO {

    @Schema(description = "主键ID，新增时为空")
    private String id;

    @Schema(description = "字典键")
    private String dictKey;

    @Schema(description = "字典名称")
    private String dictName;

    @Schema(description = "描述")
    private String description;

}
