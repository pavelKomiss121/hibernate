package ru.mentee.power.repository;

import jakarta.persistence.criteria.*;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import ru.mentee.power.dto.Page;
import ru.mentee.power.dto.Pageable;
import ru.mentee.power.dto.Sort;
import ru.mentee.power.specification.Specification;

/**
 * Repository с использованием спецификаций.
 */
@Slf4j
public class HibernateSpecificationRepository<T> {

    private final SessionFactory sessionFactory;
    private final Class<T> entityClass;

    public HibernateSpecificationRepository(SessionFactory sessionFactory, Class<T> entityClass) {
        this.sessionFactory = sessionFactory;
        this.entityClass = entityClass;
    }

    public List<T> findAll(Specification<T> spec) {
        try (Session session = sessionFactory.openSession()) {
            CriteriaBuilder cb = session.getCriteriaBuilder();
            CriteriaQuery<T> query = cb.createQuery(entityClass);
            Root<T> root = query.from(entityClass);

            if (spec != null) {
                Predicate predicate = spec.toPredicate(root, query, cb);
                query.where(predicate);
            }

            return session.createQuery(query).list();
        }
    }

    public Page<T> findAll(Specification<T> spec, Pageable pageable) {
        try (Session session = sessionFactory.openSession()) {
            CriteriaBuilder cb = session.getCriteriaBuilder();

            // Count query
            CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
            Root<T> countRoot = countQuery.from(entityClass);
            countQuery.select(cb.count(countRoot));

            if (spec != null) {
                Predicate predicate = spec.toPredicate(countRoot, countQuery, cb);
                countQuery.where(predicate);
            }

            Long total = session.createQuery(countQuery).uniqueResult();

            // Data query
            CriteriaQuery<T> dataQuery = cb.createQuery(entityClass);
            Root<T> dataRoot = dataQuery.from(entityClass);

            if (spec != null) {
                Predicate predicate = spec.toPredicate(dataRoot, dataQuery, cb);
                dataQuery.where(predicate);
            }

            // Sorting
            if (pageable.getSort() != null && pageable.getSort().getOrders() != null) {
                List<Order> orders = new ArrayList<>();
                for (Sort.Order order : pageable.getSort().getOrders()) {
                    if (order.isAscending()) {
                        orders.add(cb.asc(dataRoot.get(order.getProperty())));
                    } else {
                        orders.add(cb.desc(dataRoot.get(order.getProperty())));
                    }
                }
                dataQuery.orderBy(orders);
            }

            List<T> content =
                    session.createQuery(dataQuery)
                            .setFirstResult(pageable.getPageNumber() * pageable.getPageSize())
                            .setMaxResults(pageable.getPageSize())
                            .list();

            return new Page<>(
                    content,
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    total != null ? total : 0L);
        }
    }
}
