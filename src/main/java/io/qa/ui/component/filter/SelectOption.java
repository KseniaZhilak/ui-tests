package io.qa.ui.component.filter;

/** Реализуется доменными enum'ами страницы (OrderStatus, UserRole, ...). */
public interface SelectOption {

    /** Значение data-testid опции: option-{optionId}. */
    String optionId();

    String label();
}
