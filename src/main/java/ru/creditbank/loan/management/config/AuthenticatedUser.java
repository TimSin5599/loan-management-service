package ru.creditbank.loan.management.config;

import java.util.UUID;

public record AuthenticatedUser(UUID userId, String email, String role) {
}
