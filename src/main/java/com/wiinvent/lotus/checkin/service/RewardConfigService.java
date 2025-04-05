package com.wiinvent.lotus.checkin.service;

import com.wiinvent.lotus.checkin.entity.RewardConfigEntity;
import com.wiinvent.lotus.checkin.repository.RewardConfigRepository;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.stream.Collectors;

@Service
public class RewardConfigService {
    private final RewardConfigRepository rewardConfigRepository;
    private final RedissonClient redissonClient;
    private static final String CACHE_KEY = "reward_config_cache";

    public RewardConfigService(RewardConfigRepository rewardConfigRepository, RedissonClient redissonClient) {
        this.rewardConfigRepository = rewardConfigRepository;
        this.redissonClient = redissonClient;
    }

    public HashMap<Integer, Integer> findAllConfig() {
        RMapCache<Integer, Integer> cache = redissonClient.getMapCache(CACHE_KEY);
        if (!cache.isEmpty()) {
            return new HashMap<>(cache);
        }

        HashMap<Integer, Integer> rewardConfigMap =  rewardConfigRepository.findAll().stream()
                .collect(Collectors.toMap(RewardConfigEntity::getDayNumber,
                        RewardConfigEntity::getPoints, (existing, replacement) -> existing, HashMap::new));

        cache.putAll(rewardConfigMap);

        return rewardConfigMap;
    }
}

