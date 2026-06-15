package org.quyq.gwsu.security.headless.config;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;
import lombok.Data;
import org.quyq.gwsu.common.cache.utils.CacheUtils;
import org.quyq.gwsu.security.headless.BrowserContextPool;
import org.quyq.gwsu.security.headless.HeadlessBrowserManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "headless.browser")
@ConditionalOnProperty(prefix = "headless.browser", name = "enabled", havingValue = "true", matchIfMissing = true)
public class HeadlessBrowserConfiguration {

    /** 是否启用无头浏览器功能 */
    private boolean enabled = false;

    /** 最大并发 BrowserContext 数量 */
    private int maxContexts = 30;

    /** 预创建 BrowserContext 池最小空闲数 */
    private int minIdle = 2;

    /** SSE 等待超时时间（毫秒），默认 5 分钟 */
    private long sseTimeoutMs = 300_000L;

    /** 是否无头模式 */
    private boolean headless = true;

    /** 无头登录页面 URL（含认证参数的登录地址） */
    private String loginUrl = "http://localhost:8000/sub-system/login_headless";

    /** Redis 会话 TTL（小时），默认 24 小时 */
    private int sessionTtlHours = 24;

    /** 获取 BrowserContext 的超时时间（秒），默认 60 秒 */
    private long borrowTimeoutSeconds = 60;

    @Bean(destroyMethod = "close")
    public Playwright playwright() {
        return Playwright.create();
    }

    @Bean(destroyMethod = "close")
    public Browser browser(Playwright playwright) {
        return playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setHeadless(headless));
    }

    @Bean(destroyMethod = "close")
    public BrowserContextPool browserContextPool(Browser browser) {
        return new BrowserContextPool(browser, minIdle, maxContexts);
    }

    @Bean(destroyMethod = "close")
    public HeadlessBrowserManager headlessBrowserManager(
            BrowserContextPool contextPool,
            CacheUtils cacheUtils) {
        return new HeadlessBrowserManager(this, contextPool, cacheUtils);
    }
}
