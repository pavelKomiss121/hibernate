package ru.mentee.power.entity.relationship;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Bidirectional OneToOne через таблицу.
 */
@Entity(name = "RelationshipEmployee")
@Table(name = "employees")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "email", unique = true, nullable = false)
    private String email;

    @Column(name = "department")
    private String department;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinTable(
            name = "employee_parking_spot",
            joinColumns =
                    @JoinColumn(
                            name = "employee_id",
                            referencedColumnName = "id",
                            foreignKey = @ForeignKey(name = "fk_eps_employee")),
            inverseJoinColumns =
                    @JoinColumn(
                            name = "parking_spot_id",
                            referencedColumnName = "id",
                            unique = true,
                            foreignKey = @ForeignKey(name = "fk_eps_parking_spot")))
    private ParkingSpot parkingSpot;
}
