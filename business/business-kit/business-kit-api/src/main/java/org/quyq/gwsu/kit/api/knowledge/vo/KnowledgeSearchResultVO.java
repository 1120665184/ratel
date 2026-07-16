package org.quyq.gwsu.kit.api.knowledge.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 知识库检索结果。
 */
@Data
@Accessors(chain = true)
@Schema(description = "知识库检索结果")
public class KnowledgeSearchResultVO {

    @Schema(description = "Chunk ID")
    private String chunkId;

    @Schema(description = "Page ID")
    private String pageId;

    @Schema(description = "Page版本ID")
    private String pageVersionId;

    @Schema(description = "Page Block ID")
    private String pageBlockId;

    @Schema(description = "源文档ID")
    private String sourceDocumentId;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "标题路径")
    private String headingPath;

    @Schema(description = "内容")
    private String content;

    @Schema(description = "Chunk序号")
    private Integer chunkOrder;

    @Schema(description = "相关性得分")
    private Double score;
}
