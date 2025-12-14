package ru.mentee.power.performance;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * Отчет о проблемах производительности.
 */
@Data
public class PerformanceReport {

    private final MonitoringContext context;
    private final List<PerformanceProblem> problems = new ArrayList<>();

    public PerformanceReport(MonitoringContext context) {
        this.context = context;
    }

    public void addProblem(PerformanceProblem problem) {
        problems.add(problem);
    }

    public boolean hasProblems() {
        return !problems.isEmpty();
    }

    public List<PerformanceProblem> getProblemsByType(ProblemType type) {
        return problems.stream().filter(p -> p.getType() == type).toList();
    }
}
