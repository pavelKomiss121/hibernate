package ru.mentee.power.specification;

import java.math.BigDecimal;
import ru.mentee.power.entity.relationship.Product;

/**
 * Фабрика спецификаций для Product.
 */
public class ProductSpecifications {

    public static Specification<Product> hasName(String name) {
        return (root, query, cb) ->
                cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }

    public static Specification<Product> priceGreaterThan(BigDecimal price) {
        return (root, query, cb) -> cb.greaterThan(root.get("price"), price);
    }

    public static Specification<Product> priceLessThan(BigDecimal price) {
        return (root, query, cb) -> cb.lessThan(root.get("price"), price);
    }

    public static Specification<Product> priceBetween(BigDecimal minPrice, BigDecimal maxPrice) {
        return (root, query, cb) -> cb.between(root.get("price"), minPrice, maxPrice);
    }

    public static Specification<Product> hasMinimumStock(Integer quantity) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("stockQuantity"), quantity);
    }

    public static Specification<Product> skuEquals(String sku) {
        return (root, query, cb) -> cb.equal(root.get("sku"), sku);
    }
}
