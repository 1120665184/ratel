package org.quyq.gwsu.common.cache.utils;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.quyq.gwsu.common.core.utils.ProjectUtils;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CacheUtilsTest {

    private static final Duration DOCKER_COMMAND_TIMEOUT = Duration.ofSeconds(15);

    private String redisContainerId;
    private LettuceConnectionFactory connectionFactory;
    private CacheUtils cacheUtils;

    @BeforeAll
    void setUpRedis() throws Exception {
        Assumptions.assumeTrue(isDockerAvailable(), "Docker daemon is required for Redis integration tests");
        redisContainerId = executeDocker("run", "--rm", "-d", "-p", "127.0.0.1::6379", "redis:latest",
                "redis-server", "--save", "", "--appendonly", "no").trim();
        String mappedPort = executeDocker("port", redisContainerId, "6379/tcp").trim();
        int port = Integer.parseInt(mappedPort.substring(mappedPort.lastIndexOf(':') + 1));
        waitForRedis();

        connectionFactory = new LettuceConnectionFactory("127.0.0.1", port);
        connectionFactory.afterPropertiesSet();
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(connectionFactory);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new StringRedisSerializer());
        redisTemplate.afterPropertiesSet();

        ProjectUtils projectUtils = mock(ProjectUtils.class);
        when(projectUtils.getProjectIdent()).thenReturn("gwsu");
        when(projectUtils.getApplicationName()).thenReturn("cache-service");
        when(projectUtils.getServerPrefix()).thenReturn("gwsu:cache-service");
        cacheUtils = new CacheUtils(redisTemplate, null, projectUtils);
    }

    @Test
    void deleteIfEqualsDeletesMatchingValueAndRetainsNonMatchingValue() {
        cacheUtils.set("lease:matching", "token");
        assertTrue(cacheUtils.deleteIfEquals("lease:matching", "token"));
        assertFalse(cacheUtils.exists("lease:matching"));

        cacheUtils.set("lease:non-matching", "other-token");
        assertFalse(cacheUtils.deleteIfEquals("lease:non-matching", "token"));
        assertEquals("other-token", cacheUtils.get("lease:non-matching"));
        assertTrue(cacheUtils.exists("lease:non-matching"));
    }

    @AfterAll
    void tearDownRedis() throws Exception {
        if (connectionFactory != null) {
            connectionFactory.destroy();
        }
        if (redisContainerId != null) {
            executeDocker("rm", "-f", redisContainerId);
        }
    }

    private boolean isDockerAvailable() {
        try {
            return executeDocker("info", "--format", "{{.ServerVersion}}").isBlank() == false;
        } catch (Exception ignored) {
            return false;
        }
    }

    private void waitForRedis() throws Exception {
        for (int attempt = 0; attempt < 30; attempt++) {
            try {
                if ("PONG".equals(executeDocker("exec", redisContainerId, "redis-cli", "ping").trim())) {
                    return;
                }
            } catch (IOException ignored) {
                // Redis is still starting.
            }
            Thread.sleep(100);
        }
        throw new IllegalStateException("Redis container did not become ready");
    }

    private String executeDocker(String... arguments) throws IOException, InterruptedException {
        String[] command = new String[arguments.length + 1];
        command[0] = "docker";
        System.arraycopy(arguments, 0, command, 1, arguments.length);
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        if (process.waitFor(DOCKER_COMMAND_TIMEOUT.toSeconds(), TimeUnit.SECONDS) == false) {
            process.destroyForcibly();
            throw new IOException("Docker command timed out: " + String.join(" ", command));
        }
        String output = new String(process.getInputStream().readAllBytes());
        if (process.exitValue() != 0) {
            throw new IOException("Docker command failed: " + output);
        }
        return output;
    }
}
