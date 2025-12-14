package ru.mentee.power.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Аналитика по клиенту.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerAnalytics {
    private Long customerId;
    private String customerName;
    private Long totalOrders;
    private BigDecimal totalSpent;
    private BigDecimal averageOrderValue;
    private LocalDateTime firstOrderDate;
    private LocalDateTime lastOrderDate;
    private BigDecimal lifetimeValue;
}
