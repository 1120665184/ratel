package org.quyq.gwsu.kit.knowledge.service;

import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgeSourceDocument;
import org.quyq.gwsu.kit.knowledge.engine.page.GeneratedKnowledgePage;

/**
 * 知识 Page 合并发布服务。
 */
public interface KnowledgePageMergeService {

    String publish(KitKnowledgeSourceDocument sourceDocument, GeneratedKnowledgePage generatedPage);
}
