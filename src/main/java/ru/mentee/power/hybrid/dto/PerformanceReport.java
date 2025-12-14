package ru.mentee.power.hybrid.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Отчет о производительности.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PerformanceReport {
    private String operation;
    private long hibernateTime;
    private long jdbcTime;
    private String recommendation;
    private double improvementRatio;

    public double calculateImprovementRatio() {
        if (hibernateTime == 0) {
            return 0.0;
        }
        return (double) (hibernateTime - jdbcTime) / hibernateTime * 100.0;
    }
}
