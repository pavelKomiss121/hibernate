package ru.mentee.power.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Критерии поиска продуктов.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductSearchCriteria {
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private String category;
    private String namePattern;
    private Boolean active;
    private Integer minQuantity;
}
