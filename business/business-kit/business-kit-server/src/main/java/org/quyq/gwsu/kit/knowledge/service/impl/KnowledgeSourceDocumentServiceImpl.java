package org.quyq.gwsu.kit.knowledge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.core.exception.BusinessException;
import org.quyq.gwsu.kit.api.knowledge.dto.KnowledgeDocumentRoleSaveDTO;
import org.quyq.gwsu.kit.api.knowledge.dto.KnowledgeDocumentSaveDTO;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgeDocumentStatus;
import org.quyq.gwsu.kit.errcode.KitErrorCode;
import org.quyq.gwsu.kit.knowledge.domain.KnowledgeSourceDocument;
import org.quyq.gwsu.kit.knowledge.domain.KnowledgeSourceDocumentRole;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgeSourceDocumentMapper;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgeSourceDocumentRoleMapper;
import org.quyq.gwsu.kit.knowledge.service.IKnowledgeSourceDocumentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

/**
 * 知识源文档服务实现。
 */
@Service
@RequiredArgsConstructor
public class KnowledgeSourceDocumentServiceImpl
        extends ServiceImpl<KnowledgeSourceDocumentMapper, KnowledgeSourceDocument>
        implements IKnowledgeSourceDocumentService {

    private final KnowledgeSourceDocumentMapper sourceDocumentMapper;

    private final KnowledgeSourceDocumentRoleMapper roleMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String saveDocument(KnowledgeDocumentSaveDTO dto) {
        if (!StringUtils.hasText(dto.getTenantId())) {
            throw new BusinessException(KitErrorCode.E03005);
        }
        KnowledgeSourceDocument document = new KnowledgeSourceDocument()
                .setId(dto.getId())
                .setFileId(dto.getFileId())
                .setFileName(dto.getFileName())
                .setTargetPageId(dto.getTargetPageId())
                .setDocumentStatus(KnowledgeDocumentStatus.UPLOADED);
        document.setTenantId(dto.getTenantId());

        if (StringUtils.hasText(dto.getId())) {
            long updated = baseMapper.update(document, new LambdaQueryWrapper<KnowledgeSourceDocument>()
                    .eq(KnowledgeSourceDocument::getId, dto.getId())
                    .eq(KnowledgeSourceDocument::getTenantId, dto.getTenantId())
                    .eq(KnowledgeSourceDocument::getDeleted, false));
            if (updated == 0) {
                throw new BusinessException(KitErrorCode.E03001);
            }
        } else {
            save(document);
        }

        KnowledgeDocumentRoleSaveDTO roleSaveDTO = new KnowledgeDocumentRoleSaveDTO();
        roleSaveDTO.setSourceDocumentId(document.getId());
        roleSaveDTO.setTenantId(dto.getTenantId());
        roleSaveDTO.setRoleCodes(dto.getRoleCodes());
        saveDocumentRoles(roleSaveDTO);
        return document.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveDocumentRoles(KnowledgeDocumentRoleSaveDTO dto) {
        if (!StringUtils.hasText(dto.getTenantId())) {
            throw new BusinessException(KitErrorCode.E03005);
        }
        KnowledgeSourceDocument document = getOne(new LambdaQueryWrapper<KnowledgeSourceDocument>()
                .eq(KnowledgeSourceDocument::getId, dto.getSourceDocumentId())
                .eq(KnowledgeSourceDocument::getTenantId, dto.getTenantId())
                .eq(KnowledgeSourceDocument::getDeleted, false));
        if (Objects.isNull(document)) {
            throw new BusinessException(KitErrorCode.E03001);
        }

        roleMapper.delete(new LambdaQueryWrapper<KnowledgeSourceDocumentRole>()
                .eq(KnowledgeSourceDocumentRole::getSourceDocumentId, dto.getSourceDocumentId())
                .eq(KnowledgeSourceDocumentRole::getTenantId, dto.getTenantId())
                .eq(KnowledgeSourceDocumentRole::getDeleted, false));

        if (CollectionUtils.isEmpty(dto.getRoleCodes())) {
            return;
        }

        new LinkedHashSet<>(dto.getRoleCodes()).stream()
                .filter(StringUtils::hasText)
                .map(roleCode -> new KnowledgeSourceDocumentRole()
                        .setSourceDocumentId(dto.getSourceDocumentId())
                        .setRoleCode(roleCode))
                .peek(role -> role.setTenantId(dto.getTenantId()))
                .forEach(roleMapper::insert);
    }

    @Override
    public List<String> listVisibleSourceDocumentIds(String tenantId, Collection<String> roleCodes) {
        return sourceDocumentMapper.listVisibleSourceDocumentIds(tenantId, roleCodes);
    }
}
