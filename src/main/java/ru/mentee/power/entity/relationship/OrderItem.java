package ru.mentee.power.entity.relationship;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Formula;

/**
 * Сущность позиции заказа с ManyToOne связью.
 */
@Entity(name = "RelationshipOrderItem")
@Table(
        name = "order_items",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_order_product",
                    columnNames = {"order_id", "product_id"})
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Многие позиции к одному заказу
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "order_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_order_item_order"))
    private Order order;

    // Многие позиции к одному продукту
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "product_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_order_item_product"))
    private Product product;

    @Column(name = "quantity", nullable = false)
    @Min(1)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "discount_percent")
    @Min(0)
    @Max(100)
    @Builder.Default
    private Integer discountPercent = 0;

    @Formula("unit_price * quantity * (1 - COALESCE(discount_percent, 0) / 100.0)")
    private BigDecimal lineTotal;

    // Копируем цену из продукта при создании
    @PrePersist
    public void prePersist() {
        if (unitPrice == null && product != null) {
            unitPrice = product.getPrice();
        }
    }
}
