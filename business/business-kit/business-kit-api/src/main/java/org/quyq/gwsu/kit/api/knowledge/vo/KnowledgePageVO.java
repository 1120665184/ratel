package org.quyq.gwsu.kit.api.knowledge.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.quyq.gwsu.common.core.domain.BaseVO;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgePageStatus;

/**
 * 知识 Page 视图。
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
@Schema(description = "知识Page视图")
public class KnowledgePageVO extends BaseVO {

    @Schema(description = "Page ID")
    private String id;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "Page状态")
    private KnowledgePageStatus pageStatus;

    @Schema(description = "当前版本ID")
    private String currentVersionId;
}
