package org.quyq.gwsu.kit.knowledge.engine.ingest;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 将清洗后的文档文本转换为可追溯的结构化片段。
 */
@Component
public class KnowledgeSourceSegmentationService {

    private static final Pattern PAGE_PATTERN = Pattern.compile("^Page\\s+(\\d+)$");

    private static final Pattern SLIDE_PATTERN = Pattern.compile("^Slide\\s+(\\d+)$");

    private static final Pattern SHEET_PATTERN = Pattern.compile("^Sheet\\s+(\\d+):\\s*(.*)$");

    private static final Pattern TITLE_PATTERN = Pattern.compile("^Title:\\s*(.+)$");

    private static final Pattern PARAGRAPH_PATTERN = Pattern.compile("^(?:Paragraph|Content):\\s*(.+)$");

    private static final Pattern TABLE_MARKER_PATTERN = Pattern.compile("^\\[Table\\s+(\\d+)]$");

    private static final Pattern TABLE_ROW_PATTERN = Pattern.compile("^(?:Table Row|Row\\s+\\d+):\\s*(.+)$");

    public SegmentedKnowledgeSource segment(ParsedKnowledgeDocument parsedDocument, SanitizedKnowledgeSource sanitizedSource) {
        String sourceLanguage = parsedDocument == null ? "und" : parsedDocument.sourceLanguage();
        String text = sanitizedSource == null ? "" : sanitizedSource.text();
        if (!StringUtils.hasText(text)) {
            return new SegmentedKnowledgeSource(sourceLanguage, List.of());
        }
        String currentScope = "document";
        String currentHeadingPath = "";
        int currentTableNo = 0;
        int segmentNo = 1;
        List<KnowledgeSourceSegmentDraft> segments = new ArrayList<>();
        String[] lines = text.split("\\R");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (!StringUtils.hasText(line)) {
                continue;
            }
            Matcher pageMatcher = PAGE_PATTERN.matcher(line);
            if (pageMatcher.matches()) {
                currentScope = "page:" + pageMatcher.group(1);
                currentTableNo = 0;
                continue;
            }
            Matcher slideMatcher = SLIDE_PATTERN.matcher(line);
            if (slideMatcher.matches()) {
                currentScope = "slide:" + slideMatcher.group(1);
                currentTableNo = 0;
                continue;
            }
            Matcher sheetMatcher = SHEET_PATTERN.matcher(line);
            if (sheetMatcher.matches()) {
                currentScope = "sheet:" + sheetMatcher.group(1);
                currentHeadingPath = normalizeHeadingPath(sheetMatcher.group(2), currentHeadingPath);
                currentTableNo = 0;
                continue;
            }
            Matcher titleMatcher = TITLE_PATTERN.matcher(line);
            if (titleMatcher.matches()) {
                String title = titleMatcher.group(1).trim();
                currentHeadingPath = normalizeHeadingPath(title, currentHeadingPath);
                segments.add(new KnowledgeSourceSegmentDraft(
                        segmentNo++,
                        "HEADING",
                        currentHeadingPath,
                        currentScope + "/heading:" + countHeadingDepth(currentHeadingPath),
                        title));
                continue;
            }
            Matcher tableMarkerMatcher = TABLE_MARKER_PATTERN.matcher(line);
            if (tableMarkerMatcher.matches()) {
                currentTableNo = Integer.parseInt(tableMarkerMatcher.group(1));
                StringBuilder tableContent = new StringBuilder();
                int rowStart = i + 1;
                int rowCount = 0;
                while (i + 1 < lines.length) {
                    Matcher rowMatcher = TABLE_ROW_PATTERN.matcher(lines[i + 1].trim());
                    if (!rowMatcher.matches()) {
                        break;
                    }
                    if (tableContent.length() > 0) {
                        tableContent.append('\n');
                    }
                    tableContent.append("| ").append(rowMatcher.group(1).trim().replace(" | ", " | ")).append(" |");
                    i++;
                    rowCount++;
                }
                if (tableContent.length() > 0) {
                    segments.add(new KnowledgeSourceSegmentDraft(
                            segmentNo++,
                            "TABLE",
                            currentHeadingPath,
                            currentScope + "/table:" + currentTableNo + "/rows:" + rowStart + "-" + (rowStart + rowCount - 1),
                            tableContent.toString()));
                }
                continue;
            }
            Matcher paragraphMatcher = PARAGRAPH_PATTERN.matcher(line);
            if (paragraphMatcher.matches()) {
                int currentSegmentNo = segmentNo++;
                segments.add(new KnowledgeSourceSegmentDraft(
                        currentSegmentNo,
                        detectTextSegmentType(paragraphMatcher.group(1)),
                        currentHeadingPath,
                        currentScope + "/segment:" + currentSegmentNo,
                        paragraphMatcher.group(1).trim()));
                continue;
            }
            int currentSegmentNo = segmentNo++;
            segments.add(new KnowledgeSourceSegmentDraft(
                    currentSegmentNo,
                    detectTextSegmentType(line),
                    currentHeadingPath,
                    currentScope + "/segment:" + currentSegmentNo,
                    line));
        }
        return new SegmentedKnowledgeSource(sourceLanguage, segments);
    }

    private String detectTextSegmentType(String text) {
        String normalized = text == null ? "" : text.trim();
        if (normalized.startsWith("- ") || normalized.startsWith("* ") || normalized.matches("\\d+\\.\\s+.*")) {
            return "LIST";
        }
        if (normalized.startsWith(">")) {
            return "QUOTE";
        }
        if (normalized.startsWith("```")) {
            return "CODE";
        }
        return "PARAGRAPH";
    }

    private String normalizeHeadingPath(String title, String currentHeadingPath) {
        if (!StringUtils.hasText(title)) {
            return currentHeadingPath;
        }
        if (!StringUtils.hasText(currentHeadingPath)) {
            return title.trim();
        }
        if (currentHeadingPath.endsWith(title.trim())) {
            return currentHeadingPath;
        }
        return title.trim();
    }

    private int countHeadingDepth(String headingPath) {
        if (!StringUtils.hasText(headingPath)) {
            return 1;
        }
        return Math.max(1, headingPath.split("\\s*/\\s*").length);
    }
}
