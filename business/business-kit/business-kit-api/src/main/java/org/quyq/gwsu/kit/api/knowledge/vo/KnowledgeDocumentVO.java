package org.quyq.gwsu.kit.api.knowledge.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.quyq.gwsu.common.core.domain.BaseVO;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgeDocumentStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 知识源文档视图。
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
@Schema(description = "知识源文档视图")
public class KnowledgeDocumentVO extends BaseVO {

    @Schema(description = "源文档ID")
    private String id;

    @Schema(description = "文件ID")
    private String fileId;

    @Schema(description = "文件名")
    private String fileName;

    @Schema(description = "文档处理状态")
    private KnowledgeDocumentStatus documentStatus;

    @Schema(description = "目标Page ID")
    private String targetPageId;

    @Schema(description = "处理信息")
    private String processMessage;

    @Schema(description = "处理完成时间")
    private LocalDateTime processedAt;

    @Schema(description = "授权角色编码")
    private List<String> roleCodes;
}
