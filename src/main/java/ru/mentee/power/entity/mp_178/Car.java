package ru.mentee.power.entity.mp_178;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Автомобиль - наследник Vehicle.
 */
@Entity
@Table(name = "cars", schema = "mentee_power")
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Car extends Vehicle {

    @Column(name = "num_doors")
    private Integer numberOfDoors;

    @Column(name = "trunk_capacity")
    private Integer trunkCapacity;
}
