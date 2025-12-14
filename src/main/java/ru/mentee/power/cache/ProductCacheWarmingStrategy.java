package ru.mentee.power.cache;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import ru.mentee.power.entity.relationship.Product;

/**
 * Стратегия прогрева кэша для продуктов.
 */
@Slf4j
public class ProductCacheWarmingStrategy implements CacheWarmingStrategy {

    @Override
    public void warmUp(SessionFactory sessionFactory) {
        log.info("Warming up Product cache...");
        try (Session session = sessionFactory.openSession()) {
            // Загружаем популярные продукты
            session.createQuery("FROM RelationshipProduct p ORDER BY p.id", Product.class)
                    .setMaxResults(100)
                    .setCacheable(true)
                    .list();
            log.info("Product cache warmed up");
        }
    }

    @Override
    public int getPriority() {
        return 1; // Высокий приоритет
    }

    @Override
    public boolean shouldWarmUp() {
        return true;
    }
}
