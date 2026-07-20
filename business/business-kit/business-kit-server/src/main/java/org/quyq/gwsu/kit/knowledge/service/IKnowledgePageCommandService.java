package org.quyq.gwsu.kit.knowledge.service;

import org.quyq.gwsu.kit.api.knowledge.dto.KnowledgePageSaveDTO;

/**
 * 知识 Page 命令服务。
 */
public interface IKnowledgePageCommandService {

    /**
     * 保存知识 Page，并发布为当前版本。
     *
     * @param dto 保存参数
     * @return Page ID
     */
    String savePage(KnowledgePageSaveDTO dto);
}
