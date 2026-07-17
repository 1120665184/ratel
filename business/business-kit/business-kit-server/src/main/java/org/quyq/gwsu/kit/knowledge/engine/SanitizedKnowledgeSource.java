package org.quyq.gwsu.kit.knowledge.engine;

import java.util.List;

/**
 * 清洗后的知识源文本。
 *
 * @param text 清洗文本
 * @param warnings 清洗告警
 */
public record SanitizedKnowledgeSource(String text, List<String> warnings) {

    public SanitizedKnowledgeSource {
        text = text == null ? "" : text;
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }
}
