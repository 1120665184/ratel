package org.quyq.gwsu.kit.knowledge.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.kit.api.knowledge.vo.KnowledgeSearchMetaVO;
import org.quyq.gwsu.kit.api.knowledge.KnowledgeClientApi;
import org.quyq.gwsu.kit.api.knowledge.dto.*;
import org.quyq.gwsu.kit.api.knowledge.vo.KnowledgeDocumentVO;
import org.quyq.gwsu.kit.api.knowledge.vo.KnowledgePageDetailVO;
import org.quyq.gwsu.kit.api.knowledge.vo.KnowledgePageVO;
import org.quyq.gwsu.kit.api.knowledge.vo.KnowledgeSearchResultVO;
import org.quyq.gwsu.kit.config.properties.KnowledgeProperties;
import org.quyq.gwsu.kit.knowledge.engine.search.KnowledgeSearchEngine;
import org.quyq.gwsu.kit.knowledge.service.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 知识库控制器。
 */
@RestController
@RequestMapping("knowledge")
@Tag(name = "知识库")
@RequiredArgsConstructor
//@TableModelPermission({
//        KitKnowledgeSourceDocument.class,
//        KitKnowledgeSourceDocumentRole.class,
//        KitKnowledgeIngestTask.class,
//        KitKnowledgePage.class,
//        KitKnowledgePageVersion.class,
//        KitKnowledgePageBlock.class,
//        KitKnowledgePageSourceRef.class})
public class KnowledgeController implements KnowledgeClientApi {

    private final KnowledgeProperties knowledgeProperties;

    private final KnowledgeIngestApplicationService knowledgeIngestApplicationService;

    private final IKnowledgeSourceDocumentService sourceDocumentService;

    private final IKnowledgeIngestTaskService ingestTaskService;

    private final IKnowledgePageCommandService pageCommandService;

    private final IKnowledgePageQueryService pageQueryService;

    private final KnowledgeSearchEngine knowledgeSearchEngine;

    @Override
    @GetMapping("/search/meta")
    @Operation(summary = "获取知识检索元信息")
    public R<KnowledgeSearchMetaVO> getSearchMeta() {
        return R.ok(new KnowledgeSearchMetaVO()
                .setWikiPageLanguage(knowledgeProperties.getWikiOutputLanguage()));
    }

    @PostMapping("/document/save")
    @Operation(summary = "保存知识源文档并提交导入")
    public R<String> saveDocument(@RequestBody KnowledgeDocumentSaveDTO dto) {
        return R.ok(knowledgeIngestApplicationService.saveDocumentAndSubmit(dto));
    }

    @PostMapping("/document/role/save")
    @Operation(summary = "保存知识源文档角色权限")
    public R<Void> saveDocumentRoles(@RequestBody KnowledgeDocumentRoleSaveDTO dto) {
        sourceDocumentService.saveDocumentRoles(dto);
        return R.ok();
    }

    @PostMapping("/document/page")
    @Operation(summary = "分页查询知识源文档")
    public R<IPage<KnowledgeDocumentVO>> pageDocuments(@RequestBody KnowledgeDocumentQueryDTO dto) {
        return R.ok(sourceDocumentService.pageDocuments(dto));
    }

    @PostMapping("/document/{documentId}")
    @Operation(summary = "查询知识源文档详情")
    public R<KnowledgeDocumentVO> getDocument(@PathVariable String documentId, @RequestBody KnowledgeDocumentQueryDTO dto) {
        return R.ok(sourceDocumentService.getDocument(documentId));
    }

    @PostMapping("/document/enable/{documentId}")
    @Operation(summary = "启用知识源文档")
    public R<Void> enableDocument(@PathVariable String documentId, @RequestBody KnowledgeDocumentQueryDTO dto) {
        sourceDocumentService.updateEnabled(documentId, true);
        return R.ok();
    }

    @PostMapping("/document/disable/{documentId}")
    @Operation(summary = "禁用知识源文档")
    public R<Void> disableDocument(@PathVariable String documentId, @RequestBody KnowledgeDocumentQueryDTO dto) {
        sourceDocumentService.updateEnabled(documentId, false);
        return R.ok();
    }

    @PostMapping("/document/delete/{documentId}")
    @Operation(summary = "删除知识源文档")
    public R<Void> deleteDocument(@PathVariable String documentId, @RequestBody KnowledgeDocumentQueryDTO dto) {
        sourceDocumentService.deleteDocument(documentId);
        return R.ok();
    }

    @PostMapping("/page/save")
    @Operation(summary = "保存知识Page")
    public R<String> savePage(@RequestBody KnowledgePageSaveDTO dto) {
        return R.ok(pageCommandService.savePage(dto));
    }

    @PostMapping("/page/page")
    @Operation(summary = "分页查询知识Page")
    public R<IPage<KnowledgePageVO>> pagePages(@RequestBody KnowledgePageQueryDTO dto) {
        return R.ok(pageQueryService.pagePages(dto));
    }

    @PostMapping("/page/{pageId}")
    @Operation(summary = "查询知识Page详情")
    public R<KnowledgePageDetailVO> getPage(@PathVariable String pageId,
                                            @RequestBody KnowledgePageQueryDTO dto) {
        return R.ok(pageQueryService.getPage(pageId));
    }

    @PostMapping("/task/retry/{taskId}")
    @Operation(summary = "重新提交知识导入任务")
    public R<String> retryTask(@PathVariable String taskId, @RequestBody KnowledgeIngestTaskQueryDTO dto) {
        return R.ok(knowledgeIngestApplicationService.retryAndSubmit(taskId));
    }

    @Override
    @PostMapping("/search")
    @Operation(summary = "知识库检索")
    public R<List<KnowledgeSearchResultVO>> search(@RequestBody KnowledgeSearchDTO dto) {
        return R.ok(knowledgeSearchEngine.search(dto));
    }

    @Override
    @PostMapping("/chunk/adjacent")
    @Operation(summary = "查询指定Block的上一个或下一个")
    public R<List<KnowledgeSearchResultVO>> findAdjacentChunk(@RequestBody KnowledgeChunkAdjacentDTO dto) {
        return R.ok(knowledgeSearchEngine.findAdjacentChunk(
                dto.getRoleCodes(),
                dto.getPageBlockId(),
                dto.getDirection(),
                dto.getOffset()));
    }
}
