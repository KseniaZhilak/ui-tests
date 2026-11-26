package io.qa.ui.testdata;

import io.qa.ui.page.users.UserRole;

import java.time.LocalDate;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

public record UserSeed(String email,
                       String fullName,
                       UserRole role,
                       boolean active,
                       LocalDate lastLogin) {

    public static Builder builder(String runTag) {
        return new Builder(runTag);
    }

    public static final class Builder {

        private String email;
        private String fullName = "Тестовый Пользователь";
        private UserRole role = UserRole.VIEWER;
        private boolean active = true;
        private LocalDate lastLogin = null;

        private Builder(String runTag) {
            this.email = runTag.toLowerCase(Locale.ROOT) + "-" + ThreadLocalRandom.current().nextInt(100_000, 999_999) + "@example.test";
        }

        public Builder email(String value) {
            this.email = value;
            return this;
        }

        public Builder fullName(String value) {
            this.fullName = value;
            return this;
        }

        public Builder role(UserRole value) {
            this.role = value;
            return this;
        }

        public Builder active(boolean value) {
            this.active = value;
            return this;
        }

        public Builder lastLogin(LocalDate value) {
            this.lastLogin = value;
            return this;
        }

        public UserSeed build() {
            return new UserSeed(email, fullName, role, active, lastLogin);
        }
    }
}
