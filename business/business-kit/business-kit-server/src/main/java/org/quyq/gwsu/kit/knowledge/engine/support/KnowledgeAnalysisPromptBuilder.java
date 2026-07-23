package org.quyq.gwsu.kit.knowledge.engine.support;

import org.quyq.gwsu.kit.knowledge.engine.image.KnowledgeImageMarkerSupport;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 知识源分析提示词构造器。
 */
@Component
public class KnowledgeAnalysisPromptBuilder {

    public String buildChunkAnalysisPrompt(String fileName, String sourceLanguage, String outputLanguage, int chunkNo, String chunkText) {
        return buildChunkAnalysisPrompt(fileName, sourceLanguage, outputLanguage, chunkNo, chunkText, null);
    }

    public String buildChunkAnalysisPrompt(String fileName,
                                           String sourceLanguage,
                                           String outputLanguage,
                                           int chunkNo,
                                           String chunkText,
                                           String currentGlobalDigest) {
        String markerSummary = KnowledgeImageMarkerSupport.buildMarkerSummary(chunkText);
        return """
                你是知识库导入阶段的分析助手。
                请仅基于给定片段提炼事实，不得添加原文未表达的事实。
                输出必须使用语言：%s。
                源文语言：%s。
                专有名词、产品名、文件名、库名可保留原文。
                片段编号：%d。
                文件名：%s。
                如果片段中出现 ![...](knowledge_image:fileId=...)，它表示原文中的图片占位标记。
                这些标记是后续页面生成必须保留的资源锚点；分析摘要里如果提到对应内容，必须原样保留这些标记。
                
                请提炼：
                1. 关键事实、定义、步骤、约束
                2. 重要标题与结构线索
                3. 与后续 Wiki 生成直接相关的实体、术语、配置值
                4. 图片标记与其附近语义的对应关系（若存在）
                5. 如果给出了“当前全局摘要”，请在保证事实不失真的前提下，与当前片段建立衔接，避免遗漏跨片段信息

                当前全局摘要：
                %s

                源文片段：
                %s

                %s
                """.formatted(
                outputLanguage,
                sourceLanguage,
                chunkNo,
                fileName,
                currentGlobalDigest == null ? "" : currentGlobalDigest,
                chunkText,
                markerSummary);
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
                如果片段摘要中出现 ![...](knowledge_image:fileId=...)，必须原样保留，不能删除。

                片段摘要列表：
                %s
                """.formatted(outputLanguage, sourceLanguage, fileName, String.join("\n\n", chunkDigests));
    }

    public String buildRollingDigestSummaryPrompt(String fileName,
                                                  String sourceLanguage,
                                                  String outputLanguage,
                                                  String currentGlobalDigest,
                                                  int chunkNo,
                                                  int totalChunks,
                                                  String currentChunkDigest) {
        return """
                你是知识库导入阶段的长文档汇总助手。
                请把“当前全局摘要”和“最新片段摘要”整合为新的全局摘要。
                输出必须使用语言：%s。
                源文语言：%s。
                专有名词、产品名、文件名、库名可保留原文。
                不得添加原文未表达的事实，不得把不同来源事实错误合并。
                文件名：%s。
                当前片段：%d/%d。
                如果摘要中出现 ![...](knowledge_image:fileId=...)，必须原样保留，不能删除。

                当前全局摘要：
                %s

                最新片段摘要：
                %s
                """.formatted(
                outputLanguage,
                sourceLanguage,
                fileName,
                chunkNo,
                totalChunks,
                currentGlobalDigest,
                currentChunkDigest);
    }
}
