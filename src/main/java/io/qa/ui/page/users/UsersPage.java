package io.qa.ui.page.users;

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

public final class UsersPage {

    private static final String PATH = "/users";

    private final SelenideElement root = $(ByTestId.css("page-users"));
    private final TableComponent<UserRow> table;
    private final PaginationComponent pagination;
    private final UsersFilters filters;

    private UsersPage() {
        this.table = new TableComponent<>(root.$(ByTestId.css("users-table")), UsersPage::toRow);
        this.pagination = new PaginationComponent(root.$(ByTestId.css("pagination")));
        this.filters = new UsersFilters(root.$(ByTestId.css("filter-bar")));
    }

    @Step("Открыть реестр пользователей")
    public static UsersPage open() {
        Navigator.open(PATH);
        return new UsersPage().awaitLoaded();
    }

    private UsersPage awaitLoaded() {
        root.as("страница Users").shouldBe(visible);
        table.awaitReady();
        return this;
    }

    public UsersFilters filters() {
        return filters;
    }

    @Step("Применить фильтры реестра пользователей")
    public UsersPage filterBy(Consumer<UsersFilters> specification) {
        specification.accept(filters);
        table.awaitRefreshAfter(filters::submit);
        return this;
    }

    @Step("Сбросить фильтры реестра пользователей")
    public UsersPage resetFilters() {
        table.awaitRefreshAfter(filters::resetAll);
        return this;
    }

    @Step("Отсортировать пользователей по колонке {column} ({direction})")
    public UsersPage sortBy(UsersColumn column, SortDirection direction) {
        table.sortBy(column, direction);
        return this;
    }

    @Step("Перейти на страницу {page}")
    public UsersPage goToPage(int page) {
        table.awaitRefreshAfter(() -> pagination.goToPage(page));
        return this;
    }

    @Step("Установить размер страницы {size}")
    public UsersPage setPageSize(PageSize size) {
        if (pagination.selectedPageSize() == size) {
            return this;
        }
        table.awaitRefreshAfter(() -> pagination.setPageSize(size));
        return this;
    }

    public List<UserRow> visibleUsers() {
        return table.rows();
    }

    public PageSummary paginationSummary() {
        return pagination.summary();
    }

    public SortDirection currentSort(UsersColumn column) {
        return table.currentSort(column);
    }

    public boolean isEmptyStateShown() {
        return table.isEmptyStateShown();
    }

    private static UserRow toRow(RowCells cells) {
        return new UserRow(
                cells.text(UsersColumn.EMAIL),
                cells.text(UsersColumn.FULL_NAME),
                UserRole.fromOptionId(cells.attribute(UsersColumn.ROLE, "data-value")),
                cells.flag(UsersColumn.ACTIVE),
                UiFormats.parseDate(cells.text(UsersColumn.LAST_LOGIN)));
    }
}
