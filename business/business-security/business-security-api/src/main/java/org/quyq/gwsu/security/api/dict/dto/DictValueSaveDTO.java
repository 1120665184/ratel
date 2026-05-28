package org.quyq.gwsu.security.api.dict.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 字典值保存请求
 *
 * @author Quyq
 */
@Data
@Schema(description = "字典值保存请求")
public class DictValueSaveDTO {

    @Schema(description = "主键ID，新增时为空")
    private String id;

    @Schema(description = "所属字典ID")
    private String dictId;

    @Schema(description = "字典值")
    private String dictValue;

    @Schema(description = "排序号")
    private Integer sort;

}
