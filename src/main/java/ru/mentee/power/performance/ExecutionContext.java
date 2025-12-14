package ru.mentee.power.performance;

import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Контекст выполнения запроса.
 */
@Data
@AllArgsConstructor
public class ExecutionContext {

    private Class<?> entityClass;
    private Set<String> accessedProperties;
    private int expectedResultSize;
    private boolean readOnly;
}
