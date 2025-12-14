package ru.mentee.power.performance;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Метаданные ассоциации.
 */
@Data
@AllArgsConstructor
public class AssociationMetadata {

    private String associationName;
    private String associationType; // OneToMany, ManyToOne, ManyToMany
    private Class<?> targetEntity;
    private boolean isLazy;
}
