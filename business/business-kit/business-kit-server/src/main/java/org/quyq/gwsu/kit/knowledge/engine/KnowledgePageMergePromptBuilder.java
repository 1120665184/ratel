package org.quyq.gwsu.kit.knowledge.engine;

import org.springframework.stereotype.Component;

/**
 * 知识 Page 合并提示词构造器。
 */
@Component
public class KnowledgePageMergePromptBuilder {

    public String buildPrompt(String outputLanguage, String existingContent, String incomingContent) {
        return """
                你是知识库 Page 合并助手。
                输出必须使用语言：%s。
                不得捏造事实，不得把不同来源事实错误合并。
                
                现有页面：
                %s
                
                新增来源页面：
                %s
                """.formatted(outputLanguage, existingContent, incomingContent);
    }
}
