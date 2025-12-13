package ru.mentee.power.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.CreditCardNumber;
import org.hibernate.validator.constraints.Range;
import org.hibernate.validator.constraints.URL;
import ru.mentee.power.converter.EncryptedStringConverter;

/**
 * Сущность с валидацией.
 */
@Entity
@Table(name = "customers", schema = "mentee_power")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Имя не может быть пустым")
    @Size(min = 2, max = 100, message = "Имя должно быть от 2 до 100 символов")
    @Column(name = "first_name", nullable = false)
    private String firstName;

    @NotBlank
    @Size(min = 2, max = 100)
    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Email(message = "Некорректный email")
    @NotNull @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Некорректный номер телефона")
    @Column(name = "phone_number")
    private String phoneNumber;

    @Past(message = "Дата рождения должна быть в прошлом")
    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Min(value = 0, message = "Баланс не может быть отрицательным")
    @Digits(integer = 10, fraction = 2)
    @Column(name = "account_balance")
    @Builder.Default
    private BigDecimal accountBalance = BigDecimal.ZERO;

    @CreditCardNumber // Валидация номера карты (опционально, можно убрать если не нужна)
    @Column(name = "credit_card", nullable = true)
    @Convert(converter = EncryptedStringConverter.class)
    private String creditCard;

    @URL(message = "Некорректный URL")
    @Column(name = "website")
    private String website;

    @Range(min = 0, max = 10, message = "Рейтинг должен быть от 0 до 10")
    @Column(name = "loyalty_rating")
    private Integer loyaltyRating;

    // Кастомная валидация
    @AssertTrue(message = "Email должен быть корпоративным")
    private boolean isValidCorporateEmail() {
        return email == null || email.endsWith("@company.com");
    }

    @PrePersist
    @PreUpdate
    private void validateEntity() {
        if (birthDate != null && birthDate.isAfter(LocalDate.now().minusYears(18))) {
            throw new IllegalStateException("Клиент должен быть старше 18 лет");
        }
    }
}
