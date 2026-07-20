package org.quyq.gwsu.kit.knowledge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.core.exception.BusinessException;
import org.quyq.gwsu.kit.api.knowledge.dto.KnowledgeDocumentQueryDTO;
import org.quyq.gwsu.kit.api.knowledge.dto.KnowledgeDocumentRoleSaveDTO;
import org.quyq.gwsu.kit.api.knowledge.dto.KnowledgeDocumentSaveDTO;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgeDocumentStatus;
import org.quyq.gwsu.kit.api.knowledge.vo.KnowledgeDocumentVO;
import org.quyq.gwsu.kit.errcode.KitErrorCode;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgeSourceDocument;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgeSourceDocumentRole;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgeSourceDocumentMapper;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgeSourceDocumentRoleMapper;
import org.quyq.gwsu.kit.knowledge.service.IKnowledgeSourceDocumentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 知识源文档服务实现。
 */
@Service
@RequiredArgsConstructor
public class KnowledgeSourceDocumentServiceImpl
        extends ServiceImpl<KnowledgeSourceDocumentMapper, KitKnowledgeSourceDocument>
        implements IKnowledgeSourceDocumentService {

    private final KnowledgeSourceDocumentMapper sourceDocumentMapper;

    private final KnowledgeSourceDocumentRoleMapper roleMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String saveDocument(KnowledgeDocumentSaveDTO dto) {
        KitKnowledgeSourceDocument document = new KitKnowledgeSourceDocument()
                .setId(dto.getId())
                .setFileId(dto.getFileId())
                .setFileName(dto.getFileName())
                .setTargetPageId(dto.getTargetPageId())
                .setDocumentStatus(KnowledgeDocumentStatus.UPLOADED)
                .setEmbeddingCompleted(false);

        if (StringUtils.hasText(dto.getId())) {
            long updated = baseMapper.update(document, new LambdaQueryWrapper<KitKnowledgeSourceDocument>()
                    .eq(KitKnowledgeSourceDocument::getId, dto.getId())
                    .eq(KitKnowledgeSourceDocument::getDeleted, false));
            if (updated == 0) {
                throw new BusinessException(KitErrorCode.E03001);
            }
        } else {
            save(document);
        }

        KnowledgeDocumentRoleSaveDTO roleSaveDTO = new KnowledgeDocumentRoleSaveDTO();
        roleSaveDTO.setSourceDocumentId(document.getId());
        roleSaveDTO.setRoleCodes(dto.getRoleCodes());
        saveDocumentRoles(roleSaveDTO);
        return document.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveDocumentRoles(KnowledgeDocumentRoleSaveDTO dto) {
        KitKnowledgeSourceDocument document = getOne(new LambdaQueryWrapper<KitKnowledgeSourceDocument>()
                .eq(KitKnowledgeSourceDocument::getId, dto.getSourceDocumentId())
                .eq(KitKnowledgeSourceDocument::getDeleted, false));
        if (Objects.isNull(document)) {
            throw new org.quyq.gwsu.common.core.exception.BusinessException(KitErrorCode.E03001);
        }

        roleMapper.delete(new LambdaQueryWrapper<KitKnowledgeSourceDocumentRole>()
                .eq(KitKnowledgeSourceDocumentRole::getSourceDocumentId, dto.getSourceDocumentId())
                .eq(KitKnowledgeSourceDocumentRole::getDeleted, false));

        if (CollectionUtils.isEmpty(dto.getRoleCodes())) {
            return;
        }

        new LinkedHashSet<>(dto.getRoleCodes()).stream()
                .filter(StringUtils::hasText)
                .map(roleCode -> new KitKnowledgeSourceDocumentRole()
                        .setSourceDocumentId(dto.getSourceDocumentId())
                        .setRoleCode(roleCode))
                .forEach(roleMapper::insert);
    }

    @Override
    public IPage<KnowledgeDocumentVO> pageDocuments(KnowledgeDocumentQueryDTO dto) {
        Page<KitKnowledgeSourceDocument> page = page(Page.of(dto.getPageNum(), dto.getPageSize()),
                new LambdaQueryWrapper<KitKnowledgeSourceDocument>()
                        .like(StringUtils.hasText(dto.getFileName()), KitKnowledgeSourceDocument::getFileName, dto.getFileName())
                        .eq(Objects.nonNull(dto.getDocumentStatus()), KitKnowledgeSourceDocument::getDocumentStatus, dto.getDocumentStatus())
                        .eq(KitKnowledgeSourceDocument::getDeleted, false)
                        .orderByDesc(KitKnowledgeSourceDocument::getCreateTime));
        return convertToDocumentPage(page);
    }

    @Override
    public KnowledgeDocumentVO getDocument(String documentId) {
        KitKnowledgeSourceDocument document = getOne(new LambdaQueryWrapper<KitKnowledgeSourceDocument>()
                .eq(KitKnowledgeSourceDocument::getId, documentId)
                .eq(KitKnowledgeSourceDocument::getDeleted, false));
        if (Objects.isNull(document)) {
            throw new org.quyq.gwsu.common.core.exception.BusinessException(KitErrorCode.E03001);
        }
        return toDocumentVO(document, groupRoleCodes(List.of(documentId)));
    }

    @Override
    public List<String> listVisibleSourceDocumentIds(Collection<String> roleCodes) {
        return sourceDocumentMapper.listVisibleSourceDocumentIds(roleCodes);
    }

    private IPage<KnowledgeDocumentVO> convertToDocumentPage(IPage<KitKnowledgeSourceDocument> page) {
        Page<KnowledgeDocumentVO> result = Page.of(page.getCurrent(), page.getSize(), page.getTotal());
        result.setPages(page.getPages());
        Map<String, List<String>> roleCodeMap = groupRoleCodes(page.getRecords().stream()
                .map(KitKnowledgeSourceDocument::getId)
                .toList());
        result.setRecords(page.getRecords().stream()
                .map(document -> toDocumentVO(document, roleCodeMap))
                .toList());
        return result;
    }

    private KnowledgeDocumentVO toDocumentVO(KitKnowledgeSourceDocument document, Map<String, List<String>> roleCodeMap) {
        KnowledgeDocumentVO vo = new KnowledgeDocumentVO()
                .setId(document.getId())
                .setFileId(document.getFileId())
                .setFileName(document.getFileName())
                .setDocumentStatus(document.getDocumentStatus())
                .setTargetPageId(document.getTargetPageId())
                .setProcessMessage(document.getProcessMessage())
                .setProcessedAt(document.getProcessedAt())
                .setRoleCodes(roleCodeMap.getOrDefault(document.getId(), List.of()));
        vo.copyBaseProperties(document);
        return vo;
    }

    private Map<String, List<String>> groupRoleCodes(List<String> sourceDocumentIds) {
        if (CollectionUtils.isEmpty(sourceDocumentIds)) {
            return Collections.emptyMap();
        }
        List<KitKnowledgeSourceDocumentRole> roles = roleMapper.selectBySourceDocumentIds(sourceDocumentIds);
        Map<String, List<String>> roleCodeMap = new HashMap<>();
        roles.stream()
                .filter(role -> StringUtils.hasText(role.getRoleCode()))
                .collect(Collectors.groupingBy(KitKnowledgeSourceDocumentRole::getSourceDocumentId,
                        Collectors.mapping(KitKnowledgeSourceDocumentRole::getRoleCode, Collectors.toList())))
                .forEach(roleCodeMap::put);
        return roleCodeMap;
    }
}
