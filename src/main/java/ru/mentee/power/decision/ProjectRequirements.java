package ru.mentee.power.decision;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Требования проекта для анализа.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectRequirements {
    private int expectedTps; // Transactions per second
    private int maxLatencyMs;
    private double readWriteRatio;
    private double crudPercentage;
    private Complexity domainComplexity;
    private boolean hasBulkOperations;
    private boolean hasComplexAnalytics;
    private boolean usesDatabaseSpecificFeatures;

    public enum Complexity {
        LOW,
        MEDIUM,
        HIGH
    }
}
