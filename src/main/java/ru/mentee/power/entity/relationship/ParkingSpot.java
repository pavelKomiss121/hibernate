package ru.mentee.power.entity.relationship;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Парковочное место для OneToOne через JoinTable.
 */
@Entity
@Table(name = "parking_spots")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParkingSpot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "spot_number", unique = true, nullable = false, length = 20)
    private String spotNumber;

    @Column(name = "location")
    private String location;

    @OneToOne(mappedBy = "parkingSpot", fetch = FetchType.LAZY)
    private Employee employee;
}
