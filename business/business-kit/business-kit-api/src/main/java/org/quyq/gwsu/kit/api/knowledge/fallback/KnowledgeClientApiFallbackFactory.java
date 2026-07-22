package org.quyq.gwsu.kit.api.knowledge.fallback;

import lombok.extern.slf4j.Slf4j;
import org.quyq.gwsu.common.api.fallback.FallbackFactory;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.kit.api.knowledge.KnowledgeClientApi;
import org.quyq.gwsu.kit.api.knowledge.dto.KnowledgeChunkAdjacentDTO;
import org.quyq.gwsu.kit.api.knowledge.dto.KnowledgeSearchDTO;
import org.quyq.gwsu.kit.api.knowledge.vo.KnowledgeSearchResultVO;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * KnowledgeClientApi 降级工厂。
 */
@Component
@Slf4j
public class KnowledgeClientApiFallbackFactory implements FallbackFactory<KnowledgeClientApi> {

    @Override
    public KnowledgeClientApi create(Throwable cause) {
        log.error(cause.getMessage(), cause);
        return new KnowledgeClientApi() {
            @Override
            public R<List<KnowledgeSearchResultVO>> search(KnowledgeSearchDTO dto) {
                return R.fail("服务暂时不可用: " + cause.getMessage());
            }

            @Override
            public R<List<KnowledgeSearchResultVO>> findAdjacentChunk(KnowledgeChunkAdjacentDTO dto) {
                return R.fail("服务暂时不可用: " + cause.getMessage());
            }
        };
    }
}
