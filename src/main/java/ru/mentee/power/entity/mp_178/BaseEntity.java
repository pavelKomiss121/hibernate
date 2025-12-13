package ru.mentee.power.entity.mp_178;

import java.time.Instant;

/**
 * Базовый интерфейс для всех сущностей.
 */
public interface BaseEntity<ID> {
    ID getId();

    void setId(ID id);

    Instant getCreatedAt();

    Instant getUpdatedAt();

    Long getVersion();
}
