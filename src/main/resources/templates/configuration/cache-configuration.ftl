<#setting number_format="computer">
<#assign redisSerializer = isSpringBoot3?then("Jackson2JsonRedisSerializer", "JacksonJsonRedisSerializer")>
<#if type == "REDIS" && expiration??>
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

</#if><#t>
<#if type == "CAFFEINE" && expiration??>
import java.util.concurrent.TimeUnit;

</#if><#t>
<#if type == "REDIS">
import org.springframework.beans.factory.annotation.Qualifier;
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
<#if isSpringBoot3>
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
<#else>
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
</#if>
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import org.springframework.data.redis.serializer.RedisSerializer;

${modelImports}
</#if><#t>
<#if type == "REDIS">
<#if !isSpringBoot3>
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
<#else>
<#if excludeNull?? && excludeNull>
import com.fasterxml.jackson.annotaion.JsonInclude;
</#if><#t>
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
</#if>

</#if>
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
    RedisCacheManager cacheManager(final RedisConnectionFactory factory, @Qualifier("redisObjectMapper") final ObjectMapper redisObjectMapper) {

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

            final RedisSerializer<?> serializer = new ${redisSerializer}<>(redisObjectMapper, type);
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

    <#if type == "REDIS">
    <#if isSpringBoot3>
    @Bean
    ObjectMapper redisObjectMapper() {
        
        final ObjectMapper objectMapper = JsonMapper.builder()
                .addModule(new HibernateLazyNullModule())
                .addModule(new JavaTimeModule())
                <#if excludeNull?? && excludeNull>
                .serializationInclusion(JsonInclude.Include.NON_NULL)
                </#if><#t>
                .build();

        objectMapper.setVisibility(
            com.fasterxml.jackson.annotation.PropertyAccessor.FIELD,
            com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY
        );

        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        return objectMapper;
    }
    <#else>
    @Bean
    ObjectMapper redisObjectMapper() {

        return JsonMapper.builder()
                .changeDefaultVisibility(v -> v.withFieldVisibility(
                    com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY
                ))
                .addModule(new HibernateLazyNullModule())
                .build();
    }
    </#if>
    </#if>
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