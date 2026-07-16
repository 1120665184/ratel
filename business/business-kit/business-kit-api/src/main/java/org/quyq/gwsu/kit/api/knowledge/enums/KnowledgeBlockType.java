package org.quyq.gwsu.kit.api.knowledge.enums;

/**
 * 知识 Page Block 类型。
 */
public enum KnowledgeBlockType {

    /**
     * 标题块，对应 Markdown 中以 {@code #} 开头的标题行，用于表达 Page 的层级结构和 Chunk 的标题路径。
     */
    HEADING,

    /**
     * 普通段落块，对应连续的正文文本，是知识内容最常见的承载单元。
     */
    PARAGRAPH,

    /**
     * 列表块，对应 Markdown 中的无序列表或有序列表，用于保留步骤、要点、枚举项等结构化内容。
     */
    LIST,

    /**
     * 表格块，对应 Markdown 表格行，用于保留字段说明、对比关系、配置清单等二维结构内容。
     */
    TABLE,

    /**
     * 代码块，对应 Markdown 围栏代码块，用于保留命令、配置、代码片段等需要原样展示的内容。
     */
    CODE,

    /**
     * 引用块，对应 Markdown 中以 {@code >} 开头的引用内容，用于保留说明、备注、原文引用等语义。
     */
    QUOTE
}
