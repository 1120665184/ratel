package org.quyq.gwsu.kit.config;

import org.quyq.gwsu.kit.config.properties.KnowledgeProperties;
import org.quyq.gwsu.kit.config.properties.KnowledgePdfParseProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 知识库配置。
 */
@Configuration
@EnableConfigurationProperties({KnowledgeProperties.class, KnowledgePdfParseProperties.class})
public class KnowledgeConfiguration {
}
