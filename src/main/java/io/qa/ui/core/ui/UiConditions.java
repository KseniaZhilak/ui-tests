package io.qa.ui.core.ui;

import com.codeborne.selenide.CheckResult;
import com.codeborne.selenide.Condition;
import com.codeborne.selenide.WebElementCondition;
import com.codeborne.selenide.Driver;
import org.openqa.selenium.WebElement;

public final class UiConditions {

    private UiConditions() {
    }

    /**
     * Ключевое условие «таблица перерисовалась»: фронт меняет data-render-id на каждый рендер body.
     * Снимает race, когда после фильтрации успеваем прочитать старые строки.
     */
    public static WebElementCondition attributeChangedFrom(String attribute, String previousValue) {
        return new AttributeChanged(attribute, previousValue);
    }

    public static WebElementCondition ariaSort(String expected) {
        return Condition.attribute("aria-sort", expected);
    }

    private static final class AttributeChanged extends WebElementCondition {

        private final String attribute;
        private final String previousValue;

        private AttributeChanged(String attribute, String previousValue) {
            super("атрибут '" + attribute + "' изменился со значения '" + previousValue + "'");
            this.attribute = attribute;
            this.previousValue = previousValue;
        }

        @Override
        public CheckResult check(Driver driver, WebElement element) {
            String actual = element.getAttribute(attribute);
            boolean met = actual != null && !actual.equals(previousValue);
            return new CheckResult(met, "'" + attribute + "'='" + actual + "'");
        }
    }
}
