package org.quyq.gwsu.kit.api.knowledge.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 知识 Page 明细视图。
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "知识Page明细视图")
public class KnowledgePageDetailVO extends KnowledgePageVO {

    @Schema(description = "Markdown内容快照")
    private String markdownContent;

    @Schema(description = "Block列表")
    private List<KnowledgePageBlockVO> blocks;
}
