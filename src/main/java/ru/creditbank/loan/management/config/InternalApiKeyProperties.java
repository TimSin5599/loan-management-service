package ru.creditbank.loan.management.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "internal-api")
public record InternalApiKeyProperties(String key) {
}
