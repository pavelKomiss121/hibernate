package ru.mentee.power.performance;

import jakarta.persistence.EntityGraph;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

/**
 * Реализация оптимизатора запросов.
 */
@Slf4j
public class QueryOptimizerImpl implements QueryOptimizer {

    @Override
    public OptimizedQuery optimizeHQL(String hql, ExecutionContext context) {
        List<String> optimizations = new ArrayList<>();
        String optimizedHQL = hql;

        // Добавляем JOIN FETCH если нужно
        if (context.getAccessedProperties() != null && !context.getAccessedProperties().isEmpty()) {
            for (String property : context.getAccessedProperties()) {
                if (!hql.contains("JOIN FETCH") && !hql.contains("join fetch")) {
                    optimizedHQL = addJoinFetch(optimizedHQL, property);
                    optimizations.add("Added JOIN FETCH for: " + property);
                }
            }
        }

        // Добавляем DISTINCT если есть JOIN FETCH
        if (optimizedHQL.contains("JOIN FETCH") && !optimizedHQL.contains("DISTINCT")) {
            optimizedHQL =
                    "SELECT DISTINCT " + optimizedHQL.substring(optimizedHQL.indexOf("FROM"));
            optimizations.add("Added DISTINCT to avoid duplicates");
        }

        return new OptimizedQuery(
                hql, optimizedHQL, "Query optimized based on access patterns", optimizations);
    }

    @Override
    public EntityGraph<?> suggestEntityGraph(Class<?> entityClass, Set<String> accessPatterns) {
        // Упрощенная реализация - требует Session для создания EntityGraph
        // В реальной реализации Session должен передаваться через ExecutionContext
        // или создаваться через SessionFactory
        return null;
    }

    @Override
    public FetchStrategy recommendFetchStrategy(
            AssociationMetadata association, UsageStatistics statistics) {
        if (statistics.getAccessCount() > 100 && statistics.getAverageResultSize() < 10) {
            return new FetchStrategy(
                    "BATCH",
                    25,
                    "Batch fetching recommended for frequently accessed small collections");
        } else if (statistics.getAverageResultSize() > 50) {
            return new FetchStrategy(
                    "SUBSELECT", 0, "Subselect fetching recommended for large collections");
        } else {
            return new FetchStrategy(
                    "JOIN_FETCH", 0, "JOIN FETCH recommended for immediate access");
        }
    }

    private String addJoinFetch(String hql, String property) {
        // Простая реализация - добавляет JOIN FETCH
        String upperHQL = hql.toUpperCase();
        if (upperHQL.contains("FROM")) {
            int fromIndex = upperHQL.indexOf("FROM");
            String entityAlias = extractEntityAlias(hql, fromIndex);
            if (entityAlias != null) {
                String joinFetch = " JOIN FETCH " + entityAlias + "." + property;
                return hql.substring(0, fromIndex) + joinFetch + " " + hql.substring(fromIndex);
            }
        }
        return hql;
    }

    private String extractEntityAlias(String hql, int fromIndex) {
        // Извлекаем алиас сущности после FROM
        String afterFrom = hql.substring(fromIndex + 4).trim();
        String[] parts = afterFrom.split("\\s+");
        if (parts.length > 0) {
            return parts[0].toLowerCase();
        }
        return null;
    }
}
