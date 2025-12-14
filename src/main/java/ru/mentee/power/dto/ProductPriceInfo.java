package ru.mentee.power.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Информация о цене продукта с категорией.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductPriceInfo {
    private String name;
    private BigDecimal price;
    private String category;
}
