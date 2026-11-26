package io.qa.ui.component.filter;

import java.time.LocalDate;

/** Границы включительные; null означает «граница не задана». */
public record DateRange(LocalDate from, LocalDate to) {

    public static DateRange of(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("Некорректный диапазон дат: from=" + from + " позже to=" + to);
        }
        return new DateRange(from, to);
    }

    public boolean contains(LocalDate date) {
        boolean afterFrom = from == null || !date.isBefore(from);
        boolean beforeTo = to == null || !date.isAfter(to);
        return afterFrom && beforeTo;
    }
}
