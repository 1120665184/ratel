package org.quyq.gwsu.kit.knowledge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.core.exception.BusinessException;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgeIngestTaskStatus;
import org.quyq.gwsu.kit.errcode.KitErrorCode;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgeIngestTask;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgeIngestTaskMapper;
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String retry(String tenantId, String taskId) {
        if (!StringUtils.hasText(tenantId)) {
            throw new BusinessException(KitErrorCode.E03005);
        }
        KitKnowledgeIngestTask failedTask = getOne(new LambdaQueryWrapper<KitKnowledgeIngestTask>()
                .eq(KitKnowledgeIngestTask::getId, taskId)
                .eq(KitKnowledgeIngestTask::getTenantId, tenantId)
                .eq(KitKnowledgeIngestTask::getDeleted, false));
        if (Objects.isNull(failedTask)) {
            throw new BusinessException(KitErrorCode.E03002);
        }
        if (failedTask.getTaskStatus() != KnowledgeIngestTaskStatus.FAILED) {
            throw new BusinessException(KitErrorCode.E03003);
        }

        long activeCount = count(new LambdaQueryWrapper<KitKnowledgeIngestTask>()
                .eq(KitKnowledgeIngestTask::getSourceDocumentId, failedTask.getSourceDocumentId())
                .eq(KitKnowledgeIngestTask::getTenantId, failedTask.getTenantId())
                .eq(KitKnowledgeIngestTask::getDeleted, false)
                .in(KitKnowledgeIngestTask::getTaskStatus, ACTIVE_STATUSES));
        if (activeCount > 0) {
            throw new BusinessException(KitErrorCode.E03004);
        }

        KitKnowledgeIngestTask retryTask = new KitKnowledgeIngestTask()
                .setSourceDocumentId(failedTask.getSourceDocumentId())
                .setTaskStatus(KnowledgeIngestTaskStatus.PENDING)
                .setRetryCount(Objects.requireNonNullElse(failedTask.getRetryCount(), 0) + 1);
        retryTask.setTenantId(failedTask.getTenantId());
        save(retryTask);
        return retryTask.getId();
    }
}
