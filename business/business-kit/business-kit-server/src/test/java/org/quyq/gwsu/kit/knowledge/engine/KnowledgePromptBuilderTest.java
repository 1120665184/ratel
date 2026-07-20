package org.quyq.gwsu.kit.knowledge.engine;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class KnowledgePromptBuilderTest {

    @Test
    void promptBuildersShouldEnforceConfiguredOutputLanguage() {
        KnowledgeAnalysisPromptBuilder analysisPromptBuilder = new KnowledgeAnalysisPromptBuilder();
        KnowledgeGenerationPromptBuilder generationPromptBuilder = new KnowledgeGenerationPromptBuilder();

        String analysisPrompt = analysisPromptBuilder.buildChunkAnalysisPrompt(
                "guide.md",
                "fr",
                "zh-CN",
                1,
                "contenu");
        String summaryPrompt = analysisPromptBuilder.buildDigestSummaryPrompt(
                "guide.md",
                "fr",
                "zh-CN",
                List.of("摘要一", "摘要二"));
        String generationPrompt = generationPromptBuilder.buildPrompt(new KnowledgePageGenerationRequest(
                "guide.md",
                "fr",
                "统一摘要",
                "受限原文",
                "zh-CN"));

        assertTrue(analysisPrompt.contains("zh-CN"));
        assertTrue(summaryPrompt.contains("zh-CN"));
        assertTrue(generationPrompt.contains("zh-CN"));
        assertTrue(generationPrompt.contains("不得添加原文未表达的事实"));
    }
}
