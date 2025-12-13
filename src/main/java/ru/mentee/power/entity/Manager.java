package ru.mentee.power.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Менеджер - наследник Employee.
 */
@Entity
@Table(name = "managers", schema = "mentee_power")
@PrimaryKeyJoinColumn(name = "employee_id")
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Manager extends Employee {

    @Column(name = "department")
    private String department;

    @Column(name = "budget")
    private BigDecimal budget;
}
