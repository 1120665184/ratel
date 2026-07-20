package org.quyq.gwsu.kit.knowledge.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.Test;
import org.quyq.gwsu.common.core.exception.BusinessException;
import org.quyq.gwsu.kit.api.knowledge.dto.KnowledgePageQueryDTO;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgeBlockType;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgePageStatus;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgePageVersionStatus;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgeSourceType;
import org.quyq.gwsu.kit.api.knowledge.vo.KnowledgePageDetailVO;
import org.quyq.gwsu.kit.api.knowledge.vo.KnowledgePageVO;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgePage;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgePageBlock;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgePageSourceRef;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgePageVersion;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgePageBlockMapper;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgePageMapper;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgePageSourceRefMapper;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgePageVersionMapper;
import org.quyq.gwsu.kit.knowledge.service.impl.KnowledgePageQueryServiceImpl;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KnowledgePageQueryServiceTest {

    @Test
    void pagePagesReturnsPageVOs() {
        KnowledgePageMapper pageMapper = mock(KnowledgePageMapper.class);
        KnowledgePageVersionMapper pageVersionMapper = mock(KnowledgePageVersionMapper.class);
        KnowledgePageBlockMapper pageBlockMapper = mock(KnowledgePageBlockMapper.class);
        KnowledgePageSourceRefMapper pageSourceRefMapper = mock(KnowledgePageSourceRefMapper.class);
        KnowledgePageQueryServiceImpl service = new KnowledgePageQueryServiceImpl(
                pageMapper,
                pageVersionMapper,
                pageBlockMapper,
                pageSourceRefMapper);

        KnowledgePageQueryDTO dto = new KnowledgePageQueryDTO();
        dto.setPageNum(1);
        dto.setPageSize(10);
        KitKnowledgePage page = new KitKnowledgePage()
                .setId("page-1")
                .setTitle("知识首页")
                .setPageStatus(KnowledgePageStatus.PUBLISHED)
                .setCurrentVersionId("version-1");
        Page<KitKnowledgePage> queryPage = Page.of(1, 10, 1);
        queryPage.setRecords(List.of(page));
        when(pageMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(queryPage);

        IPage<KnowledgePageVO> result = service.pagePages(dto);

        assertEquals(1, result.getRecords().size());
        assertEquals("page-1", result.getRecords().getFirst().getId());
        assertEquals("知识首页", result.getRecords().getFirst().getTitle());
        assertEquals("version-1", result.getRecords().getFirst().getCurrentVersionId());
    }

    @Test
    void getPageReturnsCurrentVersionBlocksAndSourceRefs() {
        KnowledgePageMapper pageMapper = mock(KnowledgePageMapper.class);
        KnowledgePageVersionMapper pageVersionMapper = mock(KnowledgePageVersionMapper.class);
        KnowledgePageBlockMapper pageBlockMapper = mock(KnowledgePageBlockMapper.class);
        KnowledgePageSourceRefMapper pageSourceRefMapper = mock(KnowledgePageSourceRefMapper.class);
        KnowledgePageQueryServiceImpl service = new KnowledgePageQueryServiceImpl(
                pageMapper,
                pageVersionMapper,
                pageBlockMapper,
                pageSourceRefMapper);

        KitKnowledgePage page = new KitKnowledgePage()
                .setId("page-1")
                .setTitle("知识首页")
                .setPageStatus(KnowledgePageStatus.PUBLISHED)
                .setCurrentVersionId("version-1");
        KitKnowledgePageVersion version = new KitKnowledgePageVersion()
                .setId("version-1")
                .setPageId("page-1")
                .setVersionNo(1)
                .setVersionStatus(KnowledgePageVersionStatus.PUBLISHED)
                .setMarkdownContent("# 标题");
        version.setPublishedAt(LocalDateTime.of(2026, 7, 20, 13, 0));
        KitKnowledgePageBlock block = new KitKnowledgePageBlock()
                .setId("block-1")
                .setPageVersionId("version-1")
                .setOrderNo(1)
                .setBlockType(KnowledgeBlockType.HEADING)
                .setContent("标题");
        KitKnowledgePageSourceRef ref = new KitKnowledgePageSourceRef()
                .setId("ref-1")
                .setPageBlockId("block-1")
                .setSourceType(KnowledgeSourceType.SOURCE_DOCUMENT)
                .setSourceDocumentId("doc-1")
                .setSourceLocator("page:1");

        when(pageMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(page);
        when(pageVersionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(version);
        when(pageBlockMapper.selectByVersionId("version-1")).thenReturn(List.of(block));
        when(pageSourceRefMapper.selectByPageBlockIds(List.of("block-1"))).thenReturn(List.of(ref));

        KnowledgePageDetailVO result = service.getPage("page-1");

        assertEquals("page-1", result.getId());
        assertEquals("# 标题", result.getMarkdownContent());
        assertEquals(1, result.getCurrentVersionNo());
        assertEquals(KnowledgePageVersionStatus.PUBLISHED, result.getCurrentVersionStatus());
        assertEquals(LocalDateTime.of(2026, 7, 20, 13, 0), result.getCurrentPublishedAt());
        assertEquals(1, result.getBlocks().size());
        assertEquals("doc-1", result.getBlocks().getFirst().getSourceDocumentId());
        assertEquals("page:1", result.getBlocks().getFirst().getSourceLocator());
    }

    @Test
    void getPageThrowsWhenPageMissing() {
        KnowledgePageMapper pageMapper = mock(KnowledgePageMapper.class);
        KnowledgePageVersionMapper pageVersionMapper = mock(KnowledgePageVersionMapper.class);
        KnowledgePageBlockMapper pageBlockMapper = mock(KnowledgePageBlockMapper.class);
        KnowledgePageSourceRefMapper pageSourceRefMapper = mock(KnowledgePageSourceRefMapper.class);
        KnowledgePageQueryServiceImpl service = new KnowledgePageQueryServiceImpl(
                pageMapper,
                pageVersionMapper,
                pageBlockMapper,
                pageSourceRefMapper);

        when(pageMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        assertThrows(BusinessException.class, () -> service.getPage("page-1"));
    }
}
