package org.quyq.gwsu.kit.knowledge.engine.support;

import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.core.exception.BusinessException;
import org.quyq.gwsu.kit.config.properties.KnowledgeProperties;
import org.quyq.gwsu.kit.errcode.KitErrorCode;
import org.springframework.stereotype.Component;

/**
 * 知识库上下文预算解析服务。
 */
@Component
@RequiredArgsConstructor
public class KnowledgeContextBudgetService {

    private final KnowledgeProperties properties;

    public KnowledgeContextBudget resolveBudget() {
        int analysisChunkTokens = properties.getAnalysisChunkTokenCount();
        int overlapTokens = properties.getAnalysisChunkOverlapTokenCount();
        int generationContextTokens = properties.getGenerationContextTokenCount();
        if (analysisChunkTokens <= 0 || generationContextTokens <= 1 || overlapTokens < 0 || overlapTokens >= analysisChunkTokens) {
            throw new BusinessException(KitErrorCode.E03005, "知识库上下文预算配置无效");
        }
        int digestTokens = Math.max(1, generationContextTokens / 3);
        int sourceTokens = generationContextTokens - digestTokens;
        if (sourceTokens <= 0) {
            throw new BusinessException(KitErrorCode.E03005, "知识库生成上下文预算配置无效");
        }
        return new KnowledgeContextBudget(analysisChunkTokens, overlapTokens, sourceTokens, digestTokens);
    }
}
