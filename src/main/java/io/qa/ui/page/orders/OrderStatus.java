package io.qa.ui.page.orders;

import io.qa.ui.component.filter.SelectOption;

public enum OrderStatus implements SelectOption {

    NEW("new", "Новый"),
    PAID("paid", "Оплачен"),
    SHIPPED("shipped", "Отгружен"),
    CANCELLED("cancelled", "Отменён");

    private final String optionId;
    private final String label;

    OrderStatus(String optionId, String label) {
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

    public static OrderStatus fromOptionId(String optionId) {
        for (OrderStatus status : values()) {
            if (status.optionId.equals(optionId)) {
                return status;
            }
        }
        throw new IllegalStateException("Неизвестный статус заказа в UI: '" + optionId + "'");
    }
}
