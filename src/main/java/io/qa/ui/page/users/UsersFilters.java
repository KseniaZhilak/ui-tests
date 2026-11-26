package io.qa.ui.page.users;

import com.codeborne.selenide.SelenideElement;
import io.qa.ui.component.filter.FilterBar;
import io.qa.ui.component.filter.SelectFilter;
import io.qa.ui.component.filter.TextFilter;
import io.qa.ui.component.filter.ToggleFilter;

/** Набор фильтров Users заведомо другой: нет диапазона дат, есть toggle. */
public final class UsersFilters {

    private final FilterBar bar;

    UsersFilters(SelenideElement filterBarRoot) {
        this.bar = new FilterBar(filterBarRoot);
    }

    public TextFilter email() {
        return bar.text("email");
    }

    public SelectFilter<UserRole> role() {
        return bar.select("role", UserRole.class);
    }

    public ToggleFilter onlyActive() {
        return bar.toggle("only-active");
    }

    void submit() {
        bar.submit();
    }

    void resetAll() {
        bar.resetAll();
    }
}
