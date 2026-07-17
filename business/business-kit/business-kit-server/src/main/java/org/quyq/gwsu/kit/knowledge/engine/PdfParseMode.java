package org.quyq.gwsu.kit.knowledge.engine;

/**
 * PDF 文本解析模式。
 */
public enum PdfParseMode {

    /** 使用本地解析器，文件不会离开当前服务。 */
    LOCAL,

    /** 预留给增强解析策略；未配置实现时必须回退本地解析。 */
    ENHANCED
}
