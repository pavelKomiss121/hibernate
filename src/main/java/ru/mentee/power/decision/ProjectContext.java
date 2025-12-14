package ru.mentee.power.decision;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Контекст проекта для генерации guidelines.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectContext {
    private String projectName;
    private String databaseType;
    private int teamSize;
    private String teamExpertiseLevel;
    private ProjectRequirements requirements;
}
