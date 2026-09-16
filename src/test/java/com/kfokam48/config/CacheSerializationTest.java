package com.kfokam48.config;

import com.kfokam48.dto.ConvertResponse;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/** Voir inventory-api : l'ObjectMapper du cache doit gérer les types java.time. */
class CacheSerializationTest {

    private final RedisSerializer<Object> serializer =
            new GenericJackson2JsonRedisSerializer(CacheConfig.redisObjectMapper());

    @Test
    void shouldRoundTripCachedRate() {
        Object restored = serializer.deserialize(serializer.serialize(1.1235d));

        assertThat(restored).isEqualTo(1.1235d);
    }

    @Test
    void shouldRoundTripValueWithJavaTimeFields() {
        ConvertResponse original = new ConvertResponse("EUR", "USD", 100.0, 115.0, 1.15,
                "API", LocalDateTime.of(2026, 9, 16, 10, 30));

        Object restored = serializer.deserialize(serializer.serialize(original));

        assertThat(restored).isEqualTo(original);
    }
}
