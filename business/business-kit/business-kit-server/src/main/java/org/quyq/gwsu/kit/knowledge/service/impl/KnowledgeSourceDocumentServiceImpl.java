package org.quyq.gwsu.kit.knowledge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.core.exception.BusinessException;
import org.quyq.gwsu.kit.api.knowledge.dto.KnowledgeDocumentQueryDTO;
import org.quyq.gwsu.kit.api.knowledge.dto.KnowledgeDocumentRoleSaveDTO;
import org.quyq.gwsu.kit.api.knowledge.dto.KnowledgeDocumentSaveDTO;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgeDocumentStatus;
import org.quyq.gwsu.kit.api.knowledge.vo.KnowledgeDocumentVO;
import org.quyq.gwsu.kit.api.utils.FileUtils;
import org.quyq.gwsu.kit.errcode.KitErrorCode;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgeIngestTask;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgeSourceDocument;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgeSourceDocumentRole;
import org.quyq.gwsu.kit.knowledge.engine.chunk.KnowledgeChunkIndexRepository;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgeIngestTaskMapper;
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

    private static final Gson GSON = new Gson();

    private final KnowledgeSourceDocumentMapper sourceDocumentMapper;

    private final KnowledgeSourceDocumentRoleMapper roleMapper;

    private final KnowledgeIngestTaskMapper ingestTaskMapper;

    private final KnowledgeChunkIndexRepository chunkIndexRepository;

    private final KnowledgeSourceDocumentPageSyncService pageSyncService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String saveDocument(KnowledgeDocumentSaveDTO dto) {
        KitKnowledgeSourceDocument existingDocument = resolveExistingDocument(dto);
        KitKnowledgeSourceDocument document = new KitKnowledgeSourceDocument()
                .setId(Objects.nonNull(existingDocument) ? existingDocument.getId() : dto.getId())
                .setFileId(dto.getFileId())
                .setFileName(dto.getFileName())
                .setDocumentStatus(KnowledgeDocumentStatus.UPLOADED)
                .setProcessMessage(null)
                .setImageFileIdsJson(Objects.nonNull(existingDocument) ? existingDocument.getImageFileIdsJson() : null)
                .setImageOcrParsed(false)
                .setEmbeddingCompleted(false)
                .setEnabled(true);

        if (StringUtils.hasText(document.getId())) {
            long updated = baseMapper.update(document, new LambdaQueryWrapper<KitKnowledgeSourceDocument>()
                    .eq(KitKnowledgeSourceDocument::getId, document.getId())
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

    private KitKnowledgeSourceDocument resolveExistingDocument(KnowledgeDocumentSaveDTO dto) {
        if (StringUtils.hasText(dto.getId()) || !StringUtils.hasText(dto.getFileId())) {
            return null;
        }
        return getOne(new LambdaQueryWrapper<KitKnowledgeSourceDocument>()
                .eq(KitKnowledgeSourceDocument::getFileId, dto.getFileId())
                .eq(KitKnowledgeSourceDocument::getDeleted, false)
                .last("limit 1"));
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
                        .eq(Objects.nonNull(dto.getEnabled()), KitKnowledgeSourceDocument::getEnabled, dto.getEnabled())
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
    public void updateEnabled(String documentId, boolean enabled) {
        long updated = baseMapper.update(null, new LambdaUpdateWrapper<KitKnowledgeSourceDocument>()
                .eq(KitKnowledgeSourceDocument::getId, documentId)
                .eq(KitKnowledgeSourceDocument::getDeleted, false)
                .set(KitKnowledgeSourceDocument::getEnabled, enabled));
        if (updated == 0) {
            throw new BusinessException(KitErrorCode.E03001);
        }
        if (!enabled) {
            chunkIndexRepository.deleteBySourceDocumentId(documentId);
            return;
        }
        pageSyncService.reindexCurrentPagesBySourceDocumentId(documentId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDocument(String documentId) {
        KitKnowledgeSourceDocument document = getOne(new LambdaQueryWrapper<KitKnowledgeSourceDocument>()
                .eq(KitKnowledgeSourceDocument::getId, documentId)
                .eq(KitKnowledgeSourceDocument::getDeleted, false));
        if (Objects.isNull(document)) {
            throw new BusinessException(KitErrorCode.E03001);
        }
        baseMapper.update(null, new LambdaUpdateWrapper<KitKnowledgeSourceDocument>()
                .eq(KitKnowledgeSourceDocument::getId, documentId)
                .eq(KitKnowledgeSourceDocument::getDeleted, false)
                .set(KitKnowledgeSourceDocument::getEnabled, false)
                .set(KitKnowledgeSourceDocument::getDeleted, true));
        roleMapper.delete(new LambdaQueryWrapper<KitKnowledgeSourceDocumentRole>()
                .eq(KitKnowledgeSourceDocumentRole::getSourceDocumentId, documentId)
                .eq(KitKnowledgeSourceDocumentRole::getDeleted, false));
        ingestTaskMapper.update(null, new LambdaUpdateWrapper<KitKnowledgeIngestTask>()
                .eq(KitKnowledgeIngestTask::getSourceDocumentId, documentId)
                .eq(KitKnowledgeIngestTask::getDeleted, false)
                .set(KitKnowledgeIngestTask::getDeleted, true));
        pageSyncService.removeSourceDocumentFromCurrentPages(documentId);
        chunkIndexRepository.deleteBySourceDocumentId(documentId);
        removeKnowledgeImages(document.getImageFileIdsJson());
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
        Map<String, KitKnowledgeIngestTask> latestTaskMap = groupLatestTasks(page.getRecords().stream()
                .map(KitKnowledgeSourceDocument::getId)
                .toList());
        result.setRecords(page.getRecords().stream()
                .map(document -> toDocumentVO(document, roleCodeMap, latestTaskMap.get(document.getId())))
                .toList());
        return result;
    }

    private KnowledgeDocumentVO toDocumentVO(KitKnowledgeSourceDocument document, Map<String, List<String>> roleCodeMap) {
        return toDocumentVO(document, roleCodeMap, groupLatestTasks(List.of(document.getId())).get(document.getId()));
    }

    private KnowledgeDocumentVO toDocumentVO(KitKnowledgeSourceDocument document,
                                             Map<String, List<String>> roleCodeMap,
                                             KitKnowledgeIngestTask latestTask) {
        KnowledgeDocumentVO vo = new KnowledgeDocumentVO()
                .setId(document.getId())
                .setFileId(document.getFileId())
                .setFileName(document.getFileName())
                .setDocumentStatus(document.getDocumentStatus())
                .setProcessMessage(document.getProcessMessage())
                .setImageOcrParsed(Boolean.TRUE.equals(document.getImageOcrParsed()))
                .setEmbeddingCompleted(Boolean.TRUE.equals(document.getEmbeddingCompleted()))
                .setEnabled(!Boolean.FALSE.equals(document.getEnabled()))
                .setProcessedAt(document.getProcessedAt())
                .setRoleCodes(roleCodeMap.getOrDefault(document.getId(), List.of()));
        if (Objects.nonNull(latestTask)) {
            vo.setLatestTaskId(latestTask.getId())
                    .setLatestTaskStatus(latestTask.getTaskStatus())
                    .setLatestTaskStage(latestTask.getCurrentStage())
                    .setLatestTaskRetryCount(latestTask.getRetryCount())
                    .setLatestTaskErrorMessage(latestTask.getErrorMessage())
                    .setLatestTaskStartedAt(latestTask.getStartedAt())
                    .setLatestTaskFinishedAt(latestTask.getFinishedAt());
        }
        vo.copyBaseProperties(document);
        return vo;
    }

    private Map<String, KitKnowledgeIngestTask> groupLatestTasks(List<String> sourceDocumentIds) {
        if (CollectionUtils.isEmpty(sourceDocumentIds)) {
            return Collections.emptyMap();
        }
        List<KitKnowledgeIngestTask> tasks = ingestTaskMapper.selectList(new LambdaQueryWrapper<KitKnowledgeIngestTask>()
                .in(KitKnowledgeIngestTask::getSourceDocumentId, sourceDocumentIds)
                .eq(KitKnowledgeIngestTask::getDeleted, false)
                .orderByDesc(KitKnowledgeIngestTask::getModifyTime)
                .orderByDesc(KitKnowledgeIngestTask::getCreateTime));
        Map<String, KitKnowledgeIngestTask> latestTaskMap = new LinkedHashMap<>();
        for (KitKnowledgeIngestTask task : tasks) {
            latestTaskMap.putIfAbsent(task.getSourceDocumentId(), task);
        }
        return latestTaskMap;
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

    private void removeKnowledgeImages(String imageFileIdsJson) {
        if (!StringUtils.hasText(imageFileIdsJson)) {
            return;
        }
        List<String> fileIds;
        try {
            fileIds = GSON.fromJson(imageFileIdsJson, new TypeToken<List<String>>() {
            }.getType());
        } catch (Exception ignored) {
            return;
        }
        if (CollectionUtils.isEmpty(fileIds)) {
            return;
        }
        new LinkedHashSet<>(fileIds).stream()
                .filter(StringUtils::hasText)
                .forEach(FileUtils::delete);
    }
}
