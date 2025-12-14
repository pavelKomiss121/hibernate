package ru.mentee.power.optimization;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * Определение графа для загрузки.
 */
@Data
public class GraphDefinition {

    private List<String> attributeNodes = new ArrayList<>();
    private List<SubgraphDefinition> subgraphs = new ArrayList<>();

    @Data
    public static class SubgraphDefinition {
        private String name;
        private String parentPath;
        private List<String> attributeNodes = new ArrayList<>();
    }
}
