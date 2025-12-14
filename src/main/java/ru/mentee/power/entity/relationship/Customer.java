package ru.mentee.power.entity.relationship;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.hibernate.annotations.Where;

/**
 * Сущность клиента с обратной связью OneToMany.
 */
@Entity(name = "RelationshipCustomer")
@Table(name = "customers")
@Cacheable
@Cache(
        usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE,
        region = "ru.mentee.power.entity.relationship.Customer")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@NamedEntityGraph(
        name = "customer-with-orders",
        attributeNodes = {@NamedAttributeNode(value = "orders", subgraph = "order-items")},
        subgraphs = {
            @NamedSubgraph(name = "order-items", attributeNodes = @NamedAttributeNode("orderItems"))
        })
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "email", unique = true, nullable = false)
    private String email;

    @Column(name = "phone")
    private String phone;

    // Один клиент ко многим заказам
    @OneToMany(
            mappedBy = "customer",
            cascade = {CascadeType.PERSIST, CascadeType.MERGE},
            fetch = FetchType.LAZY)
    @OrderBy("orderDate DESC") // Последние заказы первыми
    @Where(clause = "status != 'CANCELLED'") // Фильтр на уровне Hibernate
    @Cache(
            usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE,
            region = "ru.mentee.power.entity.relationship.Customer.orders")
    @BatchSize(size = 10)
    @LazyCollection(LazyCollectionOption.EXTRA) // Оптимизация для size()
    @Builder.Default
    private List<Order> orders = new ArrayList<>();

    // Статистика клиента
    @Formula(
            """
		(SELECT COUNT(*) FROM orders o
		 WHERE o.customer_id = id AND o.status = 'COMPLETED')
		""")
    private Integer completedOrdersCount;

    @Formula(
            """
		(SELECT COALESCE(SUM(o.total_amount), 0) FROM orders o
		 WHERE o.customer_id = id AND o.status = 'COMPLETED')
		""")
    private BigDecimal totalSpent;
}
