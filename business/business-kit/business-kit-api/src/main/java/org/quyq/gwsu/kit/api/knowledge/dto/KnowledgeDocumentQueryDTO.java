package org.quyq.gwsu.kit.api.knowledge.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.quyq.gwsu.common.core.domain.BaseDTO;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgeDocumentStatus;

/**
 * 知识源文档查询条件。
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "知识源文档查询条件")
public class KnowledgeDocumentQueryDTO extends BaseDTO {

    @Schema(description = "文件名")
    private String fileName;

    @Schema(description = "文档处理状态")
    private KnowledgeDocumentStatus documentStatus;
}
