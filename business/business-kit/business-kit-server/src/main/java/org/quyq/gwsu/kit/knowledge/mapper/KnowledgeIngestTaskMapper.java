package org.quyq.gwsu.kit.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgeIngestTask;

import java.util.List;

/**
 * 知识文档导入任务 Mapper。
 */
@Mapper
public interface KnowledgeIngestTaskMapper extends BaseMapper<KitKnowledgeIngestTask> {

    List<String> selectIdsBySourceDocumentId(@Param("sourceDocumentId") String sourceDocumentId);
}
