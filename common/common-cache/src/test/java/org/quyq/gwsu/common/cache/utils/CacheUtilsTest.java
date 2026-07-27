package org.quyq.gwsu.common.cache.utils;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.quyq.gwsu.common.core.utils.ProjectUtils;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CacheUtilsTest {

    private static final Duration DOCKER_COMMAND_TIMEOUT = Duration.ofSeconds(15);
    private static final Duration REDIS_START_TIMEOUT = Duration.ofMinutes(2);
    private static final String REDIS_IMAGE = "redis:7.4.2-alpine";

    private String redisContainerId;
    private LettuceConnectionFactory connectionFactory;
    private CacheUtils cacheUtils;

    @BeforeAll
    void setUpRedis() throws Exception {
        try {
            verifyDockerAvailable();
            int port = findAvailablePort();
            redisContainerId = executeDocker(REDIS_START_TIMEOUT, "run", "--rm", "-d", "-p", "127.0.0.1:%d:6379".formatted(port), REDIS_IMAGE,
                    "redis-server", "--save", "", "--appendonly", "no").trim();
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
        } catch (Exception exception) {
            cleanUpRedisContainer(exception);
            throw exception;
        }
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
        cleanUpRedisContainer(null);
    }

    private void verifyDockerAvailable() {
        try {
            if (executeDocker("info", "--format", "{{.ServerVersion}}").isBlank()) {
                throw new IllegalStateException("Docker daemon returned no version");
            }
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Docker daemon is required for CacheUtilsTest; start Docker and rerun the test.", exception);
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

    private int findAvailablePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private void cleanUpRedisContainer(Exception originalException) throws Exception {
        if (redisContainerId == null) {
            return;
        }
        try {
            executeDocker("rm", "-f", redisContainerId);
        } catch (Exception cleanupException) {
            if (originalException != null) {
                originalException.addSuppressed(cleanupException);
                return;
            }
            throw cleanupException;
        }
    }

    private String executeDocker(String... arguments) throws IOException, InterruptedException {
        return executeDocker(DOCKER_COMMAND_TIMEOUT, arguments);
    }

    private String executeDocker(Duration timeout, String... arguments) throws IOException, InterruptedException {
        String[] command = new String[arguments.length + 1];
        command[0] = "docker";
        System.arraycopy(arguments, 0, command, 1, arguments.length);
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var outputFuture = executor.submit(
                    () -> new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
            boolean completed = process.waitFor(timeout.toSeconds(), TimeUnit.SECONDS);
            if (completed == false) {
                process.destroyForcibly();
            }
            String output = readProcessOutput(outputFuture);
            if (completed == false) {
                throw new IOException("Docker command timed out: " + String.join(" ", command) + "; output: " + output);
            }
            if (process.exitValue() != 0) {
                throw new IOException("Docker command failed: " + output);
            }
            return output;
        }
    }

    private String readProcessOutput(java.util.concurrent.Future<String> outputFuture) throws IOException, InterruptedException {
        try {
            return outputFuture.get();
        } catch (ExecutionException exception) {
            throw new IOException("Failed to read Docker command output", exception.getCause());
        }
    }
}
