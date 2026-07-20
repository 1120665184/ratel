package org.quyq.gwsu.kit.knowledge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.kit.api.knowledge.dto.KnowledgePageQueryDTO;
import org.quyq.gwsu.kit.api.knowledge.vo.KnowledgePageBlockVO;
import org.quyq.gwsu.kit.api.knowledge.vo.KnowledgePageDetailVO;
import org.quyq.gwsu.kit.api.knowledge.vo.KnowledgePageVO;
import org.quyq.gwsu.kit.errcode.KitErrorCode;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgePage;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgePageBlock;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgePageSourceRef;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgePageVersion;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgePageBlockMapper;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgePageMapper;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgePageSourceRefMapper;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgePageVersionMapper;
import org.quyq.gwsu.kit.knowledge.service.IKnowledgePageQueryService;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 知识 Page 查询服务实现。
 */
@Service
@RequiredArgsConstructor
public class KnowledgePageQueryServiceImpl implements IKnowledgePageQueryService {

    private final KnowledgePageMapper pageMapper;

    private final KnowledgePageVersionMapper pageVersionMapper;

    private final KnowledgePageBlockMapper pageBlockMapper;

    private final KnowledgePageSourceRefMapper pageSourceRefMapper;

    @Override
    public IPage<KnowledgePageVO> pagePages(KnowledgePageQueryDTO dto) {
        Page<KitKnowledgePage> page = pageMapper.selectPage(Page.of(dto.getPageNum(), dto.getPageSize()),
                new LambdaQueryWrapper<KitKnowledgePage>()
                        .like(StringUtils.hasText(dto.getTitle()), KitKnowledgePage::getTitle, dto.getTitle())
                        .eq(Objects.nonNull(dto.getPageStatus()), KitKnowledgePage::getPageStatus, dto.getPageStatus())
                        .eq(KitKnowledgePage::getDeleted, false)
                        .orderByDesc(KitKnowledgePage::getCreateTime));
        Page<KnowledgePageVO> result = Page.of(page.getCurrent(), page.getSize(), page.getTotal());
        result.setPages(page.getPages());
        result.setRecords(page.getRecords().stream().map(this::toPageVO).toList());
        return result;
    }

    @Override
    public KnowledgePageDetailVO getPage(String pageId) {
        KitKnowledgePage page = pageMapper.selectOne(new LambdaQueryWrapper<KitKnowledgePage>()
                .eq(KitKnowledgePage::getId, pageId)
                .eq(KitKnowledgePage::getDeleted, false));
        if (Objects.isNull(page)) {
            throw new org.quyq.gwsu.common.core.exception.BusinessException(KitErrorCode.E03009);
        }
        KnowledgePageDetailVO detail = new KnowledgePageDetailVO();
        detail.setId(page.getId());
        detail.setTitle(page.getTitle());
        detail.setPageStatus(page.getPageStatus());
        detail.setCurrentVersionId(page.getCurrentVersionId());
        detail.copyBaseProperties(page);
        if (!StringUtils.hasText(page.getCurrentVersionId())) {
            detail.setMarkdownContent("");
            detail.setBlocks(List.of());
            return detail;
        }
        KitKnowledgePageVersion version = pageVersionMapper.selectOne(new LambdaQueryWrapper<KitKnowledgePageVersion>()
                .eq(KitKnowledgePageVersion::getId, page.getCurrentVersionId())
                .eq(KitKnowledgePageVersion::getDeleted, false));
        if (version == null) {
            detail.setMarkdownContent("");
            detail.setBlocks(List.of());
            return detail;
        }
        detail.setMarkdownContent(version.getMarkdownContent());
        detail.setCurrentVersionNo(version.getVersionNo());
        detail.setCurrentVersionStatus(version.getVersionStatus());
        detail.setCurrentPublishedAt(version.getPublishedAt());
        List<KitKnowledgePageBlock> blocks = pageBlockMapper.selectByVersionId(version.getId());
        if (CollectionUtils.isEmpty(blocks)) {
            detail.setBlocks(List.of());
            return detail;
        }
        Map<String, KitKnowledgePageSourceRef> refByBlockId = new HashMap<>();
        pageSourceRefMapper.selectByPageBlockIds(blocks.stream().map(KitKnowledgePageBlock::getId).toList())
                .forEach(ref -> refByBlockId.put(ref.getPageBlockId(), ref));
        detail.setBlocks(blocks.stream().map(block -> toBlockVO(block, refByBlockId.get(block.getId()))).toList());
        return detail;
    }

    private KnowledgePageVO toPageVO(KitKnowledgePage page) {
        KnowledgePageVO vo = new KnowledgePageVO()
                .setId(page.getId())
                .setTitle(page.getTitle())
                .setPageStatus(page.getPageStatus())
                .setCurrentVersionId(page.getCurrentVersionId());
        vo.copyBaseProperties(page);
        return vo;
    }

    private KnowledgePageBlockVO toBlockVO(KitKnowledgePageBlock block, KitKnowledgePageSourceRef ref) {
        KnowledgePageBlockVO vo = new KnowledgePageBlockVO()
                .setId(block.getId())
                .setPageVersionId(block.getPageVersionId())
                .setOrderNo(block.getOrderNo())
                .setBlockType(block.getBlockType())
                .setContent(block.getContent());
        if (ref != null) {
            vo.setSourceType(ref.getSourceType())
                    .setSourceDocumentId(ref.getSourceDocumentId())
                    .setSourceLocator(ref.getSourceLocator());
        }
        vo.copyBaseProperties(block);
        return vo;
    }
}
