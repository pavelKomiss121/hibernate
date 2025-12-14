package ru.mentee.power.relationship;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.hibernate.Hibernate;
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
 * Тест связей и каскадных операций.
 */
class RelationshipTest {

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
    @DisplayName("Should cascade save order items")
    void shouldCascadeSaveOrderItems() {
        // Given
        Customer customer = createTestCustomer();
        entityManager.getTransaction().begin();
        entityManager.persist(customer);
        entityManager.flush();

        Product product1 = createTestProduct("PROD-001");
        Product product2 = createTestProduct("PROD-002");
        entityManager.persist(product1);
        entityManager.persist(product2);
        entityManager.flush();

        Order order = new Order();
        order.setOrderNumber("ORD-001");
        order.setCustomer(customer);
        order.setStatus(OrderStatus.PENDING);
        order.setOrderDate(LocalDateTime.now());

        OrderItem item1 = createOrderItem(product1, 2);
        OrderItem item2 = createOrderItem(product2, 1);

        order.addOrderItem(item1);
        order.addOrderItem(item2);

        // When
        Order saved = entityManager.merge(order);
        entityManager.flush();
        entityManager.getTransaction().commit();
        entityManager.clear();

        // Then
        Order loaded = entityManager.find(Order.class, saved.getId());
        assertThat(loaded.getOrderItems()).hasSize(2);
        assertThat(loaded.getOrderItems())
                .extracting(OrderItem::getProduct)
                .extracting(Product::getSku)
                .containsExactlyInAnyOrder("PROD-001", "PROD-002");
    }

    @Test
    @DisplayName("Should handle orphan removal")
    void shouldHandleOrphanRemoval() {
        // Given
        entityManager.getTransaction().begin();
        Order order = createOrderWithItems();
        entityManager.persist(order);
        entityManager.flush();

        Long removedItemId = order.getOrderItems().get(0).getId();

        // When
        order.removeOrderItem(order.getOrderItems().get(0));
        entityManager.flush();
        entityManager.getTransaction().commit();
        entityManager.clear();

        // Then
        OrderItem removed = entityManager.find(OrderItem.class, removedItemId);
        assertThat(removed).isNull();

        Order reloaded = entityManager.find(Order.class, order.getId());
        assertThat(reloaded.getOrderItems()).hasSize(1);
    }

    @Test
    @DisplayName("Should avoid N+1 with JOIN FETCH")
    void shouldAvoidNPlusOneWithJoinFetch() {
        // Given
        entityManager.getTransaction().begin();
        createTestData();
        entityManager.getTransaction().commit();
        entityManager.clear();

        // When
        java.util.List<Order> orders =
                entityManager
                        .createQuery(
                                """
			SELECT DISTINCT o FROM RelationshipOrder o
			LEFT JOIN FETCH o.orderItems oi
			LEFT JOIN FETCH oi.product
			WHERE o.status = :status
			""",
                                Order.class)
                        .setParameter("status", OrderStatus.PENDING)
                        .getResultList();

        // Then - проверяем что все загружено
        assertThat(orders).isNotEmpty();
        orders.forEach(
                order -> {
                    assertThat(Hibernate.isInitialized(order.getOrderItems())).isTrue();
                    order.getOrderItems()
                            .forEach(
                                    item ->
                                            assertThat(Hibernate.isInitialized(item.getProduct()))
                                                    .isTrue());
                });
    }

    // Вспомогательные методы
    private Customer createTestCustomer() {
        Customer customer = new Customer();
        customer.setFirstName("John");
        customer.setLastName("Doe");
        customer.setEmail("john@example.com");
        return customer;
    }

    private Product createTestProduct(String sku) {
        Product product = new Product();
        product.setSku(sku);
        product.setName("Test Product " + sku);
        product.setPrice(new BigDecimal("99.99"));
        product.setStockQuantity(100);
        return product;
    }

    private OrderItem createOrderItem(Product product, Integer quantity) {
        OrderItem item = new OrderItem();
        item.setProduct(product);
        item.setQuantity(quantity);
        item.setUnitPrice(product.getPrice());
        return item;
    }

    private Order createOrderWithItems() {
        Customer customer = createTestCustomer();
        entityManager.persist(customer);

        Product product1 = createTestProduct("PROD-001");
        Product product2 = createTestProduct("PROD-002");
        entityManager.persist(product1);
        entityManager.persist(product2);

        Order order = new Order();
        order.setOrderNumber("ORD-001");
        order.setCustomer(customer);
        order.setStatus(OrderStatus.PENDING);
        order.setOrderDate(LocalDateTime.now());

        OrderItem item1 = createOrderItem(product1, 2);
        OrderItem item2 = createOrderItem(product2, 1);

        order.addOrderItem(item1);
        order.addOrderItem(item2);

        return order;
    }

    private void createTestData() {
        Customer customer = createTestCustomer();
        entityManager.persist(customer);

        Product product1 = createTestProduct("PROD-001");
        Product product2 = createTestProduct("PROD-002");
        entityManager.persist(product1);
        entityManager.persist(product2);

        Order order1 = new Order();
        order1.setOrderNumber("ORD-001");
        order1.setCustomer(customer);
        order1.setStatus(OrderStatus.PENDING);
        order1.setOrderDate(LocalDateTime.now());
        order1.addOrderItem(createOrderItem(product1, 2));

        Order order2 = new Order();
        order2.setOrderNumber("ORD-002");
        order2.setCustomer(customer);
        order2.setStatus(OrderStatus.PENDING);
        order2.setOrderDate(LocalDateTime.now());
        order2.addOrderItem(createOrderItem(product2, 1));

        entityManager.persist(order1);
        entityManager.persist(order2);
    }
}
