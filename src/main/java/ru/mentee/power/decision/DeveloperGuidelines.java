package ru.mentee.power.decision;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Guidelines для разработчиков.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeveloperGuidelines {
    private String projectName;
    @Builder.Default private List<GuidelineSection> sections = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GuidelineSection {
        private String title;
        private String description;
        @Builder.Default private List<String> rules = new ArrayList<>();
    }

    public void addSection(GuidelineSection section) {
        if (sections == null) {
            sections = new ArrayList<>();
        }
        sections.add(section);
    }
}
