package ru.mentee.power.optimization;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * Отчет об анализе запроса на N+1 проблемы.
 */
@Data
public class FetchAnalysisReport {

    private boolean hasNPlusOneIssue = false;
    private List<String> recommendations = new ArrayList<>();
    private int estimatedQueryCount = 0;
    private int optimalQueryCount = 1;

    public void addRecommendation(String recommendation) {
        recommendations.add(recommendation);
    }
}
