package ru.mentee.power.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Краткая информация о продукте.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductSummary {
    private Long id;
    private String name;
    private BigDecimal price;
    private Integer quantity;
}
