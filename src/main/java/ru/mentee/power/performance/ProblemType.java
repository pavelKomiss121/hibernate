package ru.mentee.power.performance;

/**
 * Тип проблемы производительности.
 */
public enum ProblemType {
    N_PLUS_ONE,
    SLOW_QUERY,
    LARGE_RESULT_SET,
    MISSING_INDEX,
    INEFFICIENT_QUERY,
    CACHE_MISUSE,
    LONG_TRANSACTION
}
