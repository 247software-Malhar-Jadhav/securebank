package com.securebank.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;

/**
 * Redis-backed caching configuration.
 *
 * <p>{@code @EnableCaching} turns on Spring's caching abstraction so that a
 * {@code @Cacheable} annotation on a service method transparently consults Redis
 * before invoking the method. This is the Decorator pattern applied by the
 * framework: the cache "decorates" the read so callers are unaware of it.</p>
 *
 * <p>We cache account-by-id reads (cache name {@code accounts}) with a short TTL.
 * Values are stored as JSON; entries are evicted whenever the account is updated
 * (see AccountService's {@code @CacheEvict} on writes), so a cached balance can
 * never go stale across a money movement.</p>
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /** Logical cache name for account-by-id lookups. */
    public static final String ACCOUNTS_CACHE = "accounts";

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // A Jackson serializer that records type info so polymorphic DTOs
        // round-trip correctly out of Redis.
        ObjectMapper mapper = new ObjectMapper()
                .findAndRegisterModules()
                .activateDefaultTyping(
                        BasicPolymorphicTypeValidator.builder()
                                .allowIfBaseType(Object.class).build(),
                        ObjectMapper.DefaultTyping.NON_FINAL);
        GenericJackson2JsonRedisSerializer serializer =
                new GenericJackson2JsonRedisSerializer(mapper);

        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                // Account reads change rarely between writes; a 60s TTL bounds
                // staleness even if an eviction is somehow missed.
                .entryTtl(Duration.ofSeconds(60))
                .disableCachingNullValues()
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(serializer));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .build();
    }
}
