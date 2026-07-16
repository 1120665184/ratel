package org.quyq.gwsu.kit.knowledge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.core.exception.BusinessException;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgeIngestTaskStatus;
import org.quyq.gwsu.kit.errcode.KitErrorCode;
import org.quyq.gwsu.kit.knowledge.domain.KnowledgeIngestTask;
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
        extends ServiceImpl<KnowledgeIngestTaskMapper, KnowledgeIngestTask>
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
        KnowledgeIngestTask failedTask = getOne(new LambdaQueryWrapper<KnowledgeIngestTask>()
                .eq(KnowledgeIngestTask::getId, taskId)
                .eq(KnowledgeIngestTask::getTenantId, tenantId)
                .eq(KnowledgeIngestTask::getDeleted, false));
        if (Objects.isNull(failedTask)) {
            throw new BusinessException(KitErrorCode.E03002);
        }
        if (failedTask.getTaskStatus() != KnowledgeIngestTaskStatus.FAILED) {
            throw new BusinessException(KitErrorCode.E03003);
        }

        long activeCount = count(new LambdaQueryWrapper<KnowledgeIngestTask>()
                .eq(KnowledgeIngestTask::getSourceDocumentId, failedTask.getSourceDocumentId())
                .eq(KnowledgeIngestTask::getTenantId, failedTask.getTenantId())
                .eq(KnowledgeIngestTask::getDeleted, false)
                .in(KnowledgeIngestTask::getTaskStatus, ACTIVE_STATUSES));
        if (activeCount > 0) {
            throw new BusinessException(KitErrorCode.E03004);
        }

        KnowledgeIngestTask retryTask = new KnowledgeIngestTask()
                .setSourceDocumentId(failedTask.getSourceDocumentId())
                .setTaskStatus(KnowledgeIngestTaskStatus.PENDING)
                .setRetryCount(Objects.requireNonNullElse(failedTask.getRetryCount(), 0) + 1);
        retryTask.setTenantId(failedTask.getTenantId());
        save(retryTask);
        return retryTask.getId();
    }
}
