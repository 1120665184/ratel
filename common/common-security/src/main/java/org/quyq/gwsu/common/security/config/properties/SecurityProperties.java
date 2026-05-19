package org.quyq.gwsu.common.security.config.properties;

import lombok.Data;
import org.quyq.gwsu.common.core.constants.CoreConstants;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.AntPathMatcher;

import java.util.ArrayList;
import java.util.List;

/**
 * 安全配置属性
 * <p>
 * 配置示例：
 * <pre>
 * security:
 *   ignore:
 *     urls:
 *       - /actuator/**
 *       - /swagger-ui/**
 *       - /v3/api-docs/**
 *       - /auth/login
 *       - /auth/register
 * </pre>
 *
 * @author Quyq
 * @date 2026/4/12
 */
@ConfigurationProperties(prefix = CoreConstants.Yaml.PROJECT_CONFIG_PREFIX + ".security")
@Data
public class SecurityProperties {

    /**
     * 无需认证的 URL 列表
     * 支持 Ant 风格路径匹配（如 /api/**, /user/*）
     */
    private List<String> ignoreUrls = new ArrayList<>();

    /**
     * 认证后便允许访问的url
     */
    private List<String> authAllowUrls = new ArrayList<>();

    private final AntPathMatcher pathMatcher = new AntPathMatcher();


    /**
     * 判断指定路径是否需要忽略认证
     *
     * @param path 请求路径
     * @return true 表示需要忽略认证（无需认证），false 表示需要认证
     */
    public boolean shouldIgnore(String path) {
        if (ignoreUrls == null || ignoreUrls.isEmpty()) {
            return false;
        }
        return ignoreUrls.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    /**
     * 判断指定路径是否认证成功便能访问
     *
     * @param path 请求路径
     * @return true 表示需要忽略认证（无需认证），false 表示需要认证
     */
    public boolean shouldAuthAllow(String path) {
        if (authAllowUrls == null || authAllowUrls.isEmpty()) {
            return false;
        }
        return authAllowUrls.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }
}
