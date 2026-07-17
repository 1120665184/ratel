package org.quyq.gwsu.kit.config.properties;

import lombok.Data;
import org.quyq.gwsu.common.core.constants.CoreConstants;
import org.quyq.gwsu.kit.knowledge.engine.PdfParseMode;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * PDF 解析配置。
 */
@ConfigurationProperties(CoreConstants.Yaml.PROJECT_CONFIG_PREFIX + ".knowledge.pdf")
@Data
public class KnowledgePdfParseProperties {

    /** 默认使用本地模式，增强模式需要显式提供本地增强策略。 */
    private PdfParseMode mode = PdfParseMode.LOCAL;
}
