package ru.mentee.power.repository;

import jakarta.persistence.criteria.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import ru.mentee.power.dto.CustomerStatistics;
import ru.mentee.power.dto.ProductPriceInfo;
import ru.mentee.power.dto.ProductSearchCriteria;
import ru.mentee.power.entity.relationship.Customer;
import ru.mentee.power.entity.relationship.Order;
import ru.mentee.power.entity.relationship.OrderStatus;
import ru.mentee.power.entity.relationship.Product;
import ru.mentee.power.entity.relationship.User;

/**
 * Repository с Criteria API запросами.
 */
@Slf4j
public class HibernateCriteriaRepository {

    private final SessionFactory sessionFactory;

    public HibernateCriteriaRepository(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    /**
     * Простой Criteria запрос.
     */
    public List<User> findActiveUsersCriteria() {
        try (Session session = sessionFactory.openSession()) {
            CriteriaBuilder cb = session.getCriteriaBuilder();
            CriteriaQuery<User> query = cb.createQuery(User.class);
            Root<User> user = query.from(User.class);

            query.select(user).orderBy(cb.desc(user.get("id")));

            return session.createQuery(query).list();
        }
    }

    /**
     * Criteria с множественными условиями.
     */
    public List<Product> findProductsByCriteria(ProductSearchCriteria searchCriteria) {
        try (Session session = sessionFactory.openSession()) {
            CriteriaBuilder cb = session.getCriteriaBuilder();
            CriteriaQuery<Product> query = cb.createQuery(Product.class);
            Root<Product> product = query.from(Product.class);

            List<Predicate> predicates = new ArrayList<>();

            // Динамическое построение условий
            if (searchCriteria.getMinPrice() != null) {
                predicates.add(
                        cb.greaterThanOrEqualTo(
                                product.get("price"), searchCriteria.getMinPrice()));
            }

            if (searchCriteria.getMaxPrice() != null) {
                predicates.add(
                        cb.lessThanOrEqualTo(product.get("price"), searchCriteria.getMaxPrice()));
            }

            if (searchCriteria.getNamePattern() != null) {
                predicates.add(
                        cb.like(
                                cb.lower(product.get("name")),
                                "%" + searchCriteria.getNamePattern().toLowerCase() + "%"));
            }

            if (searchCriteria.getMinQuantity() != null) {
                predicates.add(
                        cb.greaterThanOrEqualTo(
                                product.get("stockQuantity"), searchCriteria.getMinQuantity()));
            }

            if (!predicates.isEmpty()) {
                query.where(cb.and(predicates.toArray(new Predicate[0])));
            }

            query.select(product).orderBy(cb.asc(product.get("price")));

            return session.createQuery(query).list();
        }
    }

    /**
     * Criteria с JOIN.
     */
    public List<Order> findOrdersWithJoinCriteria(OrderStatus status) {
        try (Session session = sessionFactory.openSession()) {
            CriteriaBuilder cb = session.getCriteriaBuilder();
            CriteriaQuery<Order> query = cb.createQuery(Order.class);
            Root<Order> order = query.from(Order.class);

            // JOIN FETCH для избежания N+1
            order.fetch("orderItems", JoinType.LEFT);
            order.fetch("customer", JoinType.LEFT);

            query.select(order)
                    .distinct(true)
                    .where(cb.equal(order.get("status"), status))
                    .orderBy(cb.desc(order.get("orderDate")));

            return session.createQuery(query).list();
        }
    }

    /**
     * Criteria с агрегацией и группировкой.
     */
    public List<CustomerStatistics> getCustomerStatisticsCriteria() {
        try (Session session = sessionFactory.openSession()) {
            CriteriaBuilder cb = session.getCriteriaBuilder();
            CriteriaQuery<Object[]> query = cb.createQuery(Object[].class);

            Root<Customer> customer = query.from(Customer.class);
            Join<Customer, Order> orders = customer.join("orders", JoinType.LEFT);

            // Создаем проекцию - возвращаем Object[] для ручного создания объектов
            query.multiselect(
                    customer.get("id"),
                    cb.concat(cb.concat(customer.get("firstName"), " "), customer.get("lastName")),
                    cb.count(orders.get("id")),
                    cb.coalesce(cb.sum(orders.get("totalAmount")), BigDecimal.ZERO),
                    cb.coalesce(cb.avg(orders.<BigDecimal>get("totalAmount")), 0.0),
                    cb.max(orders.get("orderDate")));

            // Группировка и условия
            query.groupBy(customer.get("id"), customer.get("firstName"), customer.get("lastName"));

            query.having(cb.greaterThan(cb.count(orders.get("id")), 0L));

            query.orderBy(cb.desc(cb.sum(orders.get("totalAmount"))));

            // Преобразуем результаты в CustomerStatistics
            List<Object[]> results = session.createQuery(query).list();
            return results.stream()
                    .map(
                            row ->
                                    new CustomerStatistics(
                                            (Long) row[0],
                                            (String) row[1],
                                            ((Number) row[2]).longValue(),
                                            (BigDecimal) row[3],
                                            BigDecimal.valueOf(((Number) row[4]).doubleValue()),
                                            (java.time.LocalDateTime) row[5]))
                    .toList();
        }
    }

    /**
     * Criteria с подзапросом.
     */
    public List<Product> findProductsAboveAveragePriceCriteria() {
        try (Session session = sessionFactory.openSession()) {
            CriteriaBuilder cb = session.getCriteriaBuilder();
            CriteriaQuery<Product> query = cb.createQuery(Product.class);
            Root<Product> product = query.from(Product.class);

            // Создаем подзапрос - avg возвращает Double
            Subquery<Double> avgPriceSubquery = query.subquery(Double.class);
            Root<Product> subProduct = avgPriceSubquery.from(Product.class);
            avgPriceSubquery.select(cb.avg(subProduct.<BigDecimal>get("price")));

            // Основной запрос - преобразуем BigDecimal в double для сравнения
            Expression<Double> productPriceAsDouble =
                    product.<BigDecimal>get("price").as(Double.class);
            query.select(product)
                    .where(cb.greaterThan(productPriceAsDouble, avgPriceSubquery))
                    .orderBy(cb.desc(product.get("price")));

            return session.createQuery(query).list();
        }
    }

    /**
     * Criteria с CASE выражением.
     */
    public List<ProductPriceInfo> getProductPriceCategoriesCriteria() {
        try (Session session = sessionFactory.openSession()) {
            CriteriaBuilder cb = session.getCriteriaBuilder();
            CriteriaQuery<ProductPriceInfo> query = cb.createQuery(ProductPriceInfo.class);
            Root<Product> product = query.from(Product.class);

            // CASE выражение - используем явное приведение типов
            CriteriaBuilder.Case<Object> caseExpression = cb.selectCase();
            caseExpression
                    .when(
                            cb.lessThan(product.get("price"), BigDecimal.valueOf(100)),
                            cb.literal("Budget"))
                    .when(
                            cb.lessThan(product.get("price"), BigDecimal.valueOf(500)),
                            cb.literal("Standard"))
                    .when(
                            cb.lessThan(product.get("price"), BigDecimal.valueOf(1000)),
                            cb.literal("Premium"));
            Expression<String> priceCategory =
                    caseExpression.otherwise(cb.literal("Luxury")).as(String.class);

            query.multiselect(
                    product.get("name"), product.get("price"), priceCategory.alias("category"));

            query.orderBy(cb.asc(product.get("price")));

            return session.createQuery(query).list();
        }
    }
}
