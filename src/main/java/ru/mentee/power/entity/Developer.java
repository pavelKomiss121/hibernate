package ru.mentee.power.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Разработчик - наследник Employee.
 */
@Entity
@Table(name = "developers", schema = "mentee_power")
@PrimaryKeyJoinColumn(name = "employee_id")
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Developer extends Employee {

    @Column(name = "programming_language")
    private String programmingLanguage;

    @Column(name = "experience_years")
    private Integer experienceYears;
}
