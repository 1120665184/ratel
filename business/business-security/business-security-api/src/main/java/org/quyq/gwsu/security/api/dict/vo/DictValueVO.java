package org.quyq.gwsu.security.api.dict.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.quyq.gwsu.common.core.domain.BaseVO;

/**
 * 字典值信息
 *
 * @author Quyq
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "字典值信息")
public class DictValueVO extends BaseVO {

    @Schema(description = "主键ID")
    private String id;

    @Schema(description = "所属字典ID")
    private String dictId;

    @Schema(description = "字典值")
    private String dictValue;

    @Schema(description = "排序号")
    private Integer sort;

}
