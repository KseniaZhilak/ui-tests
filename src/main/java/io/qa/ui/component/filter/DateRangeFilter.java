package io.qa.ui.component.filter;

import com.codeborne.selenide.SelenideElement;
import io.qa.ui.core.ui.UiComponent;
import io.qa.ui.core.ui.UiFormats;

import java.time.LocalDate;

import static com.codeborne.selenide.Condition.value;
import static com.codeborne.selenide.Condition.visible;

public final class DateRangeFilter extends UiComponent implements Filter<DateRange> {

    private final String key;

    DateRangeFilter(SelenideElement root, String key) {
        super(root);
        this.key = key;
    }

    @Override
    public String key() {
        return key;
    }

    @Override
    public void apply(DateRange range) {
        setDate("filter-date-from", range.from());
        setDate("filter-date-to", range.to());
    }

    @Override
    public void clear() {
        setDate("filter-date-from", null);
        setDate("filter-date-to", null);
    }

    @Override
    public DateRange readValue() {
        return new DateRange(readDate("filter-date-from"), readDate("filter-date-to"));
    }

    private void setDate(String testId, LocalDate date) {
        String text = date == null ? "" : UiFormats.DATE.format(date);
        inner(testId).as("дата '" + testId + "' фильтра '" + key + "'")
                .shouldBe(visible)
                .setValue(text)
                .shouldHave(value(text));
    }

    private LocalDate readDate(String testId) {
        String text = inner(testId).shouldBe(visible).getValue();
        return text == null || text.isBlank() ? null : UiFormats.parseDate(text);
    }
}
