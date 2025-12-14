package ru.mentee.power.cache;

import lombok.Builder;
import lombok.Data;

/**
 * Статистика кэширования.
 */
@Data
@Builder
public class CacheStatistics {
    private long l2CacheHitCount;
    private long l2CacheMissCount;
    private long l2CachePutCount;
    private long queryCacheHitCount;
    private long queryCacheMissCount;
    private long queryCachePutCount;
    private long sessionOpenCount;
    private long sessionCloseCount;
    private long transactionCount;
    private long entityLoadCount;
    private long entityFetchCount;
    private double l2CacheHitRatio;
    private double queryCacheHitRatio;
}
