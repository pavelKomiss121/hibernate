package ru.mentee.power.decision.impl;

import lombok.extern.slf4j.Slf4j;
import ru.mentee.power.decision.ApplicationArchitecture;
import ru.mentee.power.decision.ArchitectureDecisionService;
import ru.mentee.power.decision.ArchitectureValidationReport;
import ru.mentee.power.decision.DeveloperGuidelines;
import ru.mentee.power.decision.OperationDescription;
import ru.mentee.power.decision.ProjectContext;
import ru.mentee.power.decision.TechnologyRecommendation;
import ru.mentee.power.hybrid.TechnologyChoice;

/**
 * Реализация сервиса для принятия архитектурных решений.
 */
@Slf4j
public class ArchitectureDecisionServiceImpl implements ArchitectureDecisionService {

    @Override
    public TechnologyRecommendation recommendTechnology(OperationDescription operation) {
        TechnologyRecommendation recommendation = TechnologyRecommendation.builder().build();

        int hibernatePoints = 0;
        int jdbcPoints = 0;

        // Анализ типа операции
        switch (operation.getOperationType()) {
            case CRUD -> {
                hibernatePoints += 3;
                recommendation.addReason("CRUD операции лучше выполняются через Hibernate");
            }
            case ANALYTICS -> {
                jdbcPoints += 3;
                recommendation.addReason("Аналитика требует сложных SQL запросов");
            }
            case BATCH -> {
                jdbcPoints += 3;
                recommendation.addReason("Batch операции оптимальнее через JDBC");
            }
            case SEARCH -> {
                if (operation.isRequiresComplexQueries()) {
                    jdbcPoints += 2;
                    recommendation.addReason("Сложные поисковые запросы требуют JDBC");
                } else {
                    hibernatePoints += 2;
                    recommendation.addReason("Простой поиск через Hibernate");
                }
            }
            case TRANSACTION -> {
                if (operation.isRequiresTransactionControl()) {
                    jdbcPoints += 2;
                    recommendation.addReason("Требуется точный контроль транзакций");
                } else {
                    hibernatePoints += 2;
                    recommendation.addReason("Стандартные транзакции через Hibernate");
                }
            }
        }

        // Анализ требований
        if (operation.isRequiresComplexQueries()) {
            jdbcPoints += 2;
            recommendation.addReason("Сложные запросы требуют JDBC");
        }

        if (operation.isRequiresBatchOperations()) {
            jdbcPoints += 2;
            recommendation.addReason("Batch операции требуют JDBC");
        }

        if (operation.isRequiresCaching()) {
            hibernatePoints += 2;
            recommendation.addReason("Кэширование доступно из коробки в Hibernate");
        }

        if (operation.getExpectedVolume() > 10000) {
            jdbcPoints += 1;
            recommendation.addReason("Высокий объем операций - JDBC более эффективен");
        }

        // Определение рекомендации
        TechnologyChoice recommended;
        double confidence;
        String rationale;

        if (hibernatePoints > jdbcPoints) {
            recommended = TechnologyChoice.HIBERNATE;
            confidence = 0.8;
            rationale = "Hibernate рекомендуется для этой операции";
        } else if (jdbcPoints > hibernatePoints) {
            recommended = TechnologyChoice.JDBC;
            confidence = 0.8;
            rationale = "JDBC рекомендуется для этой операции";
        } else {
            recommended = TechnologyChoice.HYBRID;
            confidence = 0.6;
            rationale = "Гибридный подход может быть оптимальным";
            recommendation.addWarning("Рассмотрите использование обоих подходов");
        }

        recommendation.setRecommendedTechnology(recommended);
        recommendation.setConfidence(confidence);
        recommendation.setRationale(rationale);

        return recommendation;
    }

    @Override
    public ArchitectureValidationReport validate(ApplicationArchitecture architecture) {
        ArchitectureValidationReport report = ArchitectureValidationReport.builder().build();

        double score = 1.0;
        int issuesCount = 0;

        // Проверка компонентов
        for (var entry : architecture.getComponents().entrySet()) {
            String componentName = entry.getKey();
            TechnologyChoice technology = entry.getValue();

            // Валидация выбора технологии
            switch (componentName.toLowerCase()) {
                case "analytics", "reporting" -> {
                    if (technology != TechnologyChoice.JDBC) {
                        report.addIssue(
                                "Компонент "
                                        + componentName
                                        + " должен использовать JDBC для аналитики");
                        score -= 0.1;
                        issuesCount++;
                    }
                }
                case "bulkimport", "bulkupdate" -> {
                    if (technology != TechnologyChoice.JDBC) {
                        report.addIssue(
                                "Компонент "
                                        + componentName
                                        + " должен использовать JDBC для batch операций");
                        score -= 0.1;
                        issuesCount++;
                    }
                }
                case "productcatalog", "orderprocessing" -> {
                    if (technology == TechnologyChoice.JDBC) {
                        report.addRecommendation(
                                "Рассмотрите использование Hibernate для "
                                        + componentName
                                        + " для упрощения CRUD");
                    }
                }
            }
        }

        // Рекомендации
        if (architecture.getComponents().containsKey("ProductCatalog")
                && architecture.getComponents().get("ProductCatalog")
                        == TechnologyChoice.HIBERNATE) {
            report.addRecommendation("Включите кэширование для ProductCatalog");
        }

        if (architecture.getComponents().containsKey("BulkImport")
                && architecture.getComponents().get("BulkImport") == TechnologyChoice.JDBC) {
            report.addRecommendation("Используйте batch операции в BulkImport");
        }

        score = Math.max(0.0, score);
        report.setScore(score);

        return report;
    }

    @Override
    public DeveloperGuidelines generateGuidelines(ProjectContext projectContext) {
        DeveloperGuidelines guidelines =
                DeveloperGuidelines.builder().projectName(projectContext.getProjectName()).build();

        // Секция: Когда использовать Hibernate
        DeveloperGuidelines.GuidelineSection hibernateSection =
                DeveloperGuidelines.GuidelineSection.builder()
                        .title("Когда использовать Hibernate")
                        .description("Используйте Hibernate для стандартных CRUD операций")
                        .build();
        hibernateSection.getRules().add("Стандартные CRUD операции с сущностями");
        hibernateSection.getRules().add("Операции с отношениями между сущностями");
        hibernateSection.getRules().add("Когда нужен кэш второго уровня");
        hibernateSection.getRules().add("Простые поисковые запросы");
        guidelines.addSection(hibernateSection);

        // Секция: Когда использовать JDBC
        DeveloperGuidelines.GuidelineSection jdbcSection =
                DeveloperGuidelines.GuidelineSection.builder()
                        .title("Когда использовать JDBC")
                        .description("Используйте JDBC для сложных запросов и batch операций")
                        .build();
        jdbcSection.getRules().add("Сложные аналитические запросы с window functions");
        jdbcSection.getRules().add("Batch операции (вставка/обновление больших объемов)");
        jdbcSection.getRules().add("Использование специфичных для БД функций");
        jdbcSection.getRules().add("Критичные по производительности операции");
        guidelines.addSection(jdbcSection);

        // Секция: Гибридный подход
        DeveloperGuidelines.GuidelineSection hybridSection =
                DeveloperGuidelines.GuidelineSection.builder()
                        .title("Гибридный подход")
                        .description(
                                "Комбинируйте Hibernate и JDBC для оптимальной производительности")
                        .build();
        hybridSection.getRules().add("Hibernate для CRUD, JDBC для аналитики");
        hybridSection.getRules().add("Инвалидируйте кэш после bulk операций через JDBC");
        hybridSection.getRules().add("Используйте Hibernate для транзакционной логики");
        guidelines.addSection(hybridSection);

        return guidelines;
    }
}
