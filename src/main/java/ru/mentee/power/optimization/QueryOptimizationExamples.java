package ru.mentee.power.optimization;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import ru.mentee.power.dto.ProductSummary;
import ru.mentee.power.entity.relationship.Order;
import ru.mentee.power.entity.relationship.OrderStatus;

/**
 * Оптимизированные запросы.
 */
@Slf4j
public class QueryOptimizationExamples {

    private final SessionFactory sessionFactory;

    public QueryOptimizationExamples(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    /**
     * Использование подсказок (hints) для оптимизации.
     */
    public List<Order> findOrdersWithHints() {
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery(
                            "FROM Order o JOIN FETCH o.orderItems WHERE o.status = :status",
                            Order.class)
                    .setParameter("status", OrderStatus.PENDING)
                    .setHint("org.hibernate.readOnly", true) // Read-only режим
                    .setHint("org.hibernate.fetchSize", 50) // Размер выборки
                    .setHint("org.hibernate.cacheable", true) // Кэширование запроса
                    .setHint("org.hibernate.cacheRegion", "orders") // Регион кэша
                    .setHint("org.hibernate.comment", "Find pending orders") // SQL комментарий
                    .list();
        }
    }

    /**
     * Проекции для уменьшения объема данных.
     */
    public List<ProductSummary> getProductSummaries() {
        try (Session session = sessionFactory.openSession()) {
            jakarta.persistence.criteria.CriteriaBuilder cb = session.getCriteriaBuilder();
            jakarta.persistence.criteria.CriteriaQuery<ProductSummary> query =
                    cb.createQuery(ProductSummary.class);
            jakarta.persistence.criteria.Root<ru.mentee.power.entity.relationship.Product> product =
                    query.from(ru.mentee.power.entity.relationship.Product.class);

            // Выбираем только нужные поля
            query.multiselect(
                    product.get("id"),
                    product.get("name"),
                    product.get("price"),
                    product.get("stockQuantity"));

            return session.createQuery(query).list();
        }
    }

    /**
     * Batch fetching для коллекций.
     */
    public void demonstrateBatchFetching() {
        try (Session session = sessionFactory.openSession()) {
            // Включаем batch fetching для сессии
            session.setJdbcBatchSize(25);

            List<ru.mentee.power.entity.relationship.Customer> customers =
                    session.createQuery(
                                    "FROM Customer c",
                                    ru.mentee.power.entity.relationship.Customer.class)
                            .list();

            // Batch fetching загрузит заказы группами по 25
            for (ru.mentee.power.entity.relationship.Customer customer : customers) {
                log.info(
                        "Customer {} has {} orders",
                        customer.getEmail(),
                        customer.getOrders().size());
            }
        }
    }
}
