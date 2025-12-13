package ru.mentee.power.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * PayPal аккаунт - наследник PaymentMethod.
 */
@Entity
@DiscriminatorValue("PAYPAL")
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PayPalAccount extends PaymentMethod {

    @Column(name = "paypal_email")
    private String email;

    @Column(name = "paypal_account_id")
    private String accountId;
}
