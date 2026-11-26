package io.qa.ui.testdata;

import io.qa.ui.core.config.ConfigProvider;

import java.util.Locale;
import java.util.UUID;

public final class TestData {

    private TestData() {
    }

    public static OrdersBackdoor orders() {
        return new RestOrdersBackdoor(ConfigProvider.get().apiBaseUrl());
    }

    /** Уникальный тег прогона: изолирует данные спеки от прода и от параллельных спек. */
    public static String runTag() {
        return "RUN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }
}
