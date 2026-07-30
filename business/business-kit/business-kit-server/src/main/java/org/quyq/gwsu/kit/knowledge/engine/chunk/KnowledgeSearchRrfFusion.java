package org.quyq.gwsu.kit.knowledge.engine.chunk;

import org.quyq.gwsu.kit.api.knowledge.vo.KnowledgeSearchResultVO;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 知识 Chunk 的 RRF 融合器。
 */
final class KnowledgeSearchRrfFusion {

    private static final int RANK_CONSTANT = 60;

    private KnowledgeSearchRrfFusion() {
    }

    static List<KnowledgeSearchResultVO> fuse(
            List<KnowledgeSearchResultVO> lexicalResults,
            List<KnowledgeSearchResultVO> vectorResults,
            int size) {
        Map<String, KnowledgeSearchResultVO> resultByChunkId = new LinkedHashMap<>();
        Map<String, Double> scoreByChunkId = new LinkedHashMap<>();
        addRrfScores(lexicalResults, resultByChunkId, scoreByChunkId);
        addRrfScores(vectorResults, resultByChunkId, scoreByChunkId);
        return scoreByChunkId.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey(Comparator.nullsLast(String::compareTo))))
                .limit(size)
                .map(entry -> resultByChunkId.get(entry.getKey()).setScore(entry.getValue()))
                .toList();
    }

    private static void addRrfScores(
            List<KnowledgeSearchResultVO> results,
            Map<String, KnowledgeSearchResultVO> resultByChunkId,
            Map<String, Double> scoreByChunkId) {
        for (int index = 0; index < results.size(); index++) {
            KnowledgeSearchResultVO result = results.get(index);
            String chunkId = result.getChunkId();
            resultByChunkId.putIfAbsent(chunkId, result);
            scoreByChunkId.merge(chunkId, 1.0 / (RANK_CONSTANT + index + 1), Double::sum);
        }
    }
}
