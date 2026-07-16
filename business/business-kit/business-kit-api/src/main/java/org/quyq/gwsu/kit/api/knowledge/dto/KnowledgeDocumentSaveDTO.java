package org.quyq.gwsu.kit.api.knowledge.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 知识源文档保存参数。
 */
@Data
@Schema(description = "知识源文档保存参数")
public class KnowledgeDocumentSaveDTO {

    @Schema(description = "源文档ID")
    private String id;

    @Schema(description = "租户ID")
    private String tenantId;

    @Schema(description = "文件ID")
    private String fileId;

    @Schema(description = "文件名")
    private String fileName;

    @Schema(description = "目标Page ID")
    private String targetPageId;

    @Schema(description = "授权角色编码；为空表示开放文档")
    private List<String> roleCodes;
}
