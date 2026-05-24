package com.extractor.unraveldocs.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;

@Configuration
public class RedisConfig {
        @Bean
        public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
                // Build a type validator that permits our application classes and common JDK types
                BasicPolymorphicTypeValidator typeValidator = BasicPolymorphicTypeValidator.builder()
                                .allowIfBaseType(Object.class)
                                .build();

                ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.findAndRegisterModules();
                // Use the thread context ClassLoader so DevTools RestartClassLoader classes resolve correctly
                objectMapper.setTypeFactory(
                                TypeFactory.defaultInstance()
                                                .withClassLoader(Thread.currentThread().getContextClassLoader()));
                // Embed @class type info so Jackson can deserialize back to the correct concrete type
                objectMapper.activateDefaultTyping(typeValidator,
                                ObjectMapper.DefaultTyping.NON_FINAL,
                                JsonTypeInfo.As.PROPERTY);

                GenericJackson2JsonRedisSerializer serializer =
                                new GenericJackson2JsonRedisSerializer(objectMapper);

                RedisCacheConfiguration redisCacheConfiguration = RedisCacheConfiguration
                                .defaultCacheConfig()
                                .entryTtl(Duration.ofHours(1))
                                .disableCachingNullValues()
                                .serializeValuesWith(
                                                RedisSerializationContext.SerializationPair.fromSerializer(serializer));

                return RedisCacheManager.builder(connectionFactory)
                                .cacheDefaults(redisCacheConfiguration)
                                .build();
        }
}
