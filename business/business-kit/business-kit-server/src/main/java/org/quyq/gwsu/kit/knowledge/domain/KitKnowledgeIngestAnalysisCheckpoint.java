package org.quyq.gwsu.kit.knowledge.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.quyq.gwsu.common.core.domain.BaseDO;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgeIngestAnalysisCheckpointStatus;

/**
 * 知识文档导入分析检查点。
 *
 * <p>用于在长文档分析中持久化已完成片段的摘要，以支持任务重试与断点恢复。</p>
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
@TableName("kit_knowledge_ingest_analysis_checkpoint")
@Schema(description = "知识文档导入分析检查点")
public class KitKnowledgeIngestAnalysisCheckpoint extends BaseDO {

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private String id;

    @Schema(description = "导入任务ID")
    private String ingestTaskId;

    @Schema(description = "源文档分析片段序号")
    private Integer chunkNo;

    @Schema(description = "源文档分析片段内容哈希")
    private String chunkContentHash;

    @Schema(description = "片段分析摘要")
    private String analysisDigest;

    @Schema(description = "检查点状态")
    private KnowledgeIngestAnalysisCheckpointStatus checkpointStatus;

    @Schema(description = "源文档片段识别语言")
    private String sourceLanguage;
}
