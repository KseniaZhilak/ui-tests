package io.qa.ui.core.browser;

import com.codeborne.selenide.Selenide;
import io.qa.ui.core.config.ConfigProvider;

public final class Navigator {

    private Navigator() {
    }

    public static void open(String path) {
        Selenide.open(ConfigProvider.get().baseUrl() + path);
    }
}
