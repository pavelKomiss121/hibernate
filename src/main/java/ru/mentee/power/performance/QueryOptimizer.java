package ru.mentee.power.performance;

import jakarta.persistence.EntityGraph;
import java.util.Set;

/**
 * Оптимизатор запросов.
 */
public interface QueryOptimizer {

    /**
     * Оптимизировать HQL запрос.
     *
     * @param hql исходный запрос
     * @param context контекст выполнения
     * @return оптимизированный запрос
     */
    OptimizedQuery optimizeHQL(String hql, ExecutionContext context);

    /**
     * Предложить Entity Graph для запроса.
     *
     * @param entityClass класс сущности
     * @param accessPatterns паттерны доступа
     * @return оптимальный Entity Graph
     */
    EntityGraph<?> suggestEntityGraph(Class<?> entityClass, Set<String> accessPatterns);

    /**
     * Определить оптимальную стратегию загрузки.
     *
     * @param association ассоциация
     * @param statistics статистика использования
     * @return рекомендуемая стратегия
     */
    FetchStrategy recommendFetchStrategy(
            AssociationMetadata association, UsageStatistics statistics);
}
