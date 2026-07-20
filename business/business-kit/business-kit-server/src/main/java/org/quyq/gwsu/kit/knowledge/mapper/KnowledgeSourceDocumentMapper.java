package org.quyq.gwsu.kit.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgeSourceDocument;

import java.util.Collection;
import java.util.List;

/**
 * 知识源文档 Mapper。
 */
@Mapper
public interface KnowledgeSourceDocumentMapper extends BaseMapper<KitKnowledgeSourceDocument> {

    List<String> listVisibleSourceDocumentIds(@Param("roleCodes") Collection<String> roleCodes);
}
