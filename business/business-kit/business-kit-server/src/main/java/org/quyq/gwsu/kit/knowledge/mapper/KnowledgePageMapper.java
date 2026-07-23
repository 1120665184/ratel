package org.quyq.gwsu.kit.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgePage;

import java.util.List;

/**
 * 知识 Page Mapper。
 */
@Mapper
public interface KnowledgePageMapper extends BaseMapper<KitKnowledgePage> {

    List<KitKnowledgePage> selectPagesBySourceDocumentId(@Param("sourceDocumentId") String sourceDocumentId);

    List<KitKnowledgePage> selectCurrentPagesBySourceDocumentId(@Param("sourceDocumentId") String sourceDocumentId);
}
