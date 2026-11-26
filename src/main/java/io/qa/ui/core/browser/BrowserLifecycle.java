package io.qa.ui.core.browser;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.WebDriverRunner;
import io.qa.ui.core.config.ConfigProvider;
import io.qa.ui.core.config.FrameworkConfig;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.logging.LoggingPreferences;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

public final class BrowserLifecycle {

    private static final AtomicBoolean CONFIGURED = new AtomicBoolean(false);

    private BrowserLifecycle() {
    }

    /** Идемпотентно: Selenide.Configuration глобален, записываем его один раз одинаковыми значениями. */
    public static void configure() {
        if (!CONFIGURED.compareAndSet(false, true)) {
            return;
        }
        FrameworkConfig config = ConfigProvider.get();

        Configuration.baseUrl = config.baseUrl();
        Configuration.browser = config.browser();
        Configuration.browserSize = config.browserSize();
        Configuration.timeout = config.timeoutMs();
        Configuration.pageLoadTimeout = config.timeoutMs() * 5;
        Configuration.reportsFolder = "build/reports/selenide";
        Configuration.browserCapabilities = capabilities(config);

        if (!config.remoteUrl().isBlank()) {
            Configuration.remote = config.remoteUrl();
        }
    }

    /**
     * headless задаётся аргументом браузера, а не Configuration.headless:
     * в Selenide 7 этот флаг объявлен устаревшим, а capabilities работают и локально, и в Selenoid.
     */
    private static MutableCapabilities capabilities(FrameworkConfig config) {
        MutableCapabilities caps = chromeAware(config);

        LoggingPreferences logs = new LoggingPreferences();
        logs.enable(LogType.BROWSER, Level.ALL);
        caps.setCapability("goog:loggingPrefs", logs);

        if (!config.remoteUrl().isBlank()) {
            Map<String, Object> selenoid = new HashMap<>();
            selenoid.put("enableVNC", config.selenoidVnc());
            selenoid.put("enableVideo", config.selenoidVideo());
            selenoid.put("screenResolution", config.browserSize() + "x24");
            caps.setCapability("selenoid:options", selenoid);
        }
        return caps;
    }

    private static MutableCapabilities chromeAware(FrameworkConfig config) {
        if (!"chrome".equalsIgnoreCase(config.browser())) {
            return new MutableCapabilities();
        }
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--disable-dev-shm-usage", "--no-sandbox", "--lang=ru-RU");
        if (config.headless()) {
            options.addArguments("--headless=new");
        }
        return options;
    }

    public static void closeBrowser() {
        if (WebDriverRunner.hasWebDriverStarted()) {
            Selenide.closeWebDriver();
        }
    }
}
