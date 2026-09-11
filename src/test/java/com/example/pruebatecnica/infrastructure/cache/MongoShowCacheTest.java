package com.example.pruebatecnica.infrastructure.cache;

import com.example.pruebatecnica.domain.AppException;
import com.example.pruebatecnica.domain.Show;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.mongodb.core.MongoTemplate;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class MongoShowCacheTest {
    private final MongoTemplate mongo = mock(MongoTemplate.class);
    private final ObjectMapper mapper = new ObjectMapper();
    private final Instant now = Instant.parse("2026-09-11T12:00:00Z");
    private final MongoShowCache cache = new MongoShowCache(mongo, mapper, Clock.fixed(now, ZoneOffset.UTC));

    @Test
    void cacheRoundTripPreservesWholeJson() throws Exception {
        var source = mapper.readValue("""
                {"id":1,"name":"Show","summary":null,"extra":{"items":[null,true,2.5]},
                "_links":{"self":{"href":"https://api.tvmaze.com/shows/1"}}}
                """, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
        cache.save(new Show(1, source));
        var captor = ArgumentCaptor.forClass(ShowCacheDocument.class);
        verify(mongo).save(captor.capture());
        var saved = captor.getValue();
        assertThat(saved.cachedAt()).isEqualTo(now);
        when(mongo.findById(1L, ShowCacheDocument.class)).thenReturn(saved);
        assertThat(cache.findById(1).orElseThrow().attributes()).isEqualTo(source);
    }

    @Test
    void missingIdReturnsEmpty() {
        assertThat(cache.findById(99)).isEmpty();
    }

    @Test
    void oldDocumentStillCountsAsHit() {
        when(mongo.findById(1L, ShowCacheDocument.class))
                .thenReturn(new ShowCacheDocument(1L, "{\"id\":1}", Instant.EPOCH));
        assertThat(cache.findById(1)).isPresent();
    }

    @Test
    void databaseReadAndWriteFailuresAreReported() {
        when(mongo.findById(1L, ShowCacheDocument.class)).thenThrow(new DataAccessResourceFailureException("offline"));
        assertThatThrownBy(() -> cache.findById(1)).isInstanceOf(AppException.class);
        when(mongo.save(any(ShowCacheDocument.class))).thenThrow(new DataAccessResourceFailureException("offline"));
        assertThatThrownBy(() -> cache.save(new Show(1, Map.of("id", 1)))).isInstanceOf(AppException.class);
    }

    @Test
    void corruptedJsonIsNotReturned() {
        when(mongo.findById(1L, ShowCacheDocument.class))
                .thenReturn(new ShowCacheDocument(1L, "{", now));
        assertThatThrownBy(() -> cache.findById(1)).isInstanceOf(AppException.class);
    }
}
