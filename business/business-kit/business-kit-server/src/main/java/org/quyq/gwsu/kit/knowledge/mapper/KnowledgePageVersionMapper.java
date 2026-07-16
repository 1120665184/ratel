package org.quyq.gwsu.kit.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.quyq.gwsu.kit.knowledge.domain.KnowledgePageVersion;

/**
 * 知识 Page 版本 Mapper。
 */
@Mapper
public interface KnowledgePageVersionMapper extends BaseMapper<KnowledgePageVersion> {

    Integer selectMaxVersionNo(@Param("tenantId") String tenantId, @Param("pageId") String pageId);
}
