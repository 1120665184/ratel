package org.quyq.gwsu.kit.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.quyq.gwsu.kit.knowledge.domain.KnowledgeSourceDocument;

import java.util.Collection;
import java.util.List;

/**
 * 知识源文档 Mapper。
 */
@Mapper
public interface KnowledgeSourceDocumentMapper extends BaseMapper<KnowledgeSourceDocument> {

    List<String> listVisibleSourceDocumentIds(
            @Param("tenantId") String tenantId,
            @Param("roleCodes") Collection<String> roleCodes);
}
