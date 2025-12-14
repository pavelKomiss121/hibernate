package ru.mentee.power.performance.monitoring;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.event.spi.PostLoadEvent;
import org.hibernate.event.spi.PostLoadEventListener;
import org.hibernate.event.spi.PreLoadEvent;
import org.hibernate.event.spi.PreLoadEventListener;
import ru.mentee.power.performance.PerformanceProblem;
import ru.mentee.power.performance.ProblemType;
import ru.mentee.power.performance.QueryExecution;
import ru.mentee.power.performance.Severity;

/**
 * Мониторинг производительности Hibernate.
 */
@Slf4j
public class HibernatePerformanceMonitor implements PostLoadEventListener, PreLoadEventListener {

    private final Map<String, QueryExecution> queryStats = new ConcurrentHashMap<>();
    private final ThreadLocal<Long> loadStartTime = new ThreadLocal<>();

    /**
     * Слушатель событий для сбора метрик.
     */
    @Override
    public void onPreLoad(PreLoadEvent event) {
        loadStartTime.set(System.currentTimeMillis());
    }

    @Override
    public void onPostLoad(PostLoadEvent event) {
        Long startTime = loadStartTime.get();
        if (startTime != null) {
            long duration = System.currentTimeMillis() - startTime;
            String entityName = event.getEntity().getClass().getSimpleName();
            log.debug("Entity {} loaded in {}ms", entityName, duration);
            loadStartTime.remove();
        }
    }

    /**
     * Записать статистику запроса.
     */
    public void recordQuery(String query, long executionTime, int rowCount) {
        queryStats.compute(
                normalizeQuery(query),
                (k, v) -> {
                    if (v == null) {
                        v = new QueryExecution(query);
                    }
                    v.recordExecution(executionTime, rowCount);
                    return v;
                });
    }

    /**
     * Анализировать производительность.
     */
    public List<PerformanceProblem> analyzePerformance() {
        List<PerformanceProblem> issues = new ArrayList<>();

        queryStats.forEach(
                (query, stats) -> {
                    // Проверка на медленные запросы
                    if (stats.getAvgExecutionTime() > 100) {
                        issues.add(
                                new PerformanceProblem(
                                        ProblemType.SLOW_QUERY,
                                        extractEntityName(query),
                                        query,
                                        stats.getExecutionCount(),
                                        String.format(
                                                "Average execution time: %dms",
                                                stats.getAvgExecutionTime()),
                                        Severity.HIGH));
                    }

                    // Проверка на частые запросы (возможный N+1)
                    if (stats.getExecutionCount() > 100 && stats.getAvgRowCount() < 2) {
                        issues.add(
                                new PerformanceProblem(
                                        ProblemType.N_PLUS_ONE,
                                        extractEntityName(query),
                                        query,
                                        stats.getExecutionCount(),
                                        String.format(
                                                "Executed %d times with avg %d rows",
                                                stats.getExecutionCount(), stats.getAvgRowCount()),
                                        Severity.HIGH));
                    }

                    // Проверка на большие выборки
                    if (stats.getMaxRowCount() > 1000) {
                        issues.add(
                                new PerformanceProblem(
                                        ProblemType.LARGE_RESULT_SET,
                                        extractEntityName(query),
                                        query,
                                        stats.getExecutionCount(),
                                        String.format(
                                                "Max rows returned: %d", stats.getMaxRowCount()),
                                        Severity.MEDIUM));
                    }
                });

        return issues;
    }

    private String normalizeQuery(String sql) {
        return sql.replaceAll("\\d+", "?").replaceAll("'[^']*'", "'?'").trim();
    }

    private String extractEntityName(String query) {
        if (query.toUpperCase().contains("FROM")) {
            String[] parts = query.toUpperCase().split("FROM");
            if (parts.length > 1) {
                String tablePart = parts[1].trim().split("\\s")[0];
                return tablePart.toLowerCase();
            }
        }
        return "unknown";
    }
}
