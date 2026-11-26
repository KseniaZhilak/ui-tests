package io.qa.ui

import com.codeborne.selenide.logevents.SelenideLogger
import io.qa.ui.core.browser.BrowserLifecycle
import io.qameta.allure.selenide.AllureSelenide
import spock.lang.Specification

/**
 * Единственный базовый класс спек. Содержит только жизненный цикл драйвера и логирование,
 * никакой бизнес-логики и никаких protected-полей страниц.
 */
abstract class BaseSpec extends Specification {

    private static final String LISTENER = "allure-selenide"

    def setupSpec() {
        BrowserLifecycle.configure()
    }

    def setup() {
        // SelenideLogger хранит слушателей в ThreadLocal — регистрация безопасна при параллельном запуске
        SelenideLogger.addListener(LISTENER, new AllureSelenide()
                .screenshots(false)
                .savePageSource(false))
    }

    def cleanup() {
        SelenideLogger.removeListener(LISTENER)
        BrowserLifecycle.closeBrowser()
    }
}
