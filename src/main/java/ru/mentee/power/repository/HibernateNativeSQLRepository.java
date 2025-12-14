package ru.mentee.power.repository;

import java.math.BigDecimal;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.type.StandardBasicTypes;
import ru.mentee.power.dto.ProductDTO;
import ru.mentee.power.entity.relationship.Product;

/**
 * Repository с нативными SQL запросами.
 */
@Slf4j
public class HibernateNativeSQLRepository {

    private final SessionFactory sessionFactory;

    public HibernateNativeSQLRepository(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    /**
     * Нативный SQL с маппингом на сущность.
     */
    public List<Product> findProductsNativeSQL() {
        try (Session session = sessionFactory.openSession()) {
            return session.createNativeQuery(
                            """
                            SELECT p.*
                            FROM products p
                            WHERE p.price > :minPrice
                            ORDER BY p.id DESC
                            """,
                            Product.class)
                    .setParameter("minPrice", new BigDecimal("100"))
                    .list();
        }
    }

    /**
     * Нативный SQL с кастомным маппингом.
     */
    @SuppressWarnings("unchecked")
    public List<ProductDTO> findProductDTOsNativeSQL() {
        try (Session session = sessionFactory.openSession()) {
            List<Object[]> results =
                    session.createNativeQuery(
                                    """
                                    SELECT
                                        p.id as id,
                                        p.name as name,
                                        p.price as price,
                                        'Category' as categoryName,
                                        COUNT(DISTINCT oi.order_id) as orderCount,
                                        COALESCE(SUM(oi.quantity), 0) as totalSold
                                    FROM products p
                                    LEFT JOIN order_items oi ON p.id = oi.product_id
                                    GROUP BY p.id, p.name, p.price
                                    ORDER BY totalSold DESC
                                    """)
                            .addScalar("id", StandardBasicTypes.LONG)
                            .addScalar("name", StandardBasicTypes.STRING)
                            .addScalar("price", StandardBasicTypes.BIG_DECIMAL)
                            .addScalar("categoryName", StandardBasicTypes.STRING)
                            .addScalar("orderCount", StandardBasicTypes.INTEGER)
                            .addScalar("totalSold", StandardBasicTypes.INTEGER)
                            .list();

            return results.stream()
                    .map(
                            row ->
                                    new ProductDTO(
                                            (Long) row[0],
                                            (String) row[1],
                                            (BigDecimal) row[2],
                                            (String) row[3],
                                            (Integer) row[4],
                                            (Integer) row[5]))
                    .toList();
        }
    }

    /**
     * Нативный SQL с именованными параметрами.
     */
    @SuppressWarnings("unchecked")
    public List<Object[]> getMonthlyStatistics(int year) {
        try (Session session = sessionFactory.openSession()) {
            return session.createNativeQuery(
                            """
                            SELECT
                                TO_CHAR(o.order_date, 'YYYY-MM') as month_str,
                                COUNT(DISTINCT o.id) as order_count,
                                COUNT(DISTINCT o.customer_id) as customer_count,
                                SUM(o.total_amount) as revenue
                            FROM orders o
                            WHERE EXTRACT(YEAR FROM o.order_date) = :year
                              AND o.status = 'COMPLETED'
                            GROUP BY TO_CHAR(o.order_date, 'YYYY-MM')
                            ORDER BY month_str
                            """)
                    .setParameter("year", year)
                    .list();
        }
    }

    /**
     * Stored procedure через нативный SQL.
     */
    public BigDecimal calculateCustomerLifetimeValue(Long customerId) {
        try (Session session = sessionFactory.openSession()) {
            // Для PostgreSQL используем функцию
            Object result =
                    session.createNativeQuery(
                                    """
                                    SELECT COALESCE(SUM(o.total_amount), 0)
                                    FROM orders o
                                    WHERE o.customer_id = :customerId
                                      AND o.status = 'COMPLETED'
                                    """)
                            .setParameter("customerId", customerId)
                            .uniqueResult();

            return result != null ? (BigDecimal) result : BigDecimal.ZERO;
        }
    }
}
