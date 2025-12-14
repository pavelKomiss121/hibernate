package ru.mentee.power.search;

import java.util.List;
import ru.mentee.power.dto.FacetedSearchResult;
import ru.mentee.power.dto.Page;
import ru.mentee.power.dto.Pageable;
import ru.mentee.power.dto.SearchCriteria;

/**
 * Универсальный поисковый сервис.
 */
public interface SearchService<T> {

    /**
     * Поиск с динамическими критериями.
     *
     * @param criteria критерии поиска
     * @param pageable параметры пагинации
     * @return страница результатов
     */
    Page<T> search(SearchCriteria criteria, Pageable pageable);

    /**
     * Полнотекстовый поиск.
     *
     * @param query поисковый запрос
     * @param fields поля для поиска
     * @return результаты поиска
     */
    List<T> fullTextSearch(String query, String... fields);

    /**
     * Фасетный поиск с агрегацией.
     *
     * @param criteria критерии
     * @return результаты с фасетами
     */
    FacetedSearchResult<T> facetedSearch(SearchCriteria criteria);

    /**
     * Автодополнение для поиска.
     *
     * @param prefix префикс
     * @param field поле
     * @param limit максимум результатов
     * @return варианты автодополнения
     */
    List<String> autocomplete(String prefix, String field, int limit);
}
