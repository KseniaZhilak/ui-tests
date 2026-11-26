package io.qa.ui.component.table;

@FunctionalInterface
public interface RowMapper<T> {

    /** Маппинг DOM-строки в доменный record. Реализуется на уровне страницы. */
    T map(RowCells cells);
}
