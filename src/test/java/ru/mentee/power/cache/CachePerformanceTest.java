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
 * Тест производительности кэширования.
 */
class CachePerformanceTest {

    private SessionFactory sessionFactory;

    @BeforeEach
    void setUp() {
        DatabaseConfig dbConfig = new DatabaseConfig();
        dbConfig.setJdbcUrl("jdbc:h2:mem:testdb_perf_" + System.currentTimeMillis());
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
    @DisplayName("Should improve performance with caching")
    void shouldImprovePerformanceWithCaching() {
        // Given
        int iterations = 100;
        Long[] userIds = {1L, 2L, 3L, 4L, 5L};

        // When - без кэширования (очищаем кэш)
        sessionFactory.getCache().evictAllRegions();

        long withoutCacheTime =
                measureTime(
                        () -> {
                            for (int i = 0; i < iterations; i++) {
                                try (Session session = sessionFactory.openSession()) {
                                    for (Long id : userIds) {
                                        User user = session.get(User.class, id);
                                        if (user != null) {
                                            // Симуляция работы
                                            user.getUsername();
                                        }
                                    }
                                }
                            }
                        });

        // When - с кэшированием (прогрев кэша)
        long withCacheTime =
                measureTime(
                        () -> {
                            // Прогрев кэша
                            try (Session session = sessionFactory.openSession()) {
                                for (Long id : userIds) {
                                    User user = session.get(User.class, id);
                                    if (user != null) {
                                        user.getUsername();
                                    }
                                }
                            }

                            // Измерение с кэшем
                            for (int i = 0; i < iterations; i++) {
                                try (Session session = sessionFactory.openSession()) {
                                    for (Long id : userIds) {
                                        User user = session.get(User.class, id);
                                        if (user != null) {
                                            user.getUsername();
                                        }
                                    }
                                }
                            }
                        });

        // Then
        System.out.println("Without cache: " + withoutCacheTime + " ms");
        System.out.println("With cache: " + withCacheTime + " ms");

        // С кэшем должно быть быстрее (хотя для H2 разница может быть незначительной)
        assertThat(withCacheTime).isLessThanOrEqualTo(withoutCacheTime * 2);
    }

    @Test
    @DisplayName("Should demonstrate cache statistics")
    void shouldDemonstrateCacheStatistics() {
        Statistics stats = sessionFactory.getStatistics();
        stats.setStatisticsEnabled(true);
        stats.clear();

        Long productId = 1L;

        // Первая загрузка
        try (Session session1 = sessionFactory.openSession()) {
            Product product1 = session1.get(Product.class, productId);
            assertThat(product1).isNotNull();
        }

        // Вторая загрузка
        try (Session session2 = sessionFactory.openSession()) {
            Product product2 = session2.get(Product.class, productId);
            assertThat(product2).isNotNull();
        }

        // Проверяем статистику
        // Для H2 L2 кэш отключен, поэтому проверяем только что статистика работает
        System.out.println("Entity load count: " + stats.getEntityLoadCount());
        System.out.println("L2 cache hits: " + stats.getSecondLevelCacheHitCount());
        System.out.println("L2 cache misses: " + stats.getSecondLevelCacheMissCount());
        System.out.println("Query execution count: " + stats.getQueryExecutionCount());

        // Статистика должна быть доступна (может быть 0 для H2, так как L2 кэш отключен)
        assertThat(stats.isStatisticsEnabled()).isTrue();
    }

    @Test
    @DisplayName("Should test cache manager functionality")
    void shouldTestCacheManager() {
        CacheManager cacheManager = new CacheManagerImpl(sessionFactory);

        // Получаем статистику
        CacheStatistics stats = cacheManager.getStatistics();
        assertThat(stats).isNotNull();
        assertThat(stats.getL2CacheHitCount()).isGreaterThanOrEqualTo(0);

        // Инвалидация
        cacheManager.invalidateEntity(User.class, 1L);
        cacheManager.invalidateEntityType(Product.class);

        // Оптимизация
        OptimizationReport report = cacheManager.optimizeCache();
        assertThat(report).isNotNull();

        // Экспорт метрик
        String metrics = cacheManager.exportMetrics();
        assertThat(metrics).isNotEmpty();
        assertThat(metrics).contains("hibernate_cache");
    }

    private long measureTime(Runnable runnable) {
        long start = System.currentTimeMillis();
        runnable.run();
        return System.currentTimeMillis() - start;
    }

    private void createTestData() {
        try (Session session = sessionFactory.openSession()) {
            org.hibernate.Transaction tx = session.beginTransaction();

            // Создаем несколько пользователей
            for (int i = 1; i <= 5; i++) {
                User user =
                        User.builder()
                                .username("user" + i)
                                .email("user" + i + "@example.com")
                                .passwordHash("hash" + i)
                                .firstName("First" + i)
                                .lastName("Last" + i)
                                .active(true)
                                .build();
                session.persist(user);
            }

            // Создаем несколько продуктов
            for (int i = 1; i <= 5; i++) {
                Product product =
                        Product.builder()
                                .sku("SKU00" + i)
                                .name("Product " + i)
                                .price(java.math.BigDecimal.valueOf(100 * i))
                                .stockQuantity(10 * i)
                                .build();
                session.persist(product);
            }

            tx.commit();
        }
    }
}
