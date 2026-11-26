package io.qa.ui.page.users;

import io.qa.ui.component.table.ColumnKey;

public enum UsersColumn implements ColumnKey {

    EMAIL("email", "E-mail"),
    FULL_NAME("full-name", "ФИО"),
    ROLE("role", "Роль"),
    ACTIVE("active", "Активен"),
    LAST_LOGIN("last-login", "Последний вход");

    private final String testId;
    private final String title;

    UsersColumn(String testId, String title) {
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
