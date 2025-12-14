package ru.mentee.power.search;

import jakarta.persistence.criteria.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import ru.mentee.power.dto.FacetedSearchResult;
import ru.mentee.power.dto.Page;
import ru.mentee.power.dto.Pageable;
import ru.mentee.power.dto.SearchCriteria;
import ru.mentee.power.entity.relationship.Product;

/**
 * Реализация поискового сервиса для продуктов.
 */
@Slf4j
public class ProductSearchService implements SearchService<Product> {

    private final SessionFactory sessionFactory;

    public ProductSearchService(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public Page<Product> search(SearchCriteria criteria, Pageable pageable) {
        try (Session session = sessionFactory.openSession()) {
            CriteriaBuilder cb = session.getCriteriaBuilder();

            // Count query
            CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
            Root<Product> countRoot = countQuery.from(Product.class);
            countQuery.select(cb.count(countRoot));

            // Data query
            CriteriaQuery<Product> dataQuery = cb.createQuery(Product.class);
            Root<Product> dataRoot = dataQuery.from(Product.class);

            // Построение предикатов из критериев
            List<Predicate> predicates = buildPredicates(criteria, cb, dataRoot);

            if (!predicates.isEmpty()) {
                Predicate predicate = cb.and(predicates.toArray(new Predicate[0]));
                countQuery.where(predicate);
                dataQuery.where(predicate);
            }

            // Сортировка
            if (pageable.getSort() != null && pageable.getSort().getOrders() != null) {
                List<Order> orders = new ArrayList<>();
                for (ru.mentee.power.dto.Sort.Order order : pageable.getSort().getOrders()) {
                    if (order.isAscending()) {
                        orders.add(cb.asc(dataRoot.get(order.getProperty())));
                    } else {
                        orders.add(cb.desc(dataRoot.get(order.getProperty())));
                    }
                }
                dataQuery.orderBy(orders);
            }

            Long total = session.createQuery(countQuery).uniqueResult();

            List<Product> content =
                    session.createQuery(dataQuery)
                            .setFirstResult(pageable.getPageNumber() * pageable.getPageSize())
                            .setMaxResults(pageable.getPageSize())
                            .setHint("org.hibernate.readOnly", true)
                            .setHint("org.hibernate.fetchSize", 50)
                            .list();

            return new Page<>(
                    content,
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    total != null ? total : 0L);
        }
    }

    @Override
    public List<Product> fullTextSearch(String query, String... fields) {
        try (Session session = sessionFactory.openSession()) {
            CriteriaBuilder cb = session.getCriteriaBuilder();
            CriteriaQuery<Product> criteriaQuery = cb.createQuery(Product.class);
            Root<Product> root = criteriaQuery.from(Product.class);

            List<Predicate> predicates = new ArrayList<>();
            String searchPattern = "%" + query.toLowerCase() + "%";

            for (String field : fields) {
                predicates.add(cb.like(cb.lower(root.get(field)), searchPattern));
            }

            criteriaQuery
                    .select(root)
                    .where(cb.or(predicates.toArray(new Predicate[0])))
                    .orderBy(cb.desc(root.get("id")));

            return session.createQuery(criteriaQuery)
                    .setHint("org.hibernate.readOnly", true)
                    .list();
        }
    }

    @Override
    public FacetedSearchResult<Product> facetedSearch(SearchCriteria criteria) {
        try (Session session = sessionFactory.openSession()) {
            // Основной поиск
            Page<Product> results = search(criteria, Pageable.of(0, 20));

            // Фасеты по цене
            Map<String, List<FacetedSearchResult.Facet>> facets = new HashMap<>();
            facets.put("price", getPriceFacets(session));

            return new FacetedSearchResult<>(results.getContent(), facets);
        }
    }

    @Override
    public List<String> autocomplete(String prefix, String field, int limit) {
        try (Session session = sessionFactory.openSession()) {
            CriteriaBuilder cb = session.getCriteriaBuilder();
            CriteriaQuery<String> query = cb.createQuery(String.class);
            Root<Product> root = query.from(Product.class);

            query.select(root.get(field).as(String.class))
                    .where(cb.like(cb.lower(root.get(field)), prefix.toLowerCase() + "%"))
                    .distinct(true)
                    .orderBy(cb.asc(root.get(field)));

            return session.createQuery(query).setMaxResults(limit).list();
        }
    }

    private List<Predicate> buildPredicates(
            SearchCriteria criteria, CriteriaBuilder cb, Root<Product> root) {
        List<Predicate> predicates = new ArrayList<>();

        if (criteria.getFilters() != null) {
            for (Map.Entry<String, Object> entry : criteria.getFilters().entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();

                if (value != null) {
                    switch (key) {
                        case "minPrice":
                            predicates.add(
                                    cb.greaterThanOrEqualTo(
                                            root.get("price"), (java.math.BigDecimal) value));
                            break;
                        case "maxPrice":
                            predicates.add(
                                    cb.lessThanOrEqualTo(
                                            root.get("price"), (java.math.BigDecimal) value));
                            break;
                        case "name":
                            predicates.add(
                                    cb.like(
                                            cb.lower(root.get("name")),
                                            "%" + value.toString().toLowerCase() + "%"));
                            break;
                    }
                }
            }
        }

        if (criteria.getSearchText() != null && !criteria.getSearchText().isEmpty()) {
            List<Predicate> textPredicates = new ArrayList<>();
            String searchPattern = "%" + criteria.getSearchText().toLowerCase() + "%";
            textPredicates.add(cb.like(cb.lower(root.get("name")), searchPattern));
            if (root.get("description") != null) {
                textPredicates.add(cb.like(cb.lower(root.get("description")), searchPattern));
            }
            predicates.add(cb.or(textPredicates.toArray(new Predicate[0])));
        }

        return predicates;
    }

    private List<FacetedSearchResult.Facet> getPriceFacets(Session session) {
        // Упрощенная реализация фасетов
        List<FacetedSearchResult.Facet> facets = new ArrayList<>();
        facets.add(new FacetedSearchResult.Facet("0-100", 0L));
        facets.add(new FacetedSearchResult.Facet("100-500", 0L));
        facets.add(new FacetedSearchResult.Facet("500+", 0L));
        return facets;
    }
}
