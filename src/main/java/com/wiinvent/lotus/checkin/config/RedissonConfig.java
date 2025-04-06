package com.wiinvent.lotus.checkin.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {
    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer().setAddress("redis://localhost:6379")
                .setPassword(null).setDatabase(0);
        RedissonClient client = Redisson.create(config);

        RedissonInstanceHolder.set(client);
        return client;
    }
}
