package ru.mentee.power.decision;

/**
 * Сервис для принятия архитектурных решений.
 */
public interface ArchitectureDecisionService {

    /**
     * Анализировать операцию и рекомендовать технологию.
     *
     * @param operation описание операции
     * @return рекомендация с обоснованием
     */
    TechnologyRecommendation recommendTechnology(OperationDescription operation);

    /**
     * Валидировать текущую архитектуру.
     *
     * @param architecture текущая архитектура
     * @return отчет с рекомендациями
     */
    ArchitectureValidationReport validate(ApplicationArchitecture architecture);

    /**
     * Сгенерировать guidelines для команды.
     *
     * @param projectContext контекст проекта
     * @return документ с рекомендациями
     */
    DeveloperGuidelines generateGuidelines(ProjectContext projectContext);
}
