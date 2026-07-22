package org.quyq.gwsu.kit.knowledge.engine.ingest;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 知识源文本导入前清洗器。
 *
 * <p>清洗只移除不可见控制字符和空结构，不删除字母、数字或 CJK 等事实字符。</p>
 */
@Component
public class KnowledgeIngestSanitizer {

    private static final int MAX_LINE_LENGTH = 1000;

    /**
     * 清洗源文本。对已清洗文本重复调用不会再改变结果。
     *
     * @param sourceText 原始文本
     * @return 清洗结果
     */
    public SanitizedKnowledgeSource sanitize(String sourceText) {
        if (sourceText == null || sourceText.isEmpty()) {
            return new SanitizedKnowledgeSource("", List.of());
        }
        List<String> warnings = new ArrayList<>();
        String normalized = normalizeLineEnding(sourceText);
        String withoutControls = removeControlCharacters(normalized);
        if (!normalized.equals(withoutControls)) {
            warnings.add("已移除源文档中的不可见控制字符。");
        }
        String withoutEmptyTableRows = removeEmptyTableRows(withoutControls);
        if (!withoutControls.equals(withoutEmptyTableRows)) {
            warnings.add("已移除空表格行。");
        }
        String compacted = compactBlankLines(withoutEmptyTableRows);
        String lineWrapped = insertSafeLineBreaks(compacted);
        if (!compacted.equals(lineWrapped)) {
            warnings.add("已在超长无断行文本的安全边界补充分行。");
        }
        return new SanitizedKnowledgeSource(lineWrapped, warnings);
    }

    /**
     * 清洗已解析文档，并合并解析阶段与清洗阶段告警。
     *
     * @param document 已解析文档
     * @return 清洗结果
     */
    public SanitizedKnowledgeSource sanitize(ParsedKnowledgeDocument document) {
        if (document == null) {
            return new SanitizedKnowledgeSource("", List.of());
        }
        SanitizedKnowledgeSource sanitized = sanitize(document.text());
        if (document.parseWarnings().isEmpty()) {
            return sanitized;
        }
        List<String> warnings = new ArrayList<>(document.parseWarnings().size() + sanitized.warnings().size());
        warnings.addAll(document.parseWarnings());
        warnings.addAll(sanitized.warnings());
        return new SanitizedKnowledgeSource(sanitized.text(), warnings);
    }

    private String normalizeLineEnding(String text) {
        return text.replace("\r\n", "\n").replace('\r', '\n');
    }

    private String removeControlCharacters(String text) {
        StringBuilder result = new StringBuilder(text.length());
        text.codePoints().forEach(codePoint -> {
            if (codePoint == '\n' || codePoint == '\t' || !Character.isISOControl(codePoint)) {
                result.appendCodePoint(codePoint);
            }
        });
        return result.toString();
    }

    private String removeEmptyTableRows(String text) {
        List<String> remainingLines = new ArrayList<>();
        boolean inCodeBlock = false;
        String[] lines = text.split("\\n", -1);
        for (String line : lines) {
            boolean fenceLine = line.stripLeading().startsWith("```");
            if (inCodeBlock || !isEmptyTableRow(line)) {
                remainingLines.add(line);
            }
            if (fenceLine) {
                inCodeBlock = !inCodeBlock;
            }
        }
        return String.join("\n", remainingLines);
    }

    private boolean isEmptyTableRow(String line) {
        String trimmed = line.trim();
        return trimmed.length() >= 2 && trimmed.startsWith("|") && trimmed.endsWith("|")
                && trimmed.substring(1, trimmed.length() - 1).replace("|", "").trim().isEmpty();
    }

    private String compactBlankLines(String text) {
        StringBuilder result = new StringBuilder(text.length());
        boolean inCodeBlock = false;
        int blankLineCount = 0;
        String[] lines = text.split("\\n", -1);
        for (int index = 0; index < lines.length; index++) {
            String line = lines[index];
            boolean fenceLine = line.stripLeading().startsWith("```");
            if (!inCodeBlock && line.isBlank() && !fenceLine) {
                blankLineCount++;
                if (blankLineCount > 1) {
                    continue;
                }
            } else {
                blankLineCount = 0;
            }
            result.append(line);
            if (index < lines.length - 1) {
                result.append('\n');
            }
            if (fenceLine) {
                inCodeBlock = !inCodeBlock;
                blankLineCount = 0;
            }
        }
        return result.toString();
    }

    private String insertSafeLineBreaks(String text) {
        StringBuilder result = new StringBuilder(text.length() + text.length() / MAX_LINE_LENGTH);
        boolean inCodeBlock = false;
        String[] lines = text.split("\\n", -1);
        for (int index = 0; index < lines.length; index++) {
            String line = lines[index];
            if (line.stripLeading().startsWith("```")) {
                inCodeBlock = !inCodeBlock;
            }
            result.append(inCodeBlock ? line : wrapAtSafeBoundaries(line));
            if (index < lines.length - 1) {
                result.append('\n');
            }
        }
        return result.toString();
    }

    private String wrapAtSafeBoundaries(String line) {
        if (line.length() <= MAX_LINE_LENGTH) {
            return line;
        }
        StringBuilder result = new StringBuilder(line.length() + line.length() / MAX_LINE_LENGTH);
        int offset = 0;
        while (line.length() - offset > MAX_LINE_LENGTH) {
            int breakIndex = findSafeBreak(line, offset, Math.min(line.length(), offset + MAX_LINE_LENGTH));
            if (breakIndex < 0 && offset == 0) {
                breakIndex = findSafeBreak(line, 0, line.length());
            }
            if (breakIndex < 0) {
                break;
            }
            result.append(line, offset, breakIndex + 1).append('\n');
            offset = breakIndex + 1;
        }
        result.append(line, offset, line.length());
        return result.toString();
    }

    private int findSafeBreak(String line, int startInclusive, int endExclusive) {
        for (int index = endExclusive - 1; index >= startInclusive; index--) {
            if (isSafeBreakCharacter(line.charAt(index))) {
                return index;
            }
        }
        return -1;
    }

    private boolean isSafeBreakCharacter(char character) {
        return Character.isWhitespace(character)
                || ",.;:!?，。；：！？、)]}".indexOf(character) >= 0;
    }
}
