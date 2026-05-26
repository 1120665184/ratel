package org.quyq.gwsu.common.authentication.config.properties;


import lombok.Data;
import org.quyq.gwsu.common.authentication.login.domain.ThreePlatformConfig;
import org.quyq.gwsu.common.core.constants.CoreConstants;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

/**
 * @author Quyq
 * @date 2026/4/8
 * @description 三方平台认证信息
 */
@Data
@ConfigurationProperties(value = CoreConstants.Yaml.PROJECT_CONFIG_PREFIX + ".auth.platform")
public class LoginProperties {

    /**
     * 三方平台配置信息
     * key: 登录方式
     */
    private Map<String, ThreePlatformConfig> threePlatform;

}
