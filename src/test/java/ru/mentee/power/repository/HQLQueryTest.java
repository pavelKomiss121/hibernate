package ru.mentee.power.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.mentee.power.config.DatabaseConfig;
import ru.mentee.power.config.HibernateConfig;
import ru.mentee.power.dto.CustomerStatistics;
import ru.mentee.power.entity.relationship.Customer;
import ru.mentee.power.entity.relationship.Order;
import ru.mentee.power.entity.relationship.OrderItem;
import ru.mentee.power.entity.relationship.OrderStatus;
import ru.mentee.power.entity.relationship.Product;

/**
 * Тест HQL запросов.
 */
class HQLQueryTest {

    private SessionFactory sessionFactory;
    private HibernateHQLRepository repository;

    @BeforeEach
    void setUp() {
        DatabaseConfig dbConfig = new DatabaseConfig();
        // Используем уникальное имя базы данных для каждого теста
        dbConfig.setJdbcUrl("jdbc:h2:mem:testdb_hql_" + System.currentTimeMillis());
        dbConfig.setDriverClassName("org.h2.Driver");
        dbConfig.setUsername("sa");
        dbConfig.setPassword("");
        dbConfig.setHbm2ddlAuto("create-drop");
        dbConfig.setShowSql(false);
        HibernateConfig hibernateConfig = new HibernateConfig(dbConfig);
        sessionFactory = hibernateConfig.buildSessionFactory();
        repository = new HibernateHQLRepository(sessionFactory);

        // Создаем тестовые данные
        createTestData();
    }

    @Test
    @DisplayName("Should execute complex HQL with aggregation")
    void shouldExecuteComplexHQLQuery() {
        // When
        List<CustomerStatistics> results = repository.getCustomerStatistics();

        // Then
        assertThat(results).isNotEmpty();
        assertThat(results.get(0).getOrderCount()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should find products by price range")
    void shouldFindProductsByPriceRange() {
        // When
        List<Product> results =
                repository.findProductsByPriceRange(new BigDecimal("50"), new BigDecimal("200"));

        // Then
        assertThat(results).isNotEmpty();
        assertThat(results)
                .allMatch(
                        p ->
                                p.getPrice().compareTo(new BigDecimal("50")) >= 0
                                        && p.getPrice().compareTo(new BigDecimal("200")) <= 0);
    }

    @Test
    @DisplayName("Should find products with pagination")
    void shouldFindProductsWithPagination() {
        // When
        var page1 = repository.findProductsPaginated(0, 5);
        var page2 = repository.findProductsPaginated(1, 5);

        // Then
        assertThat(page1.getContent()).hasSizeLessThanOrEqualTo(5);
        assertThat(page1.getTotalElements()).isGreaterThan(0);
        assertThat(page2.getContent()).hasSizeLessThanOrEqualTo(5);
    }

    @Test
    @DisplayName("Should update product prices")
    void shouldUpdateProductPrices() {
        // When
        int updated = repository.updateProductPrices("SKU001", new BigDecimal("0.1"));

        // Then
        assertThat(updated).isGreaterThanOrEqualTo(0);
    }

    private void createTestData() {
        try (Session session = sessionFactory.openSession()) {
            org.hibernate.Transaction tx = session.beginTransaction();

            // Создаем продукты
            Product product1 =
                    Product.builder()
                            .sku("SKU001")
                            .name("Product 1")
                            .price(new BigDecimal("100"))
                            .stockQuantity(10)
                            .build();
            Product product2 =
                    Product.builder()
                            .sku("SKU002")
                            .name("Product 2")
                            .price(new BigDecimal("150"))
                            .stockQuantity(20)
                            .build();

            session.persist(product1);
            session.persist(product2);

            // Создаем клиента
            Customer customer =
                    Customer.builder()
                            .firstName("John")
                            .lastName("Doe")
                            .email("john@example.com")
                            .build();
            session.persist(customer);

            // Создаем заказ
            Order order =
                    Order.builder()
                            .orderNumber("ORD001")
                            .customer(customer)
                            .status(OrderStatus.COMPLETED)
                            .orderDate(LocalDateTime.now())
                            .build();

            OrderItem item1 =
                    OrderItem.builder()
                            .order(order)
                            .product(product1)
                            .quantity(2)
                            .unitPrice(product1.getPrice())
                            .build();

            order.addOrderItem(item1);
            session.persist(order);

            tx.commit();
        }
    }
}
