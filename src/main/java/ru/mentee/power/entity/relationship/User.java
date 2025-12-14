package ru.mentee.power.entity.relationship;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * OneToOne через общий первичный ключ.
 * Сущность пользователя с READ_WRITE кэшированием.
 */
@Entity(name = "RelationshipUser")
@Table(name = "users")
@Cacheable
@Cache(
        usage = CacheConcurrencyStrategy.READ_WRITE,
        region = "ru.mentee.power.entity.relationship.User")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", unique = true, nullable = false, length = 50)
    @NaturalId
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private String username;

    @Column(name = "email", unique = true, nullable = false)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "active")
    @Builder.Default
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // OneToOne с общим первичным ключом
    @OneToOne(
            mappedBy = "user",
            cascade = CascadeType.ALL,
            fetch = FetchType.LAZY,
            optional = false)
    @PrimaryKeyJoinColumn // Использует тот же ID
    private UserProfile profile;

    // OneToOne через внешний ключ
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(
            name = "address_id",
            referencedColumnName = "id",
            foreignKey = @ForeignKey(name = "fk_user_address"))
    private Address address;

    // Вспомогательные методы
    public void setProfile(UserProfile profile) {
        if (profile == null) {
            if (this.profile != null) {
                this.profile.setUser(null);
            }
        } else {
            profile.setUser(this);
        }
        this.profile = profile;
    }
}
