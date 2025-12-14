package ru.mentee.power.util;

import java.util.Collection;
import org.hibernate.Hibernate;
import org.hibernate.collection.spi.PersistentCollection;

/**
 * Утилитный класс для работы со связями.
 */
public class RelationshipUtils {

    /**
     * Безопасная инициализация lazy proxy.
     */
    public static <T> T initializeProxy(T proxy) {
        if (proxy != null && !Hibernate.isInitialized(proxy)) {
            Hibernate.initialize(proxy);
        }
        return proxy;
    }

    /**
     * Проверка загружена ли коллекция.
     */
    public static boolean isCollectionInitialized(Collection<?> collection) {
        return collection != null && Hibernate.isInitialized(collection);
    }

    /**
     * Безопасное получение размера коллекции.
     */
    public static int getCollectionSize(Collection<?> collection) {
        if (collection instanceof PersistentCollection) {
            PersistentCollection pc = (PersistentCollection) collection;
            if (pc.wasInitialized()) {
                return collection.size();
            } else {
                // Для @LazyCollection(LazyCollectionOption.EXTRA)
                // size() не загрузит всю коллекцию
                return collection.size();
            }
        }
        return collection != null ? collection.size() : 0;
    }
}
