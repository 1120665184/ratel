package org.quyq.gwsu.kit.knowledge.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.common.security.annotation.TableModelPermission;
import org.quyq.gwsu.kit.api.knowledge.KnowledgeClientApi;
import org.quyq.gwsu.kit.api.knowledge.dto.KnowledgeChunkAdjacentDTO;
import org.quyq.gwsu.kit.api.knowledge.dto.KnowledgeSearchDTO;
import org.quyq.gwsu.kit.api.knowledge.vo.KnowledgeSearchResultVO;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgeSourceDocument;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgeSourceDocumentRole;
import org.quyq.gwsu.kit.knowledge.engine.KnowledgeSearchEngine;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 知识库检索控制器。
 */
@RestController
@RequestMapping("knowledge")
@Tag(name = "知识库检索")
@RequiredArgsConstructor
@TableModelPermission({KitKnowledgeSourceDocument.class, KitKnowledgeSourceDocumentRole.class})
public class KnowledgeController implements KnowledgeClientApi {

    private final KnowledgeSearchEngine knowledgeSearchEngine;

    @Override
    @PostMapping("/search")
    @Operation(summary = "知识库检索")
    public R<List<KnowledgeSearchResultVO>> search(@RequestBody KnowledgeSearchDTO dto) {
        return R.ok(knowledgeSearchEngine.search(dto));
    }

    @Override
    @PostMapping("/chunk/adjacent")
    @Operation(summary = "查询指定Chunk的上一个或下一个")
    public R<KnowledgeSearchResultVO> findAdjacentChunk(@RequestBody KnowledgeChunkAdjacentDTO dto) {
        return R.ok(knowledgeSearchEngine.findAdjacentChunk(
                dto.getTenantId(),
                dto.getRoleCodes(),
                dto.getChunkId(),
                dto.getDirection()).orElse(null));
    }
}
