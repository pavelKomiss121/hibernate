package ru.mentee.power.config;

import org.hibernate.SessionFactory;
import ru.mentee.power.exception.ConfigurationException;

/**
 * Конфигуратор Hibernate.
 */
public interface HibernateConfigurator {
    /**
     * Создать SessionFactory с настройками по умолчанию.
     * @return настроенная SessionFactory
     */
    SessionFactory createSessionFactory();

    /**
     * Создать SessionFactory с кастомными настройками.
     * @param configFile путь к конфигурационному файлу
     * @return настроенная SessionFactory
     */
    SessionFactory createSessionFactory(String configFile);

    /**
     * Создать SessionFactory для тестов с H2.
     * @return SessionFactory для in-memory БД
     */
    SessionFactory createTestSessionFactory();

    /**
     * Валидировать конфигурацию.
     * @throws ConfigurationException если конфигурация невалидна
     */
    void validateConfiguration() throws ConfigurationException;
}
