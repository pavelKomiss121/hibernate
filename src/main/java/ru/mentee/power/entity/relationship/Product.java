package ru.mentee.power.entity.relationship;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.NaturalId;

/**
 * Продукт с ManyToMany связью к категориям.
 * Сущность продукта с READ_ONLY кэшированием.
 */
@Entity(name = "RelationshipProduct")
@Table(name = "products")
@Cacheable
@Cache(
        usage = CacheConcurrencyStrategy.READ_ONLY,
        region = "ru.mentee.power.entity.relationship.Product",
        include = "non-lazy")
@Immutable
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sku", unique = true, nullable = false, length = 50)
    @NaturalId
    private String sku;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    @Basic(fetch = FetchType.LAZY)
    private String description;

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "stock_quantity")
    private Integer stockQuantity;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
