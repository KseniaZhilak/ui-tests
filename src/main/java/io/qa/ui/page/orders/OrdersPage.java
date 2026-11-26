package io.qa.ui.page.orders;

import com.codeborne.selenide.SelenideElement;
import io.qa.ui.component.pagination.PageSize;
import io.qa.ui.component.pagination.PageSummary;
import io.qa.ui.component.pagination.PaginationComponent;
import io.qa.ui.component.table.RowCells;
import io.qa.ui.component.table.SortDirection;
import io.qa.ui.component.table.TableComponent;
import io.qa.ui.core.browser.Navigator;
import io.qa.ui.core.ui.ByTestId;
import io.qa.ui.core.ui.UiFormats;
import io.qameta.allure.Step;

import java.util.List;
import java.util.function.Consumer;

import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;

/**
 * Страница — композиция трёх независимых компонентов. Общего предка нет.
 * Наружу не выходит ни один SelenideElement и нет ни одной проверки.
 */
public final class OrdersPage {

    private static final String PATH = "/orders";

    private final SelenideElement root = $(ByTestId.css("page-orders"));
    private final TableComponent<OrderRow> table;
    private final PaginationComponent pagination;
    private final OrdersFilters filters;

    private OrdersPage() {
        this.table = new TableComponent<>(root.$(ByTestId.css("orders-table")), OrdersPage::toRow);
        this.pagination = new PaginationComponent(root.$(ByTestId.css("pagination")));
        this.filters = new OrdersFilters(root.$(ByTestId.css("filter-bar")));
    }

    @Step("Открыть реестр заказов")
    public static OrdersPage open() {
        Navigator.open(PATH);
        return new OrdersPage().awaitLoaded();
    }

    private OrdersPage awaitLoaded() {
        root.as("страница Orders").shouldBe(visible);
        table.awaitReady();
        return this;
    }

    public OrdersFilters filters() {
        return filters;
    }

    @Step("Применить фильтры реестра заказов")
    public OrdersPage filterBy(Consumer<OrdersFilters> specification) {
        specification.accept(filters);
        table.awaitRefreshAfter(filters::submit);
        return this;
    }

    @Step("Сбросить фильтры реестра заказов")
    public OrdersPage resetFilters() {
        table.awaitRefreshAfter(filters::resetAll);
        return this;
    }

    @Step("Отсортировать заказы по колонке {column} ({direction})")
    public OrdersPage sortBy(OrdersColumn column, SortDirection direction) {
        table.sortBy(column, direction);
        return this;
    }

    @Step("Перейти на страницу {page}")
    public OrdersPage goToPage(int page) {
        table.awaitRefreshAfter(() -> pagination.goToPage(page));
        return this;
    }

    @Step("Установить размер страницы {size}")
    public OrdersPage setPageSize(PageSize size) {
        // Если размер уже выбран, перерисовки не будет — ждать её нечего (не ветвление теста, а корректность ожидания)
        if (pagination.selectedPageSize() == size) {
            return this;
        }
        table.awaitRefreshAfter(() -> pagination.setPageSize(size));
        return this;
    }

    public List<OrderRow> visibleOrders() {
        return table.rows();
    }

    public PageSummary paginationSummary() {
        return pagination.summary();
    }

    public PageSize selectedPageSize() {
        return pagination.selectedPageSize();
    }

    public int currentPage() {
        return pagination.currentPage();
    }

    public SortDirection currentSort(OrdersColumn column) {
        return table.currentSort(column);
    }

    public boolean isEmptyStateShown() {
        return table.isEmptyStateShown();
    }

    private static OrderRow toRow(RowCells cells) {
        return new OrderRow(
                cells.text(OrdersColumn.NUMBER),
                cells.text(OrdersColumn.CUSTOMER),
                OrderStatus.fromOptionId(cells.attribute(OrdersColumn.STATUS, "data-value")),
                UiFormats.parseMoney(cells.text(OrdersColumn.TOTAL)),
                UiFormats.parseDate(cells.text(OrdersColumn.CREATED_AT)));
    }
}
