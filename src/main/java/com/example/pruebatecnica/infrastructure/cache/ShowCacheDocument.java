package com.example.pruebatecnica.infrastructure.cache;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("show_cache")
public record ShowCacheDocument(@Id Long id, String payload, Instant cachedAt) {
}
