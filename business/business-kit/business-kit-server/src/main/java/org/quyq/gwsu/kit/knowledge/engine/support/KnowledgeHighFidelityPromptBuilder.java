package org.quyq.gwsu.kit.knowledge.engine.support;

import org.quyq.gwsu.kit.knowledge.engine.ingest.KnowledgeSourceSegmentDraft;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 高保真分段生成提示词构造器。
 */
@Component
public class KnowledgeHighFidelityPromptBuilder {

    public String buildBatchPrompt(String fileName,
                                   String sourceLanguage,
                                   String outputLanguage,
                                   int batchNo,
                                   int batchTotal,
                                   List<KnowledgeSourceSegmentDraft> batchSegments) {
        StringBuilder source = new StringBuilder();
        for (KnowledgeSourceSegmentDraft segment : batchSegments) {
            source.append("## Segment ").append(segment.segmentNo())
                    .append(" [").append(segment.segmentType()).append("]")
                    .append('\n');
            if (segment.headingPath() != null && !segment.headingPath().isBlank()) {
                source.append("headingPath: ").append(segment.headingPath()).append('\n');
            }
            if (segment.sourceLocator() != null && !segment.sourceLocator().isBlank()) {
                source.append("locator: ").append(segment.sourceLocator()).append('\n');
            }
            source.append(segment.content()).append("\n\n");
        }
        return """
                你是高保真知识文档整理助手。
                你的任务不是总结，而是把当前批次原文片段忠实整理为 Wiki blocks。
                输出必须使用语言：%s。
                源文语言：%s。
                文件名：%s。
                当前批次：%d/%d。
                保持原文顺序，不得跨批次补写，不得删除重要限制条件，不得臆造结论。
                如果原文出现图片标记 ![...](knowledge_image:fileId=...)，它表示已经完成 OCR/占位处理的图片内容，对这类图片标记必须逐字符原样保留，不能改 alt 文本，不能改 fileId，不能删除，不能移动到错误位置。
                根据规则输出JSON对象。
                # 当前批次原文片段：
                ```text
                %s
                ```
                """.formatted(outputLanguage, sourceLanguage, fileName, batchNo, batchTotal, source);
    }

    public String buildTitlePrompt(String fileName,
                                   String sourceLanguage,
                                   String outputLanguage,
                                   List<KnowledgeSourceSegmentDraft> titleSegments) {
        StringBuilder source = new StringBuilder();
        for (KnowledgeSourceSegmentDraft segment : titleSegments) {
            source.append("## Segment ").append(segment.segmentNo())
                    .append(" [").append(segment.segmentType()).append("]")
                    .append('\n');
            if (segment.headingPath() != null && !segment.headingPath().isBlank()) {
                source.append("headingPath: ").append(segment.headingPath()).append('\n');
            }
            source.append(segment.content()).append("\n\n");
        }
        return """
                你是知识文档标题生成助手。
                请基于文件名和文档开头的重要标题/导语，生成一个适合作为 Wiki Page 的标题。
                输出语言：%s。
                源文语言：%s。
                文件名：%s。

                要求：
                1. 标题应简洁自然，优先准确表达文档主题。
                2. 可以结合文件名和正文标题优化表达，但不得臆造原文没有的主题。
                3. 不要输出章节名、目录名、公司名加“制度汇编”这类泛化标题，除非原文主题本来如此。
                4. 只输出标题本身，不要输出引号、序号、解释、thinking、代码围栏或 JSON。

                文档开头片段：
                %s
                """.formatted(outputLanguage, sourceLanguage, fileName, source);
    }
}
