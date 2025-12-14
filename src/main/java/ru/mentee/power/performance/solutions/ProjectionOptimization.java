package ru.mentee.power.performance.solutions;

import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;
import java.math.BigDecimal;
import java.util.List;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import ru.mentee.power.entity.relationship.Order;
import ru.mentee.power.entity.relationship.OrderStatus;

/**
 * Оптимизация через проекции для уменьшения объема данных.
 */
@Slf4j
public class ProjectionOptimization {

    private final SessionFactory sessionFactory;

    public ProjectionOptimization(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    /**
     * DTO для проекции.
     */
    @Value
    public static class OrderSummaryDTO {
        Long id;
        String orderNumber;
        BigDecimal totalAmount;
        String customerName;
        Long itemCount;

        public OrderSummaryDTO(
                Long id,
                String orderNumber,
                BigDecimal totalAmount,
                String customerName,
                Long itemCount) {
            this.id = id;
            this.orderNumber = orderNumber;
            this.totalAmount = totalAmount;
            this.customerName = customerName;
            this.itemCount = itemCount;
        }
    }

    /**
     * Загрузка только нужных данных через проекцию.
     */
    public List<OrderSummaryDTO> loadOrderSummaries() {
        try (Session session = sessionFactory.openSession()) {
            // Один оптимизированный запрос вместо загрузки полных сущностей
            return session.createQuery(
                            """
                    SELECT NEW ru.mentee.power.performance.solutions.ProjectionOptimization$OrderSummaryDTO(
                        o.id,
                        o.orderNumber,
                        o.totalAmount,
                        CONCAT(c.firstName, ' ', c.lastName),
                        COUNT(oi.id)
                    )
                    FROM RelationshipOrder o
                    JOIN o.customer c
                    LEFT JOIN o.orderItems oi
                    WHERE o.status = :status
                    GROUP BY o.id, o.orderNumber, o.totalAmount, c.firstName, c.lastName
                    """,
                            OrderSummaryDTO.class)
                    .setParameter("status", OrderStatus.PENDING)
                    .list();
        }
    }

    /**
     * Tuple проекция для динамических запросов.
     */
    @SuppressWarnings("unchecked")
    public List<Tuple> loadOrderStatistics() {
        try (Session session = sessionFactory.openSession()) {
            CriteriaBuilder cb = session.getCriteriaBuilder();
            CriteriaQuery<Tuple> query = cb.createTupleQuery();

            Root<Order> order = query.from(Order.class);
            var customer = order.join("customer");
            var items = order.join("orderItems", JoinType.LEFT);

            query.multiselect(
                    order.get("id").alias("orderId"),
                    order.get("orderDate").alias("date"),
                    customer.get("email").alias("customerEmail"),
                    cb.sum(cb.prod(items.get("quantity"), items.get("unitPrice")))
                            .alias("totalItems"),
                    cb.count(items).alias("itemCount"));

            query.groupBy(order.get("id"), order.get("orderDate"), customer.get("email"));

            return session.createQuery(query).list();
        }
    }
}
