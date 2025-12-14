package ru.mentee.power.performance;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Проблема производительности.
 */
@Data
@AllArgsConstructor
public class PerformanceProblem {

    private ProblemType type;
    private String entity;
    private String query;
    private int queryCount;
    private String description;
    private Severity severity;
}
