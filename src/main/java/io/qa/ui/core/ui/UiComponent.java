package io.qa.ui.core.ui;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

/**
 * Базовый компонент. Не является базовой страницей: содержит ТОЛЬКО скоупинг внутри root.
 * Любой поиск идёт от root ({@code root.$(...)}), поэтому два одинаковых компонента
 * на одной странице не конфликтуют.
 */
public abstract class UiComponent {

    protected final SelenideElement root;

    protected UiComponent(SelenideElement root) {
        this.root = root;
    }

    /** Скоупинг: ищем ВНУТРИ своего root, а не по всему документу. */
    protected SelenideElement inner(String testId) {
        return root.$(ByTestId.css(testId));
    }

    protected ElementsCollection innerAll(String testId) {
        return root.$$(ByTestId.css(testId));
    }
}
