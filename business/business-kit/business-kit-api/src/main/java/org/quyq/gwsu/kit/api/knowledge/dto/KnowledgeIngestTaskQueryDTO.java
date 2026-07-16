package org.quyq.gwsu.kit.api.knowledge.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.quyq.gwsu.common.core.domain.BaseDTO;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgeIngestTaskStatus;

/**
 * 知识文档导入任务查询条件。
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "知识文档导入任务查询条件")
public class KnowledgeIngestTaskQueryDTO extends BaseDTO {

    @Schema(description = "源文档ID")
    private String sourceDocumentId;

    @Schema(description = "任务状态")
    private KnowledgeIngestTaskStatus taskStatus;
}
