package org.quyq.gwsu.kit.api.knowledge;

import org.quyq.gwsu.common.api.annotation.ApiClient;
import org.quyq.gwsu.common.core.constants.CoreConstants;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.kit.api.knowledge.dto.KnowledgeChunkAdjacentDTO;
import org.quyq.gwsu.kit.api.knowledge.dto.KnowledgeSearchDTO;
import org.quyq.gwsu.kit.api.knowledge.fallback.KnowledgeClientApiFallbackFactory;
import org.quyq.gwsu.kit.api.knowledge.vo.KnowledgeSearchMetaVO;
import org.quyq.gwsu.kit.api.knowledge.vo.KnowledgeSearchResultVO;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import java.util.List;

/**
 * 服务间知识库检索 API。
 */
@ApiClient(value = CoreConstants.Server.KIT_NAME, note = "知识库检索客户端API",
        fallbackFactory = KnowledgeClientApiFallbackFactory.class)
@HttpExchange("/knowledge")
public interface KnowledgeClientApi {

    /**
     * 获取知识检索元信息。
     *
     * @return 检索元信息
     */
    @GetExchange("/search/meta")
    R<KnowledgeSearchMetaVO> getSearchMeta();

    /**
     * 检索当前角色可见的知识 Chunk。
     *
     * @param dto 检索条件
     * @return 检索结果
     */
    @PostExchange("/search")
    R<List<KnowledgeSearchResultVO>> search(@RequestBody KnowledgeSearchDTO dto);

    /**
     * 查询指定 Block 的可见 Block。
     *
     * @param dto 邻近查询条件
     * @return 邻近 Block 列表，不存在或不可见时 data 为空列表
     */
    @PostExchange("/chunk/adjacent")
    R<List<KnowledgeSearchResultVO>> findAdjacentChunk(@RequestBody KnowledgeChunkAdjacentDTO dto);
}
