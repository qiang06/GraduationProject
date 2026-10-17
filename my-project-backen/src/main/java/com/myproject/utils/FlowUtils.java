package com.myproject.utils;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 限流通用工具
 * 针对于不同的情况进行限流操作，支持限流升级
 */
@Slf4j
@Component
public class FlowUtils {

    @Resource
    StringRedisTemplate template;

    private static final LimitAction defaultAction = overclock -> !overclock;

    /**
     * 针对于单次频率限制，请求成功后，在冷却时间内不得再次进行请求，如3秒内不能再次发起请求
     * @param key 键
     * @param blockTime 限制时间
     * @return 是否通过限流检查
     */
    public boolean limitOnceCheck(String key, int blockTime){
        return this.internalCheck(key, 1, blockTime, defaultAction);
    }
    
    /**
     * 针对于在时间段内多次请求限制，如3秒内限制请求20次，超出频率则封禁一段时间
     * @param counterKey 计数键
     * @param blockKey 封禁键
     * @param blockTime 封禁时间
     * @param frequency 请求频率
     * @param period 计数周期
     * @return 是否通过限流检查
     */
    public boolean limitPeriodCheck(String counterKey, String blockKey, int blockTime, int frequency, int period){
        return this.internalCheck(counterKey, frequency, period, (overclock) -> {
                    if (overclock)
                        template.opsForValue().set(blockKey, "", blockTime, TimeUnit.SECONDS);
                    return !overclock;
                });
    }

    /**
     * 针对于在时间段内多次请求限制，如3秒内20次请求
     * @param counterKey 计数键
     * @param frequency 请求频率
     * @param period 计数周期
     * @return 是否通过限流检查
     */
    public boolean limitPeriodCounterCheck(String counterKey, int frequency, int period){
        return this.internalCheck(counterKey, frequency, period, defaultAction);
    }

    /**
     * 内部使用请求限制主要逻辑
     * @param key 计数键
     * @param frequency 请求频率
     * @param period 计数周期
     * @param action 限制行为与策略
     * @return 是否通过限流检查
     */
    private boolean internalCheck(String key, int frequency, int period, LimitAction action){
        // 直接用 INCR 的返回值判定，避免 hasKey + set 的检查-设置竞态导致计数被重置
        Long value = template.opsForValue().increment(key);
        if (value == null) {
            log.warn("限流计数失败，Redis 不可用，本次请求放行: {}", key);
            return action.run(false);
        }
        if (value == 1L) {
            // 仅在计数器首次创建时设置过期时间
            template.expire(key, period, TimeUnit.SECONDS);
        }
        return action.run(value > frequency);
    }

    /**
     * 内部使用，限制行为与策略
     */
    private interface LimitAction {
        boolean run(boolean overclock);
    }
}
