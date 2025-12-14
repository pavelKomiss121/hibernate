package ru.mentee.power.performance;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Стратегия загрузки данных.
 */
@Data
@AllArgsConstructor
public class FetchStrategy {

    private String strategy; // JOIN_FETCH, BATCH, ENTITY_GRAPH, SUBSELECT
    private int batchSize;
    private String explanation;
}
