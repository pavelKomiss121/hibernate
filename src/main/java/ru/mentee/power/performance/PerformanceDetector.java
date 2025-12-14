package ru.mentee.power.performance;

import java.util.List;
import org.hibernate.Session;

/**
 * Детектор проблем производительности.
 */
public interface PerformanceDetector {

    /**
     * Начать мониторинг сессии.
     *
     * @param session Hibernate сессия
     * @return контекст мониторинга
     */
    MonitoringContext startMonitoring(Session session);

    /**
     * Анализировать собранные данные.
     *
     * @param context контекст мониторинга
     * @return отчет о проблемах
     */
    PerformanceReport analyze(MonitoringContext context);

    /**
     * Получить рекомендации по оптимизации.
     *
     * @param report отчет о проблемах
     * @return список рекомендаций
     */
    List<OptimizationRecommendation> getRecommendations(PerformanceReport report);

    /**
     * Автоматически применить оптимизации.
     *
     * @param hql проблемный HQL запрос
     * @return оптимизированный HQL запрос
     */
    String optimizeQuery(String hql);
}
