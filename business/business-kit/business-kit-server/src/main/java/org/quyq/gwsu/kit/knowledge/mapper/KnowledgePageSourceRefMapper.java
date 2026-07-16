package org.quyq.gwsu.kit.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.quyq.gwsu.kit.knowledge.domain.KnowledgePageSourceRef;

import java.util.Collection;
import java.util.List;

/**
 * 知识 Page Block 来源 Mapper。
 */
@Mapper
public interface KnowledgePageSourceRefMapper extends BaseMapper<KnowledgePageSourceRef> {

    List<KnowledgePageSourceRef> selectByPageBlockIds(@Param("tenantId") String tenantId,
                                                      @Param("pageBlockIds") Collection<String> pageBlockIds);
}
