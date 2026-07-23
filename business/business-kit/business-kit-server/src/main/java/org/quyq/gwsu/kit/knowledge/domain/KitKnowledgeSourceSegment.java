package org.quyq.gwsu.kit.knowledge.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.quyq.gwsu.common.core.domain.BaseDO;

/**
 * 知识源文档解析片段。
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
@TableName("kit_knowledge_source_segment")
@Schema(description = "知识源文档解析片段")
public class KitKnowledgeSourceSegment extends BaseDO {

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private String id;

    @Schema(description = "源文档ID")
    private String sourceDocumentId;

    @Schema(description = "片段顺序号")
    private Integer segmentNo;

    @Schema(description = "片段类型")
    private String segmentType;

    @Schema(description = "标题路径")
    private String headingPath;

    @Schema(description = "来源定位")
    private String sourceLocator;

    @Schema(description = "片段内容")
    private String content;
}
