package ru.mentee.power.hybrid.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для аналитики продуктов.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductAnalytics {
    private Long productId;
    private String productName;
    private String sku;
    private Long categoryId;
    private String categoryName;
    private Integer categoryLevel;
    private Long orderCount;
    private Long totalSold;
    private BigDecimal revenue;
    private Double avgRating;
    private Long reviewCount;
    private Integer categoryRank;
    private Double overallPercentile;
    private BigDecimal revenueGap;
    private BigDecimal runningCategoryRevenue;
    private BigDecimal revenueVsCategoryAvg;
}
