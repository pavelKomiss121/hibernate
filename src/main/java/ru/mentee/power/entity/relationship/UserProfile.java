package ru.mentee.power.entity.relationship;

import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * UserProfile с общим ID.
 */
@Entity
@Table(name = "user_profiles")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfile {

    @Id private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId // Использует ID от User
    @JoinColumn(name = "id", foreignKey = @ForeignKey(name = "fk_profile_user"))
    private User user;

    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Embedded private SocialMediaLinks socialMedia;
}
