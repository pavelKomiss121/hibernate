package ru.mentee.power.dao;

import java.util.List;
import java.util.Optional;

/**
 * Базовый интерфейс для работы с сущностями через Hibernate.
 */
public interface GenericDAO<T, ID> {
    /**
     * Сохранить новую сущность.
     * @param entity сущность для сохранения
     * @return сохраненная сущность с присвоенным ID
     */
    T save(T entity);

    /**
     * Найти сущность по ID.
     * @param id идентификатор
     * @return Optional с сущностью или empty
     */
    Optional<T> findById(ID id);

    /**
     * Получить все сущности.
     * @return список всех сущностей
     */
    List<T> findAll();

    /**
     * Обновить сущность.
     * @param entity сущность с изменениями
     * @return обновленная сущность
     */
    T update(T entity);

    /**
     * Удалить сущность.
     * @param entity сущность для удаления
     */
    void delete(T entity);

    /**
     * Удалить сущность по ID.
     * @param id идентификатор
     */
    void deleteById(ID id);

    /**
     * Проверить существование по ID.
     * @param id идентификатор
     * @return true если существует
     */
    boolean existsById(ID id);

    /**
     * Получить количество сущностей.
     * @return общее количество
     */
    long count();
}
