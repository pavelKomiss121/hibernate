package ru.mentee.power.hybrid;

/**
 * Выбор технологии для операции.
 */
public enum TechnologyChoice {
    HIBERNATE("Hibernate", "Использовать Hibernate для этой операции"),
    JDBC("JDBC", "Использовать JDBC для этой операции"),
    HYBRID("Hybrid", "Использовать гибридный подход");

    private final String name;
    private final String description;

    TechnologyChoice(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}
