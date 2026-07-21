package org.quyq.gwsu.kit.api.knowledge.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.quyq.gwsu.common.core.domain.BaseVO;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgeIngestStage;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgeIngestTaskStatus;

import java.time.LocalDateTime;

/**
 * 知识文档导入任务视图。
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
@Schema(description = "知识文档导入任务视图")
public class KnowledgeIngestTaskVO extends BaseVO {

    @Schema(description = "任务ID")
    private String id;

    @Schema(description = "源文档ID")
    private String sourceDocumentId;

    @Schema(description = "源文档名称")
    private String sourceDocumentName;

    @Schema(description = "任务状态")
    private KnowledgeIngestTaskStatus taskStatus;

    @Schema(description = "当前处理阶段")
    private KnowledgeIngestStage currentStage;

    @Schema(description = "重试次数")
    private Integer retryCount;

    @Schema(description = "错误信息")
    private String errorMessage;

    @Schema(description = "开始时间")
    private LocalDateTime startedAt;

    @Schema(description = "完成时间")
    private LocalDateTime finishedAt;
}
