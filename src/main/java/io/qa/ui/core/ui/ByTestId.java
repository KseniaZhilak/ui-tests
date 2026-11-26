package io.qa.ui.core.ui;

import org.openqa.selenium.By;

/** Единая точка формирования локаторов. Нигде в коде не должно быть «голых» CSS/XPath-строк. */
public final class ByTestId {

    private ByTestId() {
    }

    public static String css(String testId) {
        return "[data-testid='" + testId + "']";
    }

    public static String css(String prefix, String testId) {
        return css(prefix + "-" + testId);
    }

    public static By by(String testId) {
        return By.cssSelector(css(testId));
    }
}
