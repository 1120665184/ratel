package org.quyq.gwsu.system.config.properties;

import lombok.Data;
import org.quyq.gwsu.common.core.constants.CoreConstants;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * API_KEY 配置
 *
 * @author Quyq
 */
@Data
@ConfigurationProperties(prefix = CoreConstants.Yaml.PROJECT_CONFIG_PREFIX + ".api-key")
public class ApiKeyProperties {

    /**
     * 服务端摘要 pepper
     */
    private String pepper;

    /**
     * 摘要版本
     */
    private Integer version = 1;
}
