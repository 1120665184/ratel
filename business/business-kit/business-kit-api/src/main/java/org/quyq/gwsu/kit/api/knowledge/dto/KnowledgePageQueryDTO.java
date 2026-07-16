package org.quyq.gwsu.kit.api.knowledge.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.quyq.gwsu.common.core.domain.BaseDTO;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgePageStatus;

/**
 * 知识 Page 查询条件。
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "知识Page查询条件")
public class KnowledgePageQueryDTO extends BaseDTO {

    @Schema(description = "标题")
    private String title;

    @Schema(description = "Page状态")
    private KnowledgePageStatus pageStatus;
}
