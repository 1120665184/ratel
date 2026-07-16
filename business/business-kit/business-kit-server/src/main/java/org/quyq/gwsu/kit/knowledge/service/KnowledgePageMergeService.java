package org.quyq.gwsu.kit.knowledge.service;

import org.quyq.gwsu.kit.knowledge.domain.KnowledgeSourceDocument;
import org.quyq.gwsu.kit.knowledge.engine.GeneratedKnowledgePage;

/**
 * 知识 Page 合并发布服务。
 */
public interface KnowledgePageMergeService {

    String publish(String tenantId, KnowledgeSourceDocument sourceDocument, GeneratedKnowledgePage generatedPage);
}
