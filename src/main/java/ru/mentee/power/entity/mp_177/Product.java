package ru.mentee.power.entity.mp_177;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.*;
import ru.mentee.power.entity.mp_178.Address;
import ru.mentee.power.entity.mp_178.Dimensions;
import ru.mentee.power.entity.mp_178.ProductCategory;
import ru.mentee.power.entity.mp_178.ProductStatus;

/**
 * Сущность товара с демонстрацией всех типов маппинга.
 */
@Entity
@jakarta.persistence.Table(
        name = "products",
        schema = "mentee_power",
        indexes = {
            @jakarta.persistence.Index(name = "idx_product_sku", columnList = "sku", unique = true),
            @jakarta.persistence.Index(name = "idx_product_category", columnList = "category"),
            @jakarta.persistence.Index(name = "idx_product_price", columnList = "price")
        },
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_product_sku",
                    columnNames = {"sku"})
        })
@SecondaryTable(
        name = "product_details",
        pkJoinColumns = @PrimaryKeyJoinColumn(name = "product_id"))
@DynamicInsert // Генерировать INSERT только для не-null полей
@DynamicUpdate // Генерировать UPDATE только для измененных полей
@SelectBeforeUpdate // Проверять изменения перед UPDATE
@BatchSize(size = 25) // Batch загрузка коллекций
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"description", "imageData"}) // Исключаем большие поля
public class Product {

    // ========== Идентификаторы ==========
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "product_seq")
    @SequenceGenerator(
            name = "product_seq",
            sequenceName = "product_sequence",
            allocationSize = 50 // Оптимизация: получаем 50 ID за раз
            )
    private Long id;

    @NaturalId // Натуральный бизнес-ключ
    @Column(name = "sku", nullable = false, length = 50)
    private String sku;

    @Column(name = "external_id", columnDefinition = "uuid")
    private UUID externalId;

    // ========== Базовые поля ==========
    @Column(name = "name", nullable = false, length = 200)
    @NotFound(action = NotFoundAction.EXCEPTION)
    private String name;

    @Lob // Large Object
    @Column(name = "description", columnDefinition = "TEXT")
    @Basic(fetch = FetchType.LAZY) // Ленивая загрузка для больших полей
    private String description;

    // ========== Числовые типы ==========
    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "quantity", nullable = false)
    @ColumnDefault("0") // Значение по умолчанию в БД
    @Min(0) // Валидация Hibernate
    private Integer quantity;

    @Column(name = "weight", columnDefinition = "DECIMAL(8,3)")
    private Double weight;

    // ========== Перечисления ==========
    @Enumerated(EnumType.STRING) // Хранить как строку
    @Column(name = "category", length = 50)
    private ProductCategory category;

    @Enumerated(EnumType.ORDINAL) // Хранить как число (не рекомендуется)
    @Column(name = "status", columnDefinition = "INTEGER")
    private ProductStatus status;

    // ========== Даты и время ==========
    @Column(name = "manufacture_date")
    private LocalDate manufactureDate;

    @Column(name = "expiry_date_time")
    private LocalDateTime expiryDateTime;

    @Column(name = "last_restock_time")
    private LocalTime lastRestockTime;

    @Column(name = "warranty_period")
    @Convert(
            converter =
                    ru.mentee.power.converter.PeriodStringConverter.class) // Кастомный конвертер
    private Period warrantyPeriod;

    // ========== Флаги и булевы значения ==========
    @Column(name = "active", nullable = false)
    @ColumnDefault("true")
    @Builder.Default
    private boolean active = true;

    @Column(name = "featured")
    private Boolean featured;

    // ========== Версионирование и аудит ==========
    @Version
    @Column(name = "version")
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "created_by", updatable = false)
    private String createdBy;

    @Column(name = "modified_by")
    private String modifiedBy;

    // ========== Вторичная таблица ==========
    @Column(name = "manufacturer", table = "product_details")
    private String manufacturer;

    @Column(name = "country_of_origin", table = "product_details")
    private String countryOfOrigin;

    @Lob
    @Column(name = "specifications", table = "product_details")
    private String specifications;

    // ========== Бинарные данные ==========
    @Lob
    @Column(name = "image_data", columnDefinition = "BINARY LARGE OBJECT")
    @Basic(fetch = FetchType.LAZY)
    private byte[] imageData;

    @Column(name = "image_content_type")
    private String imageContentType;

    // ========== Встраиваемые объекты ==========
    @Embedded private Dimensions dimensions;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "street", column = @Column(name = "warehouse_street")),
        @AttributeOverride(name = "city", column = @Column(name = "warehouse_city")),
        @AttributeOverride(name = "zipCode", column = @Column(name = "warehouse_zip"))
    })
    private Address warehouseAddress;

    // ========== Formula - вычисляемые поля ==========
    @Formula("price * quantity") // SQL выражение
    private BigDecimal totalValue;

    @Formula("(SELECT COUNT(*) FROM mentee_power.order_items oi WHERE oi.product_id = id)")
    private Integer orderCount;

    // ========== Transient поля ==========
    @Transient // Не сохраняется в БД
    private String searchKeywords;

    @Transient private BigDecimal discountedPrice;

    // ========== Callback методы ==========
    @PrePersist
    protected void onCreate() {
        if (externalId == null) {
            externalId = UUID.randomUUID();
        }
        if (status == null) {
            status = ProductStatus.AVAILABLE;
        }
    }

    @PostLoad
    protected void calculateTransientFields() {
        if (price != null && featured != null && featured) {
            discountedPrice = price.multiply(new BigDecimal("0.9"));
        }
    }

    @PreUpdate
    protected void onUpdate() {
        // Валидация перед обновлением
        if (quantity < 0) {
            throw new IllegalStateException("Количество не может быть отрицательным");
        }
    }
}
