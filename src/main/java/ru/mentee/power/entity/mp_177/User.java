package ru.mentee.power.entity.mp_177;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Базовая сущность пользователя.
 */
@Entity
@Table(name = "users", schema = "mentee_power")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Slf4j
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version")
    private Long version;

    // Callback методы жизненного цикла
    @PrePersist
    protected void onCreate() {
        log.debug("PrePersist: создание пользователя {}", username);
    }

    @PostPersist
    protected void afterCreate() {
        log.debug("PostPersist: пользователь {} создан с ID {}", username, id);
    }

    @PreUpdate
    protected void onUpdate() {
        log.debug("PreUpdate: обновление пользователя {}", username);
    }

    @PostLoad
    protected void afterLoad() {
        log.debug("PostLoad: пользователь {} загружен из БД", username);
    }
}
