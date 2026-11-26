package io.qa.ui.testdata;

import io.qa.ui.page.orders.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.ThreadLocalRandom;

public record OrderSeed(String number,
                        String customer,
                        OrderStatus status,
                        BigDecimal total,
                        LocalDate createdAt) {

    public static Builder builder() {
        return new Builder();
    }

    /** Осмысленные дефолты: тест задаёт только то, что реально влияет на проверку. */
    public static final class Builder {

        private String number = "ORD-" + ThreadLocalRandom.current().nextInt(100_000, 999_999);
        private String customer = "CUSTOMER-" + ThreadLocalRandom.current().nextInt(1_000, 9_999);
        private OrderStatus status = OrderStatus.NEW;
        private BigDecimal total = new BigDecimal("1000.00");
        private LocalDate createdAt = LocalDate.now();

        public Builder number(String value) {
            this.number = value;
            return this;
        }

        public Builder customer(String value) {
            this.customer = value;
            return this;
        }

        public Builder status(OrderStatus value) {
            this.status = value;
            return this;
        }

        public Builder total(String value) {
            this.total = new BigDecimal(value);
            return this;
        }

        public Builder createdAt(LocalDate value) {
            this.createdAt = value;
            return this;
        }

        public OrderSeed build() {
            return new OrderSeed(number, customer, status, total, createdAt);
        }
    }
}
