package ru.mentee.power.entity.relationship;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

/**
 * Сущность заказа с OneToMany связью.
 */
@Entity(name = "RelationshipOrder")
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"customer", "orderItems"}) // Избегаем циклических ссылок
@EqualsAndHashCode(exclude = {"customer", "orderItems"})
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", unique = true, nullable = false, length = 50)
    private String orderNumber;

    // Многие заказы к одному клиенту
    @ManyToOne(fetch = FetchType.LAZY) // LAZY по умолчанию для @ManyToOne
    @JoinColumn(
            name = "customer_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_order_customer"))
    @NotFound(action = NotFoundAction.EXCEPTION) // Выбросить исключение если customer не найден
    private Customer customer;

    // Один заказ ко многим позициям
    @OneToMany(
            mappedBy = "order", // Поле в OrderItem, которое ссылается на Order
            cascade = CascadeType.ALL, // Каскадное сохранение/удаление
            orphanRemoval = true, // Удалять осиротевшие OrderItem
            fetch = FetchType.LAZY // LAZY по умолчанию для коллекций
            )
    @jakarta.persistence.OrderBy("id ASC") // Сортировка при загрузке
    @BatchSize(size = 25) // Оптимизация для batch загрузки
    @Fetch(FetchMode.SUBSELECT) // Загрузка через подзапрос вместо N+1
    @Builder.Default
    private List<OrderItem> orderItems = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrderStatus status;

    @Column(name = "total_amount", precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "order_date", nullable = false)
    private LocalDateTime orderDate;

    @Column(name = "delivery_date")
    private LocalDateTime deliveryDate;

    // Вспомогательные методы для управления двунаправленной связью
    public void addOrderItem(OrderItem item) {
        orderItems.add(item);
        item.setOrder(this); // Устанавливаем обратную ссылку
    }

    public void removeOrderItem(OrderItem item) {
        orderItems.remove(item);
        item.setOrder(null); // Убираем обратную ссылку
    }

    public void clearOrderItems() {
        // Правильная очистка коллекции с обратными ссылками
        for (OrderItem item : new ArrayList<>(orderItems)) {
            removeOrderItem(item);
        }
    }

    // Вычисление общей суммы
    @PrePersist
    @PreUpdate
    public void calculateTotalAmount() {
        this.totalAmount =
                orderItems.stream()
                        .map(
                                item ->
                                        item.getUnitPrice()
                                                .multiply(new BigDecimal(item.getQuantity())))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
