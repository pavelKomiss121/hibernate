package ru.mentee.power.decision;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Описание операции для анализа.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OperationDescription {
    private String operationName;
    private OperationType operationType;
    private int expectedVolume;
    private boolean requiresComplexQueries;
    private boolean requiresBatchOperations;
    private boolean requiresCaching;
    private boolean requiresTransactionControl;
    private Map<String, Object> metadata;

    public enum OperationType {
        CRUD,
        ANALYTICS,
        BATCH,
        SEARCH,
        TRANSACTION
    }
}
