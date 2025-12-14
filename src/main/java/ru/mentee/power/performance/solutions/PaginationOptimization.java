package ru.mentee.power.performance.solutions;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import ru.mentee.power.dto.Page;
import ru.mentee.power.entity.relationship.Order;

/**
 * Оптимизация пагинации для больших датасетов.
 */
@Slf4j
public class PaginationOptimization {

    private final SessionFactory sessionFactory;

    public PaginationOptimization(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    /**
     * Оптимизированная пагинация.
     */
    public Page<Order> optimizedPagination(int page, int size) {
        try (Session session = sessionFactory.openSession()) {
            // Сначала получаем ID с пагинацией
            List<Long> ids =
                    session.createQuery(
                                    "SELECT o.id FROM RelationshipOrder o ORDER BY o.orderDate"
                                            + " DESC",
                                    Long.class)
                            .setFirstResult(page * size)
                            .setMaxResults(size)
                            .list();

            // Затем загружаем полные данные по ID
            List<Order> orders =
                    session.createQuery(
                                    """
                    FROM RelationshipOrder o
                    LEFT JOIN FETCH o.customer
                    WHERE o.id IN :ids
                    ORDER BY o.orderDate DESC
                    """,
                                    Order.class)
                            .setParameter("ids", ids)
                            .list();

            // Count запрос отдельно
            Long total =
                    session.createQuery("SELECT COUNT(o) FROM RelationshipOrder o", Long.class)
                            .uniqueResult();

            return new Page<>(orders, page, size, total);
        }
    }
}
