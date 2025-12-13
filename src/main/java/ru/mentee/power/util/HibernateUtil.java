package ru.mentee.power.util;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import ru.mentee.power.entity.mp_177.Order;
import ru.mentee.power.entity.mp_177.Product;
import ru.mentee.power.entity.mp_177.User;

/**
 * Утилита для создания и управления SessionFactory.
 * Паттерн Singleton для единственного экземпляра SessionFactory.
 */
@Slf4j
public class HibernateUtil {
    private static StandardServiceRegistry registry;
    private static SessionFactory sessionFactory;

    /**
     * Получить экземпляр SessionFactory.
     * Создается только один раз при первом обращении.
     */
    public static SessionFactory getSessionFactory() {
        if (sessionFactory == null) {
            try {
                // Создание реестра сервисов из hibernate.cfg.xml
                registry =
                        new StandardServiceRegistryBuilder()
                                .configure() // по умолчанию ищет hibernate.cfg.xml
                                .build();

                // Создание метаданных
                MetadataSources sources = new MetadataSources(registry);

                // Добавление аннотированных классов
                sources.addAnnotatedClass(User.class);
                sources.addAnnotatedClass(Order.class);
                sources.addAnnotatedClass(Product.class);

                Metadata metadata = sources.getMetadataBuilder().build();

                // Создание SessionFactory
                sessionFactory = metadata.getSessionFactoryBuilder().build();

                log.info("SessionFactory успешно создана");
            } catch (Exception e) {
                log.error("Ошибка создания SessionFactory", e);
                if (registry != null) {
                    StandardServiceRegistryBuilder.destroy(registry);
                }
                throw new ExceptionInInitializerError(e);
            }
        }
        return sessionFactory;
    }

    /**
     * Закрыть SessionFactory при завершении приложения.
     */
    public static void shutdown() {
        if (registry != null) {
            StandardServiceRegistryBuilder.destroy(registry);
        }
    }
}
