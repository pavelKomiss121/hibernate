package ru.mentee.power.entity.mp_178;

/**
 * Интерфейс для сущностей с натуральным ключом.
 */
public interface NaturalIdEntity<T> {
    T getNaturalId();

    void setNaturalId(T naturalId);
}
