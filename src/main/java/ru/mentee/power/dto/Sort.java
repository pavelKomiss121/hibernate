package ru.mentee.power.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Параметры сортировки.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Sort {
    private List<Order> orders;

    public static Sort by(String property) {
        return new Sort(List.of(new Order(property, Direction.ASC)));
    }

    public static Sort by(String property, Direction direction) {
        return new Sort(List.of(new Order(property, direction)));
    }

    public static Sort by(Order... orders) {
        return new Sort(List.of(orders));
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Order {
        private String property;
        private Direction direction;

        public boolean isAscending() {
            return direction == Direction.ASC;
        }
    }

    public enum Direction {
        ASC,
        DESC
    }
}
