package ru.mentee.power.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Рейтинг продукта.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductRanking {
    private Long productId;
    private String productName;
    private BigDecimal metricValue;
    private Integer rank;
}
