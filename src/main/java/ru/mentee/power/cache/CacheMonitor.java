package ru.mentee.power.cache;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;

/**
 * Мониторинг и оптимизация кэширования.
 */
@Slf4j
public class CacheMonitor {

    private final SessionFactory sessionFactory;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public CacheMonitor(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
        startMonitoring();
    }

    /**
     * Запуск периодического мониторинга.
     */
    private void startMonitoring() {
        scheduler.scheduleAtFixedRate(this::collectAndLogStatistics, 0, 60, TimeUnit.SECONDS);
    }

    /**
     * Сбор и логирование статистики.
     */
    private void collectAndLogStatistics() {
        Statistics stats = sessionFactory.getStatistics();

        if (!stats.isStatisticsEnabled()) {
            return;
        }

        log.info("=== Cache Statistics ===");
        log.info("L2 Cache Hit Count: {}", stats.getSecondLevelCacheHitCount());
        log.info("L2 Cache Miss Count: {}", stats.getSecondLevelCacheMissCount());
        log.info("L2 Cache Put Count: {}", stats.getSecondLevelCachePutCount());

        long l2Hits = stats.getSecondLevelCacheHitCount();
        long l2Misses = stats.getSecondLevelCacheMissCount();
        if (l2Hits + l2Misses > 0) {
            double hitRatio = (double) l2Hits / (l2Hits + l2Misses);
            log.info("L2 Cache Hit Ratio: {}%", String.format("%.2f", hitRatio * 100));
        }

        // Детальная статистика по регионам
        for (String region : stats.getSecondLevelCacheRegionNames()) {
            var regionStats = stats.getCacheRegionStatistics(region);
            log.info(
                    "Region [{}]: hits={}, misses={}, puts={}, size={}",
                    region,
                    regionStats.getHitCount(),
                    regionStats.getMissCount(),
                    regionStats.getPutCount(),
                    regionStats.getElementCountInMemory());
        }

        checkPerformanceIssues(stats);
    }

    /**
     * Проверка проблем производительности.
     */
    private void checkPerformanceIssues(Statistics stats) {
        long l2Hits = stats.getSecondLevelCacheHitCount();
        long l2Misses = stats.getSecondLevelCacheMissCount();

        if (l2Hits + l2Misses > 0) {
            double l2HitRatio = (double) l2Hits / (l2Hits + l2Misses);
            if (l2HitRatio < 0.7) {
                log.warn(
                        "Low L2 cache hit ratio: {}%. Consider cache tuning.",
                        String.format("%.2f", l2HitRatio * 100));
            }
        }
    }

    /**
     * Рекомендации по оптимизации.
     */
    public OptimizationReport analyzeAndRecommend() {
        Statistics stats = sessionFactory.getStatistics();
        OptimizationReport report = new OptimizationReport();

        // Анализ hit ratio по регионам
        for (String region : stats.getSecondLevelCacheRegionNames()) {
            var regionStats = stats.getCacheRegionStatistics(region);
            long hits = regionStats.getHitCount();
            long misses = regionStats.getMissCount();

            if (hits + misses > 0) {
                double hitRatio = (double) hits / (hits + misses);
                if (hitRatio < 0.5) {
                    report.addRecommendation(
                            "Low hit ratio for region '"
                                    + region
                                    + "'. Consider: 1) Increasing cache size, "
                                    + "2) Adjusting TTL, 3) Review access patterns");
                }
            }

            if (regionStats.getElementCountInMemory() > 10000) {
                report.addRecommendation(
                        "Large cache size for region '"
                                + region
                                + "'. Consider: 1) Enable off-heap storage, "
                                + "2) Implement cache warming, 3) Use lazy loading");
            }
        }

        // Анализ query cache
        long queryHits = stats.getQueryCacheHitCount();
        long queryMisses = stats.getQueryCacheMissCount();
        if (queryHits + queryMisses > 0) {
            double queryCacheHitRatio = (double) queryHits / (queryHits + queryMisses);
            if (queryCacheHitRatio < 0.3) {
                report.addRecommendation(
                        "Low query cache hit ratio. Consider: "
                                + "1) Review query parameters variability, "
                                + "2) Increase query cache size, "
                                + "3) Disable query cache for dynamic queries");
            }
        }

        return report;
    }

    /**
     * Остановка мониторинга.
     */
    public void stop() {
        scheduler.shutdown();
    }
}
