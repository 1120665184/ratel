package org.quyq.gwsu.kit.knowledge.service;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.quyq.gwsu.common.core.utils.ThreadPoolUtil;
import org.quyq.gwsu.kit.knowledge.task.KnowledgeIngestExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.ExecutorService;

/**
 * 知识导入异步派发器。
 */
@Slf4j
@Component
public class KnowledgeIngestDispatcher {

    private final ExecutorService executorService = ThreadPoolUtil.newVirtualThreadPerTaskExecutor();

    private final KnowledgeIngestExecutor ingestExecutor;

    public KnowledgeIngestDispatcher(KnowledgeIngestExecutor ingestExecutor) {
        this.ingestExecutor = ingestExecutor;
    }

    public void dispatchAfterCommit(String taskId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    dispatch(taskId);
                }
            });
            return;
        }
        dispatch(taskId);
    }

    public void dispatch(String taskId) {
        executorService.submit(() -> {
            try {
                ingestExecutor.execute(taskId);
            } catch (Exception ex) {
                log.error("知识导入任务执行失败, taskId={}", taskId, ex);
            }
        });
    }

    @PreDestroy
    public void destroy() {
        executorService.shutdown();
    }
}
