package ru.mentee.power.entity.mp_178;

import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Стратегия JOINED - соединение таблиц.
 */
@Entity
@Table(name = "employees", schema = "mentee_power")
@Inheritance(strategy = InheritanceType.JOINED)
@Data
@NoArgsConstructor
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "hire_date")
    private LocalDate hireDate;
}
