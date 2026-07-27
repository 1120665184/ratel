package org.quyq.gwsu.common.cache.utils;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.quyq.gwsu.common.core.utils.ProjectUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CacheUtilsTest {

    @Test
    void deleteIfEqualsDeletesOnlyWhenRedisScriptReportsMatch() {
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        ProjectUtils projectUtils = mock(ProjectUtils.class);
        when(projectUtils.getProjectIdent()).thenReturn("gwsu");
        when(projectUtils.getApplicationName()).thenReturn("cache-service");
        when(projectUtils.getServerPrefix()).thenReturn("gwsu:cache-service");
        when(redisTemplate.execute(any(RedisScript.class), anyList(), eq("token"))).thenReturn(1L);
        CacheUtils cacheUtils = new CacheUtils(redisTemplate, null, projectUtils);

        assertTrue(cacheUtils.deleteIfEquals("lease:1", "token"));

        ArgumentCaptor<RedisScript<Long>> scriptCaptor = ArgumentCaptor.forClass(RedisScript.class);
        verify(redisTemplate).execute(scriptCaptor.capture(), eq(List.of("{gwsu}:cache-service:lease:1")), eq("token"));
        assertEquals("if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) end return 0",
                scriptCaptor.getValue().getScriptAsString());
    }

    @Test
    void deleteIfEqualsReturnsFalseWhenValueDoesNotMatch() {
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        ProjectUtils projectUtils = mock(ProjectUtils.class);
        when(projectUtils.getProjectIdent()).thenReturn("gwsu");
        when(projectUtils.getApplicationName()).thenReturn("cache-service");
        when(projectUtils.getServerPrefix()).thenReturn("gwsu:cache-service");
        when(redisTemplate.execute(any(RedisScript.class), anyList(), eq("token"))).thenReturn(0L);
        CacheUtils cacheUtils = new CacheUtils(redisTemplate, null, projectUtils);

        assertFalse(cacheUtils.deleteIfEquals("lease:1", "token"));
    }
}
