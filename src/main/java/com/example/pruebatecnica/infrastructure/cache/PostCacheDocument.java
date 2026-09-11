package com.example.pruebatecnica.infrastructure.cache;

import com.example.pruebatecnica.domain.Post;
import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("post_cache")
public record PostCacheDocument(
        @Id Long id,
        Post post,
        @Indexed(name = "post_cache_expiry", expireAfter = "0s") Instant expiresAt) {
}
