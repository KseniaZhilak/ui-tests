package io.qa.ui.component.table;

public enum SortDirection {

    NONE("none"),
    ASCENDING("ascending"),
    DESCENDING("descending");

    private final String ariaValue;

    SortDirection(String ariaValue) {
        this.ariaValue = ariaValue;
    }

    public String ariaValue() {
        return ariaValue;
    }

    public static SortDirection fromAria(String value) {
        for (SortDirection direction : values()) {
            if (direction.ariaValue.equalsIgnoreCase(value)) {
                return direction;
            }
        }
        return NONE;
    }
}
