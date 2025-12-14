package ru.mentee.power.dto;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Результат фасетного поиска с агрегацией.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FacetedSearchResult<T> {
    private List<T> results;
    private Map<String, List<Facet>> facets;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Facet {
        private String value;
        private Long count;
    }
}
