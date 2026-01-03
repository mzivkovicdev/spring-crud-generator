<#setting number_format="computer">
<#if type == "REDIS" && expiration??>
import java.time.Duration;
import java.util.Map;
import java.util.HashMap;

</#if><#t>
<#if type == "CAFFEINE" && expiration??>
import java.util.concurrent.TimeUnit;

</#if><#t>
<#if type == "CAFFEINE">
import org.springframework.cache.CacheManager;
</#if><#t>
import org.springframework.cache.annotation.EnableCaching;
<#if type != "SIMPLE">
import org.springframework.context.annotation.Bean;
</#if><#t>
import org.springframework.context.annotation.Configuration;
<#if type == "CAFFEINE">
import org.springframework.cache.caffeine.CaffeineCacheManager;

import com.github.benmanes.caffeine.cache.Caffeine;
</#if><#t>
<#if type == "REDIS">
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;

${modelImports}
</#if><#t>

@Configuration
@EnableCaching
public class CacheConfiguration {

    <#if type == "REDIS">
    private static final Map<String, Class<?>> TYPED_CACHES = Map.of(
        <#list entities as entity>
        "${entity?uncap_first}", ${entity}.class<#if entity_has_next>,</#if>
        </#list>
    );

    @Bean
    @SuppressWarnings("unchecked")
    RedisCacheManager cacheManager(final RedisConnectionFactory factory) {

        final RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                <#if expiration??>
                .entryTtl(Duration.ofMinutes(${expiration}))
                </#if><#t>
                .disableCachingNullValues()
                .serializeKeysWith(SerializationPair.fromSerializer(RedisSerializer.string()));

        final Map<String, RedisCacheConfiguration> perCache = new HashMap<>();

        TYPED_CACHES.entrySet().forEach(e -> {
            final String cacheName = e.getKey();
            final Class<?> type = e.getValue();

            final RedisSerializer<?> serializer = new JacksonJsonRedisSerializer<>(type);
            final RedisCacheConfiguration cfg = config.serializeValuesWith(
                SerializationPair.fromSerializer((RedisSerializer<Object>) serializer)
            );
            perCache.put(cacheName, cfg);
        });

        return RedisCacheManager.builder(factory)
                .cacheDefaults(config)
                .withInitialCacheConfigurations(perCache)
                .build();
    }
    </#if><#t>
    <#if type == "CAFFEINE">
    @Bean
    CacheManager cacheManager() {

        final CaffeineCacheManager manager = new CaffeineCacheManager();

        manager.setCaffeine(
            Caffeine.newBuilder()
                    <#if expiration??>
                    .expireAfterWrite(${expiration}, TimeUnit.MINUTES)
                    </#if><#t>
                    <#if maxSize??>
                    .maximumSize(${maxSize})
                    </#if><#t>
        );

        return manager;
    }
    </#if><#t>
}