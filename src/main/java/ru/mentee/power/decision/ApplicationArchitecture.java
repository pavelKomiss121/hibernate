package ru.mentee.power.decision;

import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.mentee.power.hybrid.TechnologyChoice;

/**
 * Архитектура приложения.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationArchitecture {
    @Builder.Default private Map<String, TechnologyChoice> components = new HashMap<>();

    public void addComponent(String name, TechnologyChoice technology) {
        components.put(name, technology);
    }
}
