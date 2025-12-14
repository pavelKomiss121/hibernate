package ru.mentee.power.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import ru.mentee.power.dto.CustomerStatistics;
import ru.mentee.power.dto.Page;
import ru.mentee.power.entity.relationship.Order;
import ru.mentee.power.entity.relationship.OrderStatus;
import ru.mentee.power.entity.relationship.Product;
import ru.mentee.power.entity.relationship.User;

/**
 * Repository с HQL запросами.
 */
@Slf4j
public class HibernateHQLRepository {

    private final SessionFactory sessionFactory;

    public HibernateHQLRepository(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    /**
     * Простой HQL запрос.
     */
    public List<User> findActiveUsers() {
        try (Session session = sessionFactory.openSession()) {
            // HQL использует имена классов и полей, не таблиц
            return session.createQuery("FROM RelationshipUser u ORDER BY u.id DESC", User.class)
                    .list();
        }
    }

    /**
     * HQL с параметрами.
     */
    public List<Product> findProductsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery(
                            """
                            FROM RelationshipProduct p
                            WHERE p.price BETWEEN :minPrice AND :maxPrice
                            ORDER BY p.price ASC
                            """,
                            Product.class)
                    .setParameter("minPrice", minPrice)
                    .setParameter("maxPrice", maxPrice)
                    .list();
        }
    }

    /**
     * HQL с JOIN.
     */
    public List<Order> findOrdersWithItems() {
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery(
                            """
                            SELECT DISTINCT o
                            FROM RelationshipOrder o
                            JOIN FETCH o.orderItems oi
                            JOIN FETCH oi.product p
                            WHERE o.status = :status
                              AND o.totalAmount > :minAmount
                            ORDER BY o.orderDate DESC
                            """,
                            Order.class)
                    .setParameter("status", OrderStatus.PENDING)
                    .setParameter("minAmount", new BigDecimal("100"))
                    .list();
        }
    }

    /**
     * HQL с агрегацией.
     */
    public List<CustomerStatistics> getCustomerStatistics() {
        try (Session session = sessionFactory.openSession()) {
            // AVG возвращает Double, поэтому возвращаем Object[] и создаем объекты вручную
            @SuppressWarnings("unchecked")
            List<Object[]> results =
                    session.createQuery(
                                    """
                                    SELECT
                                        c.id,
                                        c.firstName || ' ' || c.lastName,
                                        COUNT(o.id),
                                        COALESCE(SUM(o.totalAmount), 0),
                                        COALESCE(AVG(o.totalAmount), 0),
                                        MAX(o.orderDate)
                                    FROM RelationshipCustomer c
                                    LEFT JOIN c.orders o
                                    GROUP BY c.id, c.firstName, c.lastName
                                    HAVING COUNT(o.id) > 0
                                    ORDER BY SUM(o.totalAmount) DESC
                                    """,
                                    Object[].class)
                            .list();

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
     * HQL с подзапросом.
     */
    public List<Product> findProductsAboveAveragePrice() {
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery(
                            """
                            FROM RelationshipProduct p
                            WHERE p.price > (
                                SELECT AVG(p2.price)
                                FROM RelationshipProduct p2
                            )
                            ORDER BY p.price DESC
                            """,
                            Product.class)
                    .list();
        }
    }

    /**
     * HQL с CASE выражением.
     */
    public List<Object[]> getProductPriceCategories() {
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery(
                            """
                            SELECT
                                p.name,
                                p.price,
                                CASE
                                    WHEN p.price < 100 THEN 'Budget'
                                    WHEN p.price < 500 THEN 'Standard'
                                    WHEN p.price < 1000 THEN 'Premium'
                                    ELSE 'Luxury'
                                END as priceCategory
                            FROM RelationshipProduct p
                            ORDER BY p.price
                            """,
                            Object[].class)
                    .list();
        }
    }

    /**
     * HQL с пагинацией.
     */
    public Page<Product> findProductsPaginated(int pageNumber, int pageSize) {
        try (Session session = sessionFactory.openSession()) {
            // Сначала получаем общее количество
            Long totalElements =
                    session.createQuery("SELECT COUNT(p) FROM RelationshipProduct p", Long.class)
                            .uniqueResult();

            // Затем получаем страницу данных
            List<Product> content =
                    session.createQuery(
                                    "FROM RelationshipProduct p ORDER BY p.id DESC", Product.class)
                            .setFirstResult(pageNumber * pageSize)
                            .setMaxResults(pageSize)
                            .list();

            return new Page<>(content, pageNumber, pageSize, totalElements);
        }
    }

    /**
     * HQL UPDATE запрос.
     */
    public int updateProductPrices(String sku, BigDecimal percentage) {
        try (Session session = sessionFactory.openSession()) {
            Transaction tx = session.beginTransaction();
            int updatedCount =
                    session.createMutationQuery(
                                    """
                                    UPDATE RelationshipProduct p
                                    SET p.price = p.price * :multiplier
                                    WHERE p.sku = :sku
                                    """)
                            .setParameter("multiplier", BigDecimal.ONE.add(percentage))
                            .setParameter("sku", sku)
                            .executeUpdate();
            tx.commit();
            return updatedCount;
        }
    }

    /**
     * HQL DELETE запрос.
     */
    public int deleteInactiveUsers(LocalDate beforeDate) {
        try (Session session = sessionFactory.openSession()) {
            Transaction tx = session.beginTransaction();
            int deletedCount =
                    session.createMutationQuery(
                                    """
                                    DELETE FROM RelationshipUser u
                                    WHERE NOT EXISTS (
                                        SELECT 1 FROM RelationshipOrder o WHERE o.customer.id = u.id
                                    )
                                    """)
                            .executeUpdate();
            tx.commit();
            return deletedCount;
        }
    }
}
