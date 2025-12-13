package ru.mentee.power.dao;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.mentee.power.entity.mp_177.User;

/**
 * Тесты для HibernateUserDAO.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HibernateUserDAOTest {
    private static SessionFactory sessionFactory;
    private UserDAO userDAO;

    @BeforeAll
    void setUp() {
        // Создаем SessionFactory для тестов с H2
        Configuration configuration = new Configuration();
        configuration.setProperty("hibernate.connection.driver_class", "org.h2.Driver");
        // Создаем схему через INIT для H2
        configuration.setProperty(
                "hibernate.connection.url",
                "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;INIT=CREATE SCHEMA IF NOT EXISTS"
                        + " mentee_power;USER=sa;PASSWORD=");
        configuration.setProperty("hibernate.connection.username", "sa");
        configuration.setProperty("hibernate.connection.password", "");
        configuration.setProperty("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
        configuration.setProperty("hibernate.hbm2ddl.auto", "create-drop");
        configuration.setProperty("hibernate.show_sql", "true");
        configuration.addAnnotatedClass(User.class);
        sessionFactory = configuration.buildSessionFactory();
        userDAO = new HibernateUserDAO(sessionFactory);
    }

    @Test
    @DisplayName("Should save and retrieve user")
    void shouldSaveAndRetrieveUser() {
        // Given
        User user =
                User.builder()
                        .username("testuser")
                        .email("test@example.com")
                        .firstName("Test")
                        .lastName("User")
                        .active(true)
                        .build();

        // When
        User savedUser = userDAO.save(user);
        Optional<User> foundUser = userDAO.findById(savedUser.getId());

        // Then
        assertThat(savedUser.getId()).isNotNull();
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getUsername()).isEqualTo("testuser");
        assertThat(foundUser.get().getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should handle entity lifecycle correctly")
    void shouldHandleEntityLifecycle() {
        // Given - Transient state
        User user = User.builder().username("lifecycle").email("lifecycle@example.com").build();

        // When - Persistent state
        try (Session session = sessionFactory.openSession()) {
            Transaction tx = session.beginTransaction();
            assertThat(session.contains(user)).isFalse(); // Transient
            session.persist(user);
            assertThat(session.contains(user)).isTrue(); // Persistent
            tx.commit();
        }

        // Then - Detached state
        try (Session newSession = sessionFactory.openSession()) {
            assertThat(newSession.contains(user)).isFalse(); // Detached
            // Re-attach
            Transaction tx = newSession.beginTransaction();
            User mergedUser = newSession.merge(user);
            assertThat(newSession.contains(mergedUser)).isTrue(); // Persistent again
            tx.commit();
        }
    }

    @AfterAll
    void tearDown() {
        if (sessionFactory != null) {
            sessionFactory.close();
        }
    }
}
