package com.dgq.seckill.common.constant;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Redis 缓存 key 常量类
 */
public class RedisKeyConstants {

    // 商品列表缓存Key前缀 seckill:goods:list:{activityId}
    public static final String GOODS_LIST_PREFIX = "seckill:goods:list:";

    // 商品列表缓存过期时间（单位：分钟）
    public static final long GOODS_LIST_TTL_MINUTES = 30;

    public static final String GOODS_DETAIL_PREFIX = "seckill:goods:detail:";

    public static final long GOODS_DETAIL_TTL_MINUTES = 30;

    /**
     * 活动结束后，缓存保存的短过期时间（单位：分钟）
     * 防止活动结束后仍有余温流量，每次都打到 DB
     */
    public static final long ENDED_ACTIVITY_TTL_MINUTES = 5;

    // 安全缓冲时间
    private static final long SAFETY_BUFFER_SECONDS = 30 * 60; // 30分钟

    /**
     * 根据活动结束时间动态计算缓存 TTL（秒）
     *
     * 公式：TTL = （活动结束时间 - 当前时间） + 安全缓存时间
     * @param endTime
     * @return
     */
    public static Long calculateTtlSeconds(LocalDateTime endTime) {
        if (Objects.isNull(endTime))
            return null;

        long ttlSeconds = Duration.between(LocalDateTime.now(), endTime).getSeconds() + SAFETY_BUFFER_SECONDS;
        return ttlSeconds > 0 ? ttlSeconds : null;
    }
}
