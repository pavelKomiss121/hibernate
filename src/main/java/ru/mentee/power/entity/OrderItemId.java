package ru.mentee.power.entity;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Составной ключ через @IdClass.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemId implements Serializable {

    private Long orderId;

    private Long productId;
}
