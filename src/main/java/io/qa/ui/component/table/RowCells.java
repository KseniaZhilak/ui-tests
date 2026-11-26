package io.qa.ui.component.table;

import com.codeborne.selenide.SelenideElement;
import io.qa.ui.core.ui.ByTestId;

import static com.codeborne.selenide.Condition.visible;

/**
 * Доступ к ячейкам одной строки. Наружу отдаёт только примитивы/строки —
 * SelenideElement за пределы компонента не выходит.
 */
public final class RowCells {

    private final SelenideElement row;

    RowCells(SelenideElement row) {
        this.row = row;
    }

    public String text(ColumnKey column) {
        return cell(column).getText().trim();
    }

    public String attribute(ColumnKey column, String attribute) {
        String value = cell(column).getAttribute(attribute);
        if (value == null) {
            throw new IllegalStateException(
                    "В ячейке колонки '" + column.title() + "' отсутствует атрибут '" + attribute + "'");
        }
        return value;
    }

    public boolean flag(ColumnKey column) {
        return Boolean.parseBoolean(attribute(column, "data-value"));
    }

    public String rowId() {
        return row.getAttribute("data-row-id");
    }

    private SelenideElement cell(ColumnKey column) {
        return row.$(ByTestId.css("cell", column.testId()))
                .as("ячейка колонки '" + column.title() + "'")
                .shouldBe(visible);
    }
}
