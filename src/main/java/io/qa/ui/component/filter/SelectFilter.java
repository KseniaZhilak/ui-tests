package io.qa.ui.component.filter;

import com.codeborne.selenide.SelenideElement;
import io.qa.ui.core.ui.ByTestId;
import io.qa.ui.core.ui.UiComponent;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.StreamSupport;

import static com.codeborne.selenide.Condition.visible;

/** Multiselect: значение — множество доменных enum'ов, а не строк. */
public final class SelectFilter<E extends Enum<E> & SelectOption> extends UiComponent implements Filter<Set<E>> {

    private final String key;
    private final Class<E> optionType;

    SelectFilter(SelenideElement root, String key, Class<E> optionType) {
        super(root);
        this.key = key;
        this.optionType = optionType;
    }

    @Override
    public String key() {
        return key;
    }

    @Override
    public void apply(Set<E> values) {
        if (values.isEmpty()) {
            throw new IllegalArgumentException("Фильтр '" + key + "': передан пустой набор значений. "
                    + "Для сброса используйте clear().");
        }
        openDropdown();
        clearSelection();
        for (E option : values) {
            inner("option-" + option.optionId())
                    .as("опция '" + option.label() + "' фильтра '" + key + "'")
                    .shouldBe(visible)
                    .click();
        }
        closeDropdown();
    }

    @Override
    public void clear() {
        openDropdown();
        clearSelection();
        closeDropdown();
    }

    @Override
    public Set<E> readValue() {
        Set<E> selected = new LinkedHashSet<>();
        StreamSupport.stream(innerAll("selected-option").asFixedIterable().spliterator(), false)
                .map(chip -> chip.getAttribute("data-option"))
                .forEach(optionId -> selected.add(resolve(optionId)));
        return selected;
    }

    private void openDropdown() {
        SelenideElement toggle = inner("filter-toggle").as("раскрытие фильтра '" + key + "'").shouldBe(visible);
        if (!inner("filter-options").is(visible)) {
            toggle.click();
        }
        inner("filter-options").shouldBe(visible);
    }

    private void closeDropdown() {
        inner("filter-toggle").click();
        inner("filter-options").shouldNotBe(visible);
    }

    private void clearSelection() {
        SelenideElement clearButton = root.$(ByTestId.css("filter-clear"));
        if (clearButton.is(visible)) {
            clearButton.click();
        }
    }

    private E resolve(String optionId) {
        for (E candidate : optionType.getEnumConstants()) {
            if (candidate.optionId().equals(optionId)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Фильтр '" + key + "': UI отдал неизвестную опцию '" + optionId
                + "'. Известные: " + Arrays.toString(optionType.getEnumConstants()));
    }
}
