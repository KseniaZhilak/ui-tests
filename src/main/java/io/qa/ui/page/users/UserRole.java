package io.qa.ui.page.users;

import io.qa.ui.component.filter.SelectOption;

public enum UserRole implements SelectOption {

    ADMIN("admin", "Администратор"),
    MANAGER("manager", "Менеджер"),
    VIEWER("viewer", "Наблюдатель");

    private final String optionId;
    private final String label;

    UserRole(String optionId, String label) {
        this.optionId = optionId;
        this.label = label;
    }

    @Override
    public String optionId() {
        return optionId;
    }

    @Override
    public String label() {
        return label;
    }

    public static UserRole fromOptionId(String optionId) {
        for (UserRole role : values()) {
            if (role.optionId.equals(optionId)) {
                return role;
            }
        }
        throw new IllegalStateException("Неизвестная роль пользователя в UI: '" + optionId + "'");
    }
}
