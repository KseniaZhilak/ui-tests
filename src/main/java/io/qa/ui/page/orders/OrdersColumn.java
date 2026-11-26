package io.qa.ui.page.orders;

import io.qa.ui.component.table.ColumnKey;

public enum OrdersColumn implements ColumnKey {

    NUMBER("order-number", "Номер"),
    CUSTOMER("customer", "Клиент"),
    STATUS("status", "Статус"),
    TOTAL("total", "Сумма"),
    CREATED_AT("created-at", "Создан");

    private final String testId;
    private final String title;

    OrdersColumn(String testId, String title) {
        this.testId = testId;
        this.title = title;
    }

    @Override
    public String testId() {
        return testId;
    }

    @Override
    public String title() {
        return title;
    }
}
