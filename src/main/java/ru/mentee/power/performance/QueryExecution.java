package ru.mentee.power.performance;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * Информация о выполнении запроса.
 */
@Data
public class QueryExecution {

    private final String query;
    private final List<Long> executionTimes = new ArrayList<>();
    private final List<Integer> rowCounts = new ArrayList<>();

    public QueryExecution(String query) {
        this.query = query;
    }

    public void recordExecution(long executionTime, int rowCount) {
        executionTimes.add(executionTime);
        rowCounts.add(rowCount);
    }

    public int getExecutionCount() {
        return executionTimes.size();
    }

    public long getAvgExecutionTime() {
        if (executionTimes.isEmpty()) {
            return 0;
        }
        return executionTimes.stream().mapToLong(Long::longValue).sum() / executionTimes.size();
    }

    public int getAvgRowCount() {
        if (rowCounts.isEmpty()) {
            return 0;
        }
        return rowCounts.stream().mapToInt(Integer::intValue).sum() / rowCounts.size();
    }

    public int getMaxRowCount() {
        return rowCounts.stream().mapToInt(Integer::intValue).max().orElse(0);
    }
}
