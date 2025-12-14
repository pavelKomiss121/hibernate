package ru.mentee.power.performance;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Статистика использования ассоциации.
 */
@Data
@AllArgsConstructor
public class UsageStatistics {

    private int accessCount;
    private double accessFrequency;
    private int averageResultSize;
}
