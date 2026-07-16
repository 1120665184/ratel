package org.quyq.gwsu.kit.knowledge.engine;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.kit.config.properties.KnowledgeProperties;
import org.quyq.gwsu.kit.knowledge.domain.KnowledgePageBlock;
import org.quyq.gwsu.kit.knowledge.domain.KnowledgePageSourceRef;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 从已发布 Page Block 构建 ES-only Chunk。
 */
@Component
@RequiredArgsConstructor
public class KnowledgeChunkBuilder {

    private static final String PUBLISHED = "PUBLISHED";

    private final KnowledgeProperties properties;

    public List<KnowledgeChunkDocument> build(KnowledgeChunkBuildRequest request) {
        Map<String, KnowledgePageSourceRef> refByBlockId = new HashMap<>();
        for (KnowledgePageSourceRef ref : request.sourceRefs()) {
            refByBlockId.put(ref.getPageBlockId(), ref);
        }
        List<KnowledgeChunkDocument> chunks = new ArrayList<>();
        List<KnowledgePageBlock> orderedBlocks = request.blocks().stream()
                .sorted(Comparator.comparing(KnowledgePageBlock::getOrderNo))
                .toList();
        String currentHeading = "";
        for (KnowledgePageBlock block : orderedBlocks) {
            if (block.getBlockType() != null && "HEADING".equals(block.getBlockType().name())) {
                currentHeading = block.getContent();
            }
            KnowledgePageSourceRef ref = refByBlockId.get(block.getId());
            if (ref == null || !StringUtils.hasText(ref.getSourceDocumentId())) {
                continue;
            }
            for (String content : splitBlockContent(block.getContent())) {
                chunks.add(new KnowledgeChunkDocument(
                        IdWorker.getIdStr(),
                        request.pageId(),
                        request.pageVersion().getId(),
                        block.getId(),
                        ref.getSourceDocumentId(),
                        request.title(),
                        currentHeading,
                        content,
                        chunks.size() + 1,
                        DigestUtils.md5DigestAsHex(content.getBytes(StandardCharsets.UTF_8)),
                        PUBLISHED,
                        request.pageVersion().getVersionNo(),
                        Instant.now(),
                        null,
                        null));
            }
        }
        return List.copyOf(chunks);
    }

    private List<String> splitBlockContent(String content) {
        if (!StringUtils.hasText(content)) {
            return List.of();
        }
        int maxToken = Math.max(1, properties.getMaxToken());
        if (content.length() <= maxToken) {
            return List.of(content);
        }
        List<String> chunks = new ArrayList<>();
        String remaining = content;
        while (remaining.length() > maxToken) {
            int splitAt = findNaturalBoundary(remaining, maxToken);
            chunks.add(remaining.substring(0, splitAt).trim());
            remaining = remaining.substring(splitAt).trim();
        }
        if (StringUtils.hasText(remaining)) {
            chunks.add(remaining);
        }
        return chunks;
    }

    private int findNaturalBoundary(String text, int maxToken) {
        int boundary = -1;
        String window = text.substring(0, Math.min(text.length(), maxToken + 1));
        for (String delimiter : List.of("。", ".", "\n", "；", ";", "，", ",")) {
            int index = window.lastIndexOf(delimiter);
            if (index > boundary) {
                boundary = index + delimiter.length();
            }
        }
        if (boundary > 0) {
            return boundary;
        }
        return maxToken;
    }
}
