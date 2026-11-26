package io.qa.ui.page.users;

import java.time.LocalDate;

public record UserRow(String email,
                      String fullName,
                      UserRole role,
                      boolean active,
                      LocalDate lastLogin) {
}
