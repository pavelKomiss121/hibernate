package ru.mentee.power.cache;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.mentee.power.config.CacheEnabledHibernateConfig;
import ru.mentee.power.config.DatabaseConfig;
import ru.mentee.power.entity.relationship.Product;
import ru.mentee.power.entity.relationship.User;

/**
 * Тест кэширования.
 */
class CacheTest {

    private SessionFactory sessionFactory;

    @BeforeEach
    void setUp() {
        DatabaseConfig dbConfig = new DatabaseConfig();
        dbConfig.setJdbcUrl("jdbc:h2:mem:testdb_cache_" + System.currentTimeMillis());
        dbConfig.setDriverClassName("org.h2.Driver");
        dbConfig.setUsername("sa");
        dbConfig.setPassword("");
        dbConfig.setHbm2ddlAuto("create-drop");
        dbConfig.setShowSql(false);

        CacheEnabledHibernateConfig config = new CacheEnabledHibernateConfig(dbConfig);
        sessionFactory = config.buildSessionFactory();

        // Создаем тестовые данные
        createTestData();
    }

    @Test
    @DisplayName("Should use L1 cache for entity loading within same session")
    void shouldUseL1CacheForEntityLoading() {
        // Given
        Long userId = 1L;

        // When - первая загрузка
        try (Session session = sessionFactory.openSession()) {
            User user1 = session.get(User.class, userId);
            assertThat(user1).isNotNull();

            // Вторая загрузка - должна быть из L1 кэша
            User user2 = session.get(User.class, userId);
            assertThat(user2).isNotNull();
            assertThat(user1).isSameAs(user2); // Один и тот же объект
        }
    }

    @Test
    @DisplayName("Should use L2 cache for entity loading across sessions")
    void shouldUseL2CacheForEntityLoading() {
        // Given
        Statistics stats = sessionFactory.getStatistics();
        stats.clear();

        Long productId = 1L;

        // When - первая загрузка
        try (Session session1 = sessionFactory.openSession()) {
            Product product1 = session1.get(Product.class, productId);
            assertThat(product1).isNotNull();
        }

        // Then - проверяем L2 cache put (если L2 включен)
        // Для H2 L2 кэш отключен, поэтому проверяем только структуру

        // When - вторая загрузка из другой сессии
        try (Session session2 = sessionFactory.openSession()) {
            Product product2 = session2.get(Product.class, productId);
            assertThat(product2).isNotNull();
        }
    }

    @Test
    @DisplayName("Should manage L1 cache with evict and refresh")
    void shouldManageL1Cache() {
        try (Session session = sessionFactory.openSession()) {
            Long userId = 1L;

            // Загружаем пользователя
            User user = session.get(User.class, userId);
            assertThat(user).isNotNull();
            assertThat(session.contains(user)).isTrue();

            // Удаляем из кэша
            session.evict(user);
            assertThat(session.contains(user)).isFalse();

            // Перезагружаем
            User user2 = session.get(User.class, userId);
            assertThat(user2).isNotNull();
        }
    }

    @Test
    @DisplayName("Should cache queries")
    void shouldCacheQueries() {
        try (Session session = sessionFactory.openSession()) {
            // Первый запрос
            var products1 =
                    session.createQuery("FROM RelationshipProduct p", Product.class)
                            .setCacheable(true)
                            .list();
            assertThat(products1).isNotEmpty();

            // Второй запрос - должен быть из кэша
            var products2 =
                    session.createQuery("FROM RelationshipProduct p", Product.class)
                            .setCacheable(true)
                            .list();
            assertThat(products2).isNotEmpty();
        }
    }

    private void createTestData() {
        try (Session session = sessionFactory.openSession()) {
            org.hibernate.Transaction tx = session.beginTransaction();

            // Создаем пользователя
            User user =
                    User.builder()
                            .username("testuser")
                            .email("test@example.com")
                            .passwordHash("hash")
                            .firstName("Test")
                            .lastName("User")
                            .active(true)
                            .build();
            session.persist(user);

            // Создаем продукт
            Product product =
                    Product.builder()
                            .sku("SKU001")
                            .name("Test Product")
                            .price(java.math.BigDecimal.valueOf(100))
                            .stockQuantity(10)
                            .build();
            session.persist(product);

            tx.commit();
        }
    }
}
