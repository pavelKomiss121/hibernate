package ru.mentee.power.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Прогноз спроса.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DemandForecast {
    private Long productId;
    private String productName;
    private List<ForecastPoint> forecast;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ForecastPoint {
        private LocalDate date;
        private BigDecimal predictedDemand;
        private BigDecimal confidence;
    }
}
