package ru.mentee.power.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Статистика продаж.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SalesStatistics {
    private LocalDateTime period;
    private Long orderCount;
    private Long customerCount;
    private BigDecimal revenue;
    private BigDecimal previousRevenue;
    private BigDecimal growth;
}
