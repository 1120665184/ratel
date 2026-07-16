package org.quyq.gwsu.kit.knowledge.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.quyq.gwsu.common.core.domain.BaseDO;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgePageVersionStatus;

import java.time.LocalDateTime;

/**
 * 知识 Page 版本。
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
@TableName("knowledge_page_version")
@Schema(description = "知识Page版本")
public class KnowledgePageVersion extends BaseDO {

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private String id;

    @Schema(description = "Page ID")
    private String pageId;

    @Schema(description = "版本号")
    private Integer versionNo;

    @Schema(description = "版本状态")
    private KnowledgePageVersionStatus versionStatus;

    @Schema(description = "Markdown内容快照")
    private String markdownContent;

    @Schema(description = "发布时间")
    private LocalDateTime publishedAt;
}
