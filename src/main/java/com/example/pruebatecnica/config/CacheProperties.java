package com.example.pruebatecnica.config;

import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("app.cache")
public record CacheProperties(@NotNull @DurationMin(seconds = 1) Duration ttl) {
}
