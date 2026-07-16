package org.quyq.gwsu.kit.knowledge.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.quyq.gwsu.common.core.domain.BaseDO;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgePageStatus;

/**
 * 知识 Page。
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
@TableName("kit_knowledge_page")
@Schema(description = "知识Page")
public class KitKnowledgePage extends BaseDO {

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private String id;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "Page状态")
    private KnowledgePageStatus pageStatus;

    @Schema(description = "当前版本ID")
    private String currentVersionId;
}
