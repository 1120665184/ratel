package org.quyq.gwsu.kit.knowledge.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.quyq.gwsu.common.core.domain.BaseDO;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgeBlockType;

/**
 * 知识 Page Block。
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
@TableName("kit_knowledge_page_block")
@Schema(description = "知识Page Block")
public class KitKnowledgePageBlock extends BaseDO {

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private String id;

    @Schema(description = "Page版本ID")
    private String pageVersionId;

    @Schema(description = "排序号")
    private Integer orderNo;

    @Schema(description = "Block类型")
    private KnowledgeBlockType blockType;

    @Schema(description = "Block内容")
    private String content;
}
