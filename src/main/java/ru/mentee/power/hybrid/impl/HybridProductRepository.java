package ru.mentee.power.hybrid.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import ru.mentee.power.dto.Page;
import ru.mentee.power.dto.Pageable;
import ru.mentee.power.dto.SearchCriteria;
import ru.mentee.power.entity.relationship.Product;
import ru.mentee.power.hybrid.HybridRepository;
import ru.mentee.power.hybrid.TechnologyChoice;

/**
 * Гибридный репозиторий для продуктов.
 */
@Slf4j
public class HybridProductRepository implements HybridRepository<Product, Long> {

    private final SessionFactory sessionFactory;
    private final DataSource dataSource;

    public HybridProductRepository(SessionFactory sessionFactory, DataSource dataSource) {
        this.sessionFactory = sessionFactory;
        this.dataSource = dataSource;
    }

    // ========== Hibernate-based методы ==========

    @Override
    public Product save(Product product) {
        try (Session session = sessionFactory.openSession()) {
            Transaction tx = session.beginTransaction();
            try {
                if (product.getId() == null) {
                    session.persist(product);
                } else {
                    product = session.merge(product);
                }
                tx.commit();
                return product;
            } catch (Exception e) {
                tx.rollback();
                throw e;
            }
        }
    }

    @Override
    public Optional<Product> findById(Long id) {
        try (Session session = sessionFactory.openSession()) {
            return Optional.ofNullable(session.get(Product.class, id));
        }
    }

    @Override
    public List<Product> findAll() {
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery("FROM RelationshipProduct", Product.class).list();
        }
    }

    @Override
    public void delete(Product entity) {
        try (Session session = sessionFactory.openSession()) {
            Transaction tx = session.beginTransaction();
            try {
                session.remove(session.contains(entity) ? entity : session.merge(entity));
                tx.commit();
            } catch (Exception e) {
                tx.rollback();
                throw e;
            }
        }
    }

    // ========== JDBC-based методы ==========

    @Override
    public void bulkInsert(List<Product> products) {
        String sql =
                """
                INSERT INTO products (sku, name, description, price, stock_quantity, created_at)
                VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                """;

        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = dataSource.getConnection();
            conn.setAutoCommit(false);
            ps = conn.prepareStatement(sql);

            for (int i = 0; i < products.size(); i++) {
                Product product = products.get(i);
                ps.setString(1, product.getSku());
                ps.setString(2, product.getName());
                ps.setString(3, product.getDescription());
                ps.setBigDecimal(4, product.getPrice());
                ps.setObject(5, product.getStockQuantity());
                ps.addBatch();

                // Execute batch every 1000 records
                if ((i + 1) % 1000 == 0) {
                    ps.executeBatch();
                    ps.clearBatch();
                }
            }

            // Execute remaining
            ps.executeBatch();
            conn.commit();
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    log.error("Error rolling back transaction", ex);
                }
            }
            throw new RuntimeException("Bulk insert failed", e);
        } finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException e) {
                    log.error("Error closing PreparedStatement", e);
                }
            }
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    log.error("Error closing Connection", e);
                }
            }
        }

        // Инвалидируем кэш после bulk операции
        sessionFactory.getCache().evictRegion("ru.mentee.power.entity.relationship.Product");
    }

    @Override
    public int bulkUpdate(String updateQuery, Object... params) {
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(updateQuery)) {

            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }

            int updated = ps.executeUpdate();

            // Инвалидируем кэш после bulk операции
            sessionFactory.getCache().evictRegion("ru.mentee.power.entity.relationship.Product");

            return updated;
        } catch (SQLException e) {
            throw new RuntimeException("Bulk update failed", e);
        }
    }

    @Override
    public <R> List<R> executeComplexQuery(String sql, RowMapper<R> mapper, Object... params) {
        List<R> results = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }

            try (ResultSet rs = ps.executeQuery()) {
                int rowNum = 0;
                while (rs.next()) {
                    results.add(mapper.mapRow(rs, rowNum++));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Complex query execution failed", e);
        }
        return results;
    }

    // ========== Гибридные методы ==========

    @Override
    public Page<Product> findWithComplexCriteria(SearchCriteria criteria, Pageable pageable) {
        // Для сложных критериев используем JDBC
        if (criteria.getFilters() != null && !criteria.getFilters().isEmpty()) {
            return findWithJDBC(criteria, pageable);
        }

        // Для простых случаев используем Hibernate
        try (Session session = sessionFactory.openSession()) {
            String hql = "FROM RelationshipProduct";
            var query = session.createQuery(hql, Product.class);

            // Применяем пагинацию
            int offset = pageable.getPageNumber() * pageable.getPageSize();
            query.setFirstResult(offset);
            query.setMaxResults(pageable.getPageSize());

            List<Product> content = query.list();

            // Подсчет общего количества
            Long totalElements =
                    (Long)
                            session.createQuery("SELECT COUNT(*) FROM RelationshipProduct")
                                    .uniqueResult();

            return new Page<>(
                    content,
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    totalElements != null ? totalElements : 0);
        }
    }

    private Page<Product> findWithJDBC(SearchCriteria criteria, Pageable pageable) {
        StringBuilder sql = new StringBuilder("SELECT * FROM products WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (criteria.getFilters() != null) {
            for (var entry : criteria.getFilters().entrySet()) {
                sql.append(" AND ").append(entry.getKey()).append(" = ?");
                params.add(entry.getValue());
            }
        }

        // Подсчет общего количества
        String countSql = "SELECT COUNT(*) FROM (" + sql + ") as count_query";
        long totalElements = 0;
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(countSql)) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    totalElements = rs.getLong(1);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Count query failed", e);
        }

        // Пагинация
        int offset = pageable.getPageNumber() * pageable.getPageSize();
        sql.append(" LIMIT ? OFFSET ?");
        params.add(pageable.getPageSize());
        params.add(offset);

        List<Product> content =
                executeComplexQuery(
                        sql.toString(),
                        (rs, rowNum) -> {
                            Product product = new Product();
                            product.setId(rs.getLong("id"));
                            product.setSku(rs.getString("sku"));
                            product.setName(rs.getString("name"));
                            product.setDescription(rs.getString("description"));
                            product.setPrice(rs.getBigDecimal("price"));
                            product.setStockQuantity(rs.getObject("stock_quantity", Integer.class));
                            return product;
                        },
                        params.toArray());

        return new Page<>(content, pageable.getPageNumber(), pageable.getPageSize(), totalElements);
    }

    @Override
    public TechnologyChoice getTechnologyChoiceFor(String operation) {
        return switch (operation.toLowerCase()) {
            case "save", "findbyid", "findall", "delete" -> TechnologyChoice.HIBERNATE;
            case "bulkinsert", "bulkupdate", "complexquery" -> TechnologyChoice.JDBC;
            default -> TechnologyChoice.HYBRID;
        };
    }
}
