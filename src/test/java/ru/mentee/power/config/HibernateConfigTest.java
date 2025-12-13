package ru.mentee.power.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Тесты для конфигурации Hibernate.
 */
class HibernateConfigTest {
    @Test
    @DisplayName("Should create SessionFactory with programmatic configuration")
    void shouldCreateSessionFactoryProgrammatically() {
        // Given
        DatabaseConfig dbConfig = new DatabaseConfig();
        // Создаем схему через INIT для H2
        dbConfig.setJdbcUrl(
                "jdbc:h2:mem:test;INIT=CREATE SCHEMA IF NOT EXISTS mentee_power;USER=sa;PASSWORD=");
        dbConfig.setUsername("sa");
        dbConfig.setPassword("");

        HibernateConfig hibernateConfig = new HibernateConfig(dbConfig);

        // When
        SessionFactory sessionFactory = hibernateConfig.buildSessionFactory();

        // Then
        assertThat(sessionFactory).isNotNull();
        assertThat(sessionFactory.isClosed()).isFalse();

        // Проверяем, что можем создать сессию
        try (Session session = sessionFactory.openSession()) {
            assertThat(session.isOpen()).isTrue();
        }

        sessionFactory.close();
    }
}
