package io.qa.ui.page.orders;

import com.codeborne.selenide.SelenideElement;
import io.qa.ui.component.filter.DateRangeFilter;
import io.qa.ui.component.filter.FilterBar;
import io.qa.ui.component.filter.SelectFilter;
import io.qa.ui.component.filter.TextFilter;

/**
 * Типизированный фасад фильтров Orders. Набор фильтров объявлен здесь:
 * обращение к несуществующему фильтру (например, role()) не компилируется.
 */
public final class OrdersFilters {

    private final FilterBar bar;

    OrdersFilters(SelenideElement filterBarRoot) {
        this.bar = new FilterBar(filterBarRoot);
    }

    public TextFilter customer() {
        return bar.text("customer");
    }

    public SelectFilter<OrderStatus> status() {
        return bar.select("status", OrderStatus.class);
    }

    public DateRangeFilter createdAt() {
        return bar.dateRange("created-at");
    }

    void submit() {
        bar.submit();
    }

    void resetAll() {
        bar.resetAll();
    }
}
