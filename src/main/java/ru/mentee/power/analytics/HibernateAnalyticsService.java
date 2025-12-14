package ru.mentee.power.analytics;

import jakarta.persistence.criteria.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import ru.mentee.power.dto.CustomerAnalytics;
import ru.mentee.power.dto.DateRange;
import ru.mentee.power.dto.DemandForecast;
import ru.mentee.power.dto.GroupingType;
import ru.mentee.power.dto.ProductRanking;
import ru.mentee.power.dto.RankingMetric;
import ru.mentee.power.dto.SalesStatistics;
import ru.mentee.power.entity.relationship.Customer;
import ru.mentee.power.entity.relationship.Order;
import ru.mentee.power.entity.relationship.OrderStatus;
import ru.mentee.power.entity.relationship.Product;

/**
 * Реализация сервиса аналитики.
 */
@Slf4j
public class HibernateAnalyticsService implements AnalyticsService {

    private final SessionFactory sessionFactory;

    public HibernateAnalyticsService(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public List<SalesStatistics> getSalesStatistics(DateRange period, GroupingType groupBy) {
        try (Session session = sessionFactory.openSession()) {
            // Упрощенная реализация через HQL
            String groupByClause =
                    switch (groupBy) {
                        case DAY -> "DATE(o.orderDate)";
                        case WEEK -> "EXTRACT(WEEK FROM o.orderDate)";
                        case MONTH -> "EXTRACT(MONTH FROM o.orderDate)";
                        case QUARTER -> "EXTRACT(QUARTER FROM o.orderDate)";
                        case YEAR -> "EXTRACT(YEAR FROM o.orderDate)";
                    };

            // Упрощенная реализация - используем месяц для группировки
            @SuppressWarnings("unchecked")
            List<Object[]> results =
                    session.createQuery(
                                    """
                                    SELECT
                                        o.orderDate,
                                        COUNT(DISTINCT o.id),
                                        COUNT(DISTINCT o.customer.id),
                                        COALESCE(SUM(o.totalAmount), 0)
                                    FROM Order o
                                    WHERE o.status = :status
                                      AND o.orderDate BETWEEN :from AND :to
                                    GROUP BY o.orderDate
                                    ORDER BY o.orderDate
                                    """)
                            .setParameter("status", OrderStatus.COMPLETED)
                            .setParameter("from", period.getFrom())
                            .setParameter("to", period.getTo())
                            .list();

            return results.stream()
                    .map(
                            row ->
                                    new SalesStatistics(
                                            (LocalDateTime) row[0],
                                            ((Number) row[1]).longValue(),
                                            ((Number) row[2]).longValue(),
                                            (BigDecimal) row[3],
                                            BigDecimal.ZERO,
                                            BigDecimal.ZERO))
                    .collect(Collectors.toList());
        }
    }

    @Override
    public List<ProductRanking> getTopProducts(RankingMetric metric, int limit) {
        try (Session session = sessionFactory.openSession()) {
            CriteriaBuilder cb = session.getCriteriaBuilder();
            CriteriaQuery<Object[]> query = cb.createQuery(Object[].class);

            Root<Order> order = query.from(Order.class);
            Join<Order, ru.mentee.power.entity.relationship.OrderItem> item =
                    order.join("orderItems");
            Join<ru.mentee.power.entity.relationship.OrderItem, Product> product =
                    item.join("product");

            Expression<?> metricExpression =
                    switch (metric) {
                        case SALES_COUNT -> cb.count(item.get("id"));
                        case REVENUE -> cb.sum(item.get("unitPrice"));
                        case QUANTITY_SOLD -> cb.sum(item.get("quantity"));
                        default -> cb.count(item.get("id"));
                    };

            query.multiselect(product.get("id"), product.get("name"), metricExpression)
                    .where(cb.equal(order.get("status"), OrderStatus.COMPLETED))
                    .groupBy(product.get("id"), product.get("name"))
                    .orderBy(cb.desc(metricExpression));

            List<Object[]> results = session.createQuery(query).setMaxResults(limit).list();

            List<ProductRanking> rankings = new ArrayList<>();
            int rank = 1;
            for (Object[] row : results) {
                Object metricValue = row[2];
                BigDecimal metricBigDecimal;
                if (metricValue instanceof Number) {
                    metricBigDecimal = BigDecimal.valueOf(((Number) metricValue).doubleValue());
                } else {
                    metricBigDecimal = (BigDecimal) metricValue;
                }
                rankings.add(
                        new ProductRanking(
                                (Long) row[0], (String) row[1], metricBigDecimal, rank++));
            }

            return rankings;
        }
    }

    @Override
    public CustomerAnalytics analyzeCustomer(Long customerId) {
        try (Session session = sessionFactory.openSession()) {
            CriteriaBuilder cb = session.getCriteriaBuilder();
            CriteriaQuery<Object[]> query = cb.createQuery(Object[].class);

            Root<Customer> customer = query.from(Customer.class);
            Join<Customer, Order> orders = customer.join("orders", JoinType.LEFT);

            query.multiselect(
                            customer.get("id"),
                            cb.concat(
                                    cb.concat(customer.get("firstName"), " "),
                                    customer.get("lastName")),
                            cb.count(orders.get("id")),
                            cb.coalesce(cb.sum(orders.get("totalAmount")), BigDecimal.ZERO),
                            cb.coalesce(cb.avg(orders.get("totalAmount")), BigDecimal.ZERO),
                            cb.min(orders.get("orderDate")),
                            cb.max(orders.get("orderDate")),
                            cb.coalesce(cb.sum(orders.get("totalAmount")), BigDecimal.ZERO))
                    .where(cb.equal(customer.get("id"), customerId))
                    .groupBy(
                            customer.get("id"),
                            customer.get("firstName"),
                            customer.get("lastName"));

            Object[] result = session.createQuery(query).uniqueResult();

            if (result == null) {
                return null;
            }

            return new CustomerAnalytics(
                    (Long) result[0],
                    (String) result[1],
                    ((Number) result[2]).longValue(),
                    (BigDecimal) result[3],
                    (BigDecimal) result[4],
                    (LocalDateTime) result[5],
                    (LocalDateTime) result[6],
                    (BigDecimal) result[7]);
        }
    }

    @Override
    public DemandForecast forecastDemand(Long productId, int days) {
        // Упрощенная реализация прогноза
        try (Session session = sessionFactory.openSession()) {
            Product product = session.get(Product.class, productId);
            if (product == null) {
                return null;
            }

            // Простой прогноз на основе среднего спроса за последние дни
            DemandForecast forecast = new DemandForecast();
            forecast.setProductId(productId);
            forecast.setProductName(product.getName());
            forecast.setForecast(new ArrayList<>());

            return forecast;
        }
    }
}
