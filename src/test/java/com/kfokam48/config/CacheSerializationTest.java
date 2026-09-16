package com.kfokam48.config;

import com.kfokam48.dto.ConvertResponse;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/** Voir inventory-api : l'ObjectMapper du cache doit gérer les types java.time. */
class CacheSerializationTest {

    private final RedisSerializer<Object> serializer =
            new GenericJackson2JsonRedisSerializer(CacheConfig.redisObjectMapper());

    /** Les taux mis en cache sont des BigDecimal : l'échelle doit survivre au round-trip. */
    @Test
    void shouldRoundTripCachedRate() {
        BigDecimal rate = new BigDecimal("1.1235");

        Object restored = serializer.deserialize(serializer.serialize(rate));

        assertThat(restored).isInstanceOf(BigDecimal.class);
        assertThat((BigDecimal) restored).isEqualByComparingTo(rate);
    }

    @Test
    void shouldRoundTripValueWithJavaTimeFields() {
        ConvertResponse original = new ConvertResponse("EUR", "USD",
                new BigDecimal("100"), new BigDecimal("115.00"), new BigDecimal("1.15"),
                "API", LocalDateTime.of(2026, 9, 16, 10, 30));

        Object restored = serializer.deserialize(serializer.serialize(original));

        assertThat(restored).isEqualTo(original);
    }
}
