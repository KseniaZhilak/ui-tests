package io.qa.ui.component.table;

import com.codeborne.selenide.SelenideElement;
import io.qa.ui.core.config.ConfigProvider;
import io.qa.ui.core.ui.ByTestId;
import io.qa.ui.core.ui.UiComponent;
import io.qa.ui.core.ui.UiConditions;
import io.qameta.allure.Allure;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.visible;

public final class TableComponent<T> extends UiComponent {

    private static final String RENDER_ID = "data-render-id";
    /** Контракт фронта: data-render-id = "0" до тех пор, пока первый рендер не завершён. */
    private static final String NOT_RENDERED_YET = "0";
    private static final String ROWS_OR_EMPTY = "[data-testid='table-row'], [data-testid='table-empty']";

    private final RowMapper<T> rowMapper;
    private final Duration refreshTimeout;

    public TableComponent(SelenideElement root, RowMapper<T> rowMapper) {
        this(root, rowMapper, Duration.ofMillis(ConfigProvider.get().tableRefreshTimeoutMs()));
    }

    public TableComponent(SelenideElement root, RowMapper<T> rowMapper, Duration refreshTimeout) {
        super(root);
        this.rowMapper = rowMapper;
        this.refreshTimeout = refreshTimeout;
    }

    /** Таблица отрисована: лоадер скрыт и есть либо строки, либо пустое состояние. */
    public TableComponent<T> awaitReady() {
        root.as("таблица").shouldBe(visible, refreshTimeout);
        // Без этой проверки открытие страницы могло бы «успеть» прочитать таблицу до первой отрисовки
        root.should(UiConditions.attributeChangedFrom(RENDER_ID, NOT_RENDERED_YET), refreshTimeout);
        loader().shouldBe(hidden, refreshTimeout);
        root.$(ROWS_OR_EMPTY).as("строки таблицы или пустое состояние").should(exist, refreshTimeout);
        return this;
    }

    /**
     * Выполняет действие (фильтрация, пагинация, сортировка) и ждёт ИМЕННО перерисовку,
     * а не просто появление строк: иначе читаются данные предыдущего состояния.
     */
    public TableComponent<T> awaitRefreshAfter(Runnable action) {
        String before = root.shouldBe(visible).getAttribute(RENDER_ID);
        if (before == null) {
            throw new IllegalStateException("Таблица не отдаёт атрибут " + RENDER_ID
                    + ". Без него невозможно детерминированно дождаться перерисовки — см. README, раздел про testid.");
        }
        action.run();
        root.should(UiConditions.attributeChangedFrom(RENDER_ID, before), refreshTimeout);
        return awaitReady();
    }

    public List<T> rows() {
        awaitReady();
        if (isEmptyStateShown()) {
            return List.of();
        }
        return StreamSupport.stream(innerAll("table-row").asFixedIterable().spliterator(), false)
                .map(row -> rowMapper.map(new RowCells(row)))
                .collect(Collectors.toList());
    }

    public Optional<T> findRow(Predicate<T> predicate) {
        return rows().stream().filter(predicate).findFirst();
    }

    public int visibleRowCount() {
        return rows().size();
    }

    public boolean isEmptyStateShown() {
        return inner("table-empty").is(visible);
    }

    public SortDirection currentSort(ColumnKey column) {
        return SortDirection.fromAria(sortButton(column).getAttribute("aria-sort"));
    }

    /**
     * Кнопка сортировки циклична: none -> ascending -> descending -> none.
     * Количество кликов вычисляется арифметически, это НЕ polling-цикл.
     */
    public TableComponent<T> sortBy(ColumnKey column, SortDirection direction) {
        Allure.step("Сортировка по колонке '" + column.title() + "': " + direction);
        int clicks = Math.floorMod(direction.ordinal() - currentSort(column).ordinal(), SortDirection.values().length);
        for (int i = 0; i < clicks; i++) {
            awaitRefreshAfter(() -> sortButton(column).click());
        }
        sortButton(column).should(UiConditions.ariaSort(direction.ariaValue()), refreshTimeout);
        return this;
    }

    private SelenideElement sortButton(ColumnKey column) {
        return root.$(ByTestId.css("sort", column.testId()))
                .as("кнопка сортировки колонки '" + column.title() + "'")
                .shouldBe(visible);
    }

    private SelenideElement loader() {
        return inner("table-loader").as("лоадер таблицы");
    }
}
