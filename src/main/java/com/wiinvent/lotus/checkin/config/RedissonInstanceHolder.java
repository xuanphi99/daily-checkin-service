package com.wiinvent.lotus.checkin.config;

import org.redisson.api.RedissonClient;

public class RedissonInstanceHolder {
    private static RedissonClient redissonClient;

    public static void set(RedissonClient client) {
        redissonClient = client;
    }

    public static RedissonClient get() {
        if (redissonClient == null) {
            throw new IllegalStateException("RedissonClient has not been initialized");
        }
        return redissonClient;
    }
}