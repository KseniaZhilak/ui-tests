package io.qa.ui.component.filter;

/**
 * Единый контракт фильтра. Параметр V делает невозможной передачу строки туда,
 * где ожидается множество enum'ов или диапазон дат.
 */
public sealed interface Filter<V> permits TextFilter, SelectFilter, DateRangeFilter, ToggleFilter {

    String key();

    void apply(V value);

    void clear();

    V readValue();
}
