package ru.mentee.power.hybrid;

import java.util.List;
import java.util.Optional;
import ru.mentee.power.dto.Page;
import ru.mentee.power.dto.Pageable;
import ru.mentee.power.dto.SearchCriteria;
import ru.mentee.power.hybrid.impl.RowMapper;

/**
 * Гибридный репозиторий с оптимальным выбором технологии.
 *
 * @param <T> тип сущности
 * @param <ID> тип идентификатора
 */
public interface HybridRepository<T, ID> {

    // ========== Hibernate-based методы ==========

    /**
     * Сохранить сущность через Hibernate.
     */
    T save(T entity);

    /**
     * Найти по ID через Hibernate.
     */
    Optional<T> findById(ID id);

    /**
     * Найти все через Hibernate.
     */
    List<T> findAll();

    /**
     * Удалить сущность через Hibernate.
     */
    void delete(T entity);

    // ========== JDBC-based методы ==========

    /**
     * Массовая вставка через JDBC.
     */
    void bulkInsert(List<T> entities);

    /**
     * Массовое обновление через JDBC.
     */
    int bulkUpdate(String updateQuery, Object... params);

    /**
     * Выполнить сложный запрос через JDBC.
     */
    <R> List<R> executeComplexQuery(String sql, RowMapper<R> mapper, Object... params);

    // ========== Гибридные методы ==========

    /**
     * Поиск со сложными критериями.
     */
    Page<T> findWithComplexCriteria(SearchCriteria criteria, Pageable pageable);

    /**
     * Получить выбор технологии для операции.
     */
    TechnologyChoice getTechnologyChoiceFor(String operation);
}
