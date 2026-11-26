package io.qa.ui.core.diagnostics;

import com.codeborne.selenide.WebDriverRunner;
import io.qameta.allure.Allure;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.logging.LogEntries;
import org.openqa.selenium.logging.LogType;

import java.util.Optional;
import java.util.stream.Collectors;

public final class Diagnostics {

    private Diagnostics() {
    }

    /** Вызывается только из инфраструктуры (Spock extension), не из тестов. */
    public static void captureFailure(String context) {
        if (!WebDriverRunner.hasWebDriverStarted()) {
            Allure.addAttachment("Контекст падения", "Браузер не был запущен: " + context);
            return;
        }
        WebDriver driver = WebDriverRunner.getWebDriver();
        Allure.addAttachment("URL на момент падения", driver.getCurrentUrl());
        Allure.getLifecycle().addAttachment("Скриншот — " + context, "image/png", "png",
                ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES));
        Allure.addAttachment("Page source — " + context, "text/html", driver.getPageSource(), ".html");
        browserLogs(driver).ifPresent(logs ->
                Allure.addAttachment("Console log — " + context, "text/plain", logs, ".log"));
    }

    private static Optional<String> browserLogs(WebDriver driver) {
        try {
            LogEntries entries = driver.manage().logs().get(LogType.BROWSER);
            String text = entries.getAll().stream()
                    .map(e -> e.getLevel() + " " + e.getMessage())
                    .collect(Collectors.joining(System.lineSeparator()));
            return text.isBlank() ? Optional.empty() : Optional.of(text);
        } catch (RuntimeException e) {
            // Драйвер может не поддерживать browser logs (Firefox) — это не причина ронять отчёт
            return Optional.of("Browser logs недоступны: " + e.getMessage());
        }
    }
}
