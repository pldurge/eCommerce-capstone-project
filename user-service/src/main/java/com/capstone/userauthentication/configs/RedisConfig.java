package com.capstone.userauthentication.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class RedisConfig {

    /*
     * StringRedisTemplate is used for simple string key-value pairs:
     *   - Token blacklist:  "blacklist:{token}"  → "true"
     *   - Refresh tokens:   "refresh:{token}"    → userEmail
     *   - Password resets:  "pwd_reset:{token}"  → userEmail
     */

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory factory) {
        return new StringRedisTemplate(factory);
    }
}
