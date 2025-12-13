package ru.mentee.power.entity.mp_178;

/**
 * Интерфейс для аудируемых сущностей.
 */
public interface AuditableEntity {
    String getCreatedBy();

    void setCreatedBy(String createdBy);

    String getModifiedBy();

    void setModifiedBy(String modifiedBy);
}
