package ru.mentee.power.config;

import java.util.Properties;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import ru.mentee.power.entity.*;

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
            String jdbcUrl = dbConfig.getJdbcUrl() != null ? dbConfig.getJdbcUrl() : "";
            String driver =
                    dbConfig.getDriverClassName() != null
                            ? dbConfig.getDriverClassName()
                            : "org.postgresql.Driver";
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
            String username = dbConfig.getUsername();
            String password = dbConfig.getPassword();
            // Для H2 используем дефолтные учетные данные если не указаны
            if (jdbcUrl != null && jdbcUrl.startsWith("jdbc:h2:")) {
                // Для H2 in-memory всегда используем "sa" и пустой пароль
                username = "sa";
                password = "";
            } else {
                // Для других БД используем переданные значения или значения по умолчанию
                if (username == null || username.isEmpty()) {
                    username = "sa";
                }
                if (password == null) {
                    password = "";
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
            settings.put(Environment.USE_SQL_COMMENTS, "true");

            // Валидация Bean Validation (опционально, можно включить при необходимости)
            // settings.put("jakarta.persistence.validation.mode", "callback");

            // Настройки пула HikariCP (отключаем для H2 in-memory, используем простой провайдер)
            if (!jdbcUrl.startsWith("jdbc:h2:")) {
                settings.put(
                        Environment.CONNECTION_PROVIDER,
                        "org.hibernate.hikaricp.internal.HikariCPConnectionProvider");
                settings.put("hibernate.hikari.minimumIdle", "5");
                settings.put("hibernate.hikari.maximumPoolSize", "20");
                settings.put("hibernate.hikari.idleTimeout", "30000");
                settings.put("hibernate.hikari.connectionTimeout", "20000");
            } else {
                // Для H2 явно используем стандартный провайдер
                // (DriverManagerConnectionProviderImpl)
                settings.put(
                        Environment.CONNECTION_PROVIDER,
                        "org.hibernate.engine.jdbc.connections.internal.DriverManagerConnectionProviderImpl");
            }

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

            // Новые сущности для практического задания
            configuration.addAnnotatedClass(OrderItem.class);
            configuration.addAnnotatedClass(Inventory.class);
            configuration.addAnnotatedClass(Customer.class);

            // Наследование - PaymentMethod
            configuration.addAnnotatedClass(PaymentMethod.class);
            configuration.addAnnotatedClass(CreditCard.class);
            configuration.addAnnotatedClass(PayPalAccount.class);

            // Наследование - Vehicle (временно отключено из-за проблем с H2 и зарезервированным
            // словом "year")
            // configuration.addAnnotatedClass(Vehicle.class);
            // configuration.addAnnotatedClass(Car.class);
            // configuration.addAnnotatedClass(Motorcycle.class);

            // Наследование - Employee
            configuration.addAnnotatedClass(Employee.class);
            configuration.addAnnotatedClass(Manager.class);
            configuration.addAnnotatedClass(Developer.class);

            return configuration.buildSessionFactory();
        } catch (Exception e) {
            log.error("Ошибка создания SessionFactory", e);
            throw new RuntimeException("Не удалось создать SessionFactory", e);
        }
    }
}
