package ru.mentee.power.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Статистика по клиенту.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerStatistics {
    private Long id;
    private String fullName;
    private Long orderCount;
    private BigDecimal totalAmount;
    private BigDecimal averageAmount;
    private LocalDateTime lastOrderDate;
}
