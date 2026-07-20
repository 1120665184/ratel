package org.quyq.gwsu.kit.knowledge.engine;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.EncodingType;
import com.knuddels.jtokkit.api.IntArrayList;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgeIngestAnalysisCheckpointStatus;
import org.quyq.gwsu.kit.config.properties.KnowledgeProperties;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgeIngestAnalysisCheckpoint;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgeIngestTask;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgeIngestAnalysisCheckpointMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 长文档分析服务。
 */
@Component
@RequiredArgsConstructor
public class LongSourceAnalysisService {

    private static final EncodingRegistry TOKEN_ENCODING_REGISTRY = Encodings.newLazyEncodingRegistry();

    private static final Encoding TOKEN_ENCODING = TOKEN_ENCODING_REGISTRY.getEncoding(EncodingType.CL100K_BASE);

    private final KnowledgeProperties properties;

    private final KnowledgeContextBudgetService budgetService;

    private final KnowledgeAnalysisPromptBuilder promptBuilder;

    private final KnowledgeAnalysisModelClient analysisModelClient;

    private final KnowledgeIngestAnalysisCheckpointMapper checkpointMapper;

    public AnalyzedKnowledgeSource analyze(KitKnowledgeIngestTask task, ParsedKnowledgeDocument parsedDocument, SanitizedKnowledgeSource sanitizedSource) {
        KnowledgeContextBudget budget = budgetService.resolveBudget();
        List<String> chunks = split(sanitizedSource.text(), budget);
        String sourceLanguage = StringUtils.hasText(parsedDocument.sourceLanguage()) ? parsedDocument.sourceLanguage() : "und";
        if (chunks.isEmpty()) {
            return new AnalyzedKnowledgeSource(
                    sourceLanguage,
                    "",
                    trimToTokens(sanitizedSource.text(), budget.generationSourceTokens()));
        }
        List<KitKnowledgeIngestAnalysisCheckpoint> existingCheckpoints = checkpointMapper.selectByTaskId(task.getId());
        Map<Integer, KitKnowledgeIngestAnalysisCheckpoint> checkpointByChunkNo = new LinkedHashMap<>();
        existingCheckpoints.forEach(checkpoint -> checkpointByChunkNo.put(checkpoint.getChunkNo(), checkpoint));

        List<String> chunkDigests = new ArrayList<>(chunks.size());
        for (int i = 0; i < chunks.size(); i++) {
            int chunkNo = i + 1;
            String chunkText = chunks.get(i);
            String chunkHash = DigestUtils.md5DigestAsHex(chunkText.getBytes(StandardCharsets.UTF_8));
            KitKnowledgeIngestAnalysisCheckpoint checkpoint = checkpointByChunkNo.get(chunkNo);
            if (checkpoint != null
                    && checkpoint.getCheckpointStatus() == KnowledgeIngestAnalysisCheckpointStatus.SUCCEEDED
                    && chunkHash.equals(checkpoint.getChunkContentHash())
                    && StringUtils.hasText(checkpoint.getAnalysisDigest())) {
                chunkDigests.add(checkpoint.getAnalysisDigest());
                if (StringUtils.hasText(checkpoint.getSourceLanguage())) {
                    sourceLanguage = checkpoint.getSourceLanguage();
                }
                continue;
            }
            String digest = analysisModelClient.analyzeChunk(promptBuilder.buildChunkAnalysisPrompt(
                    parsedDocument.fileName(),
                    sourceLanguage,
                    properties.getWikiOutputLanguage(),
                    chunkNo,
                    chunkText));
            upsertCheckpoint(task, checkpoint, chunkNo, chunkHash, digest, sourceLanguage);
            chunkDigests.add(digest);
        }
        String analysisDigest = chunkDigests.size() == 1
                ? chunkDigests.getFirst()
                : analysisModelClient.summarizeDigests(promptBuilder.buildDigestSummaryPrompt(
                parsedDocument.fileName(),
                sourceLanguage,
                properties.getWikiOutputLanguage(),
                chunkDigests));
        return new AnalyzedKnowledgeSource(
                sourceLanguage,
                trimToTokens(analysisDigest, budget.digestTokens()),
                trimToTokens(sanitizedSource.text(), budget.generationSourceTokens()));
    }

    List<String> split(String text, KnowledgeContextBudget budget) {
        if (!StringUtils.hasText(text)) {
            return List.of();
        }
        List<String> units = splitIntoSemanticUnits(text);
        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String unit : units) {
            if (!StringUtils.hasText(unit)) {
                continue;
            }
            if (current.isEmpty()) {
                current.append(unit);
                continue;
            }
            String candidate = current + "\n\n" + unit;
            if (estimateTokens(candidate) <= budget.analysisChunkTokens()) {
                current.append("\n\n").append(unit);
                continue;
            }
            chunks.add(current.toString().trim());
            String overlap = tailByTokens(current.toString(), budget.overlapTokens());
            current = new StringBuilder();
            if (StringUtils.hasText(overlap)) {
                current.append(overlap).append("\n\n");
            }
            if (estimateTokens(unit) > budget.analysisChunkTokens()) {
                for (String part : splitOversizedUnit(unit, budget.analysisChunkTokens())) {
                    if (current.isEmpty()) {
                        current.append(part);
                    } else {
                        chunks.add(current.toString().trim());
                        current.setLength(0);
                        current.append(part);
                    }
                }
            } else {
                current.append(unit);
            }
        }
        if (!current.isEmpty()) {
            chunks.add(current.toString().trim());
        }
        return chunks;
    }

    private void upsertCheckpoint(KitKnowledgeIngestTask task,
                                  KitKnowledgeIngestAnalysisCheckpoint checkpoint,
                                  int chunkNo,
                                  String chunkHash,
                                  String digest,
                                  String sourceLanguage) {
        KitKnowledgeIngestAnalysisCheckpoint target = checkpoint == null
                ? new KitKnowledgeIngestAnalysisCheckpoint()
                : checkpoint;
        target.setIngestTaskId(task.getId());
        target.setChunkNo(chunkNo);
        target.setChunkContentHash(chunkHash);
        target.setAnalysisDigest(digest);
        target.setCheckpointStatus(KnowledgeIngestAnalysisCheckpointStatus.SUCCEEDED);
        target.setSourceLanguage(sourceLanguage);
        if (!StringUtils.hasText(target.getId())) {
            checkpointMapper.insert(target);
            return;
        }
        checkpointMapper.updateById(target);
    }

    private List<String> splitIntoSemanticUnits(String text) {
        List<String> units = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inCodeBlock = false;
        for (String line : text.split("\\R", -1)) {
            String trimmed = line.trim();
            if (trimmed.startsWith("```")) {
                inCodeBlock = !inCodeBlock;
            }
            boolean startsNewUnit = !inCodeBlock && (
                    trimmed.startsWith("#")
                            || trimmed.startsWith("- ")
                            || trimmed.startsWith("* ")
                            || trimmed.startsWith("|")
                            || trimmed.isEmpty());
            if (startsNewUnit && !current.isEmpty()) {
                units.add(current.toString().trim());
                current.setLength(0);
            }
            if (current.length() > 0) {
                current.append('\n');
            }
            current.append(line);
        }
        if (!current.isEmpty()) {
            units.add(current.toString().trim());
        }
        return units.stream().filter(StringUtils::hasText).toList();
    }

    private List<String> splitOversizedUnit(String text, int maxTokens) {
        List<String> parts = new ArrayList<>();
        String remaining = text;
        while (estimateTokens(remaining) > maxTokens) {
            int splitAt = Math.min(maxTokens, remaining.length());
            int boundary = findBoundary(remaining, splitAt);
            parts.add(remaining.substring(0, boundary).trim());
            remaining = remaining.substring(boundary).trim();
        }
        if (StringUtils.hasText(remaining)) {
            parts.add(remaining);
        }
        return parts;
    }

    private int findBoundary(String text, int maxLength) {
        int boundary = text.lastIndexOf('\n', maxLength);
        if (boundary > 0) {
            return boundary;
        }
        boundary = text.lastIndexOf(' ', maxLength);
        return boundary > 0 ? boundary : maxLength;
    }

    private String tailByTokens(String text, int tokens) {
        if (!StringUtils.hasText(text) || tokens <= 0) {
            return "";
        }
        IntArrayList encoded = TOKEN_ENCODING.encode(text);
        if (encoded.size() <= tokens) {
            return text.trim();
        }
        IntArrayList tail = new IntArrayList(tokens);
        for (int i = encoded.size() - tokens; i < encoded.size(); i++) {
            tail.add(encoded.get(i));
        }
        return TOKEN_ENCODING.decode(tail).trim();
    }

    private String trimToTokens(String text, int maxTokens) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        if (estimateTokens(text) <= maxTokens) {
            return text;
        }
        return TOKEN_ENCODING.decode(TOKEN_ENCODING.encode(text, maxTokens).getTokens()).trim();
    }

    private int estimateTokens(String text) {
        return text == null ? 0 : TOKEN_ENCODING.countTokens(text);
    }
}
