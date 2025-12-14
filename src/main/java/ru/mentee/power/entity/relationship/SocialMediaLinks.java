package ru.mentee.power.entity.relationship;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Встраиваемый класс для социальных сетей.
 */
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SocialMediaLinks {

    @Column(name = "linkedin_url")
    private String linkedinUrl;

    @Column(name = "github_url")
    private String githubUrl;

    @Column(name = "twitter_url")
    private String twitterUrl;
}
