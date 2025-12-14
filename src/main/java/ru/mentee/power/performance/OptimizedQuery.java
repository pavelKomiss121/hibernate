package ru.mentee.power.performance;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Оптимизированный запрос.
 */
@Data
@AllArgsConstructor
public class OptimizedQuery {

    private String originalHQL;
    private String optimizedHQL;
    private String explanation;
    private List<String> optimizations;
}
