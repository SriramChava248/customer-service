package com.fooddelivery.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fooddelivery.persistence.model.Customer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis configuration for caching customer objects.
 * 
 * Provides:
 * - RedisTemplate with JSON serialization for Customer objects
 * - String keys, JSON values
 * - Proper serialization/deserialization of Customer entities
 */
@Configuration
public class RedisConfig {

    @Value("${cache.customer.ttl:3600}")
    private long customerCacheTtl;

    /**
     * Configure RedisTemplate for Customer caching.
     * 
     * Used for caching customer objects with TWO keys:
     * 1. "customer:{id}" -> Customer object (for ID-based lookups)
     * 2. "customer:email:{email}" -> Customer object (for email-based lookups)
     * 
     * Key serialization: String (customer IDs or email keys)
     * Value serialization: JSON (Customer objects)
     * 
     * @param connectionFactory Redis connection factory (auto-configured by Spring Boot)
     * @return Configured RedisTemplate for Customer operations
     */
    @Bean
    public RedisTemplate<String, Customer> customerRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Customer> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Configure ObjectMapper for JSON serialization
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        // Add type information to JSON for proper deserialization
        // Using PolymorphicTypeValidator for type safety (replaces deprecated activateDefaultTyping)
        objectMapper.activateDefaultTyping(
                objectMapper.getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );

        // Key serializer: String (customer IDs like "1", "2", or email keys like "customer:email:user@example.com")
        template.setKeySerializer(new StringRedisSerializer());
        
        // Value serializer: JSON (Customer objects)
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer(objectMapper));
        
        // Hash key/value serializers (for hash operations, if needed)
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer(objectMapper));

        template.afterPropertiesSet();
        return template;
    }

    /**
     * Get customer cache TTL (Time To Live) in seconds.
     * 
     * @return Cache TTL in seconds
     */
    public long getCustomerCacheTtl() {
        return customerCacheTtl;
    }
}

