package ru.mentee.power.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Мотоцикл - наследник Vehicle.
 */
@Entity
@Table(name = "motorcycles", schema = "mentee_power")
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Motorcycle extends Vehicle {

    @Column(name = "engine_displacement")
    private Integer engineDisplacement;

    @Column(name = "has_sidecar")
    private boolean hasSidecar;
}
