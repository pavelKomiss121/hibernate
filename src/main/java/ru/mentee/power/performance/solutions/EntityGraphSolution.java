package ru.mentee.power.performance.solutions;

import jakarta.persistence.EntityGraph;
import jakarta.persistence.Subgraph;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.graph.GraphSemantic;
import ru.mentee.power.entity.relationship.Customer;
import ru.mentee.power.entity.relationship.Order;
import ru.mentee.power.entity.relationship.OrderStatus;

/**
 * Решение через Entity Graphs.
 */
@Slf4j
public class EntityGraphSolution {

    private final SessionFactory sessionFactory;

    public EntityGraphSolution(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    /**
     * Загрузка с использованием Entity Graph.
     */
    public List<Order> loadWithEntityGraph() {
        try (Session session = sessionFactory.openSession()) {
            EntityGraph<?> graph = session.getEntityGraph("order-with-details");
            return session.createQuery(
                            "FROM RelationshipOrder o WHERE o.status = :status", Order.class)
                    .setParameter("status", OrderStatus.PENDING)
                    .setHint(GraphSemantic.FETCH.getJpaHintName(), graph)
                    .list();
        }
    }

    /**
     * Динамический Entity Graph.
     */
    public List<Customer> loadCustomersWithDynamicGraph(
            boolean includeOrders, boolean includeAddresses) {
        try (Session session = sessionFactory.openSession()) {
            EntityGraph<Customer> graph = session.createEntityGraph(Customer.class);

            // Динамически добавляем атрибуты
            if (includeOrders) {
                Subgraph<Order> orderGraph = graph.addSubgraph("orders", Order.class);
                orderGraph.addAttributeNodes("orderItems");
            }
            // if (includeAddresses) {
            //     graph.addAttributeNodes("addresses");
            // }

            return session.createQuery(
                            "FROM RelationshipCustomer c WHERE c.email IS NOT NULL", Customer.class)
                    .setHint(GraphSemantic.FETCH.getJpaHintName(), graph)
                    .list();
        }
    }

    /**
     * Переключение между FETCH и LOAD семантикой.
     */
    public void demonstrateGraphSemantics() {
        try (Session session = sessionFactory.openSession()) {
            EntityGraph<Order> graph = session.createEntityGraph(Order.class);
            graph.addAttributeNodes("customer");
            graph.addAttributeNodes("orderItems");

            // FETCH - загружает только указанное в графе
            List<Order> fetchResult =
                    session.createQuery("FROM RelationshipOrder", Order.class)
                            .setHint(GraphSemantic.FETCH.getJpaHintName(), graph)
                            .list();

            // LOAD - загружает указанное в графе + все EAGER ассоциации
            List<Order> loadResult =
                    session.createQuery("FROM RelationshipOrder", Order.class)
                            .setHint(GraphSemantic.LOAD.getJpaHintName(), graph)
                            .list();

            log.info("FETCH result size: {}", fetchResult.size());
            log.info("LOAD result size: {}", loadResult.size());
        }
    }
}
