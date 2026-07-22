package org.quyq.gwsu.kit.knowledge.engine.image;

import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 知识库图片短标记工具。
 */
public final class KnowledgeImageMarkerSupport {

    private static final Pattern IMAGE_MARKER_PATTERN =
            Pattern.compile("!\\[([^\\]]*)]\\(knowledge_image:fileId=([^\\)\\s]+)\\)");

    private static final int MAX_ALT_TEXT_LENGTH = 500;

    private KnowledgeImageMarkerSupport() {
    }

    public static String marker(String fileId) {
        return marker(fileId, "");
    }

    public static String marker(String fileId, String altText) {
        if (!StringUtils.hasText(fileId)) {
            return "";
        }
        return "![%s](knowledge_image:fileId=%s)".formatted(sanitizeAltText(altText), fileId.trim());
    }

    public static Set<String> extractFileIds(String content) {
        if (!StringUtils.hasText(content)) {
            return Set.of();
        }
        Set<String> fileIds = new LinkedHashSet<>();
        Matcher matcher = IMAGE_MARKER_PATTERN.matcher(content);
        while (matcher.find()) {
            String fileId = matcher.group(2);
            if (StringUtils.hasText(fileId)) {
                fileIds.add(fileId.trim());
            }
        }
        return fileIds;
    }

    public static String renderToMarkdown(String content, Function<String, String> urlResolver) {
        if (!StringUtils.hasText(content)) {
            return "";
        }
        Matcher matcher = IMAGE_MARKER_PATTERN.matcher(content);
        StringBuffer rendered = new StringBuffer();
        while (matcher.find()) {
            String altText = sanitizeAltText(matcher.group(1));
            String fileId = matcher.group(2) == null ? "" : matcher.group(2).trim();
            String url = StringUtils.hasText(fileId) && urlResolver != null ? urlResolver.apply(fileId) : "";
            String replacement = StringUtils.hasText(url)
                    ? "![%s](%s)".formatted(altText, url)
                    : matcher.group();
            matcher.appendReplacement(rendered, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(rendered);
        return rendered.toString();
    }

    public static String buildMarkerSummary(String content) {
        Set<String> markers = extractMarkers(content);
        if (markers.isEmpty()) {
            return "";
        }
        StringBuilder summary = new StringBuilder("图片资源标记（以下标记在最终 Wiki 页面中必须原样保留在对应语义位置，不可删除、改写或合并）：\n");
        int index = 1;
        for (String marker : markers) {
            summary.append(index++)
                    .append(". ")
                    .append(marker)
                    .append('\n');
        }
        return summary.toString().trim();
    }

    public static String appendMarkerSummary(String content, String markerSummary) {
        if (!StringUtils.hasText(markerSummary)) {
            return content == null ? "" : content;
        }
        if (!StringUtils.hasText(content)) {
            return markerSummary;
        }
        if (content.contains(markerSummary)) {
            return content;
        }
        return content.strip() + "\n\n" + markerSummary;
    }

    public static String sanitizeAltText(String altText) {
        if (!StringUtils.hasText(altText)) {
            return "";
        }
        String normalized = altText.replace('\r', ' ')
                .replace('\n', ' ')
                .replace(']', '）')
                .replace('[', '（')
                .trim()
                .replaceAll("\\s+", " ");
        if (normalized.length() <= MAX_ALT_TEXT_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, MAX_ALT_TEXT_LENGTH).trim();
    }

    private static Set<String> extractMarkers(String content) {
        if (!StringUtils.hasText(content)) {
            return Set.of();
        }
        Set<String> markers = new LinkedHashSet<>();
        Matcher matcher = IMAGE_MARKER_PATTERN.matcher(content);
        while (matcher.find()) {
            markers.add(matcher.group());
        }
        return markers;
    }
}
