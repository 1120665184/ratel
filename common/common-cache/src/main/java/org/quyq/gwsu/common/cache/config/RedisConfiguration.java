package org.quyq.gwsu.common.cache.config;


import org.quyq.gwsu.common.cache.utils.CacheUtils;
import org.quyq.gwsu.common.cache.utils.IDGenerationUtils;
import org.quyq.gwsu.common.cache.utils.lock.DistributedLockAspect;
import org.quyq.gwsu.common.core.utils.ProjectUtils;
import org.redisson.api.RedissonClient;
import org.redisson.spring.starter.RedissonAutoConfigurationV4;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@AutoConfiguration
@EnableCaching
@AutoConfigureBefore({RedissonAutoConfigurationV4.class})
public class RedisConfiguration {


    @Bean
    public GenericJacksonJsonRedisSerializer genericJacksonJsonRedisSerializer() {
        return GenericJacksonJsonRedisSerializer
                .create(it ->
                        it.enableSpringCacheNullValueSupport()
                                .enableUnsafeDefaultTyping()
                );
    }


    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory,
                                                       GenericJacksonJsonRedisSerializer valueSerializer) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);


        RedisSerializer<String> keySerializer = RedisSerializer.string();

        template.setKeySerializer(keySerializer);
        template.setHashKeySerializer(keySerializer);

        template.setValueSerializer(valueSerializer);
        template.setHashValueSerializer(valueSerializer);

        template.afterPropertiesSet();

        return template;
    }


    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory,
                                     GenericJacksonJsonRedisSerializer valueSerializer,
                                     ProjectUtils projectUtils) {


        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .computePrefixWith(cacheName -> projectUtils.getServerPrefix() + ":cache:" + cacheName + ":")
                .entryTtl(Duration.ofDays(15))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(valueSerializer))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()));
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .build();
    }

    @Bean
    public IDGenerationUtils idGenerationUtils(CacheUtils cacheUtils) {
        return new IDGenerationUtils(cacheUtils);
    }

    @Bean
    public CacheUtils cacheUtils(RedisTemplate<String, Object> redisTemplate,
                                 RedissonClient redissonClient,
                                 ProjectUtils projectUtils) {
        return new CacheUtils(redisTemplate, redissonClient, projectUtils);
    }


    @Bean
    public DistributedLockAspect distributedLockAspect(CacheUtils cacheUtils) {
        return new DistributedLockAspect(cacheUtils);
    }

}
