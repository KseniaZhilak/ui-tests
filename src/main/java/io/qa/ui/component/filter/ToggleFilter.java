package io.qa.ui.component.filter;

import com.codeborne.selenide.SelenideElement;
import io.qa.ui.core.ui.UiComponent;

import static com.codeborne.selenide.Condition.checked;
import static com.codeborne.selenide.Condition.visible;

public final class ToggleFilter extends UiComponent implements Filter<Boolean> {

    private final String key;

    ToggleFilter(SelenideElement root, String key) {
        super(root);
        this.key = key;
    }

    @Override
    public String key() {
        return key;
    }

    @Override
    public void apply(Boolean enabled) {
        SelenideElement toggle = inner("filter-value").as("переключатель '" + key + "'").shouldBe(visible);
        if (toggle.is(checked) != enabled) {
            toggle.click();
        }
        if (enabled) {
            toggle.shouldBe(checked);
        } else {
            toggle.shouldNotBe(checked);
        }
    }

    @Override
    public void clear() {
        apply(false);
    }

    @Override
    public Boolean readValue() {
        return inner("filter-value").shouldBe(visible).is(checked);
    }
}
