package ru.mentee.power.entity.mp_178;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Стратегия TABLE_PER_CLASS - отдельная таблица для каждого класса.
 */
@Entity
@Table(name = "vehicles", schema = "mentee_power")
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@Data
@NoArgsConstructor
public abstract class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.TABLE)
    private Long id;

    @Column(name = "manufacturer")
    private String manufacturer;

    @Column(name = "model")
    private String model;

    @Column(name = "manufacture_year", columnDefinition = "INTEGER")
    private Integer year;
}
