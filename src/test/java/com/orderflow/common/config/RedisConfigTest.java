package com.orderflow.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderflow.product.dto.CategoryResponse;
import com.orderflow.product.dto.ProductResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class RedisConfigTest {

    private RedisConfig redisConfig;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        redisConfig = new RedisConfig();
        ReflectionTestUtils.setField(redisConfig, "productTtlSeconds", 600L);
        objectMapper = new ObjectMapper();
    }

    @Test
    void defaultCacheConfiguration_hasCorrectTtlAndPrefix() {
        RedisCacheConfiguration cacheConfig = redisConfig.defaultCacheConfiguration(objectMapper);

        assertThat(cacheConfig.getTtl()).isEqualTo(Duration.ofSeconds(600));
        assertThat(cacheConfig.getKeyPrefixFor("products")).isEqualTo("orderflow::products::");
        assertThat(cacheConfig.getAllowCacheNullValues()).isFalse();
    }

    @Test
    void cacheManager_configuresSpecificCaches() {
        RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);
        RedisCacheConfiguration defaultCacheConfig = redisConfig.defaultCacheConfiguration(objectMapper);

        RedisCacheManager manager = redisConfig.cacheManager(connectionFactory, defaultCacheConfig);
        assertThat(manager).isNotNull();
    }

    @Test
    void jsonSerialization_roundTripsProductResponse() {
        RedisCacheConfiguration cacheConfig = redisConfig.defaultCacheConfiguration(objectMapper);

        UUID productId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        Instant now = Instant.now();

        ProductResponse original = ProductResponse.builder()
                .id(productId)
                .categoryId(categoryId)
                .categoryName("Electronics")
                .name("Noise Cancelling Headphones")
                .description("Premium sound with ANC")
                .sku("HEAD-001")
                .unitPrice(new BigDecimal("199.99"))
                .active(true)
                .createdAt(now)
                .updatedAt(now)
                .build();

        java.nio.ByteBuffer buffer = cacheConfig.getValueSerializationPair().write(original);
        assertThat(buffer).isNotNull();

        Object deserialized = cacheConfig.getValueSerializationPair().read(buffer);
        assertThat(deserialized).isInstanceOf(ProductResponse.class);

        ProductResponse result = (ProductResponse) deserialized;
        assertThat(result.getId()).isEqualTo(productId);
        assertThat(result.getSku()).isEqualTo("HEAD-001");
        assertThat(result.getUnitPrice()).isEqualTo(new BigDecimal("199.99"));
        assertThat(result.isActive()).isTrue();
    }

    @Test
    void jsonSerialization_roundTripsCategoryResponse() {
        RedisCacheConfiguration cacheConfig = redisConfig.defaultCacheConfiguration(objectMapper);

        UUID categoryId = UUID.randomUUID();
        Instant now = Instant.now();

        CategoryResponse original = CategoryResponse.builder()
                .id(categoryId)
                .name("Accessories")
                .description("Computer and mobile accessories")
                .createdAt(now)
                .build();

        java.nio.ByteBuffer buffer = cacheConfig.getValueSerializationPair().write(original);
        assertThat(buffer).isNotNull();

        Object deserialized = cacheConfig.getValueSerializationPair().read(buffer);
        assertThat(deserialized).isInstanceOf(CategoryResponse.class);

        CategoryResponse result = (CategoryResponse) deserialized;
        assertThat(result.getId()).isEqualTo(categoryId);
        assertThat(result.getName()).isEqualTo("Accessories");
    }
}
