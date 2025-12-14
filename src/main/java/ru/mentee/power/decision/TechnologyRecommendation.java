package ru.mentee.power.decision;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.mentee.power.hybrid.TechnologyChoice;

/**
 * Рекомендация по выбору технологии.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TechnologyRecommendation {
    private TechnologyChoice recommendedTechnology;
    private String rationale;
    private double confidence;
    @Builder.Default private List<String> reasons = new ArrayList<>();
    @Builder.Default private List<String> warnings = new ArrayList<>();

    public void addReason(String reason) {
        if (reasons == null) {
            reasons = new ArrayList<>();
        }
        reasons.add(reason);
    }

    public void addWarning(String warning) {
        if (warnings == null) {
            warnings = new ArrayList<>();
        }
        warnings.add(warning);
    }
}
