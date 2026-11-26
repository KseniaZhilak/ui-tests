package io.qa.ui.component.pagination;

public enum PageSize {

    SIZE_10(10),
    SIZE_25(25),
    SIZE_50(50),
    SIZE_100(100);

    private final int value;

    PageSize(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }
}
