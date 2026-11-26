package io.qa.ui.component.filter;

import com.codeborne.selenide.SelenideElement;
import io.qa.ui.core.ui.UiComponent;

import static com.codeborne.selenide.Condition.value;
import static com.codeborne.selenide.Condition.visible;

public final class TextFilter extends UiComponent implements Filter<String> {

    private final String key;

    TextFilter(SelenideElement root, String key) {
        super(root);
        this.key = key;
    }

    @Override
    public String key() {
        return key;
    }

    @Override
    public void apply(String text) {
        inner("filter-value").as("текстовый фильтр '" + key + "'")
                .shouldBe(visible)
                .setValue(text)
                .shouldHave(value(text));
    }

    @Override
    public void clear() {
        inner("filter-value").shouldBe(visible).setValue("").shouldHave(value(""));
    }

    @Override
    public String readValue() {
        return inner("filter-value").shouldBe(visible).getValue();
    }
}
