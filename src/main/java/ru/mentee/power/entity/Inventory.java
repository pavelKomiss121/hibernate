package ru.mentee.power.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Formula;

/**
 * Инвентарь с составным ключом через @EmbeddedId.
 */
@Entity
@Table(name = "inventory", schema = "mentee_power")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Inventory {

    @EmbeddedId private InventoryId id;

    @Column(name = "quantity_on_hand")
    private Integer quantityOnHand;

    @Column(name = "quantity_reserved")
    private Integer quantityReserved;

    @Column(name = "reorder_level")
    private Integer reorderLevel;

    @Formula("quantity_on_hand - quantity_reserved")
    private Integer availableQuantity;
}
