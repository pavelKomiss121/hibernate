package ru.mentee.power.performance;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Рекомендация по оптимизации.
 */
@Data
@AllArgsConstructor
public class OptimizationRecommendation {

    private String strategy;
    private String description;
    private String example;
    private Severity severity;
}
