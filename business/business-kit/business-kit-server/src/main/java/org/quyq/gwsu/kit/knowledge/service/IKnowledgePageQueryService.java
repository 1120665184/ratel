package org.quyq.gwsu.kit.knowledge.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.quyq.gwsu.kit.api.knowledge.dto.KnowledgePageQueryDTO;
import org.quyq.gwsu.kit.api.knowledge.vo.KnowledgePageDetailVO;
import org.quyq.gwsu.kit.api.knowledge.vo.KnowledgePageVO;

/**
 * 知识 Page 查询服务。
 */
public interface IKnowledgePageQueryService {

    IPage<KnowledgePageVO> pagePages(KnowledgePageQueryDTO dto);

    KnowledgePageDetailVO getPage(String pageId);
}
