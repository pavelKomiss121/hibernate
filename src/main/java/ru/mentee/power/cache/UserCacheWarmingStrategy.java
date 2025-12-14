package ru.mentee.power.cache;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import ru.mentee.power.entity.relationship.User;

/**
 * Стратегия прогрева кэша для пользователей.
 */
@Slf4j
public class UserCacheWarmingStrategy implements CacheWarmingStrategy {

    @Override
    public void warmUp(SessionFactory sessionFactory) {
        log.info("Warming up User cache...");
        try (Session session = sessionFactory.openSession()) {
            // Загружаем активных пользователей
            session.createQuery("FROM RelationshipUser u WHERE u.active = true", User.class)
                    .setMaxResults(50)
                    .setCacheable(true)
                    .list();
            log.info("User cache warmed up");
        }
    }

    @Override
    public int getPriority() {
        return 2;
    }

    @Override
    public boolean shouldWarmUp() {
        return true;
    }
}
