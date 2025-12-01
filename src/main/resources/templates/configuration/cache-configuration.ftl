<#setting number_format="computer">
<#if type == "REDIS" && expiration??>
import java.time.Duration;

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

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
</#if><#t>

@Configuration
@EnableCaching
public class CacheConfiguration {

    <#if type == "REDIS">
    @Bean
    RedisCacheManager cacheManager(final RedisConnectionFactory factory) {

        final RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                <#if expiration??>
                .entryTtl(Duration.ofMinutes(${expiration}))
                </#if><#t>
                .disableCachingNullValues();

        return RedisCacheManager.builder(factory)
                .cacheDefaults(config)
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