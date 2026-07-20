package org.quyq.gwsu.kit.knowledge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.core.exception.BusinessException;
import org.quyq.gwsu.kit.api.knowledge.dto.KnowledgeIngestTaskQueryDTO;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgeIngestTaskStatus;
import org.quyq.gwsu.kit.api.knowledge.vo.KnowledgeIngestTaskVO;
import org.quyq.gwsu.kit.errcode.KitErrorCode;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgeIngestTask;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgeSourceDocument;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgeIngestTaskMapper;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgeSourceDocumentMapper;
import org.quyq.gwsu.kit.knowledge.service.IKnowledgeIngestTaskService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;

/**
 * 知识文档导入任务服务实现。
 */
@Service
@RequiredArgsConstructor
public class KnowledgeIngestTaskServiceImpl
        extends ServiceImpl<KnowledgeIngestTaskMapper, KitKnowledgeIngestTask>
        implements IKnowledgeIngestTaskService {

    private static final List<KnowledgeIngestTaskStatus> ACTIVE_STATUSES = List.of(
            KnowledgeIngestTaskStatus.PENDING,
            KnowledgeIngestTaskStatus.RUNNING);

    private final KnowledgeSourceDocumentMapper sourceDocumentMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String createTask(String sourceDocumentId, Integer retryCount) {
        KitKnowledgeSourceDocument sourceDocument = sourceDocumentMapper.selectOne(new LambdaQueryWrapper<KitKnowledgeSourceDocument>()
                .eq(KitKnowledgeSourceDocument::getId, sourceDocumentId)
                .eq(KitKnowledgeSourceDocument::getDeleted, false));
        if (Objects.isNull(sourceDocument)) {
            throw new BusinessException(KitErrorCode.E03001);
        }
        ensureNoActiveTask(sourceDocumentId);
        KitKnowledgeIngestTask task = new KitKnowledgeIngestTask()
                .setSourceDocumentId(sourceDocumentId)
                .setTaskStatus(KnowledgeIngestTaskStatus.PENDING)
                .setRetryCount(Objects.requireNonNullElse(retryCount, 0));
        save(task);
        return task.getId();
    }

    @Override
    public void ensureNoActiveTask(String sourceDocumentId) {
        long activeCount = count(new LambdaQueryWrapper<KitKnowledgeIngestTask>()
                .eq(KitKnowledgeIngestTask::getSourceDocumentId, sourceDocumentId)
                .eq(KitKnowledgeIngestTask::getDeleted, false)
                .in(KitKnowledgeIngestTask::getTaskStatus, ACTIVE_STATUSES));
        if (activeCount > 0) {
            throw new BusinessException(KitErrorCode.E03004);
        }
    }

    @Override
    public IPage<KnowledgeIngestTaskVO> pageTasks(KnowledgeIngestTaskQueryDTO dto) {
        Page<KitKnowledgeIngestTask> page = page(Page.of(dto.getPageNum(), dto.getPageSize()),
                new LambdaQueryWrapper<KitKnowledgeIngestTask>()
                        .eq(StringUtils.hasText(dto.getSourceDocumentId()), KitKnowledgeIngestTask::getSourceDocumentId, dto.getSourceDocumentId())
                        .eq(Objects.nonNull(dto.getTaskStatus()), KitKnowledgeIngestTask::getTaskStatus, dto.getTaskStatus())
                        .eq(KitKnowledgeIngestTask::getDeleted, false)
                        .orderByDesc(KitKnowledgeIngestTask::getCreateTime));
        Page<KnowledgeIngestTaskVO> result = Page.of(page.getCurrent(), page.getSize(), page.getTotal());
        result.setPages(page.getPages());
        result.setRecords(page.getRecords().stream().map(this::toTaskVO).toList());
        return result;
    }

    @Override
    public KnowledgeIngestTaskVO getTask(String taskId) {
        KitKnowledgeIngestTask task = getOne(new LambdaQueryWrapper<KitKnowledgeIngestTask>()
                .eq(KitKnowledgeIngestTask::getId, taskId)
                .eq(KitKnowledgeIngestTask::getDeleted, false));
        if (Objects.isNull(task)) {
            throw new BusinessException(KitErrorCode.E03002);
        }
        return toTaskVO(task);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String retry(String taskId) {
        KitKnowledgeIngestTask failedTask = getOne(new LambdaQueryWrapper<KitKnowledgeIngestTask>()
                .eq(KitKnowledgeIngestTask::getId, taskId)
                .eq(KitKnowledgeIngestTask::getDeleted, false));
        if (Objects.isNull(failedTask)) {
            throw new BusinessException(KitErrorCode.E03002);
        }
        if (failedTask.getTaskStatus() != KnowledgeIngestTaskStatus.FAILED) {
            throw new BusinessException(KitErrorCode.E03003);
        }
        return createTask(failedTask.getSourceDocumentId(),
                Objects.requireNonNullElse(failedTask.getRetryCount(), 0) + 1);
    }

    private KnowledgeIngestTaskVO toTaskVO(KitKnowledgeIngestTask task) {
        KnowledgeIngestTaskVO vo = new KnowledgeIngestTaskVO()
                .setId(task.getId())
                .setSourceDocumentId(task.getSourceDocumentId())
                .setTaskStatus(task.getTaskStatus())
                .setCurrentStage(task.getCurrentStage())
                .setRetryCount(task.getRetryCount())
                .setErrorMessage(task.getErrorMessage())
                .setStartedAt(task.getStartedAt())
                .setFinishedAt(task.getFinishedAt());
        vo.copyBaseProperties(task);
        return vo;
    }
}
