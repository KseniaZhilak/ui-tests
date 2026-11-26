package io.qa.ui.component.table;

/** Колонка таблицы: страница объявляет свой enum, компонент не знает конкретных колонок. */
public interface ColumnKey {

    /** Суффикс data-testid: ячейка = cell-{testId}, кнопка сортировки = sort-{testId}. */
    String testId();

    /** Человекочитаемое имя для сообщений об ошибках. */
    String title();
}
