package org.quyq.gwsu.kit.knowledge.engine.support;

import org.quyq.gwsu.kit.knowledge.engine.image.KnowledgeImageMarkerSupport;
import org.quyq.gwsu.kit.knowledge.engine.page.KnowledgePageGenerationRequest;
import org.springframework.stereotype.Component;

/**
 * 知识 Page 生成提示词构造器。
 */
@Component
public class KnowledgeGenerationPromptBuilder {

    public String buildPrompt(KnowledgePageGenerationRequest request) {
        String markerSummary = KnowledgeImageMarkerSupport.buildMarkerSummary(request.boundedSourceText());
        return """
                你是知识库 Wiki 页面整理助手。
                请把知识源内容整理成结构清晰、事实准确的 Markdown 页面。
                输出必须使用语言：%s。
                源文语言：%s。
                专有名词、产品名、模型名、库名、文件名可保留原文。
                不得添加原文未表达的事实，不得臆造结论。
                输出必须只包含 title 和 markdownContent 两个字段。
                如果输入中出现 ![...](knowledge_image:fileId=...)，它表示原文图片占位标记。
                对于与该图片相关的内容，必须在最终 markdownContent 中保留对应标记，并放在正确语义位置。
                不允许删除、改写、合并、翻译这些标记。
                
                文件名：%s
                
                全局分析摘要：
                ```markdown
                %s
                ```
                
                受限原文上下文：
                ```text
                %s
                ```
                
                %s
                """.formatted(
                request.outputLanguage(),
                request.sourceLanguage(),
                request.fileName(),
                request.analysisDigest(),
                request.boundedSourceText(),
                markerSummary);
    }
}
