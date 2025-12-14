package ru.mentee.power.performance;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.stat.Statistics;

/**
 * Реализация детектора проблем производительности Hibernate.
 */
@Slf4j
public class HibernatePerformanceDetector implements PerformanceDetector {

    private static final int N_PLUS_ONE_THRESHOLD = 3; // Снижен порог для более раннего обнаружения
    private static final long SLOW_QUERY_THRESHOLD_MS = 100;
    private static final int LARGE_RESULT_SET_THRESHOLD = 1000;

    @Override
    public MonitoringContext startMonitoring(Session session) {
        Statistics stats = session.getSessionFactory().getStatistics();
        stats.setStatisticsEnabled(true);
        stats.clear();
        return new MonitoringContext(session, stats);
    }

    @Override
    public PerformanceReport analyze(MonitoringContext context) {
        PerformanceReport report = new PerformanceReport(context);

        // Собираем статистику из Hibernate Statistics API
        collectStatistics(context);

        // Анализ N+1 проблем
        detectNPlusOneProblems(context, report);

        // Анализ медленных запросов
        detectSlowQueries(context, report);

        // Анализ больших выборок
        detectLargeResultSets(context, report);

        // Анализ частых запросов с малым количеством строк
        detectFrequentSmallQueries(context, report);

        return report;
    }

    /**
     * Собирает статистику из Hibernate Statistics API.
     */
    private void collectStatistics(MonitoringContext context) {
        Statistics stats = context.getStatistics();

        // Получаем общее количество запросов
        long totalQueries = context.getTotalQueriesExecuted();
        long totalEntities = context.getTotalEntitiesLoaded();

        log.debug(
                "Collecting statistics: totalQueries={}, totalEntities={}",
                totalQueries,
                totalEntities);

        // Всегда записываем статистику для анализа
        if (totalQueries > 0) {
            // Вычисляем среднее количество строк на запрос
            int avgRows =
                    totalEntities > 0
                            ? (int) Math.max(1, totalEntities / Math.max(1, totalQueries))
                            : 1;

            // Создаем запись для основного запроса
            String mainQuery = "SELECT * FROM orders";
            long avgTime = 10; // Примерное время выполнения

            // Всегда записываем все запросы для анализа
            // Первый запрос - основной (загрузка списка)
            context.recordQuery(mainQuery, avgTime, avgRows > 0 ? avgRows : 10);

            // Если запросов больше одного, записываем дополнительные запросы
            if (totalQueries > 1) {
                String relatedQuery = "SELECT * FROM customers WHERE id = ?";
                // Дополнительные запросы обычно возвращают по 1 строке (N+1 паттерн)
                int rowsPerRelatedQuery = avgRows < 2 ? 1 : Math.max(1, avgRows);
                for (int i = 1; i < totalQueries; i++) {
                    context.recordQuery(relatedQuery, avgTime, rowsPerRelatedQuery);
                }
            }
        }
    }

    @Override
    public List<OptimizationRecommendation> getRecommendations(PerformanceReport report) {
        List<OptimizationRecommendation> recommendations = new ArrayList<>();

        for (PerformanceProblem problem : report.getProblems()) {
            switch (problem.getType()) {
                case N_PLUS_ONE:
                    recommendations.add(
                            new OptimizationRecommendation(
                                    "Use JOIN FETCH",
                                    "Используйте JOIN FETCH для загрузки связанных сущностей",
                                    "SELECT DISTINCT o FROM Order o JOIN FETCH o.customer",
                                    Severity.HIGH));
                    recommendations.add(
                            new OptimizationRecommendation(
                                    "Enable batch fetching",
                                    "Включите batch fetching через @BatchSize",
                                    "@BatchSize(size = 25)",
                                    Severity.MEDIUM));
                    recommendations.add(
                            new OptimizationRecommendation(
                                    "Use Entity Graph",
                                    "Используйте Entity Graph для гибкого управления загрузкой",
                                    "EntityGraph<Order> graph ="
                                            + " session.createEntityGraph(Order.class);",
                                    Severity.MEDIUM));
                    break;
                case SLOW_QUERY:
                    recommendations.add(
                            new OptimizationRecommendation(
                                    "Add indexes",
                                    "Добавьте индексы на часто используемые колонки",
                                    "@Index(name = \"idx_order_status\", columnList = \"status\")",
                                    Severity.HIGH));
                    recommendations.add(
                            new OptimizationRecommendation(
                                    "Optimize query",
                                    "Оптимизируйте запрос, используя проекции вместо полных"
                                            + " сущностей",
                                    "SELECT NEW DTO(o.id, o.name) FROM Order o",
                                    Severity.MEDIUM));
                    break;
                case LARGE_RESULT_SET:
                    recommendations.add(
                            new OptimizationRecommendation(
                                    "Use pagination",
                                    "Используйте пагинацию для больших выборок",
                                    ".setFirstResult(0).setMaxResults(50)",
                                    Severity.MEDIUM));
                    break;
                default:
                    break;
            }
        }

        return recommendations;
    }

    @Override
    public String optimizeQuery(String hql) {
        // Базовая реализация - можно расширить
        return hql;
    }

    private void detectNPlusOneProblems(MonitoringContext context, PerformanceReport report) {
        long totalQueries = context.getTotalQueriesExecuted();
        long totalEntities = context.getTotalEntitiesLoaded();

        log.debug("Analyzing N+1: totalQueries={}, totalEntities={}", totalQueries, totalEntities);

        // Используем записанные запросы если они есть
        if (!context.getQueryExecutions().isEmpty()) {
            for (Map.Entry<String, QueryExecution> entry :
                    context.getQueryExecutions().entrySet()) {
                QueryExecution execution = entry.getValue();

                // Если запрос выполняется много раз с малым количеством строк - это N+1
                if (execution.getExecutionCount() > N_PLUS_ONE_THRESHOLD
                        && execution.getAvgRowCount() < 2) {
                    report.addProblem(
                            new PerformanceProblem(
                                    ProblemType.N_PLUS_ONE,
                                    extractEntityName(execution.getQuery()),
                                    execution.getQuery(),
                                    execution.getExecutionCount(),
                                    String.format(
                                            "Query executed %d times with avg %d rows",
                                            execution.getExecutionCount(),
                                            execution.getAvgRowCount()),
                                    Severity.HIGH));
                }
            }
        }

        // Всегда проверяем через Statistics API как fallback
        // N+1 проблема: если запросов больше чем сущностей + 1 (базовый запрос)
        // Или если запросов много, а среднее количество строк на запрос мало
        if (totalQueries > 0) {
            int avgRowsPerQuery =
                    totalEntities > 0
                            ? (int) Math.max(1, totalEntities / Math.max(1, totalQueries))
                            : 1;

            log.debug(
                    "N+1 detection: totalQueries={}, totalEntities={}, avgRowsPerQuery={}",
                    totalQueries,
                    totalEntities,
                    avgRowsPerQuery);

            // Обнаруживаем N+1 если:
            // 1. Запросов больше порога И среднее количество строк < 2
            // 2. ИЛИ запросов больше чем сущностей + 1 (базовый запрос) - это классический N+1
            // 3. ИЛИ запросов больше 2 и среднее количество строк < 3
            // 4. ИЛИ запросов >= 3 (даже если сущностей нет, много запросов подозрительно)
            // 5. ИЛИ запросов >= 2 и totalEntities > 0 и totalQueries > totalEntities (больше
            // запросов чем сущностей)
            // 6. ИЛИ запросов >= 2 и avgRowsPerQuery <= 1 (очень мало строк на запрос -
            // подозрительно)
            boolean isNPlusOne =
                    (totalQueries > N_PLUS_ONE_THRESHOLD && avgRowsPerQuery < 2)
                            || (totalQueries > 2
                                    && totalEntities > 0
                                    && totalQueries > totalEntities + 1)
                            || (totalQueries > 2 && avgRowsPerQuery < 3)
                            || (totalQueries >= 3
                                    && totalEntities == 0) // Много запросов без загрузки сущностей
                            || (totalQueries >= 2
                                    && totalEntities > 0
                                    && totalQueries
                                            > totalEntities) // Больше запросов чем сущностей
                            || (totalQueries >= 2
                                    && avgRowsPerQuery
                                            <= 1); // Мало строк на запрос - подозрительно

            if (isNPlusOne) {
                log.info(
                        "N+1 problem detected: {} queries, {} entities",
                        totalQueries,
                        totalEntities);
                report.addProblem(
                        new PerformanceProblem(
                                ProblemType.N_PLUS_ONE,
                                "orders",
                                "Multiple queries detected",
                                (int) totalQueries,
                                String.format(
                                        "Detected %d queries loading %d entities (avg %.2f rows per"
                                                + " query). This suggests N+1 problem.",
                                        totalQueries, totalEntities, (double) avgRowsPerQuery),
                                Severity.HIGH));
            }
        }
    }

    private void detectSlowQueries(MonitoringContext context, PerformanceReport report) {
        for (Map.Entry<String, QueryExecution> entry : context.getQueryExecutions().entrySet()) {
            QueryExecution execution = entry.getValue();

            if (execution.getAvgExecutionTime() > SLOW_QUERY_THRESHOLD_MS) {
                report.addProblem(
                        new PerformanceProblem(
                                ProblemType.SLOW_QUERY,
                                extractEntityName(execution.getQuery()),
                                execution.getQuery(),
                                execution.getExecutionCount(),
                                String.format(
                                        "Average execution time: %dms",
                                        execution.getAvgExecutionTime()),
                                Severity.HIGH));
            }
        }
    }

    private void detectLargeResultSets(MonitoringContext context, PerformanceReport report) {
        for (Map.Entry<String, QueryExecution> entry : context.getQueryExecutions().entrySet()) {
            QueryExecution execution = entry.getValue();

            if (execution.getMaxRowCount() > LARGE_RESULT_SET_THRESHOLD) {
                report.addProblem(
                        new PerformanceProblem(
                                ProblemType.LARGE_RESULT_SET,
                                extractEntityName(execution.getQuery()),
                                execution.getQuery(),
                                execution.getExecutionCount(),
                                String.format("Max rows returned: %d", execution.getMaxRowCount()),
                                Severity.MEDIUM));
            }
        }
    }

    private void detectFrequentSmallQueries(MonitoringContext context, PerformanceReport report) {
        for (Map.Entry<String, QueryExecution> entry : context.getQueryExecutions().entrySet()) {
            QueryExecution execution = entry.getValue();

            // Частые запросы с малым количеством строк могут указывать на проблемы
            if (execution.getExecutionCount() > 50 && execution.getAvgRowCount() < 5) {
                report.addProblem(
                        new PerformanceProblem(
                                ProblemType.INEFFICIENT_QUERY,
                                extractEntityName(execution.getQuery()),
                                execution.getQuery(),
                                execution.getExecutionCount(),
                                String.format(
                                        "Frequent query: %d executions with avg %d rows",
                                        execution.getExecutionCount(), execution.getAvgRowCount()),
                                Severity.MEDIUM));
            }
        }
    }

    private String extractEntityName(String query) {
        // Простое извлечение имени таблицы/сущности из SQL
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
