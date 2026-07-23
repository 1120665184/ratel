package org.quyq.gwsu.kit.config.properties;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;
import org.quyq.gwsu.common.core.constants.CoreConstants;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 知识库配置属性。
 */
@ConfigurationProperties(CoreConstants.Yaml.PROJECT_CONFIG_PREFIX + ".knowledge")
@Validated
@Data
public class KnowledgeProperties {

    /**
     * ES 索引名。
     */
    private String indexName = "kit_knowledge_chunk";

    /**
     * 单个 Chunk 内容长度上限。
     */
    @Positive
    private int maxToken = 5000;

    /**
     * 默认检索数量。
     */
    @Positive
    private int searchSize = 10;

    /**
     * 单次批量向量化最大 Token 数。
     */
    @Positive
    private int embeddingBatchTokenCount = 8191;

    /**
     * Wiki 生成内容的统一输出语言。
     */
    @NotBlank
    private String wikiOutputLanguage = "zh-CN";

    /**
     * 单个源文档分析片段的最大 Token 数。
     */
    @Positive
    private int analysisChunkTokenCount = 6000;

    /**
     * 相邻源文档分析片段的重叠 Token 数。
     */
    @PositiveOrZero
    private int analysisChunkOverlapTokenCount = 500;

    /**
     * Wiki 生成时可使用的分析上下文最大 Token 数。
     */
    @Positive
    private int generationContextTokenCount = 12000;

    /**
     * Wiki 生成时可使用的源文上下文最大 Token 数。
     */
    @Positive
    private int generationSourceTokenCount = 2000;

    /**
     * 混合召回阶段保留的候选 Chunk 数。
     */
    @Positive
    private int hybridRecallSize = 30;

    /**
     * 回答组装时，每个命中 Chunk 可补充的相邻 Chunk 数。
     */
    @Positive
    private int answerContextAdjacentChunkCount = 1;

    /**
     * Page 归属判断时提交给模型的候选 Page 数量。
     */
    @Positive
    private int pageMatchCandidateSize = 8;

    /**
     * Page 归属候选召回时读取的最大 Page 数量。
     */
    @Positive
    private int pageMatchRecallSize = 80;

    /**
     * 校验分析片段重叠长度不得达到或超过片段长度。
     *
     * @return 是否满足重叠长度约束
     */
    @AssertTrue(message = "analysisChunkOverlapTokenCount 必须小于 analysisChunkTokenCount")
    public boolean isAnalysisChunkOverlapTokenCountValid() {
        return analysisChunkOverlapTokenCount < analysisChunkTokenCount;
    }

    @AssertTrue(message = "generationSourceTokenCount 必须小于 generationContextTokenCount")
    public boolean isGenerationSourceTokenCountValid() {
        return generationSourceTokenCount < generationContextTokenCount;
    }
}
