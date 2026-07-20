package org.quyq.gwsu.kit.api.knowledge.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgePageVersionStatus;

import java.time.LocalDateTime;
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

    @Schema(description = "当前版本号")
    private Integer currentVersionNo;

    @Schema(description = "当前版本状态")
    private KnowledgePageVersionStatus currentVersionStatus;

    @Schema(description = "当前版本发布时间")
    private LocalDateTime currentPublishedAt;

    @Schema(description = "Block列表")
    private List<KnowledgePageBlockVO> blocks;
}
