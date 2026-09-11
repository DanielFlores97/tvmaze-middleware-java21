package com.example.pruebatecnica.infrastructure.cache;

import com.example.pruebatecnica.config.CacheProperties;
import com.example.pruebatecnica.domain.AppException;
import com.example.pruebatecnica.domain.Post;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MongoPostCacheTest {
    private final MongoTemplate mongo = mock(MongoTemplate.class);
    private final Instant now = Instant.parse("2026-09-11T12:00:00Z");
    private final MongoPostCache cache = new MongoPostCache(mongo, new CacheProperties(Duration.ofMinutes(5)),
            Clock.fixed(now, ZoneOffset.UTC));
    private final Post post = new Post(1, 2, "Title", "Body");

    @Test
    void returnsOnlyUnexpiredDocuments() {
        when(mongo.findById(1L, PostCacheDocument.class))
                .thenReturn(new PostCacheDocument(1L, post, now.plusSeconds(1)));
        assertThat(cache.findById(1)).contains(post);
    }

    @Test
    void rejectsExpiredDocumentsEvenBeforeTtlCleanup() {
        when(mongo.findById(1L, PostCacheDocument.class))
                .thenReturn(new PostCacheDocument(1L, post, now.minusSeconds(1)));
        assertThat(cache.findById(1)).isEmpty();
    }

    @Test
    void expiryBoundaryIsAlreadyExpired() {
        when(mongo.findById(1L, PostCacheDocument.class))
                .thenReturn(new PostCacheDocument(1L, post, now));
        assertThat(cache.findById(1)).isEmpty();
    }

    @Test
    void missingDocumentIsCacheMiss() {
        assertThat(cache.findById(1)).isEmpty();
    }

    @Test
    void readFailureAllowsProviderFallback() {
        when(mongo.findById(1L, PostCacheDocument.class))
                .thenThrow(new DataAccessResourceFailureException("offline"));
        assertThat(cache.findById(1)).isEmpty();
    }

    @Test
    void writesExpiryUsingConfiguredTtl() {
        cache.put(post);
        var document = ArgumentCaptor.forClass(PostCacheDocument.class);
        verify(mongo).save(document.capture());
        assertThat(document.getValue().id()).isEqualTo(1L);
        assertThat(document.getValue().expiresAt()).isEqualTo(now.plusSeconds(300));
        assertThat(document.getValue().post()).isEqualTo(post);
    }

    @Test
    void writeFailureDoesNotLoseSuccessfulProviderResponse() {
        when(mongo.save(any(PostCacheDocument.class)))
                .thenThrow(new DataAccessResourceFailureException("offline"));
        assertThatCode(() -> cache.put(post)).doesNotThrowAnyException();
    }

    @Test
    void evictionUsesRequestedId() {
        cache.evict(7);
        var query = ArgumentCaptor.forClass(Query.class);
        verify(mongo).remove(query.capture(), eq(PostCacheDocument.class));
        assertThat(query.getValue().getQueryObject().get("_id")).isEqualTo(7L);
    }

    @Test
    void evictionFailureIsReported() {
        when(mongo.remove(any(Query.class), eq(PostCacheDocument.class)))
                .thenThrow(new DataAccessResourceFailureException("offline"));
        assertThatThrownBy(() -> cache.evict(1)).isInstanceOfSatisfying(AppException.class,
                exception -> assertThat(exception.code()).isEqualTo(AppException.Code.CACHE_UNAVAILABLE));
    }
}
