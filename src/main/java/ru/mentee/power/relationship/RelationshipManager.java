package ru.mentee.power.relationship;

import java.util.Collection;

/**
 * Менеджер связей между сущностями.
 */
public interface RelationshipManager {

    /**
     * Создать двунаправленную связь.
     * @param parent родительская сущность
     * @param child дочерняя сущность
     */
    <P, C> void linkBidirectional(P parent, C child);

    /**
     * Удалить двунаправленную связь.
     * @param parent родительская сущность
     * @param child дочерняя сущность
     */
    <P, C> void unlinkBidirectional(P parent, C child);

    /**
     * Инициализировать lazy коллекцию.
     * @param entity сущность
     * @param propertyName имя свойства
     * @return инициализированная коллекция
     */
    <T> Collection<T> initializeCollection(Object entity, String propertyName);

    /**
     * Загрузить сущность с указанными ассоциациями.
     * @param entityClass класс сущности
     * @param id идентификатор
     * @param fetchPaths пути для загрузки
     * @return сущность с загруженными ассоциациями
     */
    <T> T loadWithAssociations(Class<T> entityClass, Object id, String... fetchPaths);
}
