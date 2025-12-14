package ru.mentee.power.cache;

/**
 * Менеджер кэширования.
 */
public interface CacheManager {

    /**
     * Прогрев кэша при запуске.
     */
    void warmUpCache();

    /**
     * Инвалидация кэша для сущности.
     * @param entityClass класс сущности
     * @param id идентификатор
     */
    void invalidateEntity(Class<?> entityClass, Object id);

    /**
     * Инвалидация кэша для типа сущности.
     * @param entityClass класс сущности
     */
    void invalidateEntityType(Class<?> entityClass);

    /**
     * Получить статистику кэширования.
     * @return актуальная статистика
     */
    CacheStatistics getStatistics();

    /**
     * Оптимизировать настройки кэша на основе статистики.
     * @return отчет об оптимизации
     */
    OptimizationReport optimizeCache();

    /**
     * Экспорт метрик для мониторинга.
     * @return метрики в формате Prometheus
     */
    String exportMetrics();
}
