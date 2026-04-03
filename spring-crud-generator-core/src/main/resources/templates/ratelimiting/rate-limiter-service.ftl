<#setting number_format="computer">
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
<#if type == "REDIS">
import io.github.bucket4j.distributed.proxy.ProxyManager;
</#if>

@Service
public class RateLimiterService {

<#if type == "IN_MEMORY">
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
<#else>
    private final ProxyManager<String> proxyManager;
</#if>

    private final long capacity;
    private final long refillTokens;
    private final Duration refillDuration;
<#if hasOverdraft>
    private final long overdraftCapacity;
    private final long overdraftRefillTokens;
    private final Duration overdraftRefillDuration;
</#if>

<#if type == "IN_MEMORY">
    public RateLimiterService(
            @Value("\${rate.limiting.capacity:${capacity}}") final long capacity,
            @Value("\${rate.limiting.refill-tokens:${refillTokens}}") final long refillTokens,
            @Value("\${rate.limiting.refill-duration-seconds:${refillDuration}}") final long refillDurationSeconds<#if hasOverdraft>,
            @Value("\${rate.limiting.overdraft.capacity:${overdraftCapacity}}") final long overdraftCapacity,
            @Value("\${rate.limiting.overdraft.refill-tokens:${overdraftRefillTokens}}") final long overdraftRefillTokens,
            @Value("\${rate.limiting.overdraft.refill-duration-seconds:${overdraftRefillDuration}}") final long overdraftRefillDurationSeconds</#if>) {
        this.capacity = capacity;
        this.refillTokens = refillTokens;
        this.refillDuration = Duration.ofSeconds(refillDurationSeconds);
<#if hasOverdraft>
        this.overdraftCapacity = overdraftCapacity;
        this.overdraftRefillTokens = overdraftRefillTokens;
        this.overdraftRefillDuration = Duration.ofSeconds(overdraftRefillDurationSeconds);
</#if>
    }
<#else>
    public RateLimiterService(
            final ProxyManager<String> proxyManager,
            @Value("\${rate.limiting.capacity:${capacity}}") final long capacity,
            @Value("\${rate.limiting.refill-tokens:${refillTokens}}") final long refillTokens,
            @Value("\${rate.limiting.refill-duration-seconds:${refillDuration}}") final long refillDurationSeconds<#if hasOverdraft>,
            @Value("\${rate.limiting.overdraft.capacity:${overdraftCapacity}}") final long overdraftCapacity,
            @Value("\${rate.limiting.overdraft.refill-tokens:${overdraftRefillTokens}}") final long overdraftRefillTokens,
            @Value("\${rate.limiting.overdraft.refill-duration-seconds:${overdraftRefillDuration}}") final long overdraftRefillDurationSeconds</#if>) {
        this.proxyManager = proxyManager;
        this.capacity = capacity;
        this.refillTokens = refillTokens;
        this.refillDuration = Duration.ofSeconds(refillDurationSeconds);
<#if hasOverdraft>
        this.overdraftCapacity = overdraftCapacity;
        this.overdraftRefillTokens = overdraftRefillTokens;
        this.overdraftRefillDuration = Duration.ofSeconds(overdraftRefillDurationSeconds);
</#if>
    }
</#if>

    public ConsumptionProbe tryConsume(final String key) {
<#if type == "IN_MEMORY">
        final Bucket bucket = buckets.computeIfAbsent(key, k -> createBucket());
        return bucket.tryConsumeAndReturnRemaining(1);
<#else>
        final BucketConfiguration configuration = createBucketConfiguration();
        final Bucket bucket = proxyManager.getProxy(key, () -> configuration);
        return bucket.tryConsumeAndReturnRemaining(1);
</#if>
    }

    public long getCapacity() {
        return this.capacity;
    }

<#if type == "IN_MEMORY">
    private Bucket createBucket() {
        return Bucket.builder()
                .addLimit(buildMainBandwidth())<#if hasOverdraft>
                .addLimit(buildOverdraftBandwidth())</#if>
                .build();
    }
<#else>
    private BucketConfiguration createBucketConfiguration() {
        return BucketConfiguration.builder()
                .addLimit(buildMainBandwidth())<#if hasOverdraft>
                .addLimit(buildOverdraftBandwidth())</#if>
                .build();
    }
</#if>

    private Bandwidth buildMainBandwidth() {
        return Bandwidth.builder()
                .capacity(this.capacity)
                .refillGreedy(this.refillTokens, this.refillDuration)
                .build();
    }
<#if hasOverdraft>

    private Bandwidth buildOverdraftBandwidth() {
        return Bandwidth.builder()
                .capacity(this.overdraftCapacity)
                .refillGreedy(this.overdraftRefillTokens, this.overdraftRefillDuration)
                .build();
    }
</#if>

}
