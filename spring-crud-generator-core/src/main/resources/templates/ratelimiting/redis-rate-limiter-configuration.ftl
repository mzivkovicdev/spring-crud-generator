<#setting number_format="computer">
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;

@Configuration
public class RedisRateLimiterConfiguration {

    @Bean
    public ProxyManager<String> rateLimitingProxyManager(
            final LettuceConnectionFactory lettuceConnectionFactory,
            @Value("\${rate.limiting.refill-duration-seconds:${refillDuration}}") final long refillDurationSeconds) {

        final RedisClient redisClient = (RedisClient) lettuceConnectionFactory.getNativeClient();
        final StatefulRedisConnection<String, byte[]> redisConnection = redisClient.connect(
                RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE)
        );
        return LettuceBasedProxyManager.builderFor(redisConnection)
                .withExpirationAfterWrite(Duration.ofSeconds(refillDurationSeconds * 2))
                .build();
    }

}
