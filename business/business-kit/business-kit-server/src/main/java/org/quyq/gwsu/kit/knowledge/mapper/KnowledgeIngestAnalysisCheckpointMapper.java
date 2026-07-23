package org.quyq.gwsu.kit.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgeIngestAnalysisCheckpoint;

import java.util.Collection;
import java.util.List;

/**
 * 知识文档导入分析检查点 Mapper。
 */
@Mapper
public interface KnowledgeIngestAnalysisCheckpointMapper extends BaseMapper<KitKnowledgeIngestAnalysisCheckpoint> {

    List<KitKnowledgeIngestAnalysisCheckpoint> selectByTaskId(@Param("ingestTaskId") String ingestTaskId);

    KitKnowledgeIngestAnalysisCheckpoint selectByTaskIdAndChunkNo(
            @Param("ingestTaskId") String ingestTaskId,
            @Param("chunkNo") Integer chunkNo);

    List<KitKnowledgeIngestAnalysisCheckpoint> selectByTaskIds(@Param("ingestTaskIds") Collection<String> ingestTaskIds);
}
