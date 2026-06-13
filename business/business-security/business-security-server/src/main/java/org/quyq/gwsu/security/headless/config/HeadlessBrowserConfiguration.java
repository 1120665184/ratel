package org.quyq.gwsu.security.headless.config;

import lombok.Data;
import org.quyq.gwsu.security.headless.HeadlessBrowserManager;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "headless.browser")
public class HeadlessBrowserConfiguration {

    /** 最大并发 BrowserContext 数量 */
    private int maxContexts = 30;

    /** 闲置超时时间（毫秒），默认 10 分钟 */
    private long idleTimeoutMs = 600_000L;

    /** SSE 等待超时时间（毫秒），默认 5 分钟 */
    private long sseTimeoutMs = 300_000L;

    /** 是否无头模式 */
    private boolean headless = true;

    /** 应用基础 URL */
    private String baseUrl = "http://localhost:8000";

    @Bean(destroyMethod = "close")
    public HeadlessBrowserManager headlessBrowserManager() {
        return new HeadlessBrowserManager(this);
    }
}
