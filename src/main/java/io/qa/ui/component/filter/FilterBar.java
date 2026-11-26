package io.qa.ui.component.filter;

import com.codeborne.selenide.SelenideElement;
import io.qa.ui.core.config.ConfigProvider;
import io.qa.ui.core.ui.ByTestId;
import io.qa.ui.core.ui.UiComponent;

import java.time.Duration;

import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.visible;

/**
 * Фабрика фильтров, заскоупленная на панель фильтров конкретной страницы.
 * Фильтры создаются лениво — конструктор страницы не трогает DOM.
 */
public final class FilterBar extends UiComponent {

    private final Duration fastTimeout = Duration.ofMillis(ConfigProvider.get().fastTimeoutMs());

    public FilterBar(SelenideElement root) {
        super(root);
    }

    public TextFilter text(String key) {
        return new TextFilter(filterRoot(key), key);
    }

    public <E extends Enum<E> & SelectOption> SelectFilter<E> select(String key, Class<E> optionType) {
        return new SelectFilter<>(filterRoot(key), key, optionType);
    }

    public DateRangeFilter dateRange(String key) {
        return new DateRangeFilter(filterRoot(key), key);
    }

    public ToggleFilter toggle(String key) {
        return new ToggleFilter(filterRoot(key), key);
    }

    public void submit() {
        inner("filters-apply").as("кнопка «Применить фильтры»").shouldBe(visible, enabled).click();
    }

    public void resetAll() {
        inner("filters-reset").as("кнопка «Сбросить фильтры»").shouldBe(visible, enabled).click();
    }

    /**
     * Fail fast: если фильтра нет на этой странице, падаем за fast.timeout.ms
     * с осмысленным сообщением, а не через полный timeout с «element not found».
     */
    private SelenideElement filterRoot(String key) {
        return root.$(ByTestId.css("filter", key))
                .as("фильтр '" + key + "' панели фильтров")
                .should(exist, fastTimeout);
    }
}
