package org.quyq.gwsu.kit.api.knowledge.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 知识源文档角色保存参数。
 */
@Data
@Schema(description = "知识源文档角色保存参数")
public class KnowledgeDocumentRoleSaveDTO {

    @Schema(description = "源文档ID")
    private String sourceDocumentId;

    @Schema(description = "租户ID")
    private String tenantId;

    @Schema(description = "授权角色编码；为空表示开放文档")
    private List<String> roleCodes;
}
