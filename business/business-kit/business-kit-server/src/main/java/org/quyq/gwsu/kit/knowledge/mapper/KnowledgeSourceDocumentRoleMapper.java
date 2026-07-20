package org.quyq.gwsu.kit.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgeSourceDocumentRole;

import java.util.Collection;
import java.util.List;

/**
 * 知识源文档角色授权 Mapper。
 */
@Mapper
public interface KnowledgeSourceDocumentRoleMapper extends BaseMapper<KitKnowledgeSourceDocumentRole> {

    List<KitKnowledgeSourceDocumentRole> selectBySourceDocumentIds(
            @Param("sourceDocumentIds") Collection<String> sourceDocumentIds);
}
