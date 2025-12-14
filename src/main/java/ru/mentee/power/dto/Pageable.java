package ru.mentee.power.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Параметры пагинации.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Pageable {
    private int pageNumber;
    private int pageSize;
    private Sort sort;

    public static Pageable of(int pageNumber, int pageSize) {
        return new Pageable(pageNumber, pageSize, null);
    }

    public static Pageable of(int pageNumber, int pageSize, Sort sort) {
        return new Pageable(pageNumber, pageSize, sort);
    }
}
