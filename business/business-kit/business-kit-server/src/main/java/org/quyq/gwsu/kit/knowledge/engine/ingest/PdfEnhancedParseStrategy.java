package org.quyq.gwsu.kit.knowledge.engine.ingest;

/**
 * PDF 增强解析策略扩展点。
 *
 * <p>实现必须在当前服务受控环境内完成解析；不得将源文件或原始内容外发到第三方服务。</p>
 */
@FunctionalInterface
public interface PdfEnhancedParseStrategy {

    /**
     * 解析指定 PDF 文件。
     *
     * @param fileId 文件 ID
     * @return 解析结果
     */
    ParsedKnowledgeDocument parse(String fileId);
}
