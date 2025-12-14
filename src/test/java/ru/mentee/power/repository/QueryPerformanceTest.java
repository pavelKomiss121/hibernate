package ru.mentee.power.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.mentee.power.config.DatabaseConfig;
import ru.mentee.power.config.HibernateConfig;
import ru.mentee.power.entity.relationship.Customer;
import ru.mentee.power.entity.relationship.Order;
import ru.mentee.power.entity.relationship.OrderItem;
import ru.mentee.power.entity.relationship.OrderStatus;
import ru.mentee.power.entity.relationship.Product;

/**
 * Тест производительности запросов.
 */
@Slf4j
class QueryPerformanceTest {

    private SessionFactory sessionFactory;

    @BeforeEach
    void setUp() {
        DatabaseConfig dbConfig = new DatabaseConfig();
        // Используем уникальное имя базы данных для каждого теста
        dbConfig.setJdbcUrl("jdbc:h2:mem:testdb_perf_" + System.currentTimeMillis());
        dbConfig.setDriverClassName("org.h2.Driver");
        dbConfig.setUsername("sa");
        dbConfig.setPassword("");
        dbConfig.setHbm2ddlAuto("create-drop");
        dbConfig.setShowSql(false);
        HibernateConfig hibernateConfig = new HibernateConfig(dbConfig);
        sessionFactory = hibernateConfig.buildSessionFactory();

        createLargeDataset(100);
    }

    @Test
    @DisplayName("Should optimize query performance")
    void shouldOptimizeQueryPerformance() {
        // When - неоптимизированный запрос
        long startTime = System.currentTimeMillis();
        List<Customer> customers;
        try (Session session = sessionFactory.openSession()) {
            // Явно указываем имя сущности из relationship пакета
            customers = session.createQuery("FROM RelationshipCustomer", Customer.class).list();

            // Вызовет N+1 проблему
            customers.forEach(c -> c.getOrders().size());
        }
        long slowTime = System.currentTimeMillis() - startTime;

        // When - оптимизированный запрос
        startTime = System.currentTimeMillis();
        List<Customer> optimizedCustomers;
        try (Session session = sessionFactory.openSession()) {
            optimizedCustomers =
                    session.createQuery(
                                    "SELECT DISTINCT c FROM RelationshipCustomer c LEFT JOIN FETCH"
                                            + " c.orders",
                                    Customer.class)
                            .list();

            optimizedCustomers.forEach(c -> c.getOrders().size());
        }
        long fastTime = System.currentTimeMillis() - startTime;

        // Then
        assertThat(fastTime).isLessThanOrEqualTo(slowTime * 2);
        log.info("Slow query: {}ms, Optimized query: {}ms", slowTime, fastTime);
    }

    private void createLargeDataset(int count) {
        try (Session session = sessionFactory.openSession()) {
            org.hibernate.Transaction tx = session.beginTransaction();

            for (int i = 0; i < count; i++) {
                Product product =
                        Product.builder()
                                .sku("SKU" + i)
                                .name("Product " + i)
                                .price(new BigDecimal(100 + i))
                                .stockQuantity(10 + i)
                                .build();
                session.persist(product);

                Customer customer =
                        Customer.builder()
                                .firstName("Customer" + i)
                                .lastName("Last" + i)
                                .email("customer" + i + "@example.com")
                                .build();
                session.persist(customer);

                Order order =
                        Order.builder()
                                .orderNumber("ORD" + i)
                                .customer(customer)
                                .status(OrderStatus.COMPLETED)
                                .orderDate(LocalDateTime.now())
                                .build();

                OrderItem item =
                        OrderItem.builder()
                                .order(order)
                                .product(product)
                                .quantity(1)
                                .unitPrice(product.getPrice())
                                .build();

                order.addOrderItem(item);
                session.persist(order);
            }

            tx.commit();
        }
    }
}
