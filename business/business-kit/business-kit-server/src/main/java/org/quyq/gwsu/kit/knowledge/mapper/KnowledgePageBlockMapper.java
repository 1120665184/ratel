package org.quyq.gwsu.kit.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgePageBlock;

import java.util.List;

/**
 * 知识 Page Block Mapper。
 */
@Mapper
public interface KnowledgePageBlockMapper extends BaseMapper<KitKnowledgePageBlock> {

    List<KitKnowledgePageBlock> selectByVersionId(@Param("tenantId") String tenantId,
                                               @Param("pageVersionId") String pageVersionId);
}
