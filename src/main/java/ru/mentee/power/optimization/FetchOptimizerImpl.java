package ru.mentee.power.optimization;

import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Реализация оптимизатора запросов.
 */
@Slf4j
@RequiredArgsConstructor
public class FetchOptimizerImpl implements FetchOptimizer {

    private final EntityManagerFactory entityManagerFactory;
    private static final Pattern JOIN_FETCH_PATTERN =
            Pattern.compile("JOIN\\s+FETCH", Pattern.CASE_INSENSITIVE);
    private static final Pattern LAZY_COLLECTION_PATTERN =
            Pattern.compile("@OneToMany|@ManyToMany", Pattern.CASE_INSENSITIVE);

    @Override
    public <T> TypedQuery<T> createOptimizedQuery(
            Class<T> entityClass, FetchAssociation... associations) {

        EntityManager em = entityManagerFactory.createEntityManager();
        StringBuilder jpql = new StringBuilder("SELECT DISTINCT e FROM ");
        jpql.append(entityClass.getSimpleName()).append(" e ");

        for (FetchAssociation assoc : associations) {
            String joinType =
                    assoc.getJoinType() == FetchAssociation.JoinType.INNER ? "INNER" : "LEFT";
            jpql.append(joinType).append(" JOIN FETCH e.").append(assoc.getPath()).append(" ");
        }

        TypedQuery<T> query = em.createQuery(jpql.toString(), entityClass);
        return query;
    }

    @Override
    public <T> EntityGraph<T> createEntityGraph(
            Class<T> entityClass, GraphDefinition graphDefinition) {

        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            EntityGraph<T> graph = em.createEntityGraph(entityClass);

            for (String attributeNode : graphDefinition.getAttributeNodes()) {
                graph.addAttributeNodes(attributeNode);
            }

            for (GraphDefinition.SubgraphDefinition subgraph : graphDefinition.getSubgraphs()) {
                var sub = graph.addSubgraph(subgraph.getParentPath());
                for (String attributeNode : subgraph.getAttributeNodes()) {
                    sub.addAttributeNodes(attributeNode);
                }
            }

            return graph;
        } finally {
            em.close();
        }
    }

    @Override
    public FetchAnalysisReport analyzeQuery(Query query) {
        FetchAnalysisReport report = new FetchAnalysisReport();

        String queryString = query.toString();

        // Проверяем наличие JOIN FETCH
        if (!JOIN_FETCH_PATTERN.matcher(queryString).find()) {
            report.setHasNPlusOneIssue(true);
            report.addRecommendation("Consider using JOIN FETCH to avoid N+1 queries");
        }

        // Подсчитываем примерное количество запросов
        // Это упрощенная логика, в реальности нужен более сложный анализ
        if (report.isHasNPlusOneIssue()) {
            report.setEstimatedQueryCount(10); // Примерное значение
            report.setOptimalQueryCount(1);
        } else {
            report.setEstimatedQueryCount(1);
            report.setOptimalQueryCount(1);
        }

        return report;
    }
}
