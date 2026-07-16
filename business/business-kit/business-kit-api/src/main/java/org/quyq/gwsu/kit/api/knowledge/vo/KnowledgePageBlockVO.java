package org.quyq.gwsu.kit.api.knowledge.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.quyq.gwsu.common.core.domain.BaseVO;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgeBlockType;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgeSourceType;

/**
 * 知识 Page Block 视图。
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
@Schema(description = "知识Page Block视图")
public class KnowledgePageBlockVO extends BaseVO {

    @Schema(description = "Block ID")
    private String id;

    @Schema(description = "Page版本ID")
    private String pageVersionId;

    @Schema(description = "排序号")
    private Integer orderNo;

    @Schema(description = "Block类型")
    private KnowledgeBlockType blockType;

    @Schema(description = "Block内容")
    private String content;

    @Schema(description = "来源类型")
    private KnowledgeSourceType sourceType;

    @Schema(description = "源文档ID")
    private String sourceDocumentId;

    @Schema(description = "来源定位")
    private String sourceLocator;
}
