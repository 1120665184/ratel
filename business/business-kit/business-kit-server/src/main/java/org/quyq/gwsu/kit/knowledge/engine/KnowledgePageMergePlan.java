package org.quyq.gwsu.kit.knowledge.engine;

import java.util.List;

/**
 * Page 合并计划。
 *
 * @param title 合并后的 Page 标题
 * @param items 合并后的块顺序
 */
public record KnowledgePageMergePlan(String title, List<Item> items) {

    /**
     * 合并计划项。
     *
     * @param type HEADING、EXISTING_BLOCK、INCOMING_BLOCK
     * @param refId E1/I1 这类稳定引用 ID
     * @param content HEADING 的内容
     */
    public record Item(String type, String refId, String content) {
    }
}
