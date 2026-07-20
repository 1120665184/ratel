package org.quyq.gwsu.kit.knowledge.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.quyq.gwsu.kit.api.knowledge.dto.KnowledgeIngestTaskQueryDTO;
import org.quyq.gwsu.kit.api.knowledge.vo.KnowledgeIngestTaskVO;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgeIngestTask;

/**
 * 知识文档导入任务服务。
 */
public interface IKnowledgeIngestTaskService extends IService<KitKnowledgeIngestTask> {

    String createTask(String sourceDocumentId, Integer retryCount);

    void ensureNoActiveTask(String sourceDocumentId);

    IPage<KnowledgeIngestTaskVO> pageTasks(KnowledgeIngestTaskQueryDTO dto);

    KnowledgeIngestTaskVO getTask(String taskId);

    String retry(String taskId);
}
