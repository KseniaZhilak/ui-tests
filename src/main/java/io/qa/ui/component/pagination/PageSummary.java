package io.qa.ui.component.pagination;

/** Счётчик «N–M из K». Инварианты проверяются в спеках, компонент только отдаёт факты. */
public record PageSummary(int from, int to, int total) {

    public int size() {
        return total == 0 ? 0 : to - from + 1;
    }

    public boolean isEmpty() {
        return total == 0;
    }
}
