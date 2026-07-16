package org.quyq.gwsu.kit.knowledge.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.quyq.gwsu.kit.knowledge.domain.KnowledgeIngestTask;

/**
 * 知识文档导入任务服务。
 */
public interface IKnowledgeIngestTaskService extends IService<KnowledgeIngestTask> {

    String retry(String tenantId, String taskId);
}
