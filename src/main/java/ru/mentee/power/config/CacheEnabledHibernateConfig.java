package ru.mentee.power.config;

import java.util.Properties;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Environment;
import ru.mentee.power.entity.relationship.Address;
import ru.mentee.power.entity.relationship.Customer;
import ru.mentee.power.entity.relationship.Order;
import ru.mentee.power.entity.relationship.OrderItem;
import ru.mentee.power.entity.relationship.Product;
import ru.mentee.power.entity.relationship.User;
import ru.mentee.power.entity.relationship.UserProfile;

/**
 * Конфигурация Hibernate с настройкой кэширования.
 */
@Slf4j
public class CacheEnabledHibernateConfig {

    private final DatabaseConfig dbConfig;

    public CacheEnabledHibernateConfig(DatabaseConfig dbConfig) {
        this.dbConfig = dbConfig;
    }

    /**
     * Создание SessionFactory с настроенным кэшированием.
     */
    public SessionFactory buildSessionFactory() {
        Properties settings = new Properties();

        // Настройки подключения к БД
        String jdbcUrl = dbConfig.getJdbcUrl() != null ? dbConfig.getJdbcUrl() : "";
        String driver =
                dbConfig.getDriverClassName() != null
                        ? dbConfig.getDriverClassName()
                        : "org.postgresql.Driver";
        String dialect;

        // Определяем драйвер и диалект по URL
        if (jdbcUrl.startsWith("jdbc:h2:")) {
            driver = "org.h2.Driver";
            dialect = "org.hibernate.dialect.H2Dialect";
        } else if (jdbcUrl.startsWith("jdbc:postgresql:")) {
            driver = "org.postgresql.Driver";
            dialect = "org.hibernate.dialect.PostgreSQLDialect";
        } else {
            if (driver.contains("h2")) {
                dialect = "org.hibernate.dialect.H2Dialect";
            } else {
                dialect = "org.hibernate.dialect.PostgreSQLDialect";
            }
        }

        settings.put(Environment.DRIVER, driver);
        settings.put(Environment.URL, jdbcUrl);

        String username = dbConfig.getUsername();
        String password = dbConfig.getPassword();
        if (jdbcUrl.startsWith("jdbc:h2:")) {
            username = "sa";
            password = "";
        } else {
            if (username == null || username.isEmpty()) {
                username = System.getenv("DB_USER") != null ? System.getenv("DB_USER") : "sa";
            }
            if (password == null) {
                password = System.getenv("DB_PASSWORD") != null ? System.getenv("DB_PASSWORD") : "";
            }
        }

        settings.put(Environment.USER, username);
        settings.put(Environment.PASS, password);

        // Настройки Hibernate
        settings.put(Environment.DIALECT, dialect);
        settings.put(
                Environment.HBM2DDL_AUTO,
                dbConfig.getHbm2ddlAuto() != null ? dbConfig.getHbm2ddlAuto() : "update");
        settings.put(Environment.SHOW_SQL, String.valueOf(dbConfig.isShowSql()));
        settings.put(Environment.FORMAT_SQL, "true");

        // Настройки кэша второго уровня (только для PostgreSQL)
        if (!jdbcUrl.startsWith("jdbc:h2:")) {
            settings.put(Environment.USE_SECOND_LEVEL_CACHE, "true");
            settings.put(Environment.USE_QUERY_CACHE, "true");

            // Провайдер кэша - EhCache
            settings.put(
                    Environment.CACHE_REGION_FACTORY,
                    "org.hibernate.cache.jcache.internal.JCacheRegionFactory");
            settings.put(
                    "hibernate.javax.cache.provider", "org.ehcache.jsr107.EhcacheCachingProvider");
            settings.put("hibernate.javax.cache.uri", "classpath:ehcache.xml");

            // Статистика кэша
            settings.put(Environment.GENERATE_STATISTICS, "true");
            settings.put(Environment.USE_STRUCTURED_CACHE, "true");
        } else {
            settings.put(Environment.USE_SECOND_LEVEL_CACHE, "false");
            settings.put(Environment.USE_QUERY_CACHE, "false");
        }

        // Batch fetching для оптимизации
        settings.put(Environment.DEFAULT_BATCH_FETCH_SIZE, "16");
        settings.put(Environment.BATCH_VERSIONED_DATA, "true");

        // Настройки пула соединений
        if (!jdbcUrl.startsWith("jdbc:h2:")) {
            settings.put(
                    Environment.CONNECTION_PROVIDER,
                    "org.hibernate.hikaricp.internal.HikariCPConnectionProvider");
            settings.put("hibernate.hikari.minimumIdle", "5");
            settings.put("hibernate.hikari.maximumPoolSize", "20");
        } else {
            settings.put(
                    Environment.CONNECTION_PROVIDER,
                    "org.hibernate.engine.jdbc.connections.internal.DriverManagerConnectionProviderImpl");
        }

        StandardServiceRegistry registry =
                new StandardServiceRegistryBuilder().applySettings(settings).build();

        try {
            MetadataSources sources = new MetadataSources(registry);

            // Добавляем сущности
            sources.addAnnotatedClass(User.class);
            sources.addAnnotatedClass(UserProfile.class);
            sources.addAnnotatedClass(Address.class);
            sources.addAnnotatedClass(Product.class);
            sources.addAnnotatedClass(Order.class);
            sources.addAnnotatedClass(OrderItem.class);
            sources.addAnnotatedClass(Customer.class);

            Metadata metadata = sources.getMetadataBuilder().build();

            SessionFactory sessionFactory = metadata.getSessionFactoryBuilder().build();

            log.info("SessionFactory создана с поддержкой кэширования");
            logCacheConfiguration(sessionFactory);

            return sessionFactory;

        } catch (Exception e) {
            StandardServiceRegistryBuilder.destroy(registry);
            throw new RuntimeException("Ошибка создания SessionFactory", e);
        }
    }

    private void logCacheConfiguration(SessionFactory sf) {
        log.info(
                "Second level cache enabled: {}",
                sf.getSessionFactoryOptions().isSecondLevelCacheEnabled());
        log.info("Query cache enabled: {}", sf.getSessionFactoryOptions().isQueryCacheEnabled());
        log.info("Statistics enabled: {}", sf.getStatistics().isStatisticsEnabled());
    }
}
