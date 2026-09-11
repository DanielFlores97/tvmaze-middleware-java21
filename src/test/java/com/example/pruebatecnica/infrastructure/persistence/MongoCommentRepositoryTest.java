package com.example.pruebatecnica.infrastructure.persistence;

import com.example.pruebatecnica.domain.AppException;
import com.example.pruebatecnica.domain.Comment;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.mongodb.core.MongoTemplate;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class MongoCommentRepositoryTest {
    private final MongoTemplate mongo = mock(MongoTemplate.class);
    private final Instant now = Instant.parse("2026-09-11T12:00:00Z");
    private final MongoCommentRepository repository = new MongoCommentRepository(mongo, Clock.fixed(now, ZoneOffset.UTC));

    @Test
    void insertsIndependentCommentLinkedToShow() {
        repository.save(42, new Comment("Good", new BigDecimal("4.5")));
        var captor = ArgumentCaptor.forClass(CommentDocument.class);
        verify(mongo).insert(captor.capture());
        assertThat(captor.getValue().showId()).isEqualTo(42);
        assertThat(captor.getValue().rating()).isEqualByComparingTo("4.5");
        assertThat(captor.getValue().createdAt()).isEqualTo(now);
        assertThat(captor.getValue().id()).isNull();
    }

    @Test
    void insertFailureIsPropagated() {
        when(mongo.insert(any(CommentDocument.class))).thenThrow(new DataAccessResourceFailureException("offline"));
        assertThatThrownBy(() -> repository.save(1, new Comment("Good", BigDecimal.ONE)))
                .isInstanceOf(AppException.class);
    }

    @Test
    void batchesAndGroupsOnlyRequestedShows() {
        when(mongo.find(any(org.springframework.data.mongodb.core.query.Query.class), eq(CommentDocument.class)))
                .thenReturn(java.util.List.of(
                        new CommentDocument("a", 1, "First", BigDecimal.ONE, now),
                        new CommentDocument("b", 1, "Second", BigDecimal.TEN.divide(BigDecimal.valueOf(2)), now),
                        new CommentDocument("c", 2, "Third", BigDecimal.ZERO, now)));
        var result = repository.findByShowIds(java.util.List.of(1L, 2L));
        assertThat(result.get(1L)).extracting(Comment::comment).containsExactly("First", "Second");
        assertThat(result.get(2L)).hasSize(1);
        var query = ArgumentCaptor.forClass(org.springframework.data.mongodb.core.query.Query.class);
        verify(mongo, times(1)).find(query.capture(), eq(CommentDocument.class));
        assertThat(query.getValue().getQueryObject().toJson()).contains("$in", "showId");
        assertThat(query.getValue().getSortObject()).containsKeys("showId", "createdAt", "_id");
    }

    @Test
    void emptyIdBatchDoesNotReadDatabase() {
        assertThat(repository.findByShowIds(java.util.List.of())).isEmpty();
        verifyNoInteractions(mongo);
    }

    @Test
    void readFailureIsPropagated() {
        when(mongo.find(any(org.springframework.data.mongodb.core.query.Query.class), eq(CommentDocument.class)))
                .thenThrow(new DataAccessResourceFailureException("offline"));
        assertThatThrownBy(() -> repository.findByShowIds(java.util.List.of(1L)))
                .isInstanceOf(AppException.class);
    }
}
