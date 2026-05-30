package org.quyq.gwsu.common.security.api.vo;

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

    @Schema(description = "所属字典Key")
    private String dictKey;

    @Schema(description = "字典值")
    private String dictValue;

    @Schema(description = "字典标签")
    private String dictLabel;

    @Schema(description = "排序号")
    private Integer sort;

}
