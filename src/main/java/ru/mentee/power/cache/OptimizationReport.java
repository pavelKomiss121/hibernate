package ru.mentee.power.cache;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * Отчет об оптимизации кэша.
 */
@Data
public class OptimizationReport {
    private List<String> recommendations = new ArrayList<>();
    private boolean hasIssues;

    public void addRecommendation(String recommendation) {
        this.recommendations.add(recommendation);
        this.hasIssues = true;
    }
}
