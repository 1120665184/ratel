package org.quyq.gwsu.kit.config.properties;

import lombok.Data;
import org.quyq.gwsu.common.core.constants.CoreConstants;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 知识库配置属性。
 */
@ConfigurationProperties(CoreConstants.Yaml.PROJECT_CONFIG_PREFIX + ".knowledge")
@Data
public class KnowledgeProperties {

    /**
     * ES 索引名。
     */
    private String indexName = "kit_knowledge_chunk";

    /**
     * 单个 Chunk 内容长度上限。
     */
    private int maxToken = 1000;

    /**
     * 默认检索数量。
     */
    private int searchSize = 10;
}
