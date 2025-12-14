package ru.mentee.power.optimization;

import jakarta.persistence.EntityGraph;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;

/**
 * Оптимизатор запросов для связанных сущностей.
 */
public interface FetchOptimizer {

    /**
     * Создать оптимизированный запрос с JOIN FETCH.
     * @param entityClass основная сущность
     * @param associations ассоциации для загрузки
     * @return оптимизированный запрос
     */
    <T> TypedQuery<T> createOptimizedQuery(Class<T> entityClass, FetchAssociation... associations);

    /**
     * Создать Entity Graph для загрузки.
     * @param entityClass класс сущности
     * @param graphDefinition определение графа
     * @return настроенный entity graph
     */
    <T> EntityGraph<T> createEntityGraph(Class<T> entityClass, GraphDefinition graphDefinition);

    /**
     * Анализировать запрос на N+1 проблемы.
     * @param query запрос для анализа
     * @return отчет с рекомендациями
     */
    FetchAnalysisReport analyzeQuery(Query query);
}
