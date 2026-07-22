package org.quyq.gwsu.kit.knowledge.engine.image;

/**
 * 图片 OCR / 描述结果。
 */
public record KnowledgeImageOcrResult(String altText, boolean parsed, String warning) {

    public static KnowledgeImageOcrResult skipped(String warning) {
        return new KnowledgeImageOcrResult("", false, warning == null ? "" : warning);
    }

    public static KnowledgeImageOcrResult success(String altText) {
        return new KnowledgeImageOcrResult(altText == null ? "" : altText, true, "");
    }
}
