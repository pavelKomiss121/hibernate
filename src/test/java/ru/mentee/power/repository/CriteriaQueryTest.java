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
import ru.mentee.power.dto.ProductSearchCriteria;
import ru.mentee.power.entity.relationship.Customer;
import ru.mentee.power.entity.relationship.Order;
import ru.mentee.power.entity.relationship.OrderItem;
import ru.mentee.power.entity.relationship.OrderStatus;
import ru.mentee.power.entity.relationship.Product;

/**
 * Тест Criteria API запросов.
 */
class CriteriaQueryTest {

    private SessionFactory sessionFactory;
    private HibernateCriteriaRepository repository;

    @BeforeEach
    void setUp() {
        DatabaseConfig dbConfig = new DatabaseConfig();
        // Используем уникальное имя базы данных для каждого теста
        dbConfig.setJdbcUrl("jdbc:h2:mem:testdb_criteria_" + System.currentTimeMillis());
        dbConfig.setDriverClassName("org.h2.Driver");
        dbConfig.setUsername("sa");
        dbConfig.setPassword("");
        dbConfig.setHbm2ddlAuto("create-drop");
        dbConfig.setShowSql(false);
        HibernateConfig hibernateConfig = new HibernateConfig(dbConfig);
        sessionFactory = hibernateConfig.buildSessionFactory();
        repository = new HibernateCriteriaRepository(sessionFactory);

        createTestData();
    }

    @Test
    @DisplayName("Should build dynamic Criteria query")
    void shouldBuildDynamicCriteriaQuery() {
        // Given
        ProductSearchCriteria criteria =
                ProductSearchCriteria.builder()
                        .minPrice(new BigDecimal("100"))
                        .maxPrice(new BigDecimal("1000"))
                        .namePattern("Product")
                        .minQuantity(5)
                        .build();

        // When
        List<Product> results = repository.findProductsByCriteria(criteria);

        // Then
        assertThat(results).isNotEmpty();
        assertThat(results)
                .allMatch(
                        p ->
                                p.getPrice().compareTo(criteria.getMinPrice()) >= 0
                                        && p.getPrice().compareTo(criteria.getMaxPrice()) <= 0
                                        && p.getName().toLowerCase().contains("product")
                                        && p.getStockQuantity() >= criteria.getMinQuantity());
    }

    @Test
    @DisplayName("Should find products above average price")
    void shouldFindProductsAboveAveragePrice() {
        // When
        List<Product> results = repository.findProductsAboveAveragePriceCriteria();

        // Then
        assertThat(results).isNotEmpty();
    }

    @Test
    @DisplayName("Should get customer statistics with Criteria")
    void shouldGetCustomerStatisticsWithCriteria() {
        // When
        var results = repository.getCustomerStatisticsCriteria();

        // Then
        assertThat(results).isNotEmpty();
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
                            .price(new BigDecimal("200"))
                            .stockQuantity(15)
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
