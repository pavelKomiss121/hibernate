package ru.mentee.power.performance;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.Data;
import org.hibernate.Session;
import org.hibernate.stat.Statistics;

/**
 * Контекст мониторинга производительности.
 */
@Data
public class MonitoringContext {

    private final Session session;
    private final Statistics statistics;
    private final LocalDateTime startTime;
    private final ConcurrentMap<String, QueryExecution> queryExecutions = new ConcurrentHashMap<>();
    private final List<String> executedQueries = new ArrayList<>();
    private long initialQueryCount;
    private long initialEntityLoadCount;

    public MonitoringContext(Session session, Statistics statistics) {
        this.session = session;
        this.statistics = statistics;
        this.startTime = LocalDateTime.now();
        this.initialQueryCount = statistics.getPrepareStatementCount();
        this.initialEntityLoadCount = statistics.getEntityLoadCount();
    }

    public void recordQuery(String sql, long executionTime, int rowCount) {
        executedQueries.add(sql);
        queryExecutions.compute(
                normalizeQuery(sql),
                (k, v) -> {
                    if (v == null) {
                        v = new QueryExecution(sql);
                    }
                    v.recordExecution(executionTime, rowCount);
                    return v;
                });
    }

    public long getTotalQueriesExecuted() {
        return statistics.getPrepareStatementCount() - initialQueryCount;
    }

    public long getTotalEntitiesLoaded() {
        return statistics.getEntityLoadCount() - initialEntityLoadCount;
    }

    private String normalizeQuery(String sql) {
        // Нормализуем запрос для группировки похожих
        return sql.replaceAll("\\d+", "?").replaceAll("'[^']*'", "'?'").trim();
    }
}
