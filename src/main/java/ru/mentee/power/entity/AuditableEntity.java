package ru.mentee.power.entity;

/**
 * Интерфейс для аудируемых сущностей.
 */
public interface AuditableEntity {
    String getCreatedBy();

    void setCreatedBy(String createdBy);

    String getModifiedBy();

    void setModifiedBy(String modifiedBy);
}
