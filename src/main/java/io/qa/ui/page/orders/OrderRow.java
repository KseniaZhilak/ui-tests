package io.qa.ui.page.orders;

import java.math.BigDecimal;
import java.time.LocalDate;

public record OrderRow(String number,
                       String customer,
                       OrderStatus status,
                       BigDecimal total,
                       LocalDate createdAt) {
}
