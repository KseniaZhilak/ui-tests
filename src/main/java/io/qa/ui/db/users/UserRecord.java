package io.qa.ui.db.users;

import io.qa.ui.page.users.UserRole;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record UserRecord(long id,
                         String email,
                         String fullName,
                         UserRole role,
                         boolean active,
                         LocalDate lastLogin,
                         LocalDateTime createdAt) {
}
