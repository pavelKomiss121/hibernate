package ru.mentee.power.cache;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Cache;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;

/**
 * Реализация менеджера кэширования.
 */
@Slf4j
@RequiredArgsConstructor
public class CacheManagerImpl implements CacheManager {

    private final SessionFactory sessionFactory;
    private final List<CacheWarmingStrategy> warmingStrategies;

    public CacheManagerImpl(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
        this.warmingStrategies = new ArrayList<>();
    }

    @Override
    public void warmUpCache() {
        log.info("Starting cache warm-up...");
        warmingStrategies.stream()
                .filter(CacheWarmingStrategy::shouldWarmUp)
                .sorted(Comparator.comparingInt(CacheWarmingStrategy::getPriority))
                .forEach(
                        strategy -> {
                            try {
                                strategy.warmUp(sessionFactory);
                            } catch (Exception e) {
                                log.error(
                                        "Error during cache warm-up with strategy: {}",
                                        strategy.getClass().getSimpleName(),
                                        e);
                            }
                        });
        log.info("Cache warm-up completed");
    }

    @Override
    public void invalidateEntity(Class<?> entityClass, Object id) {
        Cache cache = sessionFactory.getCache();
        // Используем имя класса как регион
        String regionName = entityClass.getName();
        cache.evictRegion(regionName);
        log.debug("Invalidated entity: {}#{}", entityClass.getSimpleName(), id);
    }

    @Override
    public void invalidateEntityType(Class<?> entityClass) {
        Cache cache = sessionFactory.getCache();
        // Очищаем весь регион для типа сущности
        String regionName = entityClass.getName();
        cache.evictRegion(regionName);
        log.debug("Invalidated entity type: {}", entityClass.getSimpleName());
    }

    @Override
    public CacheStatistics getStatistics() {
        Statistics stats = sessionFactory.getStatistics();

        long l2Hit = stats.getSecondLevelCacheHitCount();
        long l2Miss = stats.getSecondLevelCacheMissCount();
        long queryHit = stats.getQueryCacheHitCount();
        long queryMiss = stats.getQueryCacheMissCount();

        double l2HitRatio = (l2Hit + l2Miss > 0) ? (double) l2Hit / (l2Hit + l2Miss) : 0.0;
        double queryHitRatio =
                (queryHit + queryMiss > 0) ? (double) queryHit / (queryHit + queryMiss) : 0.0;

        return CacheStatistics.builder()
                .l2CacheHitCount(l2Hit)
                .l2CacheMissCount(l2Miss)
                .l2CachePutCount(stats.getSecondLevelCachePutCount())
                .queryCacheHitCount(queryHit)
                .queryCacheMissCount(queryMiss)
                .queryCachePutCount(stats.getQueryCachePutCount())
                .sessionOpenCount(stats.getSessionOpenCount())
                .sessionCloseCount(stats.getSessionCloseCount())
                .transactionCount(stats.getTransactionCount())
                .entityLoadCount(stats.getEntityLoadCount())
                .entityFetchCount(stats.getEntityFetchCount())
                .l2CacheHitRatio(l2HitRatio)
                .queryCacheHitRatio(queryHitRatio)
                .build();
    }

    @Override
    public OptimizationReport optimizeCache() {
        OptimizationReport report = new OptimizationReport();
        Statistics stats = sessionFactory.getStatistics();

        if (!stats.isStatisticsEnabled()) {
            report.addRecommendation("Enable statistics for cache optimization");
            return report;
        }

        // Анализ hit ratio по регионам
        for (String region : stats.getSecondLevelCacheRegionNames()) {
            var regionStats = stats.getCacheRegionStatistics(region);
            long hits = regionStats.getHitCount();
            long misses = regionStats.getMissCount();

            if (hits + misses > 0) {
                double hitRatio = (double) hits / (hits + misses);
                if (hitRatio < 0.5) {
                    report.addRecommendation(
                            String.format(
                                    "Low hit ratio (%.2f%%) for region '%s'. Consider: 1)"
                                            + " Increasing cache size, 2) Adjusting TTL, 3) Review"
                                            + " access patterns",
                                    hitRatio * 100, region));
                }
            }

            if (regionStats.getElementCountInMemory() > 10000) {
                report.addRecommendation(
                        String.format(
                                "Large cache size (%d) for region '%s'. Consider: 1) Enable"
                                    + " off-heap storage, 2) Implement cache warming, 3) Use lazy"
                                    + " loading",
                                regionStats.getElementCountInMemory(), region));
            }
        }

        // Анализ query cache
        long queryHits = stats.getQueryCacheHitCount();
        long queryMisses = stats.getQueryCacheMissCount();
        if (queryHits + queryMisses > 0) {
            double queryHitRatio = (double) queryHits / (queryHits + queryMisses);
            if (queryHitRatio < 0.3) {
                report.addRecommendation(
                        String.format(
                                "Low query cache hit ratio (%.2f%%). Consider: 1) Review query"
                                    + " parameters variability, 2) Increase query cache size, 3)"
                                    + " Disable query cache for dynamic queries",
                                queryHitRatio * 100));
            }
        }

        return report;
    }

    @Override
    public String exportMetrics() {
        CacheStatistics stats = getStatistics();
        StringBuilder sb = new StringBuilder();

        sb.append("# Hibernate Cache Metrics\n");
        sb.append(String.format("hibernate_cache_l2_hits_total %d\n", stats.getL2CacheHitCount()));
        sb.append(
                String.format("hibernate_cache_l2_misses_total %d\n", stats.getL2CacheMissCount()));
        sb.append(String.format("hibernate_cache_l2_puts_total %d\n", stats.getL2CachePutCount()));
        sb.append(String.format("hibernate_cache_l2_hit_ratio %.4f\n", stats.getL2CacheHitRatio()));
        sb.append(
                String.format(
                        "hibernate_cache_query_hits_total %d\n", stats.getQueryCacheHitCount()));
        sb.append(
                String.format(
                        "hibernate_cache_query_misses_total %d\n", stats.getQueryCacheMissCount()));
        sb.append(
                String.format(
                        "hibernate_cache_query_puts_total %d\n", stats.getQueryCachePutCount()));
        sb.append(
                String.format(
                        "hibernate_cache_query_hit_ratio %.4f\n", stats.getQueryCacheHitRatio()));
        sb.append(
                String.format("hibernate_sessions_opened_total %d\n", stats.getSessionOpenCount()));
        sb.append(
                String.format(
                        "hibernate_sessions_closed_total %d\n", stats.getSessionCloseCount()));
        sb.append(String.format("hibernate_transactions_total %d\n", stats.getTransactionCount()));
        sb.append(
                String.format("hibernate_entities_loaded_total %d\n", stats.getEntityLoadCount()));
        sb.append(
                String.format(
                        "hibernate_entities_fetched_total %d\n", stats.getEntityFetchCount()));

        return sb.toString();
    }
}
