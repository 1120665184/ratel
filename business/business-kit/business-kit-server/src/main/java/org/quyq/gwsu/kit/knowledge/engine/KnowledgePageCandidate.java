package org.quyq.gwsu.kit.knowledge.engine;

/**
 * Page 归属候选。
 *
 * @param pageId Page ID
 * @param title Page 标题
 * @param markdownExcerpt 当前版本内容摘要
 * @param score 确定性召回得分
 */
public record KnowledgePageCandidate(
        String pageId,
        String title,
        String markdownExcerpt,
        double score) {
}
