package ru.mentee.power.entity;

/**
 * Интерфейс для сущностей с натуральным ключом.
 */
public interface NaturalIdEntity<T> {
    T getNaturalId();

    void setNaturalId(T naturalId);
}
