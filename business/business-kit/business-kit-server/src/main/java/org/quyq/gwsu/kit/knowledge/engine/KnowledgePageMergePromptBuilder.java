package org.quyq.gwsu.kit.knowledge.engine;

import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * 知识 Page 合并提示词构造器。
 */
@Component
public class KnowledgePageMergePromptBuilder {

    public String buildMatchPrompt(String outputLanguage,
                                   GeneratedKnowledgePage incomingPage,
                                   List<KnowledgePageCandidate> candidates) {
        return """
                你是知识库 Wiki Page 归属判断助手。
                输出必须使用语言：%s。
                你的任务是判断“新增来源页面”应该归入哪个已有 Page，还是应该创建新 Page。
                不要只看标题是否完全相等，要综合主题、范围、核心概念和内容摘要判断。
                如果只是标题相似但主题范围不同，必须创建新 Page。
                如果主题相同但标题表述不同，可以匹配已有 Page。
                输出必须只包含 action、pageId、confidence、reason 四个字段。
                action 只能是 MATCH_EXISTING_PAGE 或 CREATE_NEW_PAGE。
                当 action=CREATE_NEW_PAGE 时，pageId 为空字符串。
                
                新增来源页面标题：
                %s
                
                新增来源页面内容：
                %s
                
                候选 Page：
                %s
                """.formatted(
                outputLanguage,
                incomingPage.title(),
                abbreviate(incomingPage.markdownContent(), 5000),
                renderCandidates(candidates));
    }

    public String buildMergePlanPrompt(String outputLanguage,
                                       String pageTitle,
                                       List<KnowledgePageMergeBlockRef> existingBlocks,
                                       List<KnowledgePageMergeBlockRef> incomingBlocks) {
        return """
                你是知识库 Wiki Page 结构合并助手。
                输出必须使用语言：%s。
                你的任务是生成一个结构化合并计划，让旧 Page 和新增来源内容合并后读起来像一个完整 Markdown 页面。
                
                重要约束：
                1. 不得捏造事实。
                2. 不得把不同来源正文改写成一个无法追踪来源的新正文块。
                3. 可以创建、去重、重排 Markdown 标题（HEADING），标题属于页面结构，不要求绑定来源。
                4. 正文、列表、表格、代码、引用必须通过 EXISTING_BLOCK 或 INCOMING_BLOCK 引用原始块。
                5. 如果旧 Page 和新 Page 有相同/相近标题，只保留一个合理的标题，并把相关正文放到这个标题下面。
                6. 输出必须只包含 title 和 items 两个字段。
                7. items 中每一项 type 只能是 HEADING、EXISTING_BLOCK、INCOMING_BLOCK。
                8. type=HEADING 时填写 content；type=EXISTING_BLOCK 或 INCOMING_BLOCK 时填写 refId。
                
                当前 Page 标题：
                %s
                
                旧 Page Blocks：
                %s
                
                新增来源 Blocks：
                %s
                """.formatted(
                outputLanguage,
                pageTitle,
                renderBlockRefs(existingBlocks),
                renderBlockRefs(incomingBlocks));
    }

    private String renderCandidates(List<KnowledgePageCandidate> candidates) {
        if (CollectionUtils.isEmpty(candidates)) {
            return "无候选 Page。";
        }
        StringBuilder builder = new StringBuilder();
        for (KnowledgePageCandidate candidate : candidates) {
            builder.append("- pageId: ").append(candidate.pageId()).append('\n')
                    .append("  title: ").append(candidate.title()).append('\n')
                    .append("  score: ").append(candidate.score()).append('\n')
                    .append("  markdownExcerpt: ").append(candidate.markdownExcerpt()).append("\n\n");
        }
        return builder.toString();
    }

    private String renderBlockRefs(List<KnowledgePageMergeBlockRef> refs) {
        if (CollectionUtils.isEmpty(refs)) {
            return "无。";
        }
        StringBuilder builder = new StringBuilder();
        for (KnowledgePageMergeBlockRef ref : refs) {
            builder.append("- refId: ").append(ref.refId()).append('\n')
                    .append("  blockType: ").append(ref.block().getBlockType()).append('\n')
                    .append("  sourceDocumentId: ")
                    .append(ref.sourceRef() == null ? "" : ref.sourceRef().getSourceDocumentId())
                    .append('\n')
                    .append("  content: |").append('\n')
                    .append(indent(abbreviate(ref.block().getContent(), 2000))).append("\n\n");
        }
        return builder.toString();
    }

    private String abbreviate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text == null ? "" : text;
        }
        return text.substring(0, maxLength) + "\n...[已截断]";
    }

    private String indent(String text) {
        if (text == null || text.isBlank()) {
            return "    ";
        }
        return "    " + text.replace("\n", "\n    ");
    }
}
