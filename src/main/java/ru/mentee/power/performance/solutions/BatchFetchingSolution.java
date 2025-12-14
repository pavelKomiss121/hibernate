package ru.mentee.power.performance.solutions;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import ru.mentee.power.entity.relationship.Customer;
import ru.mentee.power.entity.relationship.Order;

/**
 * Решение через Batch Fetching.
 */
@Slf4j
public class BatchFetchingSolution {

    private final SessionFactory sessionFactory;

    public BatchFetchingSolution(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    /**
     * Демонстрация batch fetching.
     */
    public void demonstrateBatchFetching() {
        try (Session session = sessionFactory.openSession()) {
            // Загружаем customers
            List<Customer> customers =
                    session.createQuery(
                                    "FROM RelationshipCustomer c WHERE c.email IS NOT NULL",
                                    Customer.class)
                            .list();

            log.info("Loaded {} customers", customers.size());

            // При обращении к orders, Hibernate загрузит их батчами по 25
            // Вместо 100 запросов будет всего 4 (100/25)
            for (Customer customer : customers) {
                log.debug(
                        "Customer {} has {} orders",
                        customer.getFirstName(),
                        customer.getOrders().size());
            }
        }
    }

    /**
     * Динамическая настройка batch size.
     */
    public void dynamicBatchSize() {
        try (Session session = sessionFactory.openSession()) {
            // Можно переопределить batch size для сессии
            session.setJdbcBatchSize(50);

            List<Order> orders =
                    session.createQuery("FROM RelationshipOrder o", Order.class).list();

            // Batch loading related data
            orders.forEach(o -> o.getOrderItems().size());
        }
    }

    /**
     * Subselect fetching для больших коллекций.
     */
    public void demonstrateSubselectFetching() {
        try (Session session = sessionFactory.openSession()) {
            // Загружаем customers
            List<Customer> customers =
                    session.createQuery(
                                    "FROM RelationshipCustomer c WHERE c.email IS NOT NULL",
                                    Customer.class)
                            .list();

            // При первом обращении к orders, Hibernate выполнит:
            // SELECT * FROM orders WHERE customer_id IN
            //   (SELECT id FROM customers WHERE email IS NOT NULL)
            if (!customers.isEmpty()) {
                Customer first = customers.get(0);
                first.getOrders().size(); // Триггерит subselect для ВСЕХ customers

                // Остальные orders уже загружены!
                customers.forEach(
                        c ->
                                log.info(
                                        "Customer {} has {} orders",
                                        c.getFirstName(),
                                        c.getOrders().size()));
            }
        }
    }
}
