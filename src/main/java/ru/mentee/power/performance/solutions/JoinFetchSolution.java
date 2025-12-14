package ru.mentee.power.performance.solutions;

import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import ru.mentee.power.entity.relationship.Order;
import ru.mentee.power.entity.relationship.OrderStatus;

/**
 * Решение N+1 через JOIN FETCH.
 */
@Slf4j
public class JoinFetchSolution {

    private final SessionFactory sessionFactory;

    public JoinFetchSolution(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    /**
     * Простой JOIN FETCH.
     */
    public List<Order> loadOrdersWithCustomers() {
        try (Session session = sessionFactory.openSession()) {
            // Один запрос вместо N+1
            return session.createQuery(
                            """
                    SELECT DISTINCT o
                    FROM RelationshipOrder o
                    JOIN FETCH o.customer c
                    WHERE o.status = :status
                    """,
                            Order.class)
                    .setParameter("status", OrderStatus.PENDING)
                    .list();
        }
    }

    /**
     * Множественный JOIN FETCH.
     */
    public List<Order> loadOrdersWithAllAssociations() {
        try (Session session = sessionFactory.openSession()) {
            // Загружаем orders с customers
            List<Order> orders =
                    session.createQuery(
                                    """
                    SELECT DISTINCT o
                    FROM RelationshipOrder o
                    JOIN FETCH o.customer
                    WHERE o.status = :status
                    """,
                                    Order.class)
                            .setParameter("status", OrderStatus.PENDING)
                            .list();

            // Отдельный запрос для order items (избегаем cartesian product)
            if (!orders.isEmpty()) {
                List<Long> orderIds =
                        orders.stream().map(Order::getId).collect(Collectors.toList());

                session.createQuery(
                                """
                    SELECT DISTINCT o
                    FROM RelationshipOrder o
                    JOIN FETCH o.orderItems oi
                    JOIN FETCH oi.product
                    WHERE o.id IN :orders
                    """,
                                Order.class)
                        .setParameter("orders", orderIds)
                        .list();
            }

            return orders;
        }
    }

    /**
     * Проблема Cartesian Product при множественных коллекциях.
     */
    public void demonstrateCartesianProductProblem() {
        try (Session session = sessionFactory.openSession()) {
            // ПЛОХО: Cartesian product!
            // Если есть 10 orders, каждый с 5 items и 3 payments,
            // результат будет 10 * 5 * 3 = 150 строк!
            List<Order> badQuery =
                    session.createQuery(
                                    """
                    SELECT DISTINCT o
                    FROM RelationshipOrder o
                    JOIN FETCH o.orderItems
                    """,
                                    Order.class)
                            .list();

            log.warn("Cartesian product query returned {} results", badQuery.size());
        }
    }

    /**
     * Решение Cartesian Product через несколько запросов.
     */
    public List<Order> avoidCartesianProduct() {
        try (Session session = sessionFactory.openSession()) {
            // Запрос 1: Orders с первой коллекцией
            List<Order> orders =
                    session.createQuery(
                                    """
                    SELECT DISTINCT o
                    FROM RelationshipOrder o
                    JOIN FETCH o.orderItems
                    WHERE o.status = :status
                    """,
                                    Order.class)
                            .setParameter("status", OrderStatus.PENDING)
                            .list();

            // Запрос 2: Загружаем вторую коллекцию (если бы была)
            if (!orders.isEmpty()) {
                List<Long> orderIds =
                        orders.stream().map(Order::getId).collect(Collectors.toList());

                // Пример для другой коллекции
                // session.createQuery("""
                //     SELECT DISTINCT o
                //     FROM RelationshipOrder o
                //     JOIN FETCH o.payments
                //     WHERE o.id IN :ids
                //     """, Order.class)
                //     .setParameter("ids", orderIds)
                //     .list();
            }

            return orders;
        }
    }
}
