package org.quyq.gwsu.kit.knowledge.engine;

import java.util.List;

/**
 * 知识 Page 匹配与合并模型客户端。
 */
public interface KnowledgePageMergeModelClient {

    KnowledgePageMatchDecision matchPage(String prompt, List<KnowledgePageCandidate> candidates);

    KnowledgePageMergePlan planMerge(String prompt);
}
