package org.quyq.gwsu.kit.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgeSourceSegment;

import java.util.List;

/**
 * 知识源片段 Mapper。
 */
@Mapper
public interface KnowledgeSourceSegmentMapper extends BaseMapper<KitKnowledgeSourceSegment> {

    List<KitKnowledgeSourceSegment> selectBySourceDocumentId(@Param("sourceDocumentId") String sourceDocumentId);
}
