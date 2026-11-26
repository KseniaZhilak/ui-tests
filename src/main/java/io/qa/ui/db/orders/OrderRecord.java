package io.qa.ui.db.orders;

import io.qa.ui.page.orders.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record OrderRecord(String number,
                          String customer,
                          OrderStatus status,
                          BigDecimal total,
                          LocalDate createdAt) {
}
