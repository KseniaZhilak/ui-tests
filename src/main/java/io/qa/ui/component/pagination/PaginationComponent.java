package io.qa.ui.component.pagination;

import com.codeborne.selenide.SelenideElement;
import io.qa.ui.core.ui.ByTestId;
import io.qa.ui.core.ui.UiComponent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.matchText;
import static com.codeborne.selenide.Condition.visible;

/**
 * Компонент не знает про таблицу и не ждёт её перерисовку: это ответственность страницы,
 * которая владеет обоими компонентами. Так pagination переиспользуется с любым body.
 */
public final class PaginationComponent extends UiComponent {

    private static final Pattern SUMMARY = Pattern.compile("(\\d+)\\s*[\\u2013\\u2014-]\\s*(\\d+)\\D+(\\d+)");

    public PaginationComponent(SelenideElement root) {
        super(root);
    }

    public PageSummary summary() {
        SelenideElement element = inner("page-summary").as("счётчик записей")
                .shouldBe(visible)
                .shouldHave(matchText(SUMMARY.pattern()));
        String text = element.getText();
        Matcher matcher = SUMMARY.matcher(text);
        if (!matcher.find()) {
            throw new IllegalStateException("Счётчик записей не соответствует формату 'N–M из K': '" + text + "'");
        }
        return new PageSummary(
                Integer.parseInt(matcher.group(1)),
                Integer.parseInt(matcher.group(2)),
                Integer.parseInt(matcher.group(3)));
    }

    public int currentPage() {
        String page = root.$("[data-testid='page-number'][aria-current='page']")
                .as("текущая страница")
                .shouldBe(visible)
                .getAttribute("data-page");
        return Integer.parseInt(page);
    }

    public void goToPage(int page) {
        root.$(ByTestId.css("page-number") + "[data-page='" + page + "']")
                .as("страница №" + page)
                .shouldBe(visible, enabled)
                .click();
    }

    public void next() {
        inner("page-next").as("кнопка «следующая страница»").shouldBe(enabled).click();
    }

    public void previous() {
        inner("page-prev").as("кнопка «предыдущая страница»").shouldBe(enabled).click();
    }

    public void setPageSize(PageSize size) {
        inner("page-size").as("выбор размера страницы")
                .shouldBe(visible)
                .selectOptionByValue(String.valueOf(size.value()));
    }

    public PageSize selectedPageSize() {
        String value = inner("page-size").shouldBe(visible).getSelectedOptionValue();
        for (PageSize size : PageSize.values()) {
            if (String.valueOf(size.value()).equals(value)) {
                return size;
            }
        }
        throw new IllegalStateException("UI отдал неизвестный размер страницы: '" + value + "'");
    }

    public boolean hasNextPage() {
        return inner("page-next").is(enabled);
    }
}
