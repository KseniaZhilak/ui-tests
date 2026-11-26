package io.qa.ui.core.ui;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Форматы отображения в UI тестируемого приложения (см. «Допущения»). */
public final class UiFormats {

    public static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.forLanguageTag("ru"));

    private UiFormats() {
    }

    public static LocalDate parseDate(String text) {
        return LocalDate.parse(text.trim(), DATE);
    }

    /** "1 234,50 ₽" -> 1234.50 */
    public static BigDecimal parseMoney(String text) {
        String normalized = text.replaceAll("[^0-9,.\\-]", "").replace(',', '.');
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Не удалось разобрать денежное значение из текста: '" + text + "'");
        }
        return new BigDecimal(normalized);
    }
}
