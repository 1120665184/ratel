package org.quyq.gwsu.kit.knowledge.engine;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.quyq.gwsu.common.core.utils.AssertUtils;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgeBlockType;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgeSourceType;
import org.quyq.gwsu.kit.errcode.KitErrorCode;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgePageBlock;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgePageSourceRef;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Markdown 到 Page Block 的工厂。
 */
@Component
public class KnowledgeBlockFactory {

    public KnowledgeBlockBuildResult build(String pageVersionId, String sourceDocumentId, String markdown) {
        AssertUtils.hasText(pageVersionId, KitErrorCode.E03009);
        AssertUtils.hasText(sourceDocumentId, KitErrorCode.E03009);
        AssertUtils.hasText(markdown, KitErrorCode.E03009);
        List<MarkdownSegment> segments = splitMarkdown(markdown);
        List<KitKnowledgePageBlock> blocks = new ArrayList<>();
        List<KitKnowledgePageSourceRef> refs = new ArrayList<>();
        for (int i = 0; i < segments.size(); i++) {
            MarkdownSegment segment = segments.get(i);
            String blockId = nextBlockId();
            KitKnowledgePageBlock block = new KitKnowledgePageBlock()
                    .setId(blockId)
                    .setPageVersionId(pageVersionId)
                    .setOrderNo(i + 1)
                    .setBlockType(segment.type())
                    .setContent(segment.content());
            blocks.add(block);
            if (segment.type() != KnowledgeBlockType.HEADING) {
                KitKnowledgePageSourceRef ref = new KitKnowledgePageSourceRef()
                        .setPageBlockId(blockId)
                        .setSourceType(KnowledgeSourceType.SOURCE_DOCUMENT)
                        .setSourceDocumentId(sourceDocumentId)
                        .setSourceLocator("line:" + segment.startLine() + "-" + segment.endLine());
                refs.add(ref);
            }
        }
        return new KnowledgeBlockBuildResult(List.copyOf(blocks), List.copyOf(refs));
    }

    private List<MarkdownSegment> splitMarkdown(String markdown) {
        String[] lines = markdown == null ? new String[0] : markdown.split("\\R");
        List<MarkdownSegment> segments = new ArrayList<>();
        int index = 0;
        while (index < lines.length) {
            if (!StringUtils.hasText(lines[index])) {
                index++;
                continue;
            }
            int start = index + 1;
            KnowledgeBlockType type = detectType(lines[index]);
            StringBuilder content = new StringBuilder(lines[index]);
            index++;
            if (type == KnowledgeBlockType.CODE) {
                while (index < lines.length) {
                    content.append('\n').append(lines[index]);
                    boolean closed = lines[index].trim().startsWith("```");
                    index++;
                    if (closed) {
                        break;
                    }
                }
                segments.add(new MarkdownSegment(type, content.toString(), start, index));
                continue;
            }
            while (index < lines.length && shouldJoin(type, lines[index])) {
                content.append('\n').append(lines[index]);
                index++;
            }
            segments.add(new MarkdownSegment(type, content.toString(), start, index));
        }
        return segments;
    }

    private KnowledgeBlockType detectType(String line) {
        String trimmed = line.trim();
        if (trimmed.startsWith("#")) {
            return KnowledgeBlockType.HEADING;
        }
        if (trimmed.startsWith("|")) {
            return KnowledgeBlockType.TABLE;
        }
        if (trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.matches("\\d+\\.\\s+.*")) {
            return KnowledgeBlockType.LIST;
        }
        if (trimmed.startsWith(">")) {
            return KnowledgeBlockType.QUOTE;
        }
        if (trimmed.startsWith("```")) {
            return KnowledgeBlockType.CODE;
        }
        return KnowledgeBlockType.PARAGRAPH;
    }

    private boolean shouldJoin(KnowledgeBlockType type, String line) {
        if (!StringUtils.hasText(line)) {
            return false;
        }
        return switch (type) {
            case TABLE -> line.trim().startsWith("|");
            case LIST -> {
                String trimmed = line.trim();
                yield trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.matches("\\d+\\.\\s+.*");
            }
            case PARAGRAPH -> detectType(line) == KnowledgeBlockType.PARAGRAPH;
            case QUOTE -> line.trim().startsWith(">");
            case CODE -> true;
            case HEADING -> false;
        };
    }

    private String nextBlockId() {
        return IdWorker.getIdStr();
    }

    private record MarkdownSegment(KnowledgeBlockType type, String content, int startLine, int endLine) {
    }
}
