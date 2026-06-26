package org.quyq.gwsu.headless.config;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;
import io.agentscope.core.session.Session;
import lombok.Data;
import org.quyq.gwsu.common.ai.session.DatabaseSession;
import org.quyq.gwsu.common.cache.utils.CacheUtils;
import org.quyq.gwsu.common.core.constants.CoreConstants;
import org.quyq.gwsu.headless.core.HeadlessBrowserManager;
import org.quyq.gwsu.headless.core.pool.BrowserContextPool;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Data
@Configuration
@ConfigurationProperties(prefix = CoreConstants.Yaml.PROJECT_CONFIG_PREFIX + ".headless.browser")
public class HeadlessBrowserConfiguration {

    /**
     * 最大并发 BrowserContext 数量
     */
    private int maxContexts = 30;

    /**
     * 预创建 BrowserContext 池最小空闲数
     */
    private int minIdle = 2;

    /**
     * SSE 等待超时时间（毫秒），默认 5 分钟
     */
    private long sseTimeoutMs = 300_000L;

    /**
     * 是否无头模式
     */
    private boolean headless = true;

    /**
     * 无头登录页面 URL（含认证参数的登录地址）
     */
    private String loginUrl = "http://localhost:8000/sub-system/login_headless";

    /**
     * Redis 会话 TTL（小时），默认 24 小时
     */
    private int sessionTtlHours = 24;

    /**
     * 获取 BrowserContext 的超时时间（秒），默认 60 秒
     */
    private long borrowTimeoutSeconds = 60;

    /**
     * 是否启用 Session 缓存复用，默认 true
     */
    private boolean sessionCacheEnabled = true;

    /**
     * Session 缓存沉寂淘汰时间（分钟），借用时沉寂超过此时间的 Session 可被淘汰，默认 10
     */
    private int sessionEvictIdleMinutes = 10;

    /**
     * Session 缓存最大存活时间（分钟），定时任务强制清除沉寂超过此时间的 Session，默认 30
     */
    private int sessionMaxIdleMinutes = 30;

    /**
     * Session 缓存最大数量（按用户计），默认等于 maxContexts
     */
    private int maxCachedSessions = 30;

    /**
     * 定时清除扫描间隔（分钟），默认 5
     */
    private int cleanupIntervalMinutes = 5;

    /**
     * 智能体会话记录表名
     */
    public static final String SESSION_TABLE_NAME = "security_brain_sessions";


    /**
     * 智能体上下文持久化
     *
     * @param dataSource
     * @return
     */
    @Bean
    @ConditionalOnMissingBean
    public Session databaseAgentSession(DataSource dataSource) {
        return new DatabaseSession(dataSource, SESSION_TABLE_NAME);
    }

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
        return new BrowserContextPool(browser, this);
    }

    @Bean(destroyMethod = "close")
    public HeadlessBrowserManager headlessBrowserManager(
            BrowserContextPool contextPool,
            CacheUtils cacheUtils) {
        return new HeadlessBrowserManager(this, contextPool, cacheUtils);
    }
}
