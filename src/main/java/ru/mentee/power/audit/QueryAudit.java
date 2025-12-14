package ru.mentee.power.audit;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Аудит выполнения запроса.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QueryAudit {
    private String query;
    private long executionTime;
    private LocalDateTime timestamp;
    private int resultCount;
    private String queryType; // HQL, Criteria, Native
}
