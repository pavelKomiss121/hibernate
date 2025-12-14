package ru.mentee.power.hybrid.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для аналитики продаж продуктов.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductSalesAnalytics {
    private Long productId;
    private String sku;
    private String productName;
    private LocalDate saleDate;
    private Long unitsSold;
    private BigDecimal revenue;
    private Double movingAvg7d;
    private Double growthRate30d;
    private Integer dailyRank;
    private String categoryName;
}
