package ru.mentee.power.config;

import java.util.Properties;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import ru.mentee.power.entity.Order;
import ru.mentee.power.entity.Product;
import ru.mentee.power.entity.User;

/**
 * Программная конфигурация Hibernate без XML.
 */
@Slf4j
public class HibernateConfig {
    private final DatabaseConfig dbConfig;

    public HibernateConfig(DatabaseConfig dbConfig) {
        this.dbConfig = dbConfig;
    }

    /**
     * Создание SessionFactory с программной конфигурацией.
     */
    public SessionFactory buildSessionFactory() {
        try {
            Configuration configuration = new Configuration();

            // Настройки подключения
            Properties settings = new Properties();
            String jdbcUrl = dbConfig.getJdbcUrl();
            String driver = dbConfig.getDriverClassName();
            String dialect;

            // Определяем драйвер и диалект по URL (приоритет URL над driverClassName)
            if (jdbcUrl != null && jdbcUrl.startsWith("jdbc:h2:")) {
                driver = "org.h2.Driver";
                dialect = "org.hibernate.dialect.H2Dialect";
            } else if (jdbcUrl != null && jdbcUrl.startsWith("jdbc:postgresql:")) {
                driver = "org.postgresql.Driver";
                dialect = "org.hibernate.dialect.PostgreSQLDialect";
            } else {
                // Если URL не распознан, используем driverClassName или значение по умолчанию
                if (driver == null || driver.isEmpty() || driver.equals("org.postgresql.Driver")) {
                    driver = "org.postgresql.Driver";
                    dialect = "org.hibernate.dialect.PostgreSQLDialect";
                } else if (driver.contains("h2")) {
                    dialect = "org.hibernate.dialect.H2Dialect";
                } else {
                    dialect = "org.hibernate.dialect.PostgreSQLDialect";
                }
            }

            settings.put(Environment.DRIVER, driver);
            settings.put(Environment.URL, jdbcUrl);
            settings.put(Environment.USER, dbConfig.getUsername());
            settings.put(Environment.PASS, dbConfig.getPassword());

            // Настройки Hibernate
            settings.put(Environment.DIALECT, dialect);
            settings.put(Environment.SHOW_SQL, "true");
            settings.put(Environment.FORMAT_SQL, "true");
            settings.put(Environment.USE_SQL_COMMENTS, "true");

            // Настройки пула HikariCP
            settings.put(
                    Environment.CONNECTION_PROVIDER,
                    "org.hibernate.hikaricp.internal.HikariCPConnectionProvider");
            settings.put("hibernate.hikari.minimumIdle", "5");
            settings.put("hibernate.hikari.maximumPoolSize", "20");
            settings.put("hibernate.hikari.idleTimeout", "30000");
            settings.put("hibernate.hikari.connectionTimeout", "20000");

            // Кэширование (отключаем для H2, включаем для PostgreSQL)
            if (!jdbcUrl.startsWith("jdbc:h2:")) {
                settings.put(Environment.USE_SECOND_LEVEL_CACHE, "true");
                settings.put(
                        Environment.CACHE_REGION_FACTORY,
                        "org.hibernate.cache.jcache.internal.JCacheRegionFactory");
                settings.put(
                        "hibernate.javax.cache.provider",
                        "org.ehcache.jsr107.EhcacheCachingProvider");
            } else {
                settings.put(Environment.USE_SECOND_LEVEL_CACHE, "false");
            }

            // Batch операции
            settings.put(Environment.STATEMENT_BATCH_SIZE, "25");
            settings.put(Environment.ORDER_INSERTS, "true");
            settings.put(Environment.ORDER_UPDATES, "true");
            settings.put(Environment.BATCH_VERSIONED_DATA, "true");

            configuration.setProperties(settings);

            // Добавление сущностей
            configuration.addAnnotatedClass(User.class);
            configuration.addAnnotatedClass(Order.class);
            configuration.addAnnotatedClass(Product.class);

            return configuration.buildSessionFactory();
        } catch (Exception e) {
            log.error("Ошибка создания SessionFactory", e);
            throw new RuntimeException("Не удалось создать SessionFactory", e);
        }
    }
}
