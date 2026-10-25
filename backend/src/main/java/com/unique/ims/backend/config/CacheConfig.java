package com.unique.ims.backend.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis-backed cache configuration.
 *
 * <p>Cached values are stored as JSON with the cache name and key rendered as
 * plain strings, so entries are readable with {@code redis-cli} and remain
 * portable across application restarts. Each region gets a TTL suited to how
 * volatile its data is, rather than one global timeout.</p>
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /** Fallback TTL for any cache region that is not explicitly listed below. */
    private static final Duration DEFAULT_TTL = Duration.ofMinutes(10);

    /** TTLs per region, tuned to how often the underlying table changes. */
    private static final Map<String, Duration> TTLS = Map.of(
            CacheNames.CATEGORIES, Duration.ofMinutes(30),
            CacheNames.BRANDS, Duration.ofMinutes(30),
            CacheNames.WAREHOUSES, Duration.ofMinutes(30),
            CacheNames.CUSTOMERS, Duration.ofMinutes(15),
            CacheNames.SUPPLIERS, Duration.ofMinutes(15),
            CacheNames.PRODUCTS, Duration.ofMinutes(5),
            CacheNames.LOW_STOCK_PRODUCTS, Duration.ofMinutes(2)
    );

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory, ObjectMapper objectMapper) {
        RedisCacheConfiguration base = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(DEFAULT_TTL)
                .disableCachingNullValues()
                .computePrefixWith(cacheName -> "ims::" + cacheName + "::")
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson3RedisSerializer(objectMapper)));

        Map<String, RedisCacheConfiguration> perCache = new HashMap<>();
        TTLS.forEach((name, ttl) -> perCache.put(name, base.entryTtl(ttl)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(base)
                .withInitialCacheConfigurations(perCache)
                .build();
    }
}
