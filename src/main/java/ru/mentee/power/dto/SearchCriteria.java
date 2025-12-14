package ru.mentee.power.dto;

import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Универсальные критерии поиска.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchCriteria {
    @Builder.Default private Map<String, Object> filters = new HashMap<>();
    private String searchText;
    private String[] searchFields;

    public SearchCriteria addFilter(String key, Object value) {
        if (filters == null) {
            filters = new HashMap<>();
        }
        filters.put(key, value);
        return this;
    }
}
