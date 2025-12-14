package ru.mentee.power.decision;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Отчет о валидации архитектуры.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArchitectureValidationReport {
    private double score; // 0.0 - 1.0
    @Builder.Default private List<String> issues = new ArrayList<>();
    @Builder.Default private List<String> recommendations = new ArrayList<>();

    public void addIssue(String issue) {
        if (issues == null) {
            issues = new ArrayList<>();
        }
        issues.add(issue);
    }

    public void addRecommendation(String recommendation) {
        if (recommendations == null) {
            recommendations = new ArrayList<>();
        }
        recommendations.add(recommendation);
    }
}
