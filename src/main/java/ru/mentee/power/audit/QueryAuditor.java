package ru.mentee.power.audit;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;

/**
 * Аудитор выполнения запросов.
 */
@Slf4j
public class QueryAuditor {

    private final SessionFactory sessionFactory;
    private final List<QueryAudit> auditLog = new ArrayList<>();

    public QueryAuditor(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    /**
     * Аудит выполнения запроса.
     */
    public <T> T auditQuery(String queryType, String queryDescription, QueryExecutor<T> executor) {
        long startTime = System.currentTimeMillis();

        try {
            T result = executor.execute();
            long executionTime = System.currentTimeMillis() - startTime;

            int resultCount = result instanceof List ? ((List<?>) result).size() : 1;

            QueryAudit audit =
                    new QueryAudit(
                            queryDescription,
                            executionTime,
                            LocalDateTime.now(),
                            resultCount,
                            queryType);

            auditLog.add(audit);
            log.debug(
                    "Query executed: {} in {}ms, results: {}",
                    queryDescription,
                    executionTime,
                    resultCount);

            return result;
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("Query failed: {} in {}ms", queryDescription, executionTime, e);
            throw e;
        }
    }

    /**
     * Получить статистику Hibernate.
     */
    public Statistics getStatistics() {
        return sessionFactory.getStatistics();
    }

    /**
     * Получить лог аудита.
     */
    public List<QueryAudit> getAuditLog() {
        return new ArrayList<>(auditLog);
    }

    /**
     * Очистить лог аудита.
     */
    public void clearAuditLog() {
        auditLog.clear();
    }

    /**
     * Функциональный интерфейс для выполнения запросов.
     */
    @FunctionalInterface
    public interface QueryExecutor<T> {
        T execute();
    }
}
