package ru.mentee.power.entity.mp_178;

import jakarta.persistence.*;
import java.time.YearMonth;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import ru.mentee.power.converter.EncryptedStringConverter;

/**
 * Кредитная карта - наследник PaymentMethod.
 */
@Entity
@DiscriminatorValue("CARD")
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CreditCard extends PaymentMethod {

    @Column(name = "card_number")
    @Convert(converter = EncryptedStringConverter.class)
    private String cardNumber;

    @Column(name = "card_holder")
    private String cardHolder;

    @Column(name = "expiry_date", columnDefinition = "VARCHAR(7)")
    private YearMonth expiryDate;
}
