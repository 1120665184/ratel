package org.quyq.gwsu.kit.knowledge.engine;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 知识源分析提示词构造器。
 */
@Component
public class KnowledgeAnalysisPromptBuilder {

    public String buildChunkAnalysisPrompt(String fileName, String sourceLanguage, String outputLanguage, int chunkNo, String chunkText) {
        return """
                你是知识库导入阶段的分析助手。
                请仅基于给定片段提炼事实，不得添加原文未表达的事实。
                输出必须使用语言：%s。
                源文语言：%s。
                专有名词、产品名、文件名、库名可保留原文。
                片段编号：%d。
                文件名：%s。
                
                请提炼：
                1. 关键事实、定义、步骤、约束
                2. 重要标题与结构线索
                3. 与后续 Wiki 生成直接相关的实体、术语、配置值
                
                源文片段：
                %s
                """.formatted(outputLanguage, sourceLanguage, chunkNo, fileName, chunkText);
    }

    public String buildDigestSummaryPrompt(String fileName, String sourceLanguage, String outputLanguage, List<String> chunkDigests) {
        return """
                你是知识库导入阶段的汇总助手。
                请把多个片段摘要整理为统一的全局分析摘要。
                输出必须使用语言：%s。
                源文语言：%s。
                专有名词、产品名、文件名、库名可保留原文。
                不得添加原文未表达的事实，不得把不同来源事实错误合并。
                文件名：%s。
                
                片段摘要列表：
                %s
                """.formatted(outputLanguage, sourceLanguage, fileName, String.join("\n\n", chunkDigests));
    }
}
