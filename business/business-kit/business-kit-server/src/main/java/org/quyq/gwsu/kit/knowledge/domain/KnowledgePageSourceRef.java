package org.quyq.gwsu.kit.knowledge.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.quyq.gwsu.common.core.domain.BaseDO;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgeSourceType;

/**
 * 知识 Page Block 来源关系。
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
@TableName("knowledge_page_source_ref")
@Schema(description = "知识Page Block来源关系")
public class KnowledgePageSourceRef extends BaseDO {

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private String id;

    @Schema(description = "Page Block ID")
    private String pageBlockId;

    @Schema(description = "来源类型")
    private KnowledgeSourceType sourceType;

    @Schema(description = "源文档ID")
    private String sourceDocumentId;

    @Schema(description = "来源定位")
    private String sourceLocator;
}
