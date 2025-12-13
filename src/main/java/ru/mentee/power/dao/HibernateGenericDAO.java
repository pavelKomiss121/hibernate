package ru.mentee.power.dao;

import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import ru.mentee.power.exception.DataAccessException;

/**
 * Базовая реализация DAO с использованием Hibernate.
 */
@Slf4j
public class HibernateGenericDAO<T, ID> implements GenericDAO<T, ID> {
    private final SessionFactory sessionFactory;
    private final Class<T> entityClass;

    public HibernateGenericDAO(SessionFactory sessionFactory, Class<T> entityClass) {
        this.sessionFactory = sessionFactory;
        this.entityClass = entityClass;
    }

    @Override
    public T save(T entity) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();
            session.persist(entity);
            transaction.commit();
            log.debug("Сущность {} сохранена", entity);
            return entity;
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            log.error("Ошибка сохранения сущности", e);
            throw new DataAccessException("Не удалось сохранить сущность", e);
        }
    }

    @Override
    public Optional<T> findById(ID id) {
        try (Session session = sessionFactory.openSession()) {
            T entity = session.get(entityClass, id);
            return Optional.ofNullable(entity);
        } catch (Exception e) {
            log.error("Ошибка поиска сущности по ID: {}", id, e);
            throw new DataAccessException("Не удалось найти сущность", e);
        }
    }

    @Override
    public List<T> findAll() {
        try (Session session = sessionFactory.openSession()) {
            String hql = "FROM " + entityClass.getSimpleName();
            return session.createQuery(hql, entityClass).list();
        } catch (Exception e) {
            log.error("Ошибка получения всех сущностей", e);
            throw new DataAccessException("Не удалось получить список сущностей", e);
        }
    }

    @Override
    public T update(T entity) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();
            T mergedEntity = session.merge(entity);
            transaction.commit();
            log.debug("Сущность {} обновлена", entity);
            return mergedEntity;
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            log.error("Ошибка обновления сущности", e);
            throw new DataAccessException("Не удалось обновить сущность", e);
        }
    }

    @Override
    public void delete(T entity) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();
            if (!session.contains(entity)) {
                entity = session.merge(entity);
            }
            session.remove(entity);
            transaction.commit();
            log.debug("Сущность {} удалена", entity);
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            log.error("Ошибка удаления сущности", e);
            throw new DataAccessException("Не удалось удалить сущность", e);
        }
    }

    @Override
    public void deleteById(ID id) {
        findById(id).ifPresent(this::delete);
    }

    @Override
    public boolean existsById(ID id) {
        return findById(id).isPresent();
    }

    @Override
    public long count() {
        try (Session session = sessionFactory.openSession()) {
            String hql = "SELECT COUNT(*) FROM " + entityClass.getSimpleName();
            return session.createQuery(hql, Long.class).uniqueResult();
        } catch (Exception e) {
            log.error("Ошибка подсчета сущностей", e);
            throw new DataAccessException("Не удалось подсчитать сущности", e);
        }
    }

    /**
     * Выполнение операции в рамках существующей сессии.
     */
    protected <R> R executeInSession(SessionOperation<R> operation) {
        try (Session session = sessionFactory.openSession()) {
            return operation.execute(session);
        } catch (Exception e) {
            log.error("Ошибка выполнения операции в сессии", e);
            throw new DataAccessException("Ошибка операции с БД", e);
        }
    }

    /**
     * Выполнение операции в транзакции.
     */
    protected <R> R executeInTransaction(TransactionalOperation<R> operation) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();
            R result = operation.execute(session);
            transaction.commit();
            return result;
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw new DataAccessException("Ошибка транзакционной операции", e);
        }
    }

    @FunctionalInterface
    protected interface SessionOperation<R> {
        R execute(Session session) throws Exception;
    }

    @FunctionalInterface
    protected interface TransactionalOperation<R> {
        R execute(Session session) throws Exception;
    }
}
