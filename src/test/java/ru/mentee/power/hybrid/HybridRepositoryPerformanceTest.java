package ru.mentee.power.hybrid;

import static org.assertj.core.api.Assertions.assertThat;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.mentee.power.config.DatabaseConfig;
import ru.mentee.power.config.HibernateConfig;
import ru.mentee.power.decision.ApplicationArchitecture;
import ru.mentee.power.decision.ArchitectureDecisionService;
import ru.mentee.power.decision.ArchitectureValidationReport;
import ru.mentee.power.decision.OperationDescription;
import ru.mentee.power.decision.TechnologyRecommendation;
import ru.mentee.power.decision.impl.ArchitectureDecisionServiceImpl;
import ru.mentee.power.entity.relationship.Product;
import ru.mentee.power.hybrid.dto.PerformanceReport;
import ru.mentee.power.hybrid.impl.HybridProductRepository;

/**
 * Performance тесты для сравнения JDBC и Hibernate.
 */
@Slf4j
class HybridRepositoryPerformanceTest {

    private SessionFactory sessionFactory;
    private DataSource dataSource;
    private HybridProductRepository hybridRepository;

    @BeforeEach
    void setUp() {
        DatabaseConfig dbConfig = new DatabaseConfig();
        String dbUrl = "jdbc:h2:mem:testdb_perf_" + System.currentTimeMillis();
        dbConfig.setJdbcUrl(dbUrl);
        dbConfig.setDriverClassName("org.h2.Driver");
        dbConfig.setUsername("sa");
        dbConfig.setPassword("");
        dbConfig.setHbm2ddlAuto("create-drop");
        dbConfig.setShowSql(false);

        HibernateConfig hibernateConfig = new HibernateConfig(dbConfig);
        sessionFactory = hibernateConfig.buildSessionFactory();

        // Создаем DataSource для JDBC
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(dbUrl);
        hikariConfig.setUsername("sa");
        hikariConfig.setPassword("");
        hikariConfig.setMaximumPoolSize(10);
        dataSource = new HikariDataSource(hikariConfig);

        hybridRepository = new HybridProductRepository(sessionFactory, dataSource);

        // Создаем тестовые данные
        createTestData();
    }

    private void createTestData() {
        try (Session session = sessionFactory.openSession()) {
            Transaction tx = session.beginTransaction();
            try {
                for (int i = 0; i < 100; i++) {
                    Product product =
                            Product.builder()
                                    .sku("SKU-" + i)
                                    .name("Product " + i)
                                    .description("Description " + i)
                                    .price(new BigDecimal("10.00").add(new BigDecimal(i)))
                                    .stockQuantity(100 + i)
                                    .createdAt(LocalDateTime.now())
                                    .build();
                    session.persist(product);
                }
                tx.commit();
            } catch (Exception e) {
                tx.rollback();
                throw e;
            }
        }
    }

    private long measureTime(Runnable operation) {
        long start = System.currentTimeMillis();
        operation.run();
        return System.currentTimeMillis() - start;
    }

    @Test
    @DisplayName("Should choose optimal technology based on performance - Batch Insert")
    void shouldChooseOptimalTechnologyForBatchInsert() {
        // Given - тестовые данные для JDBC
        int recordCount = 1000;
        List<Product> jdbcProducts = generateTestProducts(recordCount, "JDBC-SKU-");

        // When - измеряем batch insert через JDBC
        long jdbcBatchInsertTime =
                measureTime(
                        () -> {
                            hybridRepository.bulkInsert(jdbcProducts);
                        });

        // Given - тестовые данные для Hibernate (другие SKU)
        List<Product> hibernateProducts = generateTestProducts(recordCount, "HIB-SKU-");

        // When - измеряем insert через Hibernate
        long hibernateInsertTime =
                measureTime(
                        () -> {
                            for (Product product : hibernateProducts) {
                                hybridRepository.save(product);
                            }
                        });

        // Then - анализируем результаты
        PerformanceReport report =
                PerformanceReport.builder()
                        .operation("Batch Insert")
                        .hibernateTime(hibernateInsertTime)
                        .jdbcTime(jdbcBatchInsertTime)
                        .recommendation("Use JDBC for batch operations")
                        .build();

        report.setImprovementRatio(report.calculateImprovementRatio());

        log.info("Performance Report: {}", report);
        log.info("JDBC time: {}ms, Hibernate time: {}ms", jdbcBatchInsertTime, hibernateInsertTime);
        log.info("Improvement: {:.2f}%", report.getImprovementRatio());

        // JDBC должен быть быстрее для batch операций
        assertThat(jdbcBatchInsertTime).isLessThan(hibernateInsertTime);
    }

    @Test
    @DisplayName("Should choose optimal technology based on performance - Simple Find")
    void shouldChooseOptimalTechnologyForSimpleFind() {
        // Given
        Long productId = 1L;

        // When - измеряем find через Hibernate (с кэшем)
        // Прогрев кэша
        hybridRepository.findById(productId);

        long hibernateFindTime =
                measureTime(
                        () -> {
                            for (int i = 0; i < 1000; i++) {
                                hybridRepository.findById(productId);
                            }
                        });

        // Then
        log.info("Hibernate find time (with cache): {}ms", hibernateFindTime);
        assertThat(hibernateFindTime).isLessThan(1000); // Должно быть быстро с кэшем
    }

    @Test
    @DisplayName("Should validate architecture decisions")
    void shouldValidateArchitectureDecisions() {
        // Given
        ApplicationArchitecture architecture = ApplicationArchitecture.builder().build();
        architecture.addComponent("ProductCatalog", TechnologyChoice.HIBERNATE);
        architecture.addComponent("OrderProcessing", TechnologyChoice.HIBERNATE);
        architecture.addComponent("Analytics", TechnologyChoice.JDBC);
        architecture.addComponent("BulkImport", TechnologyChoice.JDBC);
        architecture.addComponent("Search", TechnologyChoice.HYBRID);

        // When
        ArchitectureDecisionService decisionService = new ArchitectureDecisionServiceImpl();
        ArchitectureValidationReport report = decisionService.validate(architecture);

        // Then
        assertThat(report.getScore()).isGreaterThan(0.7);
        log.info("Architecture validation score: {}", report.getScore());
        log.info("Recommendations: {}", report.getRecommendations());
    }

    @Test
    @DisplayName("Should recommend technology for operation")
    void shouldRecommendTechnologyForOperation() {
        // Given
        OperationDescription crudOperation =
                OperationDescription.builder()
                        .operationName("saveProduct")
                        .operationType(OperationDescription.OperationType.CRUD)
                        .expectedVolume(100)
                        .requiresCaching(true)
                        .build();

        OperationDescription analyticsOperation =
                OperationDescription.builder()
                        .operationName("getProductAnalytics")
                        .operationType(OperationDescription.OperationType.ANALYTICS)
                        .requiresComplexQueries(true)
                        .build();

        OperationDescription batchOperation =
                OperationDescription.builder()
                        .operationName("bulkImport")
                        .operationType(OperationDescription.OperationType.BATCH)
                        .expectedVolume(10000)
                        .requiresBatchOperations(true)
                        .build();

        // When
        ArchitectureDecisionService decisionService = new ArchitectureDecisionServiceImpl();

        TechnologyRecommendation crudRecommendation =
                decisionService.recommendTechnology(crudOperation);
        TechnologyRecommendation analyticsRecommendation =
                decisionService.recommendTechnology(analyticsOperation);
        TechnologyRecommendation batchRecommendation =
                decisionService.recommendTechnology(batchOperation);

        // Then
        assertThat(crudRecommendation.getRecommendedTechnology())
                .isEqualTo(TechnologyChoice.HIBERNATE);
        assertThat(analyticsRecommendation.getRecommendedTechnology())
                .isEqualTo(TechnologyChoice.JDBC);
        assertThat(batchRecommendation.getRecommendedTechnology()).isEqualTo(TechnologyChoice.JDBC);

        log.info("CRUD recommendation: {}", crudRecommendation.getRecommendedTechnology());
        log.info(
                "Analytics recommendation: {}", analyticsRecommendation.getRecommendedTechnology());
        log.info("Batch recommendation: {}", batchRecommendation.getRecommendedTechnology());
    }

    private List<Product> generateTestProducts(int count) {
        return generateTestProducts(count, "TEST-SKU-");
    }

    private List<Product> generateTestProducts(int count, String skuPrefix) {
        List<Product> products = new ArrayList<>();
        long timestamp = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            products.add(
                    Product.builder()
                            .sku(skuPrefix + timestamp + "-" + i)
                            .name("Test Product " + i)
                            .description("Test Description " + i)
                            .price(new BigDecimal("10.00"))
                            .stockQuantity(100)
                            .createdAt(LocalDateTime.now())
                            .build());
        }
        return products;
    }
}
