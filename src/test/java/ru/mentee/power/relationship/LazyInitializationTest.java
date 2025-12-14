package ru.mentee.power.relationship;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDateTime;
import org.hibernate.Hibernate;
import org.hibernate.LazyInitializationException;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterEach;
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
 * Тест обработки LazyInitializationException.
 */
class LazyInitializationTest {

    private EntityManagerFactory entityManagerFactory;
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        DatabaseConfig dbConfig = new DatabaseConfig();
        dbConfig.setJdbcUrl(
                "jdbc:h2:mem:test;INIT=CREATE SCHEMA IF NOT EXISTS"
                        + " mentee_power;MODE=PostgreSQL;USER=sa;PASSWORD=");
        dbConfig.setUsername("sa");
        dbConfig.setPassword("");
        dbConfig.setHbm2ddlAuto("create-drop");
        dbConfig.setShowSql(true);

        HibernateConfig hibernateConfig = new HibernateConfig(dbConfig);
        SessionFactory sessionFactory = hibernateConfig.buildSessionFactory();
        entityManagerFactory = sessionFactory;
        entityManager = entityManagerFactory.createEntityManager();
    }

    @AfterEach
    void tearDown() {
        if (entityManager != null && entityManager.isOpen()) {
            entityManager.close();
        }
        if (entityManagerFactory != null && entityManagerFactory.isOpen()) {
            entityManagerFactory.close();
        }
    }

    @Test
    @DisplayName("Should handle lazy initialization correctly")
    void shouldHandleLazyInitialization() {
        // Given
        entityManager.getTransaction().begin();

        Customer customer = new Customer();
        customer.setFirstName("John");
        customer.setLastName("Doe");
        customer.setEmail("john@example.com");
        entityManager.persist(customer);

        Product product = new Product();
        product.setSku("PROD-001");
        product.setName("Test Product");
        product.setPrice(new java.math.BigDecimal("99.99"));
        product.setStockQuantity(100);
        entityManager.persist(product);

        Order order = new Order();
        order.setOrderNumber("ORD-001");
        order.setCustomer(customer);
        order.setStatus(OrderStatus.PENDING);
        order.setOrderDate(LocalDateTime.now());

        OrderItem item = new OrderItem();
        item.setProduct(product);
        item.setQuantity(2);
        item.setUnitPrice(product.getPrice());
        order.addOrderItem(item);

        entityManager.persist(order);
        entityManager.getTransaction().commit();
        entityManager.clear();

        // When loading without fetch
        Order loaded = entityManager.find(Order.class, order.getId());

        // Then - коллекция не инициализирована
        assertThat(Hibernate.isInitialized(loaded.getOrderItems())).isFalse();

        // Попытка доступа вне сессии вызовет исключение
        entityManager.close();

        assertThatThrownBy(() -> loaded.getOrderItems().size())
                .isInstanceOf(LazyInitializationException.class)
                .hasMessageContaining("could not initialize proxy");
    }

    @Test
    @DisplayName("Should initialize lazy collection before closing session")
    void shouldInitializeLazyCollectionBeforeClosing() {
        // Given
        entityManager.getTransaction().begin();

        Customer customer = new Customer();
        customer.setFirstName("John");
        customer.setLastName("Doe");
        customer.setEmail("john@example.com");
        entityManager.persist(customer);

        Product product = new Product();
        product.setSku("PROD-001");
        product.setName("Test Product");
        product.setPrice(new java.math.BigDecimal("99.99"));
        product.setStockQuantity(100);
        entityManager.persist(product);

        Order order = new Order();
        order.setOrderNumber("ORD-001");
        order.setCustomer(customer);
        order.setStatus(OrderStatus.PENDING);
        order.setOrderDate(LocalDateTime.now());

        OrderItem item = new OrderItem();
        item.setProduct(product);
        item.setQuantity(2);
        item.setUnitPrice(product.getPrice());
        order.addOrderItem(item);

        entityManager.persist(order);
        entityManager.getTransaction().commit();
        entityManager.clear();

        // When
        Order loaded = entityManager.find(Order.class, order.getId());

        // Инициализируем коллекцию перед закрытием сессии
        Hibernate.initialize(loaded.getOrderItems());
        loaded.getOrderItems().size(); // Принудительная загрузка

        // Then - коллекция инициализирована
        assertThat(Hibernate.isInitialized(loaded.getOrderItems())).isTrue();

        // Теперь можно безопасно закрыть сессию
        entityManager.close();

        // Доступ к коллекции должен работать
        assertThat(loaded.getOrderItems().size()).isEqualTo(1);
    }
}
