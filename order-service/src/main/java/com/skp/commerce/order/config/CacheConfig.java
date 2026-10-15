package com.skp.commerce.order.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skp.commerce.order.api.OrderResponse;
import com.skp.commerce.order.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;

import java.time.Duration;

/**
 * Read-through Redis cache for order lookups. Entries are evicted whenever the order changes.
 * Cache failures are logged and treated as misses so a Redis outage never takes down reads.
 */
@Configuration
public class CacheConfig implements CachingConfigurer {

    private static final Logger log = LoggerFactory.getLogger(CacheConfig.class);

    @Bean
    RedisCacheManagerBuilderCustomizer orderCacheCustomizer(ObjectMapper objectMapper) {
        Jackson2JsonRedisSerializer<OrderResponse> serializer =
                new Jackson2JsonRedisSerializer<>(objectMapper, OrderResponse.class);
        return builder -> builder.withCacheConfiguration(OrderService.ORDER_CACHE,
                RedisCacheConfiguration.defaultCacheConfig()
                        .entryTtl(Duration.ofMinutes(10))
                        .disableCachingNullValues()
                        .serializeValuesWith(SerializationPair.fromSerializer(serializer)));
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException e, Cache cache, Object key) {
                log.warn("cache get failed for {}::{} - falling back to database: {}", cache.getName(), key, e.getMessage());
            }

            @Override
            public void handleCachePutError(RuntimeException e, Cache cache, Object key, Object value) {
                log.warn("cache put failed for {}::{}: {}", cache.getName(), key, e.getMessage());
            }

            @Override
            public void handleCacheEvictError(RuntimeException e, Cache cache, Object key) {
                log.warn("cache evict failed for {}::{}: {}", cache.getName(), key, e.getMessage());
            }

            @Override
            public void handleCacheClearError(RuntimeException e, Cache cache) {
                log.warn("cache clear failed for {}: {}", cache.getName(), e.getMessage());
            }
        };
    }
}
