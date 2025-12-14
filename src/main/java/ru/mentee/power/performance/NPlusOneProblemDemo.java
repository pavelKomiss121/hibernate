package ru.mentee.power.performance;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import ru.mentee.power.entity.relationship.Order;
import ru.mentee.power.entity.relationship.OrderStatus;

/**
 * Демонстрация и выявление N+1 проблемы.
 */
@Slf4j
public class NPlusOneProblemDemo {

    private final SessionFactory sessionFactory;

    public NPlusOneProblemDemo(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    /**
     * Классический пример N+1 проблемы.
     */
    public void demonstrateNPlusOneProblem() {
        // Включаем статистику для отслеживания
        Statistics stats = sessionFactory.getStatistics();
        stats.setStatisticsEnabled(true);
        stats.clear();

        try (Session session = sessionFactory.openSession()) {
            log.info("=== N+1 Problem Demo ===");

            // Запрос 1: Загружаем все заказы
            List<Order> orders =
                    session.createQuery(
                                    "FROM RelationshipOrder o WHERE o.status = :status",
                                    Order.class)
                            .setParameter("status", OrderStatus.PENDING)
                            .list();

            log.info("Orders loaded: {}", orders.size());
            log.info("Queries executed so far: {}", stats.getPrepareStatementCount());

            // N запросов: По одному для каждого customer
            for (Order order : orders) {
                // Lazy loading триггерит дополнительный SELECT
                String customerName = order.getCustomer().getFirstName();
                log.debug("Customer: {}", customerName);
            }

            log.info("Total queries executed: {}", stats.getPrepareStatementCount());
            log.info("Should be 1-2, but was: {}", stats.getPrepareStatementCount());
        }
    }
}
