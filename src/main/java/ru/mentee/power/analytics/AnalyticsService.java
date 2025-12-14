package ru.mentee.power.analytics;

import java.util.List;
import ru.mentee.power.dto.CustomerAnalytics;
import ru.mentee.power.dto.DateRange;
import ru.mentee.power.dto.DemandForecast;
import ru.mentee.power.dto.GroupingType;
import ru.mentee.power.dto.ProductRanking;
import ru.mentee.power.dto.RankingMetric;
import ru.mentee.power.dto.SalesStatistics;

/**
 * Сервис аналитики.
 */
public interface AnalyticsService {

    /**
     * Получить статистику продаж.
     *
     * @param period период
     * @param groupBy группировка
     * @return статистика
     */
    List<SalesStatistics> getSalesStatistics(DateRange period, GroupingType groupBy);

    /**
     * Топ товаров по различным метрикам.
     *
     * @param metric метрика
     * @param limit количество
     * @return топ товаров
     */
    List<ProductRanking> getTopProducts(RankingMetric metric, int limit);

    /**
     * Анализ поведения клиентов.
     *
     * @param customerId ID клиента
     * @return аналитика клиента
     */
    CustomerAnalytics analyzeCustomer(Long customerId);

    /**
     * Прогноз спроса.
     *
     * @param productId ID товара
     * @param days дней вперед
     * @return прогноз
     */
    DemandForecast forecastDemand(Long productId, int days);
}
